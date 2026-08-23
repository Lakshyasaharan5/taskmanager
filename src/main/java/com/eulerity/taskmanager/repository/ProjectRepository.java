package com.eulerity.taskmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eulerity.taskmanager.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
