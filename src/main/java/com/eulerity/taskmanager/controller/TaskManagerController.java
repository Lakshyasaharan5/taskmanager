package com.eulerity.taskmanager.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TaskManagerController {

	@PostMapping("/tasks")
	public String createTask() {
		System.out.println("Tasks as of now: dummy task list");
		return "dummy task created";
	}

	@GetMapping("/tasks")
	public String getTasks() {
		return "dummy task list";
	}

	@GetMapping("/tasks/{id}")
	public String getTask(@PathVariable String id) {
		return "dummy task with id " + id;
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
