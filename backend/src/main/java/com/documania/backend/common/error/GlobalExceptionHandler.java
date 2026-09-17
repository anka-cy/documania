package com.documania.backend.common.error;

import com.documania.backend.common.exception.BusinessRuleException;
import com.documania.backend.common.exception.LoginLockedException;
import com.documania.backend.common.exception.RateLimitExceededException;
import com.documania.backend.common.exception.ResourceNotFoundException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.ValueInstantiationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String GENERIC_500 = "Erreur interne du serveur. Réessayez plus tard.";
    private static final String ACCESS_DENIED = "Vous n'avez pas la permission d'effectuer cette action.";
    private static final String INVALID_REQUEST = "Format de requête invalide.";
    private static final String METHOD_NOT_ALLOWED = "Méthode HTTP non supportée pour cette ressource.";
    private static final String MEDIA_TYPE_NOT_SUPPORTED = "Type de contenu non supporté. Utilisez application/json.";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(
        ResourceNotFoundException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(
        BusinessRuleException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
        AuthenticationException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Identifiants invalides", request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
        AccessDeniedException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, ACCESS_DENIED, request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(LoginLockedException.class)
    public ResponseEntity<ApiError> handleLoginLocked(
        LoginLockedException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> handleRateLimitExceeded(
        RateLimitExceededException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
        MethodArgumentTypeMismatchException exception,
        HttpServletRequest request
    ) {
        String message = "Valeur invalide pour le paramètre : " + exception.getName();
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Les données envoyées sont invalides", request.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
            fieldErrors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage())
        );
        return buildResponse(HttpStatus.BAD_REQUEST, "Les paramètres envoyés sont invalides", request.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMessageNotReadable(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        String message = INVALID_REQUEST;
        Throwable cause = exception.getCause();
        if (cause instanceof InvalidFormatException ife) {
            String fieldName = ife.getPath().stream()
                .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : String.valueOf(ref.getIndex()))
                .reduce((a, b) -> a + "." + b)
                .orElse("valeur");
            message = "Valeur invalide pour le champ : " + fieldName;
        } else if (cause instanceof ValueInstantiationException) {
            message = "Valeur invalide dans le corps de la requête.";
        }
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(
        HttpRequestMethodNotSupportedException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED, request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotSupported(
        HttpMediaTypeNotSupportedException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, MEDIA_TYPE_NOT_SUPPORTED, request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(
        MissingServletRequestParameterException exception,
        HttpServletRequest request
    ) {
        String message = "Le paramètre requis est manquant : " + exception.getParameterName();
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
        DataIntegrityViolationException exception,
        HttpServletRequest request
    ) {
        log.warn("Contrainte d'intégrité violée sur {} : {}", request.getRequestURI(), exception.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Conflit avec les données existantes.", request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(
        NoResourceFoundException exception,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, "Ressource introuvable.", request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
        Exception exception,
        HttpServletRequest request
    ) {
        String traceId = UUID.randomUUID().toString();
        log.error("Erreur non gérée [traceId={}] sur {} : {}", traceId, request.getRequestURI(), exception.getMessage(), exception);
        return buildResponseWithTraceId(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_500, request.getRequestURI(), Map.of(), traceId);
    }

    private ResponseEntity<ApiError> buildResponse(
        HttpStatus status,
        String message,
        String path,
        Map<String, String> fieldErrors
    ) {
        return buildResponseWithTraceId(status, message, path, fieldErrors, null);
    }

    private ResponseEntity<ApiError> buildResponseWithTraceId(
        HttpStatus status,
        String message,
        String path,
        Map<String, String> fieldErrors,
        String traceId
    ) {
        ApiError apiError = new ApiError(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            path,
            fieldErrors,
            traceId
        );
        return ResponseEntity.status(status).body(apiError);
    }
}