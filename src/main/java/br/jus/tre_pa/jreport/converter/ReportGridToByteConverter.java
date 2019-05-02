package br.jus.tre_pa.jreport.converter;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.jus.tre_pa.jreport.types.JReportGrid;
import lombok.SneakyThrows;

public class ReportGridToByteConverter implements AttributeConverter<JReportGrid, byte[]> {

	@Override
	@SneakyThrows
	public byte[] convertToDatabaseColumn(JReportGrid attribute) {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsBytes(attribute);
	}

	@Override
	@SneakyThrows
	public JReportGrid convertToEntityAttribute(byte[] dbData) {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(dbData, new TypeReference<JReportGrid>() {});
	}

}
