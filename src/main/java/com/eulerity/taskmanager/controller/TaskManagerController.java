package com.eulerity.taskmanager.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eulerity.taskmanager.dto.request.ProjectRequestDto;
import com.eulerity.taskmanager.dto.request.TaskRequestDto;
import com.eulerity.taskmanager.dto.response.PagedResponseDto;
import com.eulerity.taskmanager.dto.response.ProjectResponseDto;
import com.eulerity.taskmanager.dto.response.TaskResponseDto;
import com.eulerity.taskmanager.entity.Project;
import com.eulerity.taskmanager.entity.Task;
import com.eulerity.taskmanager.entity.enums.TaskPriority;
import com.eulerity.taskmanager.entity.enums.TaskStatus;
import com.eulerity.taskmanager.service.ProjectService;
import com.eulerity.taskmanager.service.TaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class TaskManagerController {

	private final TaskService taskService;
	private final ProjectService projectService;

	public TaskManagerController(TaskService taskService, ProjectService projectService) {
		this.taskService = taskService;
		this.projectService = projectService;
	}

	@PostMapping("/tasks")
	public ResponseEntity<String> createTask(@Valid @RequestBody TaskRequestDto request) {
		try {
			Task created = taskService.createTask(request);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body("Task created with id " + created.getId());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping("/tasks")
	public ResponseEntity<Object> getTasks(
			@RequestParam(required = false) TaskStatus status,
			@RequestParam(required = false) TaskPriority priority,
			@RequestParam(required = false) LocalDate dueBefore,
			@RequestParam(required = false) LocalDate dueAfter,
			@RequestParam(required = false) Long projectId,
			@RequestParam(defaultValue = "dueDate") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "4") int size) {
		try {
			return ResponseEntity.ok(taskService.getFilteredTasks(
					status, priority, dueBefore, dueAfter, projectId, sortBy, sortDir, page, size));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping("/tasks/{id}")
	public ResponseEntity<TaskResponseDto> getTask(@PathVariable Long id) {
		return taskService.getTaskById(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PutMapping("/tasks/{id}")
	public ResponseEntity<String> updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequestDto request) {
		try {
			return taskService.updateTask(id, request)
					.map(task -> ResponseEntity.ok("Task updated with id " + task.getId()))
					.orElseGet(() -> ResponseEntity.notFound().build());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@DeleteMapping("/tasks/{id}")
	public ResponseEntity<String> deleteTask(@PathVariable Long id) {
		if (taskService.deleteTask(id)) {
			return ResponseEntity.ok("Task deleted with id " + id);
		}
		return ResponseEntity.notFound().build();
	}

	@PostMapping("/projects")
	public ResponseEntity<String> createProject(@Valid @RequestBody ProjectRequestDto request) {
		Project created = projectService.createProject(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body("Project created with id " + created.getId());
	}

	@GetMapping("/projects")
	public ResponseEntity<List<ProjectResponseDto>> getProjects() {
		return ResponseEntity.ok(projectService.getAllProjects());
	}

	@GetMapping("/health")
	public String health() {
		return "running fine";
	}

}
