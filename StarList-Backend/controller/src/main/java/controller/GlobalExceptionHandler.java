package controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.openai.errors.RateLimitException;
import service.exceptions.ConflictException;
import service.exceptions.NotFoundException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException e, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle(e.getTitle());
        problemDetail.setDetail(e.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException e, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle(e.getTitle());
        problemDetail.setDetail(e.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getField() + " " + err.getDefaultMessage()
                ))
                .toList();

        log.warn("Validation failed [uri={}, errors={}]", request.getRequestURI(), fieldErrors.size());
        return buildValidationResponse(request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        List<Map<String, String>> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> {
                    String field = violation.getPropertyPath().toString();
                    if (field.contains(".")) {
                        field = field.substring(field.lastIndexOf('.') + 1);
                    }
                    return Map.of(
                            "field", field,
                            "message", violation.getMessage()
                    );
                })
                .toList();

        log.warn("Constraint violation [uri={}, errors={}]", request.getRequestURI(), fieldErrors.size());
        return buildValidationResponse(request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableMessage(HttpMessageNotReadableException ex,
                                                                 HttpServletRequest request) {
        log.warn("Malformed request body [uri={}]: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Malformed request body");
        problem.setDetail("The request body could not be parsed. Check that the JSON is valid and all required fields are present.");
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ProblemDetail> handleOpenAiRateLimit(RateLimitException e, HttpServletRequest request) {
        log.warn("OpenAI quota exceeded [uri={}]", request.getRequestURI());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("AI service unavailable");
        problem.setDetail("The AI service is temporarily unavailable due to quota limits. Please try again later.");
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception [uri={}]", request.getRequestURI(), e);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal server error");
        problem.setDetail("An unexpected error occurred");
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ResponseEntity<ProblemDetail> buildValidationResponse(HttpServletRequest request,
                                                                  List<Map<String, String>> fieldErrors) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request, you doofus!");
        problem.setDetail("Validation failed with " + fieldErrors.size() + " failures. Check the 'failures' field for" +
                " more information");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("failures", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
}
