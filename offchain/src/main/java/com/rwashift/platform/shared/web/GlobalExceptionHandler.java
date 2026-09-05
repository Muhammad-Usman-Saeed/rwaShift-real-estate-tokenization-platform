package com.rwashift.platform.shared.web;

import com.rwashift.platform.shared.domain.DomainException;
import com.rwashift.platform.shared.domain.InvalidStateTransitionException;
import com.rwashift.platform.shared.domain.NotFoundException;
import com.rwashift.platform.shared.security.TenantAccessDeniedException;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates every exception surfaced by a unit's REST adapter into an RFC 9457
 * ({@code application/problem+json}) body, using Spring's native {@link ProblemDetail}. Every
 * unit's controllers should let exceptions propagate here rather than building error responses
 * themselves, so the wire format stays consistent platform-wide.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final URI TYPE_NOT_FOUND = URI.create("https://rwashift.com/problems/not-found");
    private static final URI TYPE_VALIDATION = URI.create("https://rwashift.com/problems/validation-error");
    private static final URI TYPE_STATE_CONFLICT = URI.create("https://rwashift.com/problems/invalid-state-transition");
    private static final URI TYPE_DOMAIN_RULE = URI.create("https://rwashift.com/problems/domain-rule-violation");
    private static final URI TYPE_TENANT_DENIED = URI.create("https://rwashift.com/problems/tenant-access-denied");
    private static final URI TYPE_MALFORMED_BODY = URI.create("https://rwashift.com/problems/malformed-request-body");
    private static final URI TYPE_DEPENDENT_RECORDS = URI.create("https://rwashift.com/problems/dependent-records-exist");

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest request) {
        log.warn("Not found: {} {} - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ProblemDetail problem = base(HttpStatus.NOT_FOUND, ex.getMessage(), request);
        problem.setType(TYPE_NOT_FOUND);
        problem.setTitle("Resource not found");
        return problem;
    }

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ProblemDetail handleTenantDenied(TenantAccessDeniedException ex, HttpServletRequest request) {
        log.warn("Tenant access denied: {} {} - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ProblemDetail problem = base(HttpStatus.FORBIDDEN, ex.getMessage(), request);
        problem.setType(TYPE_TENANT_DENIED);
        problem.setTitle("Tenant access denied");
        return problem;
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidStateTransitionException ex, HttpServletRequest request) {
        log.warn("Invalid state transition: {} {} - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ProblemDetail problem = base(HttpStatus.CONFLICT, ex.getMessage(), request);
        problem.setType(TYPE_STATE_CONFLICT);
        problem.setTitle("Invalid state transition");
        return problem;
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex, HttpServletRequest request) {
        log.warn("Domain rule violation: {} {} - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ProblemDetail problem = base(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
        problem.setType(TYPE_DOMAIN_RULE);
        problem.setTitle("Domain rule violation");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Validation failed: {} {} - {}", request.getMethod(), request.getRequestURI(), fieldErrors);
        ProblemDetail problem = base(HttpStatus.BAD_REQUEST, "Request payload failed validation", request);
        problem.setType(TYPE_VALIDATION);
        problem.setTitle("Validation failed");
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    /**
     * Safety net for a delete blocked by a foreign key this endpoint didn't explicitly check for
     * (an aggregate can have several kinds of dependents — see e.g. {@code
     * OrganizationApplicationService#deleteOrganization}, which checks assets explicitly but
     * can't practically enumerate every other unit's FK). Surfaces a clear message instead of a
     * raw constraint-violation 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Data integrity violation: {} {}", request.getMethod(), request.getRequestURI(), ex);
        ProblemDetail problem = base(HttpStatus.CONFLICT,
                "This record cannot be deleted because other records still reference it.", request);
        problem.setType(TYPE_DEPENDENT_RECORDS);
        problem.setTitle("Dependent records exist");
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedBody(HttpServletRequest request) {
        log.warn("Malformed request body: {} {}", request.getMethod(), request.getRequestURI());
        ProblemDetail problem = base(HttpStatus.BAD_REQUEST, "Request body could not be parsed", request);
        problem.setType(TYPE_MALFORMED_BODY);
        problem.setTitle("Malformed request body");
        return problem;
    }

    private ProblemDetail base(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        String traceId = Span.current().getSpanContext().getTraceId();
        if (traceId != null && !"00000000000000000000000000000000".equals(traceId)) {
            problem.setProperty("traceId", traceId);
        }
        Object requestCorrelationId = request.getAttribute("correlationId");
        if (requestCorrelationId != null) {
            problem.setProperty("correlationId", requestCorrelationId);
        }
        return problem;
    }
}
