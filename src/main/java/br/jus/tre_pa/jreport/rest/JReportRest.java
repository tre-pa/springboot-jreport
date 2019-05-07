package br.jus.tre_pa.jreport.rest;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.jus.tre_pa.jfilter.Filterable;
import br.jus.tre_pa.jfilter.Page;
import br.jus.tre_pa.jfilter.Payload;
import br.jus.tre_pa.jfilter.rest.AbstractCrudRest;
import br.jus.tre_pa.jreport.domain.JReport;
import br.jus.tre_pa.jreport.repository.JReportRepository;
import br.jus.tre_pa.jreport.repository.specification.JReportSpecification;
import br.jus.tre_pa.jreport.service.JReportService;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe Rest com endpoints para relatórios.
 * 
 * @author jcruz
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/jreport")
public class JReportRest extends AbstractCrudRest<JReport, Long, JReportSpecification, JReportRepository> {

	@Autowired
	private JReportService jreportService;

	/**
	 * Retorna a fonte de dados(datasource) do relatório.
	 * 
	 * @param id Id do relatório.
	 * @return
	 */
	@PostMapping("/{id}/datasource")
	public ResponseEntity<Page<List<Map<String, Object>>>> executeSQL(@PathVariable Long id, Pageable pageable, @RequestBody(required = false) Payload payload) {
		JReport jreport = this.getRepository().findById(id).orElseThrow(() -> new EntityNotFoundException("Relatório não encontrado: Id=" + id));
		return ResponseEntity.ok(jreportService.executeSQLPageable(jreport.getSql(), pageable, payload));
	}

	/**
	 * Retorna o binário (pdf) do relatório. o endpoint aceita
	 * 
	 * @param id Identificador do relatório.
	 * @return InputStreamResource do pdf.
	 */
	@PostMapping(path = "/{id}/pdf", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<?> genGPDF(@PathVariable Long id, Sort sort, @RequestBody(required = false) Filterable filter) {
		log.info("Iniciando geração do PDF para o relatório id={}", id);
		JReport jreport = this.getRepository().findById(id).orElseThrow(() -> new EntityNotFoundException("Relatório não encontrado: Id=" + id));

		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition", String.format("inline; filename=%s.pdf", jreport.getTitle()));
		ByteArrayInputStream bais = jreportService.genGpdf(jreport, sort, filter);
		// @formatter:off
		return ResponseEntity
				.ok()
				.headers(headers)
				.contentType(MediaType.APPLICATION_PDF)
				.body(new InputStreamResource(bais));
		// @formatter:on
	}
}
