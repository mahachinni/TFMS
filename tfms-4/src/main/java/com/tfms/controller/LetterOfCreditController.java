package com.tfms.controller;

import com.tfms.exception.ResourceNotFoundException;
import com.tfms.exception.UnauthorizedAccessException;
import com.tfms.model.LetterOfCredit;
import com.tfms.model.RiskAssessment;
import com.tfms.service.LetterOfCreditService;
import com.tfms.service.TradeDocumentService;
import com.tfms.service.RiskAssessmentService;
import com.tfms.security.CustomUserDetailsService.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.Objects;

@Controller
@Slf4j
@RequestMapping("/lc")
public class LetterOfCreditController {
    
    private final LetterOfCreditService lcService;
    private final TradeDocumentService documentService;
    private final RiskAssessmentService riskAssessmentService;

    public LetterOfCreditController(LetterOfCreditService lcService, TradeDocumentService documentService,
                                     RiskAssessmentService riskAssessmentService) {
        this.lcService = lcService;
        this.documentService = documentService;
        this.riskAssessmentService = riskAssessmentService;
    }
    
    /**
     * List Letters of Credit - only those created by current user
     */
    @GetMapping
    public String listLettersOfCredit(Model model, Authentication auth) {
        boolean isOfficer = auth != null && auth.getAuthorities().stream()
                .anyMatch(g -> "ROLE_OFFICER".equals(g.getAuthority()));
        if (isOfficer) {
            model.addAttribute("letterOfCredits", lcService.findAll());
        } else {
            model.addAttribute("letterOfCredits", lcService.findAllByUserOrBeneficiary(auth.getName()));
        }
        model.addAttribute("pageTitle", "Letters of Credit");
        return "lc/list";
    }
    
    /**
     * Show create LC form
     */
    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public String showCreateForm(Model model) {
        LetterOfCredit lc = new LetterOfCredit();
        lc.setIssueDate(LocalDate.now());
        lc.setExpiryDate(LocalDate.now().plusMonths(3));
        model.addAttribute("letterOfCredit", lc);
        model.addAttribute("currencies", Arrays.asList("USD", "EUR", "GBP", "JPY", "CHF", "INR", "CNY"));
        model.addAttribute("pageTitle", "Create Letter of Credit");
        return "lc/create";
    }
    
    /**
     * Process LC creation
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public String createLetterOfCredit(@Valid @ModelAttribute("letterOfCredit") LetterOfCredit lc,
                                        BindingResult result,
                                        Authentication auth,
                                        RedirectAttributes redirectAttributes,
                                        Model model) {
        if (result.hasErrors()) {
            model.addAttribute("currencies", Arrays.asList("USD", "EUR", "GBP", "JPY", "CHF", "INR", "CNY"));
            model.addAttribute("pageTitle", "Create Letter of Credit");
            log.debug("Validation errors: {}", result.getAllErrors()); return "lc/create";
        }
        
        LetterOfCredit savedLc = lcService.createLetterOfCredit(lc, auth.getName());
        redirectAttributes.addFlashAttribute("successMessage", 
                "Letter of Credit created successfully. Reference: " + savedLc.getReferenceNumber());
        return "redirect:/lc/view/" + savedLc.getLcId();
    }
    
    /**
     * View LC details - only creator or beneficiary can view; creator/beneficiary can upload docs
     */
    @GetMapping("/view/{id}")
    public String viewLetterOfCredit(@PathVariable Long id, Model model, Authentication auth) {
        LetterOfCredit lc = lcService.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("LetterOfCredit", "id", id));

        // Access check
        if (!hasAccessToLC(lc, auth)) {
            String user = auth != null ? auth.getName() : "anonymous";
            log.warn("User {} attempted unauthorized access to LC {}", user, id);
            throw new  UnauthorizedAccessException(user, "LetterOfCredit:" + id);
        }

        // Fetch related documents
        var documents = documentService.findByTradeReference(lc.getReferenceNumber());

        // Fetch risk assessment if exists
        Optional<RiskAssessment> riskAssessment = riskAssessmentService
                .findByTransactionReference(lc.getReferenceNumber());

