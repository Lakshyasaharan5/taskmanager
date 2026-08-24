package com.eulerity.taskmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eulerity.taskmanager.dto.request.ProjectRequestDto;
import com.eulerity.taskmanager.dto.response.ProjectResponseDto;
import com.eulerity.taskmanager.entity.Project;
import com.eulerity.taskmanager.entity.Task;
import com.eulerity.taskmanager.entity.enums.TaskPriority;
import com.eulerity.taskmanager.entity.enums.TaskStatus;
import com.eulerity.taskmanager.repository.ProjectRepository;
import com.eulerity.taskmanager.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private TaskRepository taskRepository;

	@InjectMocks
	private ProjectService projectService;

	@Test
	void createProject_savesAndReturnsProject() {
		ProjectRequestDto request = new ProjectRequestDto();
		request.setName("New Project");
		request.setDescription("Project description");

		Project saved = new Project();
		saved.setId(1L);
		saved.setName(request.getName());
		saved.setDescription(request.getDescription());
		when(projectRepository.save(any(Project.class))).thenReturn(saved);

		Project result = projectService.createProject(request);

		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("New Project");
		assertThat(result.getDescription()).isEqualTo("Project description");
	}

	@Test
	void deleteProject_deletesWhenNoAssociatedTasks() {
		Long id = 1L;
		Project project = new Project();
		project.setId(id);
		project.setName("Project to delete");
		when(projectRepository.findById(id)).thenReturn(Optional.of(project));
		when(taskRepository.existsByProjectId(id)).thenReturn(false);

		projectService.deleteProject(id);

		verify(projectRepository).delete(project);
	}

	@Test
	void getAllProjects_returnsMappedResponseDtosWithTaskSummaries() {
		Task task = new Task();
		task.setId(10L);
		task.setTitle("Task in project");
		task.setDueDate(LocalDate.now().plusDays(1));
		task.setPriority(TaskPriority.LOW);
		task.setStatus(TaskStatus.TODO);

		Project project = new Project();
		project.setId(1L);
		project.setName("Project with tasks");
		project.setDescription("Has one task");
		project.setTasks(List.of(task));

		when(projectRepository.findAll()).thenReturn(List.of(project));

		List<ProjectResponseDto> result = projectService.getAllProjects();

		assertThat(result).hasSize(1);
		ProjectResponseDto dto = result.get(0);
		assertThat(dto.getId()).isEqualTo(1L);
		assertThat(dto.getName()).isEqualTo("Project with tasks");
		assertThat(dto.getTasks()).hasSize(1);
		assertThat(dto.getTasks().get(0).getId()).isEqualTo(10L);
		assertThat(dto.getTasks().get(0).getTitle()).isEqualTo("Task in project");
	}

}
