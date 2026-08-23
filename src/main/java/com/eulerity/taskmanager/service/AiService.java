package com.eulerity.taskmanager.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.eulerity.taskmanager.dto.request.TaskRequestDto;
import com.eulerity.taskmanager.exception.AiSuggestionException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import tools.jackson.databind.ObjectMapper;

@Service
public class AiService {

	private static final int MAX_ATTEMPTS = 3;

	private final ChatClient chatClient;
	private final String promptTemplate;
	private final ObjectMapper objectMapper;
	private final Validator validator;
	private final long timeoutSeconds;

	public AiService(ChatClient.Builder chatClientBuilder,
			@Value("classpath:prompts/task-suggestion-prompt.xml") Resource promptResource,
			ObjectMapper objectMapper, Validator validator,
			@Value("${ai.task-suggestion.timeout-seconds}") long timeoutSeconds) throws IOException {
		this.chatClient = chatClientBuilder.build();
		this.promptTemplate = StreamUtils.copyToString(promptResource.getInputStream(), StandardCharsets.UTF_8);
		this.objectMapper = objectMapper;
		this.validator = validator;
		this.timeoutSeconds = timeoutSeconds;
	}

	public TaskRequestDto suggestTask(String rawQuery) {
		String prompt = buildPrompt(sanitize(rawQuery));

		AiSuggestionException lastFailure = null;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				String response = callWithTimeout(prompt);
				return parseAndValidate(response);
			} catch (AiSuggestionException e) {
				lastFailure = e;
			}
		}
		throw lastFailure;
	}

	private String sanitize(String rawQuery) {
		String withoutControlChars = rawQuery.replaceAll("\\p{Cntrl}", " ");
		return withoutControlChars.trim().replaceAll("\\s+", " ");
	}

	private String buildPrompt(String sanitizedQuery) {
		return promptTemplate
				.replace("{today}", LocalDate.now().toString())
				.replace("{userInput}", escapeXml(sanitizedQuery));
	}

	private String escapeXml(String input) {
		return input
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}

	private String callWithTimeout(String prompt) {
		CompletableFuture<String> future = CompletableFuture.supplyAsync(
				() -> chatClient.prompt(prompt).call().content());
		try {
			return future.get(timeoutSeconds, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			future.cancel(true);
			throw new AiSuggestionException("AI suggestion call timed out");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AiSuggestionException("AI suggestion call was interrupted");
		} catch (ExecutionException e) {
			throw new AiSuggestionException("AI suggestion call failed: " + e.getCause().getMessage());
		}
	}

	private TaskRequestDto parseAndValidate(String response) {
		TaskRequestDto suggestion;
		try {
			suggestion = objectMapper.readValue(response, TaskRequestDto.class);
		} catch (Exception e) {
			throw new AiSuggestionException("AI returned a response that could not be parsed into a task");
		}

		Set<ConstraintViolation<TaskRequestDto>> violations = validator.validate(suggestion);
		if (!violations.isEmpty()) {
			String reasons = violations.stream()
					.map(v -> v.getPropertyPath() + ": " + v.getMessage())
					.reduce((a, b) -> a + "; " + b)
					.orElse("invalid suggestion");
			throw new AiSuggestionException("AI suggestion failed validation: " + reasons);
		}

		return suggestion;
	}

}
