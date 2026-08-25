package com.eulerity.taskmanager.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import com.eulerity.taskmanager.entity.FieldChange;
import com.eulerity.taskmanager.entity.Project;
import com.eulerity.taskmanager.entity.Task;
import com.eulerity.taskmanager.entity.enums.TaskPriority;
import com.eulerity.taskmanager.entity.enums.TaskStatus;
import com.eulerity.taskmanager.exception.ResourceNotFoundException;
import com.eulerity.taskmanager.repository.ProjectRepository;
import com.eulerity.taskmanager.repository.TaskRepository;
import com.eulerity.taskmanager.specification.TaskSpecification;

@Service
public class TaskService {

	private final TaskRepository taskRepository;
	private final ProjectRepository projectRepository;
	private final AuditService auditService;

	public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository,
			AuditService auditService) {
		this.taskRepository = taskRepository;
		this.projectRepository = projectRepository;
		this.auditService = auditService;
	}

	public Task createTask(TaskRequestDto request) {
		Task task = new Task();
		task.setTitle(request.getTitle());
		task.setDescription(request.getDescription());
		task.setDueDate(request.getDueDate());
		task.setPriority(request.getPriority());
		task.setProject(resolveProject(request.getProjectId()));

		Task saved = taskRepository.save(task);
		auditService.logCreate(saved.getId());
		return saved;
	}

	public Task updateTask(Long id, TaskRequestDto request) {
		Task task = taskRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));

		if (request.getStatus() == null) {
			throw new IllegalArgumentException("status is required");
		}
		Project project = resolveProject(request.getProjectId());

		List<FieldChange> changes = computeChanges(task, request, project);
		if (changes.isEmpty()) {
			return task;
		}

		task.setTitle(request.getTitle());
		task.setDescription(request.getDescription());
		task.setDueDate(request.getDueDate());
		task.setPriority(request.getPriority());
		task.setStatus(request.getStatus());
		task.setProject(project);

		Task saved = taskRepository.save(task);
		auditService.logUpdate(id, changes);
		return saved;
	}

	private List<FieldChange> computeChanges(Task existing, TaskRequestDto request, Project newProject) {
		List<FieldChange> changes = new ArrayList<>();
		addIfChanged(changes, "title", existing.getTitle(), request.getTitle());
		addIfChanged(changes, "description", existing.getDescription(), request.getDescription());
		addIfChanged(changes, "dueDate", existing.getDueDate(), request.getDueDate());
		addIfChanged(changes, "priority", existing.getPriority(), request.getPriority());
		addIfChanged(changes, "status", existing.getStatus(), request.getStatus());
		Long oldProjectId = existing.getProject() != null ? existing.getProject().getId() : null;
		Long newProjectId = newProject != null ? newProject.getId() : null;
		addIfChanged(changes, "projectId", oldProjectId, newProjectId);
		return changes;
	}

	public void deleteTask(Long id) {
		if (!taskRepository.existsById(id)) {
			throw new ResourceNotFoundException("Task not found with id " + id);
		}
		taskRepository.deleteById(id);
		auditService.logDelete(id);
	}

	private void addIfChanged(List<FieldChange> changes, String fieldName, Object oldValue, Object newValue) {
		if (!Objects.equals(oldValue, newValue)) {
			changes.add(new FieldChange(fieldName,
					oldValue != null ? oldValue.toString() : null,
					newValue != null ? newValue.toString() : null));
		}
	}

	private Project resolveProject(Long projectId) {
		if (projectId == null) {
			return null;
		}
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));
	}

	public TaskResponseDto getTaskById(Long id) {
		return taskRepository.findById(id)
				.map(this::toResponseDto)
				.orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
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
