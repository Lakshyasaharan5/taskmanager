package com.eulerity.taskmanager.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.eulerity.taskmanager.dto.request.TaskRequestDto;
import com.eulerity.taskmanager.entity.enums.TaskStatus;
import com.eulerity.taskmanager.exception.AiSuggestionException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import tools.jackson.databind.ObjectMapper;

@Service
public class AiService {

	private static final Logger log = LoggerFactory.getLogger(AiService.class);

	private static final int MAX_ATTEMPTS = 3;

	private final ChatClient chatClient;
	private final String suggestionPromptTemplate;
	private final String safetyPromptTemplate;
	private final ObjectMapper objectMapper;
	private final Validator validator;
	private final long timeoutSeconds;
	private final String suggestionModel;
	private final String safetyModel;
	private final CacheService cacheService;
	private final long cacheTtlMinutes;

	public AiService(ChatClient.Builder chatClientBuilder,
			@Value("classpath:prompts/task-suggestion-prompt.xml") Resource suggestionPromptResource,
			@Value("classpath:prompts/safety-check-prompt.xml") Resource safetyPromptResource,
			ObjectMapper objectMapper, Validator validator,
			@Value("${ai.task-suggestion.timeout-seconds}") long timeoutSeconds,
			@Value("${ai.task-suggestion.model}") String suggestionModel,
			@Value("${ai.safety-check.model}") String safetyModel,
			CacheService cacheService,
			@Value("${ai.task-suggestion.cache-ttl-minutes}") long cacheTtlMinutes) throws IOException {
		this.chatClient = chatClientBuilder.build();
		this.suggestionPromptTemplate = StreamUtils.copyToString(suggestionPromptResource.getInputStream(),
				StandardCharsets.UTF_8);
		this.safetyPromptTemplate = StreamUtils.copyToString(safetyPromptResource.getInputStream(),
				StandardCharsets.UTF_8);
		this.objectMapper = objectMapper;
		this.validator = validator;
		this.timeoutSeconds = timeoutSeconds;
		this.suggestionModel = suggestionModel;
		this.safetyModel = safetyModel;
		this.cacheService = cacheService;
		this.cacheTtlMinutes = cacheTtlMinutes;
	}

	public TaskRequestDto suggestTask(String rawQuery) {
		String sanitizedQuery = sanitize(rawQuery);
		String cacheKey = sha256Hex(sanitizedQuery);

		Optional<String> cachedResponse = cacheService.get(cacheKey);
		if (cachedResponse.isPresent()) {
			return parseAndValidate(cachedResponse.get());
		}

		checkSafety(sanitizedQuery);

		String prompt = buildSuggestionPrompt(sanitizedQuery);
		AiSuggestionException lastFailure = null;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				String response = callWithTimeout(prompt, suggestionModel);
				TaskRequestDto suggestion = parseAndValidate(response);
				cacheService.put(cacheKey, response, Duration.ofMinutes(cacheTtlMinutes));
				return suggestion;
			} catch (AiSuggestionException e) {
				lastFailure = e;
			}
		}
		throw lastFailure;
	}

	private void checkSafety(String sanitizedQuery) {
		String prompt = buildSafetyPrompt(sanitizedQuery);
		String response = callWithTimeout(prompt, safetyModel);

		SafetyCheckResult result;
		try {
			result = objectMapper.readValue(response, SafetyCheckResult.class);
		} catch (Exception e) {
			throw new AiSuggestionException("Safety check returned a response that could not be parsed");
		}

		if (!result.isSafe()) {
			String reason = result.getReason() != null ? result.getReason() : "Query violates content policy";
			throw new IllegalArgumentException(reason);
		}
	}

	private String sanitize(String rawQuery) {
		String withoutControlChars = rawQuery.replaceAll("\\p{Cntrl}", " ");
		return withoutControlChars.trim().replaceAll("\\s+", " ");
	}

	private String sha256Hex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm not available", e);
		}
	}

	private String buildSuggestionPrompt(String sanitizedQuery) {
		return suggestionPromptTemplate
				.replace("{today}", LocalDate.now().toString())
				.replace("{userInput}", escapeXml(sanitizedQuery));
	}

	private String buildSafetyPrompt(String sanitizedQuery) {
		return safetyPromptTemplate.replace("{userInput}", escapeXml(sanitizedQuery));
	}

	private String escapeXml(String input) {
		return input
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}

	private String callWithTimeout(String prompt, String model) {
		CompletableFuture<String> future = CompletableFuture.supplyAsync(
				() -> chatClient.prompt(prompt)
						.options(OpenAiChatOptions.builder().model(model))
						.call()
						.content());
		try {
			return future.get(timeoutSeconds, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			future.cancel(true);
			throw new AiSuggestionException("AI suggestion call timed out");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AiSuggestionException("AI suggestion call was interrupted");
		} catch (ExecutionException e) {
			log.error("Upstream AI call failed", e.getCause());
			throw new AiSuggestionException("AI service is currently unavailable");
		}
	}

	private TaskRequestDto parseAndValidate(String response) {
		TaskRequestDto suggestion;
		try {
			suggestion = objectMapper.readValue(response, TaskRequestDto.class);
		} catch (Exception e) {
			throw new AiSuggestionException("AI returned a response that could not be parsed into a task");
		}

		if (suggestion.getDueDate() == null) {
			suggestion.setDueDate(LocalDate.now().plusDays(7));
		}

		Set<ConstraintViolation<TaskRequestDto>> violations = validator.validate(suggestion);
		if (!violations.isEmpty()) {
			String reasons = violations.stream()
					.map(v -> v.getPropertyPath() + ": " + v.getMessage())
					.reduce((a, b) -> a + "; " + b)
					.orElse("invalid suggestion");
			throw new AiSuggestionException("AI suggestion failed validation: " + reasons);
		}

		suggestion.setStatus(TaskStatus.TODO);
		return suggestion;
	}

	private static class SafetyCheckResult {

		private boolean safe;
		private String reason;

		public boolean isSafe() {
			return safe;
		}

		public void setSafe(boolean safe) {
			this.safe = safe;
		}

		public String getReason() {
			return reason;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}

	}

}
