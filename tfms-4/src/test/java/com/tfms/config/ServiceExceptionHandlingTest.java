package com.tfms.config;

import com.tfms.exception.InvalidStateException;
import com.tfms.model.LetterOfCredit;
import com.tfms.model.enums.LCStatus;
import com.tfms.repository.LetterOfCreditRepository;
import com.tfms.service.LetterOfCreditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Integration tests for service exception handling
 */
@SpringBootTest
@DisplayName("Service Exception Handling Tests")
public class ServiceExceptionHandlingTest {

    @Autowired
    private LetterOfCreditService lcService;

    @MockitoBean
    private LetterOfCreditRepository lcRepository;

    private LetterOfCredit testLc;

    @BeforeEach
    void setUp() {
        testLc = new LetterOfCredit();
        testLc.setLcId(1L);
        testLc.setReferenceNumber("LC-123");
        testLc.setApplicantName("Test");
        testLc.setBeneficiaryName("Beneficiary");
        testLc.setAmount(BigDecimal.valueOf(1000));
        testLc.setCurrency("USD");
        testLc.setStatus(LCStatus.DRAFT);
        testLc.setExpiryDate(LocalDate.now().plusMonths(6));
    }

    @Test
    @DisplayName("Throw exception when LC not found")
    void testThrowExceptionWhenLcNotFound() {
        when(lcRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> lcService.submitForVerification(999L));
    }

    @Test
    @DisplayName("Throw exception when amending closed LC")
    void testThrowExceptionWhenAmendingClosedLc() {
        testLc.setStatus(LCStatus.CLOSED);
        when(lcRepository.findById(1L)).thenReturn(Optional.of(testLc));
        assertThrows(InvalidStateException.class, () ->
            lcService.amendLetterOfCredit(1L, new LetterOfCredit())
        );
    }

    @Test
    @DisplayName("Throw exception when closing already closed LC")
    void testThrowExceptionWhenClosingAlreadyClosedLc() {
        testLc.setStatus(LCStatus.CLOSED);
        when(lcRepository.findById(1L)).thenReturn(Optional.of(testLc));
        assertThrows(InvalidStateException.class, () -> lcService.closeLetterOfCredit(1L));
    }
}
