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
	 * Gera o template da coluna (Lista de JReportColumn) baseado na consuta SQL.
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
		// @formatter:off
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
		// @formatter:on
		return gpdfTemplate;
	}
}
