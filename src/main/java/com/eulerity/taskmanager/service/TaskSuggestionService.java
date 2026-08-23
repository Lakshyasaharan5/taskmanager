package com.eulerity.taskmanager.service;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.eulerity.taskmanager.ai.AiTaskSuggestionService;
import com.eulerity.taskmanager.dto.request.TaskRequestDto;
import com.eulerity.taskmanager.exception.AiSuggestionException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import tools.jackson.databind.ObjectMapper;

@Service
public class TaskSuggestionService {

	private final AiTaskSuggestionService aiTaskSuggestionService;
	private final ObjectMapper objectMapper;
	private final Validator validator;

	public TaskSuggestionService(AiTaskSuggestionService aiTaskSuggestionService, ObjectMapper objectMapper,
			Validator validator) {
		this.aiTaskSuggestionService = aiTaskSuggestionService;
		this.objectMapper = objectMapper;
		this.validator = validator;
	}

	public TaskRequestDto suggestTask(String rawQuery) {
		String sanitizedQuery = sanitize(rawQuery);
		String json = aiTaskSuggestionService.getSuggestionJson(sanitizedQuery);

		TaskRequestDto suggestion;
		try {
			suggestion = objectMapper.readValue(json, TaskRequestDto.class);
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

	private String sanitize(String rawQuery) {
		String withoutControlChars = rawQuery.replaceAll("\\p{Cntrl}", " ");
		return withoutControlChars.trim().replaceAll("\\s+", " ");
	}

}
