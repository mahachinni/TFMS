package com.tfms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot context load test
 * Ensures the application context loads successfully with all configurations
 */
@SpringBootTest
@DisplayName("Application Context Tests")
public class TfmsApplicationTest {

    @Test
    @DisplayName("Should load application context successfully")
    void contextLoads() {
        // If this test passes, it means the Spring Boot application context loaded successfully
    }
}
