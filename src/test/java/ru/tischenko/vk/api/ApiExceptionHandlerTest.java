package ru.tischenko.vk.api;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import ru.tischenko.vk.service.Exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void shouldReturn401ForAuthError() {
        var response = handler.auth(new BadCredentialsException("bad"));
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void shouldReturn403ForAccessDenied() {
        var response = handler.denied(new AccessDeniedException("denied"));
        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void shouldReturn409ForBusinessRuleViolation() {
        var response = handler.business(new Exceptions.BusinessRuleException("rule"));
        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void shouldReturn404ForNotFound() {
        var response = handler.notFound(new Exceptions.NotFoundException("missing"));
        assertEquals(404, response.getStatusCode().value());
    }
}
