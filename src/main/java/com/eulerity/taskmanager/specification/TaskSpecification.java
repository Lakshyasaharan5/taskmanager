package com.eulerity.taskmanager.specification;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.eulerity.taskmanager.entity.Task;
import com.eulerity.taskmanager.entity.enums.TaskPriority;
import com.eulerity.taskmanager.entity.enums.TaskStatus;

public class TaskSpecification {

	private TaskSpecification() {
	}

	public static Specification<Task> hasStatus(TaskStatus status) {
		return (root, query, cb) -> cb.equal(root.get("status"), status);
	}

	public static Specification<Task> hasPriority(TaskPriority priority) {
		return (root, query, cb) -> cb.equal(root.get("priority"), priority);
	}

	public static Specification<Task> dueBefore(LocalDate date) {
		return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueDate"), date);
	}

	public static Specification<Task> dueAfter(LocalDate date) {
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueDate"), date);
	}

	public static Specification<Task> hasProjectId(Long projectId) {
		return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
	}

}
