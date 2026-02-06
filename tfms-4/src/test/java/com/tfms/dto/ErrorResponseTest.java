package com.tfms.dto;

import com.tfms.model.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ErrorResponse DTO
 */
@DisplayName("ErrorResponse DTO Tests")
public class ErrorResponseTest {

    @Test
    @DisplayName("Create ErrorResponse with builder")
    void testCreation() {
        ErrorResponse response = ErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .message("Validation failed")
                .path("/api/test")
                .build();

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", response.getError());
        assertEquals("Validation failed", response.getMessage());
    }

    @Test
    @DisplayName("Create ErrorResponse with factory method")
    void testFactory() {
        ErrorResponse response = ErrorResponse.of(404, "Not Found", "Resource not found", "/api/lc/1");

        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getError());
        assertNotNull(response.getTimestamp());
    }
}
