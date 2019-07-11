package br.jus.tre_pa.jreport.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

import br.jus.tre_pa.jfilter.JFilterModuleConfiguration;
import br.jus.tre_pa.jreport.JReportModuleConfiguration;
import br.jus.tre_pa.jreport.domain.JReport;
import br.jus.tre_pa.jreport.repository.FooRepository;
import br.jus.tre_pa.jreport.types.JReportGrid;
import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@DataJpaTest
@SpringBootTest(classes = { JReportModuleConfiguration.class, JFilterModuleConfiguration.class })
@Slf4j
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class JReport_JRxmlTests {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private FooRepository fooRepository;

	@Autowired
	private JReportService reportService;

	@Test
	@Transactional
	public void genPdfTest() throws IOException {
//		for (int i = 1; i < 10; i++) {
//			Foo foo = new Foo();
//			foo.setId(Long.valueOf(i));
//			foo.setName("Fulano" + i);
//			entityManager.persist(foo);
//		}
//
//		entityManager.flush();

		ClassPathResource resource = new ClassPathResource("br/jus/tre_pa/jreport/report1.jrxml");
		String jrxml = CharStreams.toString(new InputStreamReader(resource.getInputStream()));
		JReport report = new JReport(jrxml);
		report.setCreatedAt(LocalDateTime.now());
		report.setTitle("Relatório JRXML");
		report.setSubtitle("Seção de Desenvolvimento de Sistemas");
		report.setCategory("A1");
		report.setGexcel("excel");
		report.setGpdf(" ");
		report.setSql(" ");
		report.setGrid(new JReportGrid());
		entityManager.persist(report);
		entityManager.flush();

		Map<String, Object> params = new HashMap<>();
		params.put("created_by", "Fulano de Tal");

		ByteArrayInputStream bais = reportService.genGpdf(report, params);
		byte[] buffer = ByteStreams.toByteArray(bais);

		FileOutputStream fos = new FileOutputStream(new File("target/report-jrxml.pdf"));
		fos.write(buffer);
		fos.flush();
		fos.close();

		log.info("Relatório {} gerado com sucesso.", report.getTitle());
		fooRepository.deleteAll();
	}

}
