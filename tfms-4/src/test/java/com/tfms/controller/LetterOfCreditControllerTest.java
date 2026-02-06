package com.tfms.controller;

import com.tfms.model.LetterOfCredit;
import com.tfms.model.enums.LCStatus;
import com.tfms.service.LetterOfCreditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LetterOfCreditController
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Letter of Credit Controller Tests")
public class LetterOfCreditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
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
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Should display LC list page")
    void testListLettersOfCreditPage() throws Exception {
        when(lcService.findAll()).thenReturn(Arrays.asList(testLc));

        mockMvc.perform(get("/lc"))
                .andExpect(status().isOk())
                .andExpect(view().name("lc/list"))
                .andExpect(model().attributeExists("letterOfCredits"))
                .andExpect(model().attribute("letterOfCredits", hasSize(1)));

        verify(lcService, times(1)).findAll();
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Should display LC details page")
    void testViewLcDetails() throws Exception {
        when(lcService.findById(1L)).thenReturn(Optional.of(testLc));

        mockMvc.perform(get("/lc/view/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("lc/view"))
                .andExpect(model().attribute("letterOfCredit", testLc));

        verify(lcService, times(1)).findById(1L);
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Should return 404 when LC not found")
    void testViewNonExistentLc() throws Exception {
        when(lcService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/lc/view/999"))
                .andExpect(status().isNotFound());

        verify(lcService, times(1)).findById(999L);
    }
}
