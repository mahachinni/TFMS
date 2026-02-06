package com.tfms;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for the Trade Finance Management System
 * This configuration is loaded only during test execution
 */
@Configuration
@Profile("test")
public class TestConfiguration {
    // Add test-specific beans here if needed
}
