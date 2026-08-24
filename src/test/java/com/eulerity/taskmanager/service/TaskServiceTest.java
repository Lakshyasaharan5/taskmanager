package com.eulerity.taskmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.eulerity.taskmanager.dto.request.TaskRequestDto;
import com.eulerity.taskmanager.dto.response.PagedResponseDto;
import com.eulerity.taskmanager.dto.response.TaskResponseDto;
import com.eulerity.taskmanager.entity.Task;
import com.eulerity.taskmanager.entity.enums.TaskPriority;
import com.eulerity.taskmanager.entity.enums.TaskStatus;
import com.eulerity.taskmanager.repository.ProjectRepository;
import com.eulerity.taskmanager.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private TaskService taskService;

	@Test
	void createTask_savesAndReturnsTaskAndLogsAudit() {
		TaskRequestDto request = new TaskRequestDto();
		request.setTitle("Write tests");
		request.setDescription("Cover the service layer");
		request.setDueDate(LocalDate.now().plusDays(3));
		request.setPriority(TaskPriority.MEDIUM);

		Task saved = new Task();
		saved.setId(1L);
		saved.setTitle(request.getTitle());
		saved.setDescription(request.getDescription());
		saved.setDueDate(request.getDueDate());
		saved.setPriority(request.getPriority());
		when(taskRepository.save(any(Task.class))).thenReturn(saved);

		Task result = taskService.createTask(request);

		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getTitle()).isEqualTo("Write tests");
		assertThat(result.getPriority()).isEqualTo(TaskPriority.MEDIUM);
		verify(auditService).logCreate(1L);
	}

	@Test
	void updateTask_appliesChangesSavesAndLogsAudit() {
		Long id = 1L;
		Task existing = new Task();
		existing.setId(id);
		existing.setTitle("Old title");
		existing.setDescription("Old description");
		existing.setDueDate(LocalDate.now().plusDays(1));
		existing.setPriority(TaskPriority.LOW);
		existing.setStatus(TaskStatus.TODO);
		when(taskRepository.findById(id)).thenReturn(Optional.of(existing));

		TaskRequestDto request = new TaskRequestDto();
		request.setTitle("New title");
		request.setDescription("New description");
		request.setDueDate(LocalDate.now().plusDays(5));
		request.setPriority(TaskPriority.HIGH);
		request.setStatus(TaskStatus.IN_PROGRESS);

		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Task result = taskService.updateTask(id, request);

		assertThat(result.getTitle()).isEqualTo("New title");
		assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
		assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
		verify(auditService).logUpdate(eq(id), argThat(changes -> changes != null && !changes.isEmpty()));
	}

	@Test
	void deleteTask_deletesAndLogsAudit() {
		Long id = 1L;
		when(taskRepository.existsById(id)).thenReturn(true);

		taskService.deleteTask(id);

		verify(auditService).logDelete(id);
		verify(taskRepository).deleteById(id);
	}

	@Test
	void getTaskById_returnsMappedResponseDto() {
		Long id = 1L;
		Task task = new Task();
		task.setId(id);
		task.setTitle("Some task");
		task.setDescription("Some description");
		task.setDueDate(LocalDate.now().plusDays(2));
		task.setPriority(TaskPriority.LOW);
		task.setStatus(TaskStatus.TODO);
		when(taskRepository.findById(id)).thenReturn(Optional.of(task));

		TaskResponseDto result = taskService.getTaskById(id);

		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getTitle()).isEqualTo("Some task");
		assertThat(result.getPriority()).isEqualTo(TaskPriority.LOW);
		assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);
		assertThat(result.getProject()).isNull();
	}

	@Test
	void getFilteredTasks_returnsPagedResponse() {
		Task task = new Task();
		task.setId(1L);
		task.setTitle("Some task");
		task.setDueDate(LocalDate.now().plusDays(2));
		task.setPriority(TaskPriority.LOW);
		task.setStatus(TaskStatus.TODO);

		Pageable pageable = PageRequest.of(0, 4, Sort.by(Sort.Direction.ASC, "dueDate"));
		Page<Task> page = new PageImpl<>(List.of(task), pageable, 1);
		when(taskRepository.findAll(ArgumentMatchers.<Specification<Task>>any(), any(Pageable.class)))
				.thenReturn(page);

		PagedResponseDto<TaskResponseDto> result = taskService.getFilteredTasks(
				TaskStatus.TODO, null, null, null, null, "dueDate", "asc", 0, 4);

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getTitle()).isEqualTo("Some task");
		assertThat(result.getPage()).isZero();
		assertThat(result.getSize()).isEqualTo(4);
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getTotalPages()).isEqualTo(1);
	}

}
