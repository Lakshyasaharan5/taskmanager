package com.eulerity.taskmanager.dto.response;

import java.util.List;

public class ErrorResponseDto {

	private String error;

	private String message;

	private List<FieldErrorDto> fields;

	public ErrorResponseDto() {
	}

	public ErrorResponseDto(String error, String message, List<FieldErrorDto> fields) {
		this.error = error;
		this.message = message;
		this.fields = fields;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<FieldErrorDto> getFields() {
		return fields;
	}

	public void setFields(List<FieldErrorDto> fields) {
		this.fields = fields;
	}

}
