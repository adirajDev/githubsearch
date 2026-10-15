package com.adidev.githubsearch.web;

import com.adidev.githubsearch.domain.InvalidParameterException;
import com.adidev.githubsearch.github.exception.GitHubInvalidQueryException;
import com.adidev.githubsearch.github.exception.GitHubRateLimitException;
import com.adidev.githubsearch.github.exception.GitHubTimeoutException;
import com.adidev.githubsearch.github.exception.GitHubUpstreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- request validation (Spring MVC exceptions, overridden for a consistent shape) ----------

    /** Invalid @RequestBody fields. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.merge(error.getField(), String.valueOf(error.getDefaultMessage()), (a, b) -> a + "; " + b));
        return handleExceptionInternal(ex, validationProblem(errors), headers, status, request);
    }

    /** Invalid @RequestParam values (@Min, @Pattern, ...). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String name = parameterName(result.getMethodParameter());
            result.getResolvableErrors().forEach(error ->
                    errors.merge(name, String.valueOf(error.getDefaultMessage()), (a, b) -> a + "; " + b));
        }
        return handleExceptionInternal(ex, validationProblem(errors), headers, status, request);
    }

    /** e.g. minStars=abc */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String name = ex.getPropertyName() != null ? ex.getPropertyName() : "parameter";
        String message = "has invalid value '" + ex.getValue() + "'";
        return handleExceptionInternal(ex, validationProblem(Map.of(name, message)), headers, status, request);
    }

    /** Missing or malformed JSON body. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON");
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    // ---------- our own exceptions ----------

    @ExceptionHandler(InvalidParameterException.class)
    ProblemDetail handleInvalidParameter(InvalidParameterException ex) {
        return validationProblem(Map.of(ex.getParameter(), ex.getMessage()));
    }

    @ExceptionHandler(GitHubInvalidQueryException.class)
    ProblemDetail handleInvalidQuery(GitHubInvalidQueryException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid search query");
        return problem;
    }

    @ExceptionHandler(GitHubRateLimitException.class)
    ResponseEntity<ProblemDetail> handleRateLimit(GitHubRateLimitException ex) {
        long retryAfter = ex.getRetryAfterSeconds();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "GitHub API rate limit exceeded. Retry after " + retryAfter + " seconds.");
        problem.setProperty("retryAfterSeconds", retryAfter);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter))
                .body(problem);
    }

    @ExceptionHandler(GitHubTimeoutException.class)
    ProblemDetail handleTimeout(GitHubTimeoutException ex) {
        log.warn("GitHub request timed out", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT,
                "GitHub did not respond in time. Please try again.");
    }

    @ExceptionHandler(GitHubUpstreamException.class)
    ProblemDetail handleUpstream(GitHubUpstreamException ex) {
        log.error("GitHub upstream failure: {}", ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "GitHub is unavailable or returned an invalid response. Please try again later.");
    }

    @ExceptionHandler(DataAccessException.class)
    ProblemDetail handleDatabase(DataAccessException ex) {
        log.error("Database error", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The database is temporarily unavailable. Please try again later.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    // ---------- helpers ----------

    private static ProblemDetail validationProblem(Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    private static String parameterName(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null && !requestParam.name().isEmpty()) {
            return requestParam.name();
        }
        String name = parameter.getParameterName();
        return name != null ? name : "parameter" + parameter.getParameterIndex();
    }
}