        model.addAttribute("letterOfCredit", lc);
        model.addAttribute("documents", documents);
        model.addAttribute("canUploadDocument", isDocumentUploadAllowed(lc, auth));

        // Add risk assessment data if exists
        if (riskAssessment.isPresent()) {
            RiskAssessment risk = riskAssessment.get();
            // Convert BigDecimal to double for Thymeleaf comparison
            Double riskScoreDouble = risk.getRiskScore() != null ? risk.getRiskScore().doubleValue() : null;
            model.addAttribute("riskScore", riskScoreDouble);
            model.addAttribute("riskRemarks", risk.getRecommendations());
            log.debug("Risk assessment found for LC {}: score={}, remarks={}",
                    lc.getReferenceNumber(), riskScoreDouble, risk.getRecommendations());
        } else {
            log.debug("No risk assessment found for LC: {}", lc.getReferenceNumber());
        }

        model.addAttribute("pageTitle", "LC Details - " + lc.getReferenceNumber());
        return "lc/view";
    }
    
    /**
     * Show edit form - only creator can edit
     */
    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public String showEditForm(@PathVariable Long id, Model model, Authentication auth) {
        LetterOfCredit lc = lcService.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("LetterOfCredit", "id", id));
        if (!Objects.equals(lc.getCreatedBy(), auth.getName())) {
            throw new  UnauthorizedAccessException(auth.getName(), "edit LetterOfCredit:" + id);
        }
        model.addAttribute("letterOfCredit", lc);
        model.addAttribute("currencies", Arrays.asList("USD", "EUR", "GBP", "JPY", "CHF", "INR", "CNY"));
        model.addAttribute("pageTitle", "Edit LC - " + lc.getReferenceNumber());
        return "lc/edit";
    }
    
    /**
     * Process LC update - only creator can update
     */
    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public String updateLetterOfCredit(@PathVariable Long id,
                                        @Valid @ModelAttribute("letterOfCredit") LetterOfCredit lc,
                                        BindingResult result,
                                        RedirectAttributes redirectAttributes,
                                        Model model,
                                        Authentication auth) {
        LetterOfCredit existingLc = lcService.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("LetterOfCredit", "id", id));

        if (!Objects.equals(existingLc.getCreatedBy(), auth.getName())) {
            throw new  UnauthorizedAccessException(auth.getName(), "update LetterOfCredit:" + id);
        }

        if (result.hasErrors()) {
            model.addAttribute("currencies", Arrays.asList("USD", "EUR", "GBP", "JPY", "CHF", "INR", "CNY"));
            model.addAttribute("pageTitle", "Edit Letter of Credit");
            log.debug("Validation errors: {}", result.getAllErrors());
            return "lc/edit";
        }
        
        lcService.amendLetterOfCredit(id, lc);
        redirectAttributes.addFlashAttribute("successMessage", "Letter of Credit updated successfully.");
        return "redirect:/lc/view/" + id;
    }
    
    /**
     * Submit LC for verification - only creator can submit
     */
    @PostMapping("/submit/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public String submitForVerification(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication auth) {
        LetterOfCredit lc = lcService.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("LetterOfCredit", "id", id));

        if (!Objects.equals(lc.getCreatedBy(), auth.getName())) {
            throw new  UnauthorizedAccessException(auth.getName(), "submit LetterOfCredit:" + id);
        }

        lcService.submitForVerification(id);
        redirectAttributes.addFlashAttribute("successMessage", "Letter of Credit submitted for verification.");
        return "redirect:/lc/view/" + id;
    }
    
    /**
     * Approve LC - Officer only
     */
    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String approveLetterOfCredit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        lcService.approveLetterOfCredit(id);
        redirectAttributes.addFlashAttribute("successMessage", "Letter of Credit approved successfully.");
        return "redirect:/lc/view/" + id;
    }
    
    /**
     * Reject LC - Officer only
     */
    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String rejectLetterOfCredit(@PathVariable Long id,
                                        @RequestParam(required = false) String reason,
                                        RedirectAttributes redirectAttributes) {
        lcService.rejectLetterOfCredit(id, reason != null ? reason : "Rejected by officer");
        redirectAttributes.addFlashAttribute("warningMessage", "Letter of Credit rejected.");
        return "redirect:/lc/view/" + id;
    }
    
    /**
     * Close LC - Officer only
     */
    @PostMapping("/close/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String closeLetterOfCredit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        lcService.closeLetterOfCredit(id);
        redirectAttributes.addFlashAttribute("successMessage", "Letter of Credit closed successfully.");
        return "redirect:/lc/view/" + id;
    }
    
    /**
     * List pending LCs for approval - Officer only
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('OFFICER')")
    public String listPendingApproval(Model model) {
        model.addAttribute("letterOfCredits", lcService.findPendingApproval());
        model.addAttribute("pageTitle", "Pending LC Approvals");
        return "lc/pending";
    }
    
    /**
     * Delete LC - only creator can delete
     */
    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String deleteLetterOfCredit(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication auth) {
        LetterOfCredit lc = lcService.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("LetterOfCredit", "id", id));
        if (!Objects.equals(lc.getCreatedBy(), auth.getName())) {
            throw new  UnauthorizedAccessException(auth.getName(), "delete LetterOfCredit:" + id);
        }
        lcService.deleteLetterOfCredit(id);
        redirectAttributes.addFlashAttribute("successMessage", "Letter of Credit deleted successfully.");
        return "redirect:/lc";
    }

    /**
     * Send LC to Risk - Officer only
     */
    @PostMapping("/send-to-risk/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String sendToRisk(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        lcService.sendToRiskTeam(id);
        redirectAttributes.addFlashAttribute("successMessage", "Letter of Credit sent to Risk team.");
        return "redirect:/lc/view/" + id;
    }

    /**
     * Track guarantee status - creator or beneficiary only
     */
    @GetMapping("/track/{referenceNumber}")
    public String trackGuaranteeStatus(@PathVariable String referenceNumber, Model model, Authentication auth) {
        LetterOfCredit lc = lcService.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("LetterOfCredit", "referenceNumber", referenceNumber));

        if (!hasAccessToLC(lc, auth)) {
            throw new UnauthorizedAccessException(auth != null ? auth.getName() : "anonymous", "track LetterOfCredit:" + referenceNumber);
        }

        model.addAttribute("letterOfCredit", lc);
        model.addAttribute("pageTitle", "Track LC - " + referenceNumber);
        return "lc/track";
    }

    /**
     * Helper: check access for LC
     */
    private boolean hasAccessToLC(LetterOfCredit lc, Authentication auth) {
        if (auth == null) return false; // unauthenticated can't access
        // Officers have access to all LCs (accept authorities like 'ROLE_OFFICER' or 'OFFICER')
        boolean isOfficer = auth.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority() != null && g.getAuthority().toUpperCase().contains("OFFICER"));
        if (isOfficer) return true;
        boolean isAnalyst= auth.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority() != null && g.getAuthority().toUpperCase().contains("RISK"));
        if (isAnalyst) return true;
        String username = auth.getName();
        if (username != null && Objects.equals(lc.getCreatedBy(), username)) return true;

        String beneficiary = lc.getBeneficiaryName();
        if (beneficiary == null) return false;
        beneficiary = beneficiary.trim();

        if (username != null && beneficiary.equalsIgnoreCase(username)) return true;

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            CustomUserDetails cud = (CustomUserDetails) principal;
            if (cud.getUser() != null) {
                var user = cud.getUser();
                if (user.getFullName() != null && beneficiary.equalsIgnoreCase(user.getFullName().trim())) return true;
                if (user.getEmail() != null && beneficiary.equalsIgnoreCase(user.getEmail().trim())) return true;
            }
        }

        return false;
    }

    private boolean isDocumentUploadAllowed(LetterOfCredit lc, Authentication auth) {
        return hasAccessToLC(lc, auth);
    }
}
