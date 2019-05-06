package br.jus.tre_pa.jreport;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import br.jus.tre_pa.jreport.types.JReportColumn;
import br.jus.tre_pa.jreport.types.JReportGrid;

/**
 * Componente com as definições padrão de templates de relatório.
 * 
 * @author jcruz
 *
 */
@Component
public class JReportTemplate {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Gera o template das colunas do relatório (Lista de JReportColumn) baseado na consuta SQL.
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

	/**
	 * 
	 * @param sql
	 * @return
	 */
	public JReportGrid genDefaultGridTemplate(String sql) {
		JReportGrid grid = new JReportGrid();
		grid.setColumns(this.genDefaultColumnsTemplate(sql));

		grid.getProperties().put("wordWrapEnabled", true);
		grid.getProperties().put("showBorders", true);
		grid.getProperties().put("showRowLines", true);
		grid.getProperties().put("showColumnLines", false);
		// @formatter:off
		grid.getProperties().put("sorting", Lists.newArrayList(ImmutableMap.builder()
				.put("mode", "multiple")
				.build()));
		grid.getProperties().put("paging", Lists.newArrayList(ImmutableMap.builder()
				.put("pageSize", 5)
				.build()));
		grid.getProperties().put("pager", Lists.newArrayList(ImmutableMap.builder()
				.put("allowedPageSizes", Lists.newArrayList(5,10,20))
				.put("showNavigationButtons", true)
				.put("showPageSizeSelector", true)
				.put("showInfo", true)
				.build()));
		grid.getProperties().put("filterRow", Lists.newArrayList(ImmutableMap.builder()
				.put("visible", true)
				.build()));
		grid.getProperties().put("headerFilter", Lists.newArrayList(ImmutableMap.builder()
				.put("visible", true)
				.build()));
		grid.getProperties().put("searchPanel", Lists.newArrayList(ImmutableMap.builder()
				.put("visible", true)
				.put("width", 300)
				.build()));
		// @formatter:on
		return grid;
	}

	/**
	 * Gera o template do código groovy que gera o arquivo PDF.
	 * 
	 * @param columns
	 * @return
	 */
	public String genDefaultGpdfTemplate() {
		// @formatter:off
		String gpdfTemplate = "\n"+
        "import org.springframework.core.io.ClassPathResource;\n"+
        "import ar.com.fdvs.dj.domain.ImageBanner;\n"+
        "import ar.com.fdvs.dj.domain.builders.ColumnBuilder;\n"+
        "import ar.com.fdvs.dj.domain.builders.DynamicReportBuilder;\n"+
        "import ar.com.fdvs.dj.domain.AutoText;\n"+
        "import ar.com.fdvs.dj.domain.constants.ImageScaleMode;\n"+
        "import java.util.Locale;\n"+
		"\n"+
        "def drb = new DynamicReportBuilder()\n"+
		"\n"+
        "drb.title = report.title\n"+
        "drb.subtitle = report.subtitle\n"+
        "drb.useFullPageWidth = true\n"+ 
        "drb.leftMargin = new Integer(20)\n"+
        "drb.rightMargin = new Integer(20)\n"+
        "drb.oddRowBackgroundStyle = JReportStyles.oddRowStyle\n"+
        "drb.printBackgroundOnOddRows = true\n"+
        "drb.setWhenNoData(\"Sem registros na base de dados.\", null)\n"+
        "drb.setDefaultStyles(JReportStyles.titleStyle, JReportStyles.subtitleStyle, JReportStyles.columnHeaderStyle, JReportStyles.columnDetailStyle)\n"+
		"drb.addFirstPageImageBanner(new ClassPathResource(\"BOOT-INF/brasao-republica-report-header.png\").getPath(), new Integer(90), new Integer(40), ImageBanner.ALIGN_CENTER, ImageScaleMode.REAL_SIZE  );\n"+
		"drb.addAutoText(CREATED_AT, AutoText.POSITION_FOOTER, AutoText.ALIGNMENT_LEFT, 200, JReportStyles.footerTextStyle)\n"+
		"drb.addAutoText(AutoText.AUTOTEXT_PAGE_X, AutoText.POSITION_FOOTER, AutoText.ALIGNMENT_RIGHT, 10,30, JReportStyles.footerTextStyle);\n"+
		"drb.setReportLocale(new Locale(\"pt\",\"BR\"));\n"+
		"\n"+
        "def columns = [:]\n"+
		"\n"+
        "report.grid.columns.each {\n"+ 
            "\tcolumns[it.dataField] = ColumnBuilder.new\n"+
                "\t\t.setColumnProperty(it.dataField, it.javaType)\n"+
                "\t\t.setTitle(it.caption)\n"+
                "\t\t.setWidth(it.width)\n"+
                "\t\t.build()\n"+
            "\tdrb.addColumn(columns[it.dataField])\n"+
        "}\n"+
		"\n"+
        "def dr = drb.build()\n";
		// @formatter:on
		return gpdfTemplate;
	}

	public String genDefaultGexcelTemplate() {
		// @formatter:off
		String gexcelTemplate = "\n"+
        "import ar.com.fdvs.dj.domain.builders.ColumnBuilder;\n"+
        "import ar.com.fdvs.dj.domain.builders.DynamicReportBuilder;\n"+
        "import java.util.Locale;\n"+
		"\n"+
        "def drb = new DynamicReportBuilder()\n"+
		"\n"+
        "drb.title = report.title\n"+
        "drb.subtitle = report.subtitle\n"+
        "drb.useFullPageWidth = true\n"+ 
        "drb.setPrintColumnNames(true)\n"+
        "drb.setIgnorePagination(true)\n"+
//        "drb.leftMargin = new Integer(20)\n"+
//        "drb.rightMargin = new Integer(20)\n"+
//        "drb.oddRowBackgroundStyle = JReportStyles.oddRowStyle\n"+
//        "drb.printBackgroundOnOddRows = true\n"+
        "drb.setWhenNoData(\"Sem registros na base de dados.\", null)\n"+
        "drb.setDefaultStyles(JReportStyles.titleStyle, JReportStyles.subtitleStyle, JReportStyles.columnHeaderStyle, JReportStyles.columnDetailStyle)\n"+
//		"drb.addFirstPageImageBanner(new ClassPathResource(\"BOOT-INF/brasao-republica-report-header.png\").getPath(), new Integer(90), new Integer(40), ImageBanner.ALIGN_CENTER, ImageScaleMode.REAL_SIZE  );\n"+
//		"drb.addAutoText(CREATED_AT, AutoText.POSITION_FOOTER, AutoText.ALIGNMENT_LEFT, 200, JReportStyles.footerTextStyle)\n"+
//		"drb.addAutoText(AutoText.AUTOTEXT_PAGE_X, AutoText.POSITION_FOOTER, AutoText.ALIGNMENT_RIGHT, 10,30, JReportStyles.footerTextStyle);\n"+
		"drb.setReportLocale(new Locale(\"pt\",\"BR\"));\n"+
		"\n"+
        "def columns = [:]\n"+
		"\n"+
        "report.grid.columns.each {\n"+ 
            "\tcolumns[it.dataField] = ColumnBuilder.new\n"+
                "\t\t.setColumnProperty(it.dataField, it.javaType)\n"+
                "\t\t.setTitle(it.caption)\n"+
                "\t\t.setWidth(it.width)\n"+
                "\t\t.build()\n"+
            "\tdrb.addColumn(columns[it.dataField])\n"+
        "}\n"+
		"\n"+
        "def dr = drb.build()\n";
		// @formatter:on
		return gexcelTemplate;
	}
}
