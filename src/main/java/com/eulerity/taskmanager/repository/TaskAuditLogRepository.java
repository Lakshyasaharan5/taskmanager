package com.eulerity.taskmanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eulerity.taskmanager.entity.TaskAuditLog;

public interface TaskAuditLogRepository extends JpaRepository<TaskAuditLog, Long> {

	List<TaskAuditLog> findByTaskIdOrderByChangedAtAsc(Long taskId);

}
