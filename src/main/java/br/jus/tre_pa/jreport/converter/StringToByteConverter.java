package br.jus.tre_pa.jreport.converter;

import javax.persistence.AttributeConverter;

public class StringToByteConverter implements AttributeConverter<String, byte[]> {

	@Override
	public byte[] convertToDatabaseColumn(String attribute) {
		return attribute.getBytes();
	}

	@Override
	public String convertToEntityAttribute(byte[] dbData) {
		return new String(dbData);
	}

}
