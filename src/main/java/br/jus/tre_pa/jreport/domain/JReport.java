package br.jus.tre_pa.jreport.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import br.jus.tre_pa.jreport.converter.JReportGridToByteConverter;
import br.jus.tre_pa.jreport.converter.StringToByteConverter;
import br.jus.tre_pa.jreport.types.JReportGrid;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener.class)
@Table(name = "MNGT_JREPORT")
@EqualsAndHashCode(of = { "id" })
@Getter
@Setter
@NoArgsConstructor
public class JReport {

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MNGT_JREPORT")
	@SequenceGenerator(initialValue = 1, allocationSize = 1, name = "SEQ_MNGT_JREPORT", sequenceName = "SEQ_MNGT_JREPORT")
	private Long id;

	/**
	 * Título do relatório.
	 */
	@Column(nullable = false, unique = true)
	@NotBlank
	@Deprecated
	private String title;

	/**
	 * Subtítulo do relatório.
	 */
	@Column
	@Deprecated
	private String subtitle;

	/**
	 * Quando o relatório foi criado.
	 */
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * Quando o relatório foi atualizado pela última vez.
	 */
	@Column
	private LocalDateTime lastUpdateAt;

	/**
	 * Versão do relatório.
	 */
	@Column
	@Version
	private Integer version;

	@Column
	private UUID uuid;

	/**
	 * Código SQL nativo para geração do datasource.
	 */
	@Column(name = "SQL", nullable = false, columnDefinition = "blob")
	@Convert(converter = StringToByteConverter.class)
	@Basic(fetch = FetchType.LAZY)
	@Deprecated
	private String sql;

	/**
	 * Defininição do DataGrid do relatório, que possui a definição das colunas do relatório utilizada tanto nos relatórios online quanto no relatórios PDF.
	 * 
	 */
	@Column(nullable = false, columnDefinition = "blob")
	@Convert(converter = JReportGridToByteConverter.class)
	@Basic(fetch = FetchType.LAZY)
	private JReportGrid grid;

	/**
	 * Código groovy para PDF do relatório em DynamicJasper.
	 * 
	 */
	@Column(name = "GPDF", nullable = false, columnDefinition = "blob")
	@Convert(converter = StringToByteConverter.class)
	@Basic(fetch = FetchType.LAZY)
	@Deprecated
	private String gpdf;

	/**
	 * Código groovy para Excel do relatório em DynamicJasper.
	 */
	@Column(name = "GEXCEL", nullable = false, columnDefinition = "blob")
	@Convert(converter = StringToByteConverter.class)
	@Basic(fetch = FetchType.LAZY)
	@Deprecated
	private String gexcel;

	/**
	 * Xml de relatório Jasper Report.
	 */
	@Column(name = "JRXML", nullable = false, columnDefinition = "blob")
	@Convert(converter = StringToByteConverter.class)
	@Basic(fetch = FetchType.LAZY)
	private String jrxml;

	/**
	 * Categoria do relatório.
	 */
	@Column(nullable = false)
	@NotBlank
	private String category;

	/**
	 * Flag informando se o relatório está disponível apenas aos gerentes.
	 * 
	 */
	@Column
	private boolean mngtOnly = true;

	/**
	 * Flag informando se o relatório está disponível apenas para impressão.
	 * 
	 */
	@Column
	private boolean printOnly = false;

	public JReport(@NotBlank String sql, String gpdf, JReportGrid grid) {
		super();
		this.sql = sql;
		this.grid = grid;
		this.gpdf = gpdf;
	}

	public JReport(String jrxml) {
		super();
		this.jrxml = jrxml;
	}

}
