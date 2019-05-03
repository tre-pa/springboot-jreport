package br.jus.tre_pa.jreport.rest;

import java.util.Map;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.jus.tre_pa.datafilter.Filterable;
import br.jus.tre_pa.datafilter.rest.AbstractCrudRest;
import br.jus.tre_pa.jreport.domain.JReport;
import br.jus.tre_pa.jreport.repository.JReportRepository;
import br.jus.tre_pa.jreport.repository.specification.JReportSpecification;
import br.jus.tre_pa.jreport.service.JReportService;

/**
 * Classe Rest com a API de gerenciamento de reports.
 * 
 * @author jcruz
 *
 */
@RestController
@RequestMapping("/api/mngt/jreport")
public class JReportMngtRest extends AbstractCrudRest<JReport, Long, JReportSpecification, JReportRepository> {

	@Autowired
	JReportService jreportService;

	/**
	 * Retorna a lista paginada de categories
	 * 
	 * @param pageable
	 * @return
	 */
	@GetMapping("/categories")
	ResponseEntity<Page<String>> findAllCategories(Pageable pageable) {
		return ResponseEntity.ok().body(this.getRepository().findAllCategories(pageable));
	}

	/**
	 * Cria um(a) novo(a) JReport
	 *
	 * @param jReport
	 * @return Entidade gerenciada.
	 */
	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public JReport insert(@RequestBody @Valid JReport jReport) {
		return jreportService.insert(jReport);
	}

	/**
	 * Atualiza um(a) JReport
	 *
	 * @param jReport
	 * @return Entidade gerenciada.
	 */
	@Override
	@PutMapping(path = "/{id}")
	public JReport update(@PathVariable Long id, @RequestBody @Valid JReport jReport) {
		if (!this.getRepository().existsById(id)) throw new EntityNotFoundException("Relatório não encontrado: Id=${id}");
		return jreportService.update(jReport);
	}

	/**
	 * Gera uma prévia da execução do código SQL.
	 * 
	 * @param jreport
	 * @return
	 */
	@PostMapping("/preview/datasource")
	ResponseEntity<?> previewSQL(@RequestBody Map<String, Object> params, Pageable pageable) {
		if (StringUtils.isEmpty((CharSequence) params.get("sql"))) throw new IllegalArgumentException("SQL não definida na requisição.");
		return ResponseEntity.ok(jreportService.executeSQL((String) params.get("sql"), pageable.getSort(), (Filterable) params.get("filter")));
	}

//	/**
//	 * Gera uma prévia do PDF
//	 * 
//	 * @param payload
//	 * @return
//	 */
//	@SuppressWarnings("unchecked")
//	@PostMapping(path = "/preview/pdf", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
//	ResponseEntity<?> previewPDF(@RequestBody Map<String, Object> params, Sort sort) {
//		if (StringUtils.isEmpty((CharSequence) params.get("sql"))) throw new IllegalArgumentException("SQL não definida na requisição.");
//		if (StringUtils.isEmpty((CharSequence) params.get("gpdf"))) throw new IllegalArgumentException("Código Groovy (gpdf) não definido na requisição.");
//		if (((List<JReportColumn>) params.get("columns")).isEmpty()) throw new IllegalArgumentException("Colunas não definidas na requisição.");
//
//		String sql = (String) params.get("sql");
//		String gpdf = (String) params.get("gpdf");
//		
//
//		HttpHeaders headers = new HttpHeaders();
//		headers.add("Content-Disposition", "inline; filename=preview.pdf");
//		// @formatter:off
//		ByteArrayInputStream bais = jreportService.genGPDF(
//				new JReport(
//						params.get("sql"), 
//						params.get("gpdf"), 
//						new JReportGrid(params.columns)), 
//				sort, 
//				(Filterable) params.get("filter"));
//		ResponseEntity
//				.ok()
//				.headers(headers)
//				.contentType(MediaType.APPLICATION_PDF)
//				.body(new InputStreamResource(bais));
//		// @formatter:on
//	}

	/**
	 * 
	 * @param jreport
	 * @return
	 */
	@PostMapping("/grid/template")
	ResponseEntity<?> genDefaultGridTemplate(@RequestBody JReport jreport) {
		if (StringUtils.isEmpty(jreport.getSql())) throw new IllegalArgumentException("SQL não definida na requisição.");
		return ResponseEntity.ok(jreportService.genDefaultGridTemplate(jreport.getSql()));
	}

	/**
	 * 
	 * @return
	 */
	@GetMapping("/gpdf/template")
	ResponseEntity<?> genDefaultGPDFTemplate() {
		return ResponseEntity.ok(jreportService.genDefaultGPDFTemplate());
	}
}
