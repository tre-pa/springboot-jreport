package br.jus.tre_pa.jreport.repository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.io.ByteStreams;

import br.jus.tre_pa.jfilter.JFilterModuleConfiguration;
import br.jus.tre_pa.jreport.JReportModuleConfiguration;
import br.jus.tre_pa.jreport.domain.Foo;
import br.jus.tre_pa.jreport.domain.JReport;
import br.jus.tre_pa.jreport.helper.JReportTemplate;
import br.jus.tre_pa.jreport.service.JReportService;

@RunWith(SpringRunner.class)
@DataJpaTest
@SpringBootTest(classes = { JReportModuleConfiguration.class, JFilterModuleConfiguration.class })
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
		for (int i = 1; i < 100; i++) {
			Foo foo = new Foo();
			foo.setId(Long.valueOf(i));
			foo.setName("Fulano" + i);
			entityManager.persist(foo);
		}

		System.out.println("1111111111111111");

		String SQL = "SELECT * FROM FOO";
		JReport report = new JReport(SQL, reportTemplate.genDefaultGpdfTemplate(), reportTemplate.genDefaultGridTemplate(SQL));
		report.setCreatedAt(LocalDateTime.now());
		report.setTitle("Relatório de Relatório");
		report.setSubtitle("Seção de Desenvolvimento de Sistemas");
		report.setCategory("A1");
		report.setGexcel("excel");
		entityManager.persist(report);
		entityManager.flush();

		System.out.println("22222222222222222");

		ByteArrayInputStream bais = reportService.genGpdf(report, null, null);
		byte[] buffer = ByteStreams.toByteArray(bais);

		System.out.println("33333333333333333");

		FileOutputStream fos = new FileOutputStream(new File("target/report.pdf"));
		fos.write(buffer);
		fos.flush();
		fos.close();

		fooRepository.deleteAll();

		Assert.assertTrue(true);
	}

	@Test
	@Transactional
	public void genExcelTest() throws IOException {
		for (int i = 1; i < 100; i++) {
			Foo foo = new Foo();
			foo.setId(Long.valueOf(i));
			foo.setName("Fulano" + i);
			entityManager.persist(foo);
		}

		System.out.println("1111111111111111");

		String SQL = "SELECT * FROM FOO";
		JReport report = new JReport(SQL, reportTemplate.genDefaultGpdfTemplate(), reportTemplate.genDefaultGridTemplate(SQL));
		report.setCreatedAt(LocalDateTime.now());
		report.setTitle("Relatório de Relatório");
		report.setSubtitle("Seção de Desenvolvimento de Sistemas");
		report.setCategory("A1");
		report.setGexcel(reportTemplate.genDefaultGexcelTemplate());
		entityManager.persist(report);
		entityManager.flush();

		System.out.println("22222222222222222");

		ByteArrayInputStream bais = reportService.genGexcel(report, null, null);
		byte[] buffer = ByteStreams.toByteArray(bais);

		System.out.println("33333333333333333");

		FileOutputStream fos = new FileOutputStream(new File("target/report.xls"));
		fos.write(buffer);
		fos.flush();
		fos.close();

		fooRepository.deleteAll();
	}

}
