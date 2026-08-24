package com.eulerity.taskmanager.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.eulerity.taskmanager.dto.response.FieldChangeDto;
import com.eulerity.taskmanager.dto.response.TaskAuditLogDto;
import com.eulerity.taskmanager.entity.FieldChange;
import com.eulerity.taskmanager.entity.TaskAuditLog;
import com.eulerity.taskmanager.entity.enums.AuditAction;
import com.eulerity.taskmanager.repository.TaskAuditLogRepository;

@Service
public class AuditService {

	private static final Logger log = LoggerFactory.getLogger(AuditService.class);

	private final TaskAuditLogRepository taskAuditLogRepository;

	public AuditService(TaskAuditLogRepository taskAuditLogRepository) {
		this.taskAuditLogRepository = taskAuditLogRepository;
	}

	public void logCreate(Long taskId) {
		record(taskId, AuditAction.CREATED, null);
	}

	public void logUpdate(Long taskId, List<FieldChange> changes) {
		record(taskId, AuditAction.UPDATED, changes);
	}

	public void logDelete(Long taskId) {
		record(taskId, AuditAction.DELETED, null);
	}

	private void record(Long taskId, AuditAction action, List<FieldChange> changes) {
		try {
			TaskAuditLog entry = new TaskAuditLog();
			entry.setTaskId(taskId);
			entry.setAction(action);
			entry.setChanges(changes);
			entry.setChangedAt(Instant.now());
			taskAuditLogRepository.save(entry);
		} catch (Exception e) {
			log.warn("Failed to save audit log entry for task {} action {}: {}", taskId, action, e.getMessage());
		}
	}

	public List<TaskAuditLogDto> getHistory(Long taskId) {
		return taskAuditLogRepository.findByTaskIdOrderByChangedAtAsc(taskId).stream()
				.map(this::toDto)
				.toList();
	}

	private TaskAuditLogDto toDto(TaskAuditLog entry) {
		TaskAuditLogDto dto = new TaskAuditLogDto();
		dto.setId(entry.getId());
		dto.setAction(entry.getAction());
		dto.setChangedAt(entry.getChangedAt());
		if (entry.getChanges() != null) {
			dto.setChanges(entry.getChanges().stream()
					.map(this::toDto)
					.toList());
		}
		return dto;
	}

	private FieldChangeDto toDto(FieldChange change) {
		FieldChangeDto dto = new FieldChangeDto();
		dto.setFieldName(change.getFieldName());
		dto.setOldValue(change.getOldValue());
		dto.setNewValue(change.getNewValue());
		return dto;
	}

}
