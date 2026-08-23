package com.eulerity.taskmanager.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.eulerity.taskmanager.dto.request.TaskRequestDto;
import com.eulerity.taskmanager.dto.response.TaskResponseDto;
import com.eulerity.taskmanager.entity.Task;
import com.eulerity.taskmanager.service.TaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class TaskManagerController {

	private final TaskService taskService;

	public TaskManagerController(TaskService taskService) {
		this.taskService = taskService;
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
	public ResponseEntity<List<TaskResponseDto>> getTasks() {
		return ResponseEntity.ok(taskService.getAllTasks());
	}

	@GetMapping("/tasks/{id}")
	public ResponseEntity<TaskResponseDto> getTask(@PathVariable Long id) {
		return taskService.getTaskById(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PutMapping("/tasks/{id}")
	public String updateTask(@PathVariable String id) {
		System.out.println("updating task with id " + id);
		return "dummy task updated with id " + id;
	}

	@DeleteMapping("/tasks/{id}")
	public String deleteTask(@PathVariable String id) {
		System.out.println("deleting task with id " + id);
		return "dummy task deleted with id " + id;
	}

	@GetMapping("/health")
	public String health() {
		return "running fine";
	}

}
