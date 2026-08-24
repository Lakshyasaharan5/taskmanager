package com.eulerity.taskmanager.dto.response;

import java.time.Instant;
import java.util.List;

import com.eulerity.taskmanager.entity.enums.AuditAction;

public class TaskAuditLogDto {

	private Long id;
	private AuditAction action;
	private List<FieldChangeDto> changes;
	private Instant changedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public AuditAction getAction() {
		return action;
	}

	public void setAction(AuditAction action) {
		this.action = action;
	}

	public List<FieldChangeDto> getChanges() {
		return changes;
	}

	public void setChanges(List<FieldChangeDto> changes) {
		this.changes = changes;
	}

	public Instant getChangedAt() {
		return changedAt;
	}

	public void setChangedAt(Instant changedAt) {
		this.changedAt = changedAt;
	}

}
