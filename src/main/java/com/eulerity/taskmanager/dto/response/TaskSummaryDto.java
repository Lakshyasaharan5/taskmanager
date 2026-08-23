package com.eulerity.taskmanager.dto.response;

import com.eulerity.taskmanager.entity.enums.TaskPriority;
import com.eulerity.taskmanager.entity.enums.TaskStatus;

public class TaskSummaryDto {

	private Long id;

	private String title;

	private TaskStatus status;

	private TaskPriority priority;

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

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public TaskPriority getPriority() {
		return priority;
	}

	public void setPriority(TaskPriority priority) {
		this.priority = priority;
	}

}
