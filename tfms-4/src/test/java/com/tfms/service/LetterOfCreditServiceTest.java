package com.tfms.service;

import com.tfms.model.LetterOfCredit;
import com.tfms.model.enums.LCStatus;
import com.tfms.repository.LetterOfCreditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LetterOfCreditService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LetterOfCredit Service Tests")
public class LetterOfCreditServiceTest {

    @Mock
    private LetterOfCreditRepository lcRepository;

    @InjectMocks
    private LetterOfCreditService lcService;

    private LetterOfCredit testLc;

    @BeforeEach
    void setUp() {
        testLc = new LetterOfCredit();
        testLc.setLcId(1L);
        testLc.setReferenceNumber("LC-1234567890");
        testLc.setApplicantName("Test Applicant");
        testLc.setBeneficiaryName("Test Beneficiary");
        testLc.setAmount(BigDecimal.valueOf(10000.00));
        testLc.setCurrency("USD");
        testLc.setStatus(LCStatus.DRAFT);
        testLc.setExpiryDate(LocalDate.now().plusMonths(6));
        testLc.setCreatedBy("user@test.com");
    }

    @Test
    @DisplayName("Should create a new Letter of Credit successfully")
    void testCreateLetterOfCreditSuccess() {
        LetterOfCredit newLc = new LetterOfCredit();
        newLc.setApplicantName("Test Applicant");
        newLc.setBeneficiaryName("Test Beneficiary");
        newLc.setAmount(BigDecimal.valueOf(10000.00));
        newLc.setCurrency("USD");

        when(lcRepository.save(any(LetterOfCredit.class))).thenReturn(testLc);

        LetterOfCredit result = lcService.createLetterOfCredit(newLc, "user@test.com");

        assertNotNull(result);
        assertEquals("Test Applicant", result.getApplicantName());
        assertEquals(LCStatus.DRAFT, result.getStatus());
        verify(lcRepository, times(1)).save(any(LetterOfCredit.class));
    }

