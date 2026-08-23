package com.eulerity.taskmanager.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.eulerity.taskmanager.dto.request.ProjectRequestDto;
import com.eulerity.taskmanager.dto.response.ProjectResponseDto;
import com.eulerity.taskmanager.dto.response.TaskSummaryDto;
import com.eulerity.taskmanager.entity.Project;
import com.eulerity.taskmanager.entity.Task;
import com.eulerity.taskmanager.exception.ConflictException;
import com.eulerity.taskmanager.exception.ResourceNotFoundException;
import com.eulerity.taskmanager.repository.ProjectRepository;
import com.eulerity.taskmanager.repository.TaskRepository;

@Service
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final TaskRepository taskRepository;

	public ProjectService(ProjectRepository projectRepository, TaskRepository taskRepository) {
		this.projectRepository = projectRepository;
		this.taskRepository = taskRepository;
	}

	public Project createProject(ProjectRequestDto request) {
		Project project = new Project();
		project.setName(request.getName());
		project.setDescription(request.getDescription());
		return projectRepository.save(project);
	}

	public void deleteProject(Long id) {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + id));
		if (taskRepository.existsByProjectId(project.getId())) {
			throw new ConflictException(
					"Cannot delete project with associated tasks. Please disassociate the tasks first.");
		}
		projectRepository.delete(project);
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
