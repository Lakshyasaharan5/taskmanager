package com.eulerity.taskmanager.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.eulerity.taskmanager.dto.request.ProjectRequestDto;
import com.eulerity.taskmanager.dto.response.ProjectResponseDto;
import com.eulerity.taskmanager.dto.response.TaskSummaryDto;
import com.eulerity.taskmanager.entity.Project;
import com.eulerity.taskmanager.entity.Task;
import com.eulerity.taskmanager.repository.ProjectRepository;

@Service
public class ProjectService {

	private final ProjectRepository projectRepository;

	public ProjectService(ProjectRepository projectRepository) {
		this.projectRepository = projectRepository;
	}

	public Project createProject(ProjectRequestDto request) {
		Project project = new Project();
		project.setName(request.getName());
		project.setDescription(request.getDescription());
		return projectRepository.save(project);
	}

	public List<ProjectResponseDto> getAllProjects() {
		return projectRepository.findAll().stream()
				.map(this::toResponseDto)
				.toList();
	}

	private ProjectResponseDto toResponseDto(Project project) {
		ProjectResponseDto dto = new ProjectResponseDto();
		dto.setId(project.getId());
		dto.setName(project.getName());
		dto.setDescription(project.getDescription());
		dto.setTasks(project.getTasks().stream()
				.map(this::toTaskSummaryDto)
				.toList());
		return dto;
	}

	private TaskSummaryDto toTaskSummaryDto(Task task) {
		TaskSummaryDto dto = new TaskSummaryDto();
		dto.setId(task.getId());
		dto.setTitle(task.getTitle());
		dto.setStatus(task.getStatus());
		dto.setPriority(task.getPriority());
		return dto;
	}

}
