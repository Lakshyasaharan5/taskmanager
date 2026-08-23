package com.eulerity.taskmanager.dto.response;

import java.time.LocalDate;

import com.eulerity.taskmanager.entity.enums.TaskPriority;
import com.eulerity.taskmanager.entity.enums.TaskStatus;

public class TaskResponseDto {

	private Long id;

	private String title;

	private String description;

	private LocalDate dueDate;

	private TaskPriority priority;

	private TaskStatus status;

	private ProjectSummaryDto project;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public TaskPriority getPriority() {
		return priority;
	}

	public void setPriority(TaskPriority priority) {
		this.priority = priority;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public ProjectSummaryDto getProject() {
		return project;
	}

	public void setProject(ProjectSummaryDto project) {
		this.project = project;
	}

}