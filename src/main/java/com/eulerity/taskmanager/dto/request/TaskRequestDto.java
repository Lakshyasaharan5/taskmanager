package com.eulerity.taskmanager.dto.request;

import java.time.LocalDate;

import com.eulerity.taskmanager.entity.enums.TaskPriority;
import com.eulerity.taskmanager.entity.enums.TaskStatus;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TaskRequestDto {

	@NotBlank(message = "Title is required")
	private String title;

	private String description;

	@NotNull(message = "Due date is required")
	@FutureOrPresent(message = "Due date cannot be in the past")
	private LocalDate dueDate;

	@NotNull(message = "Priority is required")
	private TaskPriority priority;

	private TaskStatus status;

	private Long projectId;

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

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

}
