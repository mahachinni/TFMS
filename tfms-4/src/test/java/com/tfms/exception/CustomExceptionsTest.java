package com.tfms.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Custom Exception Tests")
public class CustomExceptionsTest {

    @Test
    @DisplayName("ResourceNotFoundException")
    void testResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("LetterOfCredit", "id", 1L);
        assertNotNull(ex);
        assertEquals("LetterOfCredit", ex.getResourceName());
        assertEquals("id", ex.getFieldName());
    }

    @Test
    @DisplayName("InvalidStateException")
    void testInvalidState() {
        InvalidStateException ex = new InvalidStateException("LC", "DRAFT", "approve");
        assertNotNull(ex);
        assertEquals("LC", ex.getEntityName());
        assertEquals("DRAFT", ex.getCurrentState());
    }

    @Test
    @DisplayName("ValidationException with errors")
    void testValidation() {
        ValidationException ex = new ValidationException("Validation failed");
        ex.addError("applicant", "Required");
        ex.addError("amount", "Invalid");
        assertEquals(2, ex.getErrors().size());
    }

    @Test
    @DisplayName("UnauthorizedAccessException")
    void testUnauthorized() {
        UnauthorizedAccessException ex = new UnauthorizedAccessException("user@test.com", "/admin");
        assertEquals("user@test.com", ex.getUsername());
        assertEquals("/admin", ex.getResource());
    }
}
