package br.jus.tre_pa.jreport.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.fdvs.dj.core.DynamicJasperHelper;
import ar.com.fdvs.dj.core.layout.ClassicLayoutManager;
import ar.com.fdvs.dj.domain.DynamicReport;
import br.jus.tre_pa.datafilter.Aggregation;
import br.jus.tre_pa.datafilter.Filterable;
import br.jus.tre_pa.datafilter.Page;
import br.jus.tre_pa.datafilter.Payload;
import br.jus.tre_pa.datafilter.sql.SqlContext;
import br.jus.tre_pa.jreport.JReportStyles;
import br.jus.tre_pa.jreport.JReportTemplate;
import br.jus.tre_pa.jreport.domain.JReport;
import br.jus.tre_pa.jreport.repository.JReportRepository;
import groovy.json.JsonSlurper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.ListOfArrayDataSource;

@Slf4j
@Service
public class JReportService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JReportRepository jreportRepository;

	@Autowired
	private SqlContext sqlContext;
	
	@Autowired
	private JReportTemplate template;

	/**
	 * Salva um novo report.
	 * 
	 * @param jreport Instância do jreport.
	 * @return
	 */
	public JReport insert(JReport jreport) {
		jreport.setCreatedAt(LocalDateTime.now());
		jreport.setUuid(UUID.randomUUID());
		if (jreport.getGrid() == null) jreport.setGrid(template.genDefaultGridTemplate(jreport.getSql()));
		if (StringUtils.isEmpty(jreport.getGpdf())) jreport.setGpdf(template.genDefaultGPDFTemplate());
		if (StringUtils.isEmpty(jreport.getGexcel())) jreport.setGexcel("excel");
		return this.jreportRepository.save(jreport);
	}

	/**
	 * Atualiza um report.
	 * 
	 * @param jreportInstância do jreport.
	 * @return
	 */
	public JReport update(JReport jreport) {
		jreport.setLastUpdateAt(LocalDateTime.now());
		return this.jreportRepository.save(jreport);
	}

	/**
	 * Executa a consulta SQL com paginação.
	 * 
	 * @param id
	 * @return
	 */
	public Page<List<Map<String, Object>>> executeSQLPageable(String sql, Pageable pageable, Payload payload) {
		// @formatter:off
		org.springframework.data.domain.Page<List<Map<String, Object>>> page = sqlContext.selectFrom(sql)
				.where(payload.getFilterable())
				.orderBy(pageable.getSort())
				.limit(pageable)
				.fetchMaps();
		// @formatter:on
		List<Aggregation> aggregations = this.executeSQLAgg(sql, payload);
		sqlContext.clear();
		return new Page<List<Map<String, Object>>>(page, aggregations);
	}

	/**
	 * Executa a consulta SQL de aggregação.
	 *
	 * @param id
	 * @return
	 */
	public List<Aggregation> executeSQLAgg(String sql, Payload payload) {
		return sqlContext.aggregation(sql, payload);
	}

	/**
	 * Executa a cláusula SQL sem paginação. O método é chamado para a função que gera PDF do relatório.
	 * 
	 * @param sql    Código SQL válido para execução.
	 * @param sort   Informações de Sort.
	 * @param filter Informação de filtragem.
	 * @return
	 */
	public List<Map<String, Object>> executeSQL(String sql, Sort sort, Filterable filter) {
		List<Map<String, Object>> list = new ArrayList<>();
		if (Objects.nonNull(filter)) {
			// @formatter:off
			list = sqlContext.selectFrom(sql)
					.where(filter)
					.orderBy(sort)
					.fetchMaps();
			// @formatter:on
			sqlContext.clear();
			return list;
		}
		// @formatter:off
		list = sqlContext.selectFrom(sql)
				.orderBy(sort)
				.fetchMaps();
		// @formatter:on
		sqlContext.clear();
		return list;
	}

	/**
	 * Executa o código groovy para a geração do relatório em PDF.
	 * 
	 * @param id Id do Relatório.
	 * 
	 */
	@SneakyThrows
	public ByteArrayInputStream genGPDF(JReport jreport, Sort sort, Filterable filter) {
		log.info("Iniciando geração do relatório PDF '{}'", jreport.getTitle());
		Binding binding = new Binding();
		binding.setProperty("report", new JsonSlurper().parseText(new ObjectMapper().writeValueAsString(jreport)));
		binding.setProperty("jdbcTemplate", jdbcTemplate);
		binding.setProperty("JReportStyles", JReportStyles.class);
		binding.setProperty("CREATED_AT", String.format("<i>Gerado em %s</i>", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date())));
		GroovyShell groovyShell = new GroovyShell(binding);
		List<Map<String, Object>> sqlResult = executeSQL(jreport.getSql(), sort, filter);
		log.debug("sqlResult com {} registros.", sqlResult.size());
		// @formatter:off
		List<Object[]> records = sqlResult
				.stream()
				.map ( row -> row.values().toArray())
				.collect(Collectors.toList());
		String[] columnNames = jreport.getGrid().getColumns()
				.stream()
				.map ( it -> it.getDataField() )
				.toArray(String[]::new);
		// @formatter:on

		DynamicReport dynamicReport = (DynamicReport) groovyShell.evaluate(jreport.getGpdf());
		JRDataSource ds = new ListOfArrayDataSource(records, columnNames);
		JasperPrint jp = DynamicJasperHelper.generateJasperPrint(dynamicReport, new ClassicLayoutManager(), ds);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JasperExportManager.exportReportToPdfStream(jp, baos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		return bais;
	}

	
}