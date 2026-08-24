package com.eulerity.taskmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;

import com.eulerity.taskmanager.dto.request.TaskRequestDto;
import com.eulerity.taskmanager.entity.enums.TaskPriority;
import com.eulerity.taskmanager.entity.enums.TaskStatus;
import com.eulerity.taskmanager.exception.AiSuggestionException;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

	@Mock
	private ChatClient.Builder chatClientBuilder;

	@Mock
	private ChatClient chatClient;

	@Mock
	private ChatClient.ChatClientRequestSpec requestSpec;

	@Mock
	private CacheService cacheService;

	private AiService aiService;

	@BeforeEach
	void setUp() throws IOException {
		when(chatClientBuilder.build()).thenReturn(chatClient);
		when(chatClient.prompt(anyString())).thenReturn(requestSpec);
		when(requestSpec.options(any())).thenReturn(requestSpec);
		when(cacheService.get(anyString())).thenReturn(Optional.empty());

		aiService = new AiService(
				chatClientBuilder,
				new ClassPathResource("prompts/task-suggestion-prompt.xml"),
				new ClassPathResource("prompts/safety-check-prompt.xml"),
				new ObjectMapper(),
				Validation.buildDefaultValidatorFactory().getValidator(),
				5L,
				"gpt-4o",
				"gpt-4o-mini",
				cacheService,
				60L);
	}

	@Test
	void suggestTask_happyPath_returnsParsedSuggestion() {
		ChatClient.CallResponseSpec safetyResponse = mock(ChatClient.CallResponseSpec.class);
		when(safetyResponse.content()).thenReturn("{\"safe\": true, \"reason\": null}");

		ChatClient.CallResponseSpec suggestionResponse = mock(ChatClient.CallResponseSpec.class);
		when(suggestionResponse.content()).thenReturn(
				"{\"title\":\"Submit quarterly report\",\"description\":\"Finance report\",\"dueDate\":\""
						+ LocalDate.now().plusDays(5) + "\",\"priority\":\"HIGH\"}");

		when(requestSpec.call()).thenReturn(safetyResponse, suggestionResponse);

		TaskRequestDto result = aiService.suggestTask("remind me to submit the quarterly report");

		assertThat(result.getTitle()).isEqualTo("Submit quarterly report");
		assertThat(result.getDescription()).isEqualTo("Finance report");
		assertThat(result.getDueDate()).isEqualTo(LocalDate.now().plusDays(5));
		assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
		assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);
	}

	@Test
	void suggestTask_missingDueDate_appliesDefaultDueDate() {
		ChatClient.CallResponseSpec safetyResponse = mock(ChatClient.CallResponseSpec.class);
		when(safetyResponse.content()).thenReturn("{\"safe\": true, \"reason\": null}");

		ChatClient.CallResponseSpec suggestionResponse = mock(ChatClient.CallResponseSpec.class);
		when(suggestionResponse.content()).thenReturn(
				"{\"title\":\"Buy milk\",\"description\":null,\"dueDate\":null,\"priority\":\"LOW\"}");

		when(requestSpec.call()).thenReturn(safetyResponse, suggestionResponse);

		TaskRequestDto result = aiService.suggestTask("remind me to buy milk");

		assertThat(result.getTitle()).isEqualTo("Buy milk");
		assertThat(result.getDueDate()).isEqualTo(LocalDate.now().plusDays(7));
	}

	@Test
	void suggestTask_malformedSuggestionResponse_throwsAfterRetries() {
		ChatClient.CallResponseSpec safetyResponse = mock(ChatClient.CallResponseSpec.class);
		when(safetyResponse.content()).thenReturn("{\"safe\": true, \"reason\": null}");

		ChatClient.CallResponseSpec malformedResponse = mock(ChatClient.CallResponseSpec.class);
		when(malformedResponse.content()).thenReturn("this is not valid json");

		when(requestSpec.call()).thenReturn(safetyResponse, malformedResponse);

		assertThatThrownBy(() -> aiService.suggestTask("remind me to submit the quarterly report"))
				.isInstanceOf(AiSuggestionException.class);

		verify(requestSpec, times(4)).call();
	}

}
