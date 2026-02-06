package com.tfms.config;

import com.tfms.model.ErrorResponse;
import com.tfms.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
public class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Mock
    private WebRequest request;

    @Test
    @DisplayName("Handle ResourceNotFoundException")
    void testResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("LetterOfCredit", "id", 1L);
        when(request.getDescription(false)).thenReturn("uri=/lc/1");

        ResponseEntity<ErrorResponse> response = (ResponseEntity<ErrorResponse>) handler.handleResourceNotFoundException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
    }

    @Test
    @DisplayName("Handle InvalidStateException")
    void testInvalidState() {
        InvalidStateException ex = new InvalidStateException("Cannot perform operation");
        when(request.getDescription(false)).thenReturn("uri=/test");

        ResponseEntity<ErrorResponse> response = (ResponseEntity<ErrorResponse>) handler.handleInvalidStateException(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
    }

    @Test
    @DisplayName("Handle ValidationException")
    void testValidation() {
        ValidationException ex = new ValidationException("Validation failed");
        when(request.getDescription(false)).thenReturn("uri=/test");

        ResponseEntity<ErrorResponse> response = (ResponseEntity<ErrorResponse>) handler.handleValidationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
    }

    @Test
    @DisplayName("Handle UnauthorizedAccessException")
    void testUnauthorized() {
        UnauthorizedAccessException ex = new UnauthorizedAccessException("user@test.com", "/admin");
        when(request.getDescription(false)).thenReturn("uri=/admin");

        ResponseEntity<ErrorResponse> response = (ResponseEntity<ErrorResponse>) handler.handleUnauthorizedAccessException(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getStatus());
    }

    @Test
    @DisplayName("Handle generic Exception")
    void testGlobalException() {
        Exception ex = new Exception("Unexpected error");
        when(request.getDescription(false)).thenReturn("uri=/test");

        ResponseEntity<ErrorResponse> response = (ResponseEntity<ErrorResponse>) handler.handleGlobalException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
    }
}
