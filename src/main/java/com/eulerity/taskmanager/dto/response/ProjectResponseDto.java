package com.eulerity.taskmanager.dto.response;

import java.util.List;

public class ProjectResponseDto {

	private Long id;

	private String name;

	private String description;

	private List<TaskSummaryDto> tasks;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<TaskSummaryDto> getTasks() {
		return tasks;
	}

	public void setTasks(List<TaskSummaryDto> tasks) {
		this.tasks = tasks;
	}

}
