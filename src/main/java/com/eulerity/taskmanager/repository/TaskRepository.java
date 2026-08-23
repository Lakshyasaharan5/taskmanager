package com.eulerity.taskmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eulerity.taskmanager.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
