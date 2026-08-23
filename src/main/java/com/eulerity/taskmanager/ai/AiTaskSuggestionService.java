package com.eulerity.taskmanager.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.eulerity.taskmanager.exception.AiSuggestionException;

@Service
public class AiTaskSuggestionService {

	private static final int MAX_ATTEMPTS = 3;

	private final ChatClient chatClient;
	private final String promptTemplate;
	private final long timeoutSeconds;

	public AiTaskSuggestionService(ChatClient.Builder chatClientBuilder,
			@Value("classpath:prompts/task-suggestion-prompt.xml") Resource promptResource,
			@Value("${ai.task-suggestion.timeout-seconds}") long timeoutSeconds) throws IOException {
		this.chatClient = chatClientBuilder.build();
		this.promptTemplate = StreamUtils.copyToString(promptResource.getInputStream(), StandardCharsets.UTF_8);
		this.timeoutSeconds = timeoutSeconds;
	}

	public String getSuggestionJson(String sanitizedQuery) {
		String prompt = buildPrompt(sanitizedQuery);
		AiSuggestionException lastFailure = null;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				return callWithTimeout(prompt);
			} catch (AiSuggestionException e) {
				lastFailure = e;
			}
		}
		throw lastFailure;
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

}
