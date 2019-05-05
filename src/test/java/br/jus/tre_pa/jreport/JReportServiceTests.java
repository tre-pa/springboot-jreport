package br.jus.tre_pa.jreport;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.io.ByteStreams;

import br.jus.tre_pa.datafilter.config.DataFilterConfiguration;
import br.jus.tre_pa.jreport.domain.Foo;
import br.jus.tre_pa.jreport.domain.JReport;
import br.jus.tre_pa.jreport.repository.FooRepository;
import br.jus.tre_pa.jreport.repository.JReportRepository;
import br.jus.tre_pa.jreport.service.JReportService;

@RunWith(SpringRunner.class)
@DataJpaTest
@SpringBootTest(classes = { JReportTestApp.class, JReportTemplate.class, JReportService.class, DataFilterConfiguration.class }, properties = "logging.level.br=debug")
public class JReportServiceTests {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private FooRepository fooRepository;

	@Autowired
	private JReportService reportService;

	@Autowired
	private JReportRepository reportRepository;

	@Autowired
	private JReportTemplate reportTemplate;


	@Test
	@Transactional
	public void genPdfTest() throws IOException {
		Foo foo = new Foo();
		foo.setId(1L);
		foo.setName("Fulano1");
		entityManager.persist(foo);
		
		System.out.println("1111111111111111");

		String SQL = "SELECT * FROM FOO";
		JReport report = new JReport(SQL, reportTemplate.genDefaultGPDFTemplate(), reportTemplate.genDefaultGridTemplate(SQL));
		report.setCreatedAt(LocalDateTime.now());
		report.setTitle("Example");
		report.setCategory("A1");
		report.setGexcel("excel");
		entityManager.persist(report);
		entityManager.flush();
		
		System.out.println("22222222222222222");
		
		ByteArrayInputStream bais = reportService.genGPDF(report, null, null);
		byte[] buffer = ByteStreams.toByteArray(bais);
		
		System.out.println("33333333333333333");
		
		FileOutputStream fos = new FileOutputStream(new File("target/report.pdf"));
		fos.write(buffer);
		fos.flush();
		fos.close();

		fooRepository.deleteAll();
	}

}
