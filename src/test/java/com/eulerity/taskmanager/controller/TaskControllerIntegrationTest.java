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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import com.eulerity.taskmanager.dto.request.ProjectRequestDto;
import com.eulerity.taskmanager.dto.request.TaskRequestDto;
import com.eulerity.taskmanager.dto.response.ErrorResponseDto;
import com.eulerity.taskmanager.dto.response.FieldErrorDto;
import com.eulerity.taskmanager.dto.response.PagedResponseDto;
import com.eulerity.taskmanager.dto.response.TaskResponseDto;
import com.eulerity.taskmanager.entity.enums.TaskPriority;
import com.eulerity.taskmanager.entity.enums.TaskStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class TaskControllerIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void taskCrudLifecycle() {
		TaskRequestDto createRequest = new TaskRequestDto();
		createRequest.setTitle("Integration Test Task");
		createRequest.setDescription("Created by an integration test");
		createRequest.setDueDate(LocalDate.now().plusDays(3));
		createRequest.setPriority(TaskPriority.LOW);
		createRequest.setStatus(TaskStatus.TODO);

		ResponseEntity<String> createResponse = restTemplate.postForEntity("/api/tasks", createRequest, String.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Long id = extractId(createResponse.getBody());

		ResponseEntity<TaskResponseDto> getResponse = restTemplate.getForEntity("/api/tasks/" + id, TaskResponseDto.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getResponse.getBody()).isNotNull();
		assertThat(getResponse.getBody().getTitle()).isEqualTo("Integration Test Task");
		assertThat(getResponse.getBody().getPriority()).isEqualTo(TaskPriority.LOW);
		assertThat(getResponse.getBody().getStatus()).isEqualTo(TaskStatus.TODO);

		TaskRequestDto updateRequest = new TaskRequestDto();
		updateRequest.setTitle("Integration Test Task Updated");
		updateRequest.setDescription("Updated by an integration test");
		updateRequest.setDueDate(LocalDate.now().plusDays(10));
		updateRequest.setPriority(TaskPriority.HIGH);
		updateRequest.setStatus(TaskStatus.IN_PROGRESS);

		ResponseEntity<String> updateResponse = restTemplate.exchange("/api/tasks/" + id, HttpMethod.PUT,
				new HttpEntity<>(updateRequest), String.class);
		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		ResponseEntity<TaskResponseDto> getAfterUpdate = restTemplate.getForEntity("/api/tasks/" + id, TaskResponseDto.class);
		assertThat(getAfterUpdate.getBody()).isNotNull();
		assertThat(getAfterUpdate.getBody().getTitle()).isEqualTo("Integration Test Task Updated");
		assertThat(getAfterUpdate.getBody().getPriority()).isEqualTo(TaskPriority.HIGH);
		assertThat(getAfterUpdate.getBody().getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

		ResponseEntity<PagedResponseDto<TaskResponseDto>> listResponse = restTemplate.exchange(
				"/api/tasks", HttpMethod.GET, null,
				new ParameterizedTypeReference<PagedResponseDto<TaskResponseDto>>() {
				});
		assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(listResponse.getBody()).isNotNull();
		assertThat(listResponse.getBody().getContent())
				.anySatisfy(task -> assertThat(task.getId()).isEqualTo(id));

		ResponseEntity<String> deleteResponse = restTemplate.exchange("/api/tasks/" + id, HttpMethod.DELETE, null,
				String.class);
		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void getTasks_combinesMultipleFiltersWithPaginationAndSorting() {
		LocalDate base = LocalDate.now().plusYears(5);
		Long projectId = createProject("Filter Test Project");

		Long match1 = createTask("Filter match 1", base, TaskPriority.HIGH, projectId);
		Long match2 = createTask("Filter match 2", base.plusDays(1), TaskPriority.HIGH, projectId);
		Long match3 = createTask("Filter match 3", base.plusDays(2), TaskPriority.HIGH, projectId);
		Long wrongPriority = createTask("Filter wrong priority", base.plusDays(1), TaskPriority.LOW, projectId);
		Long wrongProject = createTask("Filter wrong project", base.plusDays(1), TaskPriority.HIGH, null);

		try {
			String baseQuery = UriComponentsBuilder.fromPath("/api/tasks")
					.queryParam("priority", TaskPriority.HIGH)
					.queryParam("projectId", projectId)
					.queryParam("dueAfter", base)
					.queryParam("dueBefore", base.plusDays(2))
					.queryParam("sortBy", "dueDate")
					.queryParam("sortDir", "asc")
					.queryParam("size", 2)
					.toUriString();

			ResponseEntity<PagedResponseDto<TaskResponseDto>> firstPage = restTemplate.exchange(
					baseQuery + "&page=0", HttpMethod.GET, null,
					new ParameterizedTypeReference<PagedResponseDto<TaskResponseDto>>() {
					});
			assertThat(firstPage.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(firstPage.getBody()).isNotNull();
			assertThat(firstPage.getBody().getTotalElements()).isEqualTo(3);
			assertThat(firstPage.getBody().getTotalPages()).isEqualTo(2);
			assertThat(firstPage.getBody().getPage()).isZero();
			assertThat(firstPage.getBody().getContent()).extracting(TaskResponseDto::getId)
					.containsExactly(match1, match2);

			ResponseEntity<PagedResponseDto<TaskResponseDto>> secondPage = restTemplate.exchange(
					baseQuery + "&page=1", HttpMethod.GET, null,
					new ParameterizedTypeReference<PagedResponseDto<TaskResponseDto>>() {
					});
			assertThat(secondPage.getBody()).isNotNull();
			assertThat(secondPage.getBody().getContent()).extracting(TaskResponseDto::getId)
					.containsExactly(match3);
		} finally {
			for (Long id : List.of(match1, match2, match3, wrongPriority, wrongProject)) {
				restTemplate.exchange("/api/tasks/" + id, HttpMethod.DELETE, null, String.class);
			}
			restTemplate.exchange("/api/projects/" + projectId, HttpMethod.DELETE, null, String.class);
		}
	}

	@Test
	void createTask_missingAndInvalidFields_returnsStructuredValidationError() {
		TaskRequestDto request = new TaskRequestDto();
		request.setDueDate(LocalDate.now().minusDays(1));
		request.setPriority(TaskPriority.LOW);
		request.setStatus(TaskStatus.TODO);

		ResponseEntity<ErrorResponseDto> response = restTemplate.postForEntity("/api/tasks", request,
				ErrorResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		ErrorResponseDto body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getError()).isEqualTo("VALIDATION_FAILED");
		assertThat(body.getMessage()).isEqualTo("One or more fields are invalid");
		assertThat(body.getFields()).extracting(FieldErrorDto::getField)
				.containsExactlyInAnyOrder("title", "dueDate");
	}

	@Test
	void getTask_nonExistentId_returnsStructuredNotFoundError() {
		Long id = createTask("To be deleted", LocalDate.now().plusDays(1), TaskPriority.LOW, null);
		restTemplate.exchange("/api/tasks/" + id, HttpMethod.DELETE, null, String.class);

		ResponseEntity<ErrorResponseDto> response = restTemplate.getForEntity("/api/tasks/" + id,
				ErrorResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		ErrorResponseDto body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getError()).isEqualTo("RESOURCE_NOT_FOUND");
		assertThat(body.getMessage()).isEqualTo("Task not found with id " + id);
		assertThat(body.getFields()).isEmpty();
	}

	private Long createTask(String title, LocalDate dueDate, TaskPriority priority, Long projectId) {
		TaskRequestDto request = new TaskRequestDto();
		request.setTitle(title);
		request.setDueDate(dueDate);
		request.setPriority(priority);
		request.setProjectId(projectId);
		ResponseEntity<String> response = restTemplate.postForEntity("/api/tasks", request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return extractId(response.getBody());
	}

	private Long createProject(String name) {
		ProjectRequestDto request = new ProjectRequestDto();
		request.setName(name);
		ResponseEntity<String> response = restTemplate.postForEntity("/api/projects", request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return extractId(response.getBody());
	}

	private Long extractId(String responseBody) {
		String[] parts = responseBody.split(" ");
		return Long.parseLong(parts[parts.length - 1]);
	}

}
