package com.eulerity.taskmanager.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.eulerity.taskmanager.dto.request.TaskRequestDto;
import com.eulerity.taskmanager.dto.response.PagedResponseDto;
import com.eulerity.taskmanager.dto.response.ProjectSummaryDto;
import com.eulerity.taskmanager.dto.response.TaskResponseDto;
import com.eulerity.taskmanager.entity.Project;
import com.eulerity.taskmanager.entity.Task;
import com.eulerity.taskmanager.entity.enums.TaskPriority;
import com.eulerity.taskmanager.entity.enums.TaskStatus;
import com.eulerity.taskmanager.repository.ProjectRepository;
import com.eulerity.taskmanager.repository.TaskRepository;
import com.eulerity.taskmanager.specification.TaskSpecification;

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
		task.setProject(resolveProject(request.getProjectId()));

		return taskRepository.save(task);
	}

	public Optional<Task> updateTask(Long id, TaskRequestDto request) {
		return taskRepository.findById(id).map(task -> {
			task.setTitle(request.getTitle());
			task.setDescription(request.getDescription());
			task.setDueDate(request.getDueDate());
			task.setPriority(request.getPriority());
			if (request.getStatus() == null) {
				throw new IllegalArgumentException("status is required");
			}
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

	public Optional<TaskResponseDto> getTaskById(Long id) {
		return taskRepository.findById(id)
				.map(this::toResponseDto);
	}

	public PagedResponseDto<TaskResponseDto> getFilteredTasks(TaskStatus status, TaskPriority priority,
			LocalDate dueBefore, LocalDate dueAfter, Long projectId, String sortBy, String sortDir,
			int page, int size) {
		Pageable pageable = PageRequest.of(page, size, buildSort(sortBy, sortDir));
		Specification<Task> spec = buildSpecification(status, priority, dueBefore, dueAfter, projectId);
		Page<Task> result = taskRepository.findAll(spec, pageable);

		PagedResponseDto<TaskResponseDto> response = new PagedResponseDto<>();
		response.setContent(result.getContent().stream().map(this::toResponseDto).toList());
		response.setPage(result.getNumber());
		response.setSize(result.getSize());
		response.setTotalElements(result.getTotalElements());
		response.setTotalPages(result.getTotalPages());
		return response;
	}

	private Sort buildSort(String sortBy, String sortDir) {
		if (!"dueDate".equals(sortBy) && !"priority".equals(sortBy)) {
			throw new IllegalArgumentException("sortBy must be one of: dueDate, priority");
		}
		Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
		return Sort.by(direction, sortBy);
	}

	private Specification<Task> buildSpecification(TaskStatus status, TaskPriority priority,
			LocalDate dueBefore, LocalDate dueAfter, Long projectId) {
		List<Specification<Task>> specs = new ArrayList<>();
		if (status != null) {
			specs.add(TaskSpecification.hasStatus(status));
		}
		if (priority != null) {
			specs.add(TaskSpecification.hasPriority(priority));
		}
		if (dueBefore != null) {
			specs.add(TaskSpecification.dueBefore(dueBefore));
		}
		if (dueAfter != null) {
			specs.add(TaskSpecification.dueAfter(dueAfter));
		}
		if (projectId != null) {
			specs.add(TaskSpecification.hasProjectId(projectId));
		}
		return specs.stream().reduce(Specification::and).orElse(null);
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
