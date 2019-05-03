package br.jus.tre_pa.jreport.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import ar.com.fdvs.dj.core.DynamicJasperHelper;
import ar.com.fdvs.dj.core.layout.ClassicLayoutManager;
import ar.com.fdvs.dj.domain.DynamicReport;
import br.jus.tre_pa.datafilter.Aggregation;
import br.jus.tre_pa.datafilter.Filterable;
import br.jus.tre_pa.datafilter.Page;
import br.jus.tre_pa.datafilter.Payload;
import br.jus.tre_pa.datafilter.sql.SqlContext;
import br.jus.tre_pa.jreport.JReportStyles;
import br.jus.tre_pa.jreport.domain.JReport;
import br.jus.tre_pa.jreport.repository.JReportRepository;
import br.jus.tre_pa.jreport.types.JReportColumn;
import br.jus.tre_pa.jreport.types.JReportGrid;
import groovy.json.JsonSlurper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.SneakyThrows;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.ListOfArrayDataSource;

@Service
public class JReportService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JReportRepository jreportRepository;

	@Autowired
	private SqlContext sqlContext;

	/**
	 * Salva um novo report.
	 * 
	 * @param jreport
	 * @return
	 */
	public JReport insert(JReport jreport) {
		jreport.setCreatedAt(LocalDateTime.now());
		jreport.setUuid(UUID.randomUUID());
		if (jreport.getGrid() == null) jreport.setGrid(this.genDefaultGridTemplate(jreport.getSql()));
		if (StringUtils.isEmpty(jreport.getGpdf())) jreport.setGpdf(this.genDefaultGPDFTemplate());
		if (StringUtils.isEmpty(jreport.getGexcel())) jreport.setGexcel("excel");
		return this.jreportRepository.save(jreport);
	}

	/**
	 * Atualiza um report.
	 * 
	 * @param jreport
	 * @return
	 */
	public JReport update(JReport jreport) {
		jreport.setLastUpdateAt(LocalDateTime.now());
		return this.jreportRepository.save(jreport);
	}

	/**
	 * Executa a consulta SQL.
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
	public ByteArrayInputStream genGPDF(JReport jreport,Sort sort, Filterable filter) {
		Binding binding = new Binding();
		binding.setProperty("report", new JsonSlurper().parseText(new ObjectMapper().writeValueAsString(jreport)));
		binding.setProperty("jdbcTemplate", jdbcTemplate);
		binding.setProperty("JReportStyles", JReportStyles.class);
		GroovyShell groovyShell = new GroovyShell(binding);
		List<Map<String, Object>> sqlResult = executeSQL(jreport.getSql(), sort, filter);
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

	/**
	 * Gera o template da coluna baseado em uma consuta SQL.
	 * 
	 * @param sql
	 * @return
	 */
	public List<JReportColumn> genDefaultColumnsTemplate(String sql) {
		SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(sql);
		List<JReportColumn> columns = new ArrayList<>();
		String[] columnNames = sqlRowSet.getMetaData().getColumnNames();
		for (int i = 0; i < columnNames.length; i++) {
			JReportColumn column = new JReportColumn();
			column.setDataField(columnNames[i]);
			column.setCaption(columnNames[i]);
			column.setDataType(JReportColumn.toTypescriptType(sqlRowSet.getMetaData().getColumnType(i + 1)));
			column.setJavaType(JReportColumn.toJavaType(sqlRowSet.getMetaData().getColumnType(i + 1)).getName());
			columns.add(column);
		}
		return columns;
	}

	public JReportGrid genDefaultGridTemplate(String sql) {
		JReportGrid grid = new JReportGrid();
		grid.setColumns(this.genDefaultColumnsTemplate(sql));
		
		
		grid.getProperties().put("wordWrapEnabled", true);
		grid.getProperties().put("showBorders", true);
		grid.getProperties().put("showRowLines", true);
		grid.getProperties().put("showColumnLines", false);
		// @formatter:off
		grid.getProperties().put("sorting", Lists.newArrayList(ImmutableMap.builder()
				.put("mode", "multiple")));
		grid.getProperties().put("paging", Lists.newArrayList(ImmutableMap.builder()
				.put("pageSize", 5)));
		grid.getProperties().put("pager", Lists.newArrayList(ImmutableMap.builder()
				.put("allowedPageSizes", Lists.newArrayList(5,10,20))
				.put("showNavigationButtons", true)
				.put("showPageSizeSelector", true)
				.put("showInfo", true)));
		grid.getProperties().put("filterRow", Lists.newArrayList(ImmutableMap.builder()
				.put("visible", true)));
		grid.getProperties().put("headerFilter", Lists.newArrayList(ImmutableMap.builder()
				.put("visible", true)));
		grid.getProperties().put("searchPanel", Lists.newArrayList(ImmutableMap.builder()
				.put("visible", true)
				.put("width", 300)));
		// @formatter:on
		return grid;
	}


	/**
	 * Gera o template do gpdf baseado nas colunas JReportColumn.
	 * 
	 * @param columns
	 * @return
	 */
	public String genDefaultGPDFTemplate() {
		String gpdfTemplate = "\n"+
        "import org.springframework.core.io.ClassPathResource\n"+
        "import ar.com.fdvs.dj.domain.ImageBanner;\n"+
        "import ar.com.fdvs.dj.domain.builders.ColumnBuilder\n"+
        "import ar.com.fdvs.dj.domain.builders.DynamicReportBuilder\n"+
		"\n"+
        "def drb = new DynamicReportBuilder()\n"+
		"\n"+
        "drb.title = report.title\n"+
        "drb.subtitle = report.subtitle\n"+
        "drb.useFullPageWidth = true\n"+
		"drb.addFirstPageImageBanner(new ClassPathResource(\"BOOT-INF/brasao-republica.png\").getPath(), new Integer(50), new Integer(50), ImageBanner.ALIGN_CENTER);\n"+
		"drb.setDefaultStyles(JReportStyles.titleStyle, JReportStyles.subtitleStyle, JReportStyles.columnHeaderStyle, JReportStyles.columnDetailStyle)\n"+
		"\n"+
        "def columns = [:]\n"+
		"\n"+
        "report.grid.columns.each {\n"+ 
            "columns[it.dataField] = ColumnBuilder.new\n"+
                ".setColumnProperty(it.dataField, it.javaType)\n"+
                ".setTitle(it.caption)\n"+
                ".setWidth(it.width)\n"+
                ".build()\n"+
            "drb.addColumn(columns[it.dataField])\n"+
        "}\n"+
		"\n"+
        "def dr = drb.build()\n";
		return gpdfTemplate;
	}
}