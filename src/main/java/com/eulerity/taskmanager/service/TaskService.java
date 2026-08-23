package com.eulerity.taskmanager.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.eulerity.taskmanager.dto.request.TaskRequestDto;
import com.eulerity.taskmanager.dto.response.ProjectSummaryDto;
import com.eulerity.taskmanager.dto.response.TaskResponseDto;
import com.eulerity.taskmanager.entity.Project;
import com.eulerity.taskmanager.entity.Task;
import com.eulerity.taskmanager.repository.ProjectRepository;
import com.eulerity.taskmanager.repository.TaskRepository;

@Service
public class TaskService {

	private final TaskRepository taskRepository;
	private final ProjectRepository projectRepository;

	public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository) {
		this.taskRepository = taskRepository;
		this.projectRepository = projectRepository;
	}

	public Task createTask(TaskRequestDto request) {
		Task task = new Task();
		task.setTitle(request.getTitle());
		task.setDescription(request.getDescription());
		task.setDueDate(request.getDueDate());
		task.setPriority(request.getPriority());
		task.setStatus(request.getStatus());
		task.setProject(resolveProject(request.getProjectId()));

		return taskRepository.save(task);
	}

	public Optional<Task> updateTask(Long id, TaskRequestDto request) {
		return taskRepository.findById(id).map(task -> {
			task.setTitle(request.getTitle());
			task.setDescription(request.getDescription());
			task.setDueDate(request.getDueDate());
			task.setPriority(request.getPriority());
			task.setStatus(request.getStatus());
			task.setProject(resolveProject(request.getProjectId()));
			return taskRepository.save(task);
		});
	}

	public boolean deleteTask(Long id) {
		if (!taskRepository.existsById(id)) {
			return false;
		}
		taskRepository.deleteById(id);
		return true;
	}

	private Project resolveProject(Long projectId) {
		if (projectId == null) {
			return null;
		}
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new IllegalArgumentException("Project not found with id " + projectId));
	}

	public List<TaskResponseDto> getAllTasks() {
		return taskRepository.findAll().stream()
				.map(this::toResponseDto)
				.toList();
	}

	public Optional<TaskResponseDto> getTaskById(Long id) {
		return taskRepository.findById(id)
				.map(this::toResponseDto);
	}

	private TaskResponseDto toResponseDto(Task task) {
		TaskResponseDto dto = new TaskResponseDto();
		dto.setId(task.getId());
		dto.setTitle(task.getTitle());
		dto.setDescription(task.getDescription());
		dto.setDueDate(task.getDueDate());
		dto.setPriority(task.getPriority());
		dto.setStatus(task.getStatus());

		Project project = task.getProject();
		if (project != null) {
			ProjectSummaryDto projectSummary = new ProjectSummaryDto();
			projectSummary.setId(project.getId());
			projectSummary.setName(project.getName());
			dto.setProject(projectSummary);
		}

		return dto;
	}

}
