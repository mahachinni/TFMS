package com.tfms.service;

import com.tfms.model.BankGuarantee;
import com.tfms.repository.BankGuaranteeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankGuarantee Service Tests")
public class BankGuaranteeServiceTest {

    @Mock
    private BankGuaranteeRepository bgRepository;

    @InjectMocks
    private BankGuaranteeService bgService;

    private BankGuarantee testBg;

    @BeforeEach
    void setUp() {
        testBg = new BankGuarantee();
        testBg.setGuaranteeId(1L);
        testBg.setReferenceNumber("BG-123");
        testBg.setApplicantName("Test");
    }



    @Test
    @DisplayName("Should find by ID")
    void testFindById() {
        when(bgRepository.findById(1L)).thenReturn(java.util.Optional.of(testBg));
        var result = bgService.findById(1L);
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Should find all")
    void testFindAll() {
        when(bgRepository.findAllOrderByCreatedAtDesc()).thenReturn(Arrays.asList(testBg));
        var result = bgService.findAll();
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
