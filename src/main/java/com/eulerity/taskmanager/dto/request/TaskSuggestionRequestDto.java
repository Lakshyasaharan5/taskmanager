package com.eulerity.taskmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TaskSuggestionRequestDto {

	@NotBlank(message = "Query is required")
	@Size(max = 1000, message = "Query must be 1000 characters or fewer")
	private String query;

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

}
