package com.eulerity.taskmanager.entity;

import java.time.Instant;
import java.util.List;

import com.eulerity.taskmanager.entity.converter.FieldChangeListConverter;
import com.eulerity.taskmanager.entity.enums.AuditAction;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_audit_log")
public class TaskAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "task_id", nullable = false)
	private Long taskId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuditAction action;

	@Convert(converter = FieldChangeListConverter.class)
	@Column(length = 2000)
	private List<FieldChange> changes;

	@Column(name = "changed_at", nullable = false)
	private Instant changedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public AuditAction getAction() {
		return action;
	}

	public void setAction(AuditAction action) {
		this.action = action;
	}

	public List<FieldChange> getChanges() {
		return changes;
	}

	public void setChanges(List<FieldChange> changes) {
		this.changes = changes;
	}

	public Instant getChangedAt() {
		return changedAt;
	}

	public void setChangedAt(Instant changedAt) {
		this.changedAt = changedAt;
	}

}
