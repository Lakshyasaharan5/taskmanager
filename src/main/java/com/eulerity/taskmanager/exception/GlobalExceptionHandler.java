package com.eulerity.taskmanager.exception;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.eulerity.taskmanager.dto.response.ErrorResponseDto;
import com.eulerity.taskmanager.dto.response.FieldErrorDto;

import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponseDto> handleResourceNotFound(ResourceNotFoundException e) {
		return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", e.getMessage(), Collections.emptyList());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException e) {
		List<FieldErrorDto> fields = e.getBindingResult().getFieldErrors().stream()
				.map(fe -> new FieldErrorDto(fe.getField(), fe.getDefaultMessage()))
				.toList();
		return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "One or more fields are invalid", fields);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponseDto> handleMalformedRequest(HttpMessageNotReadableException e) {
		if (e.getCause() instanceof InvalidFormatException invalidFormat && !invalidFormat.getPath().isEmpty()) {
			String field = invalidFormat.getPath().get(invalidFormat.getPath().size() - 1).getPropertyName();
			String message = "Invalid value '" + invalidFormat.getValue() + "' for field '" + field + "'";
			return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "One or more fields are invalid",
					List.of(new FieldErrorDto(field, message)));
		}
		return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Malformed JSON request", Collections.emptyList());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "a different type";
		String message = "'" + e.getValue() + "' is not a valid value for " + e.getName() + ", must be " + requiredType;
		return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "One or more fields are invalid",
				List.of(new FieldErrorDto(e.getName(), message)));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException e) {
		return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", e.getMessage(), Collections.emptyList());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDto> handleGeneric(Exception e) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred",
				Collections.emptyList());
	}

	private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String error, String message,
			List<FieldErrorDto> fields) {
		return ResponseEntity.status(status).body(new ErrorResponseDto(error, message, fields));
	}

}
