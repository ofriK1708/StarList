package controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import service.exceptions.ConflictException;
import service.exceptions.NotFoundException;

import java.net.URI;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<ProblemDetail> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<Map<String, String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getField() + " " + err.getDefaultMessage()
                ))
                .toList();

        return getValidationProblemDetailResponseEntity(request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<Map<String, String>> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> {
                    // Extract the last node in the property path
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

        return getValidationProblemDetailResponseEntity(request, fieldErrors);
    }

    @NonNull
    private ResponseEntity<ProblemDetail> getValidationProblemDetailResponseEntity(HttpServletRequest request,
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
