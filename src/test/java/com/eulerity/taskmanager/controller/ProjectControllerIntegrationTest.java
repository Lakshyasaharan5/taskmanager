package com.eulerity.taskmanager.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.eulerity.taskmanager.dto.request.ProjectRequestDto;
import com.eulerity.taskmanager.dto.request.TaskRequestDto;
import com.eulerity.taskmanager.dto.response.ProjectResponseDto;
import com.eulerity.taskmanager.entity.enums.TaskPriority;
import com.eulerity.taskmanager.entity.enums.TaskStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ProjectControllerIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void projectCrudLifecycle() {
		ProjectRequestDto createProjectRequest = new ProjectRequestDto();
		createProjectRequest.setName("Integration Test Project");
		createProjectRequest.setDescription("Created by an integration test");

		ResponseEntity<String> createProjectResponse = restTemplate.postForEntity("/api/projects",
				createProjectRequest, String.class);
		assertThat(createProjectResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Long projectId = extractId(createProjectResponse.getBody());

		TaskRequestDto createTaskRequest = new TaskRequestDto();
		createTaskRequest.setTitle("Task in integration test project");
		createTaskRequest.setDueDate(LocalDate.now().plusDays(2));
		createTaskRequest.setPriority(TaskPriority.MEDIUM);
		createTaskRequest.setStatus(TaskStatus.TODO);
		createTaskRequest.setProjectId(projectId);

		ResponseEntity<String> createTaskResponse = restTemplate.postForEntity("/api/tasks", createTaskRequest,
				String.class);
		assertThat(createTaskResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Long taskId = extractId(createTaskResponse.getBody());

		ResponseEntity<List<ProjectResponseDto>> listResponse = restTemplate.exchange(
				"/api/projects", HttpMethod.GET, null,
				new ParameterizedTypeReference<List<ProjectResponseDto>>() {
				});
		assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(listResponse.getBody()).isNotNull();
		assertThat(listResponse.getBody()).anySatisfy(project -> {
			assertThat(project.getId()).isEqualTo(projectId);
			assertThat(project.getName()).isEqualTo("Integration Test Project");
			assertThat(project.getTasks()).anySatisfy(task -> assertThat(task.getId()).isEqualTo(taskId));
		});

		ResponseEntity<String> deleteTaskResponse = restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.DELETE,
				null, String.class);
		assertThat(deleteTaskResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		ResponseEntity<String> deleteProjectResponse = restTemplate.exchange("/api/projects/" + projectId,
				HttpMethod.DELETE, null, String.class);
		assertThat(deleteProjectResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	private Long extractId(String responseBody) {
		String[] parts = responseBody.split(" ");
		return Long.parseLong(parts[parts.length - 1]);
	}

}
