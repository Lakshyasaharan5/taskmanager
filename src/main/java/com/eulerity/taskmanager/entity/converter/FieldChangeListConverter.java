package com.eulerity.taskmanager.entity.converter;

import java.util.Arrays;
import java.util.List;

import com.eulerity.taskmanager.entity.FieldChange;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import tools.jackson.databind.ObjectMapper;

@Converter
public class FieldChangeListConverter implements AttributeConverter<List<FieldChange>, String> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<FieldChange> changes) {
		if (changes == null) {
			return null;
		}
		return OBJECT_MAPPER.writeValueAsString(changes);
	}

	@Override
	public List<FieldChange> convertToEntityAttribute(String json) {
		if (json == null) {
			return null;
		}
		return Arrays.asList(OBJECT_MAPPER.readValue(json, FieldChange[].class));
	}

}
