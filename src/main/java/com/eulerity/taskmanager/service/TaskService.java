package com.eulerity.taskmanager.service;

import org.springframework.stereotype.Service;

import com.eulerity.taskmanager.dto.TaskRequestDto;
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

		if (request.getProjectId() != null) {
			Project project = projectRepository.findById(request.getProjectId())
					.orElseThrow(() -> new IllegalArgumentException(
							"Project not found with id " + request.getProjectId()));
			task.setProject(project);
		}

		return taskRepository.save(task);
	}

}