    @Test
    @DisplayName("Should find Letter of Credit by ID successfully")
    void testFindByIdSuccess() {
        when(lcRepository.findById(1L)).thenReturn(Optional.of(testLc));

        Optional<LetterOfCredit> result = lcService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("LC-1234567890", result.get().getReferenceNumber());
        verify(lcRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should find Letter of Credit by reference number")
    void testFindByReferenceNumberSuccess() {
        when(lcRepository.findByReferenceNumber("LC-1234567890")).thenReturn(Optional.of(testLc));

        Optional<LetterOfCredit> result = lcService.findByReferenceNumber("LC-1234567890");

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getLcId());
        verify(lcRepository, times(1)).findByReferenceNumber("LC-1234567890");
    }

    @Test
    @DisplayName("Should submit LC for verification successfully")
    void testSubmitForVerificationSuccess() {
        testLc.setStatus(LCStatus.DRAFT);
        when(lcRepository.findById(1L)).thenReturn(Optional.of(testLc));
        when(lcRepository.save(any(LetterOfCredit.class))).thenReturn(testLc);

        LetterOfCredit result = lcService.submitForVerification(1L);

        assertEquals(LCStatus.SUBMITTED, result.getStatus());
        verify(lcRepository, times(1)).save(any(LetterOfCredit.class));
    }

    @Test
    @DisplayName("Should throw exception when submitting closed LC for verification")
    void testSubmitClosedLcThrowsException() {
        testLc.setStatus(LCStatus.CLOSED);
        when(lcRepository.findById(1L)).thenReturn(Optional.of(testLc));

        assertThrows(IllegalStateException.class,
            () -> lcService.submitForVerification(1L));

        verify(lcRepository, never()).save(any(LetterOfCredit.class));
    }

    @Test
    @DisplayName("Should approve LC successfully")
    void testApproveLcSuccess() {
        testLc.setStatus(LCStatus.SUBMITTED);
        when(lcRepository.findById(1L)).thenReturn(Optional.of(testLc));
        when(lcRepository.save(any(LetterOfCredit.class))).thenReturn(testLc);

        LetterOfCredit result = lcService.approveLetterOfCredit(1L);

        assertEquals(LCStatus.APPROVED, result.getStatus());
        assertNotNull(result.getIssueDate());
        verify(lcRepository, times(1)).save(any(LetterOfCredit.class));
    }

    @Test
    @DisplayName("Should throw exception when approving LC with invalid status")
    void testApproveLcWithInvalidStatusThrowsException() {
        testLc.setStatus(LCStatus.DRAFT);
        when(lcRepository.findById(1L)).thenReturn(Optional.of(testLc));

        assertThrows(IllegalStateException.class,
            () -> lcService.approveLetterOfCredit(1L));

        verify(lcRepository, never()).save(any(LetterOfCredit.class));
    }

    @Test
    @DisplayName("Should reject LC successfully")
    void testRejectLcSuccess() {
        testLc.setStatus(LCStatus.SUBMITTED);
        when(lcRepository.findById(1L)).thenReturn(Optional.of(testLc));
        when(lcRepository.save(any(LetterOfCredit.class))).thenReturn(testLc);

        LetterOfCredit result = lcService.rejectLetterOfCredit(1L, "Invalid documents");

        assertEquals(LCStatus.REJECTED, result.getStatus());
        assertTrue(result.getDescription().contains("Rejection Reason: Invalid documents"));
        verify(lcRepository, times(1)).save(any(LetterOfCredit.class));
    }

    @Test
    @DisplayName("Should close LC successfully")
    void testCloseLcSuccess() {
        testLc.setStatus(LCStatus.OPEN);
        when(lcRepository.findById(1L)).thenReturn(Optional.of(testLc));
        when(lcRepository.save(any(LetterOfCredit.class))).thenReturn(testLc);

        LetterOfCredit result = lcService.closeLetterOfCredit(1L);

        assertEquals(LCStatus.CLOSED, result.getStatus());
        verify(lcRepository, times(1)).save(any(LetterOfCredit.class));
    }

    @Test
    @DisplayName("Should throw exception when closing already closed LC")
    void testCloseAlreadyClosedLcThrowsException() {
        testLc.setStatus(LCStatus.CLOSED);
        when(lcRepository.findById(1L)).thenReturn(Optional.of(testLc));

        assertThrows(IllegalStateException.class,
            () -> lcService.closeLetterOfCredit(1L));

        verify(lcRepository, never()).save(any(LetterOfCredit.class));
    }

    @Test
    @DisplayName("Should amend LC successfully with valid future expiry date")
    void testAmendLcSuccess() {
        testLc.setStatus(LCStatus.APPROVED);
        LetterOfCredit updatedLc = new LetterOfCredit();
        updatedLc.setApplicantName("Updated Applicant");
        updatedLc.setExpiryDate(LocalDate.now().plusMonths(12));

        when(lcRepository.findById(1L)).thenReturn(Optional.of(testLc));
        when(lcRepository.save(any(LetterOfCredit.class))).thenReturn(testLc);

        LetterOfCredit result = lcService.amendLetterOfCredit(1L, updatedLc);

        assertEquals(LCStatus.AMENDED, result.getStatus());
        verify(lcRepository, times(1)).save(any(LetterOfCredit.class));
    }

    @Test
    @DisplayName("Should throw exception when amending LC with past expiry date")
    void testAmendLcWithPastExpiryDateThrowsException() {
        testLc.setStatus(LCStatus.APPROVED);
        LetterOfCredit updatedLc = new LetterOfCredit();
        updatedLc.setExpiryDate(LocalDate.now().minusDays(1));

        when(lcRepository.findById(1L)).thenReturn(Optional.of(testLc));

        assertThrows(IllegalArgumentException.class,
            () -> lcService.amendLetterOfCredit(1L, updatedLc));

        verify(lcRepository, never()).save(any(LetterOfCredit.class));
    }

    @Test
    @DisplayName("Should find all LCs successfully")
    void testFindAllSuccess() {
        List<LetterOfCredit> lcList = Arrays.asList(testLc);
        when(lcRepository.findAllOrderByCreatedAtDesc()).thenReturn(lcList);

        List<LetterOfCredit> result = lcService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(lcRepository, times(1)).findAllOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Should find LCs by status")
    void testFindByStatusSuccess() {
        List<LetterOfCredit> lcList = Arrays.asList(testLc);
        when(lcRepository.findByStatus(LCStatus.DRAFT)).thenReturn(lcList);

        List<LetterOfCredit> result = lcService.findByStatus(LCStatus.DRAFT);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(LCStatus.DRAFT, result.get(0).getStatus());
        verify(lcRepository, times(1)).findByStatus(LCStatus.DRAFT);
    }

    @Test
    @DisplayName("Should find pending approval LCs")
    void testFindPendingApprovalSuccess() {
        testLc.setStatus(LCStatus.SUBMITTED);
        List<LetterOfCredit> lcList = Arrays.asList(testLc);
        when(lcRepository.findByStatusIn(any())).thenReturn(lcList);

        List<LetterOfCredit> result = lcService.findPendingApproval();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(lcRepository, times(1)).findByStatusIn(any());
    }

    @Test
    @DisplayName("Should count LCs by status")
    void testCountByStatusSuccess() {
        when(lcRepository.countByStatus(LCStatus.DRAFT)).thenReturn(5L);

        long result = lcService.countByStatus(LCStatus.DRAFT);

        assertEquals(5L, result);
        verify(lcRepository, times(1)).countByStatus(LCStatus.DRAFT);
    }

    @Test
    @DisplayName("Should delete LC successfully")
    void testDeleteLetterOfCreditSuccess() {
        lcService.deleteLetterOfCredit(1L);

        verify(lcRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should update LC successfully")
    void testUpdateLetterOfCreditSuccess() {
        when(lcRepository.save(any(LetterOfCredit.class))).thenReturn(testLc);

        LetterOfCredit result = lcService.updateLetterOfCredit(testLc);

        assertNotNull(result);
        assertEquals("LC-1234567890", result.getReferenceNumber());
        verify(lcRepository, times(1)).save(any(LetterOfCredit.class));
    }
}
