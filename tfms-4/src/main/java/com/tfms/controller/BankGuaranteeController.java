package com.tfms.controller;

import com.tfms.exception.ResourceNotFoundException;
import com.tfms.exception.UnauthorizedAccessException;
import com.tfms.model.BankGuarantee;
import com.tfms.service.BankGuaranteeService;
import com.tfms.service.TradeDocumentService;
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

import java.security.Principal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
@Controller
@RequestMapping("/guarantee")
public class BankGuaranteeController {
    
    private final BankGuaranteeService bgService;
    private final TradeDocumentService documentService;

    public BankGuaranteeController(BankGuaranteeService bgService, TradeDocumentService documentService) {
        this.bgService = bgService;
        this.documentService = documentService;
    }
    
    /**
     * List all Bank Guarantees - only those created by the current user
     */
    @GetMapping
    public String listGuarantees(Model model, Authentication auth) {
        // Officers can see all guarantees; others see those they created or where they are beneficiary

        boolean isOfficer = auth != null && auth.getAuthorities().stream()
                .anyMatch(g -> "ROLE_OFFICER".equals(g.getAuthority()));
        if (isOfficer) {
            model.addAttribute("guarantees", bgService.findAll());
        } else {
            String username = auth != null ? auth.getName() : null;
            model.addAttribute("guarantees", bgService.findAllByUserOrBeneficiary(username));
        }
        model.addAttribute("pageTitle", "Bank Guarantees");
        return "guarantee/list";
    }
    
    /**
     * Show request guarantee form
     */
    @GetMapping("/request")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public String showRequestForm(Model model) {
        BankGuarantee bg = new BankGuarantee();
        bg.setIssueDate(LocalDate.now());
        bg.setValidityPeriod(LocalDate.now().plusMonths(6));
        model.addAttribute("guarantee", bg);
        model.addAttribute("currencies", Arrays.asList("USD", "EUR", "GBP", "JPY", "CHF", "INR", "CNY"));
        model.addAttribute("guaranteeTypes", Arrays.asList(
                "Performance Guarantee",
                "Bid Bond",
                "Advance Payment Guarantee",
                "Payment Guarantee",
                "Financial Guarantee",
                "Retention Money Guarantee",
                "Warranty Guarantee"
        ));
        model.addAttribute("pageTitle", "Request Bank Guarantee");
        return "guarantee/request";
    }
    
    /**
     * Process guarantee request
     */
    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public String requestGuarantee(@Valid @ModelAttribute("guarantee") BankGuarantee guarantee,
                                    BindingResult result,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (result.hasErrors()) {
            model.addAttribute("currencies", Arrays.asList("USD", "EUR", "GBP", "JPY", "CHF", "INR", "CNY"));
            model.addAttribute("guaranteeTypes", Arrays.asList(
                    "Performance Guarantee", "Bid Bond", "Advance Payment Guarantee",
                    "Payment Guarantee", "Financial Guarantee", "Retention Money Guarantee", "Warranty Guarantee"
            ));
            model.addAttribute("pageTitle", "Request Bank Guarantee");
            log.debug("Validation errors: {}", result.getAllErrors());
            return "guarantee/request";
        }
        
        BankGuarantee savedBg = bgService.requestGuarantee(guarantee, auth.getName());
        redirectAttributes.addFlashAttribute("successMessage", 
                "Bank Guarantee request created successfully. Reference: " + savedBg.getReferenceNumber());
        return "redirect:/guarantee/view/" + savedBg.getGuaranteeId();
    }
    
    /**
     * View guarantee details - creator or beneficiary can view; creator/beneficiary can upload docs
     */
    @GetMapping("/view/{id}")
    public String viewGuarantee(@PathVariable Long id, Model model, Authentication auth) {
        BankGuarantee bg = bgService.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("BankGuarantee", "id", id));

        // Check access: either creator or beneficiary
        if (!hasAccessToGuarantee(bg, auth)) {
            String user = auth != null ? auth.getName() : "anonymous";
            log.warn("User {} attempted unauthorized access to guarantee {}", user, id);
            throw new  UnauthorizedAccessException(user, "BankGuarantee:" + id);
        }

        // Fetch related documents
        var documents = documentService.findByTradeReference(bg.getReferenceNumber());

        // Check if user can upload documents (creator or beneficiary)
        boolean canUploadDocument = isDocumentUploadAllowed(bg, auth);

        model.addAttribute("guarantee", bg);
        model.addAttribute("documents", documents);
        model.addAttribute("canUploadDocument", canUploadDocument);
        model.addAttribute("pageTitle", "BG Details - " + bg.getReferenceNumber());
        return "guarantee/view";
    }
    
    /**
     * Show edit form - only creator can edit
     */
    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public String showEditForm(@PathVariable Long id, Model model, Authentication auth) {
        BankGuarantee bg = bgService.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("BankGuarantee", "id", id));

        if (!Objects.equals(bg.getCreatedBy(), auth.getName())) {
            throw new  UnauthorizedAccessException(auth.getName(), "edit BankGuarantee:" + id);
        }
        model.addAttribute("guarantee", bg);
        model.addAttribute("currencies", Arrays.asList("USD", "EUR", "GBP", "JPY", "CHF", "INR", "CNY"));
        model.addAttribute("guaranteeTypes", Arrays.asList(
                "Performance Guarantee", "Bid Bond", "Advance Payment Guarantee",
                "Payment Guarantee", "Financial Guarantee", "Retention Money Guarantee", "Warranty Guarantee"
        ));
        model.addAttribute("pageTitle", "Edit BG - " + bg.getReferenceNumber());
        return "guarantee/edit";
    }
    
    /**
     * Process guarantee update - only creator can update
     */
    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public String updateGuarantee(@PathVariable Long id,
                                   @Valid @ModelAttribute("guarantee") BankGuarantee guarantee,
                                   BindingResult result,
                                   RedirectAttributes redirectAttributes,
                                   Model model,
                                   Authentication auth) {
        BankGuarantee existingBg = bgService.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("BankGuarantee", "id", id));

        if (!Objects.equals(existingBg.getCreatedBy(), auth.getName())) {
            throw new  UnauthorizedAccessException(auth.getName(), "update BankGuarantee:" + id);
        }

        if (result.hasErrors()) {
            model.addAttribute("currencies", Arrays.asList("USD", "EUR", "GBP", "JPY", "CHF", "INR", "CNY"));
            model.addAttribute("guaranteeTypes", Arrays.asList(
                    "Performance Guarantee", "Bid Bond", "Advance Payment Guarantee",
                    "Payment Guarantee", "Financial Guarantee", "Retention Money Guarantee", "Warranty Guarantee"
            ));
            model.addAttribute("pageTitle", "Edit Bank Guarantee");
            return "guarantee/edit";
        }
        
        bgService.updateGuarantee(id, guarantee);
        redirectAttributes.addFlashAttribute("successMessage", "Bank Guarantee updated successfully.");
        return "redirect:/guarantee/view/" + id;
    }
    
    /**
     * Submit guarantee for review - only creator can submit
     */
    @PostMapping("/submit/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public String submitForReview(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication auth) {
        BankGuarantee bg = bgService.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("BankGuarantee", "id", id));

        if (!Objects.equals(bg.getCreatedBy(), auth.getName())) {
            throw new  UnauthorizedAccessException(auth.getName(), "submit BankGuarantee:" + id);
        }

        bgService.submitForReview(id);
        redirectAttributes.addFlashAttribute("successMessage", "Bank Guarantee submitted for review.");
        return "redirect:/guarantee/view/" + id;
    }
    
    /**
     * Issue guarantee - Officer only
     */
    @PostMapping("/issue/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String issueGuarantee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bgService.issueGuarantee(id);
        redirectAttributes.addFlashAttribute("successMessage", "Bank Guarantee issued successfully.");
        return "redirect:/guarantee/view/" + id;
    }

    /**
     * Activate guarantee - Officer only
     */
    @PostMapping("/activate/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String activateGuarantee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bgService.activateGuarantee(id);
        redirectAttributes.addFlashAttribute("successMessage", "Bank Guarantee activated successfully.");
        return "redirect:/guarantee/view/" + id;
    }

    /**
     * Send guarantee to Risk team - Officer only
     */
    @PostMapping("/send-to-risk/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String sendToRisk(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bgService.sendToRiskTeam(id);
        redirectAttributes.addFlashAttribute("successMessage", "Bank Guarantee sent to Risk team.");
        return "redirect:/guarantee/view/" + id;
    }

    /**
     * Return guarantee from Risk back to Officer - Risk role only
     */
    @PostMapping("/return-to-officer/{id}")
    @PreAuthorize("hasRole('RISK')")
    public String returnToOfficer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bgService.returnToOfficer(id);
        redirectAttributes.addFlashAttribute("successMessage", "Bank Guarantee returned to officer for review.");
        return "redirect:/guarantee/view/" + id;
    }

    /**
     * Cancel guarantee - Officer only
     */
    @PostMapping("/cancel/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String cancelGuarantee(@PathVariable Long id,
                                   @RequestParam(required = false) String reason,
                                   RedirectAttributes redirectAttributes) {
        bgService.cancelGuarantee(id, reason != null ? reason : "Cancelled by officer");
        redirectAttributes.addFlashAttribute("warningMessage", "Bank Guarantee cancelled.");
        return "redirect:/guarantee/view/" + id;
    }
    
    /**
     * Claim guarantee
     */
    @PostMapping("/claim/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String claimGuarantee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bgService.claimGuarantee(id);
        redirectAttributes.addFlashAttribute("successMessage", "Bank Guarantee claimed successfully.");
        return "redirect:/guarantee/view/" + id;
    }
    
    /**
     * List pending guarantees - Officer only
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('OFFICER')")
    public String listPendingApproval(Model model) {
        model.addAttribute("guarantees", bgService.findPendingApproval());
        model.addAttribute("pageTitle", "Pending BG Approvals");
        return "guarantee/pending";
    }
    
    /**
     * Delete guarantee - only creator can delete
     */
    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String deleteGuarantee(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication auth) {
        BankGuarantee bg = bgService.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("BankGuarantee", "id", id));

        if (!Objects.equals(bg.getCreatedBy(), auth.getName())) {
            throw new UnauthorizedAccessException(auth.getName(), "delete BankGuarantee:" + id);
        }

        bgService.deleteGuarantee(id);
        redirectAttributes.addFlashAttribute("successMessage", "Bank Guarantee deleted successfully.");
        return "redirect:/guarantee";
    }
    
    /**
     * Track guarantee status - creator or beneficiary only
     */
    @GetMapping("/track/{referenceNumber}")
    public String trackGuaranteeStatus(@PathVariable String referenceNumber, Model model, Authentication auth) {
        BankGuarantee bg = bgService.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("BankGuarantee", "referenceNumber", referenceNumber));

        if (!hasAccessToGuarantee(bg, auth)) {
            throw new  UnauthorizedAccessException(auth != null ? auth.getName() : "anonymous", "track BankGuarantee:" + referenceNumber);
        }

        model.addAttribute("guarantee", bg);
        model.addAttribute("pageTitle", "Track BG - " + referenceNumber);
        return "guarantee/track";
    }

    /**
     * Helper method to check if user has access to view guarantee
     */
    private boolean hasAccessToGuarantee(BankGuarantee bg, Authentication auth) {
        if (auth == null) return false;
        // Officers have access to all Guarantees
        boolean isOfficer = auth.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority() != null && g.getAuthority().toUpperCase().contains("OFFICER"));
        if (isOfficer) return true;
        String username = auth.getName();
        if (username != null && Objects.equals(bg.getCreatedBy(), username)) return true;

        String beneficiary = bg.getBeneficiaryName();
        if (beneficiary == null) return false;
        beneficiary = beneficiary.trim();

        // Direct username match (case-insensitive)
        if (username != null && beneficiary.equalsIgnoreCase(username)) return true;

        // If principal exposes more user info, check fullName and email
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

    /**
     * Helper method to check if user can upload documents
     */
    private boolean isDocumentUploadAllowed(BankGuarantee bg, Authentication auth) {
        return hasAccessToGuarantee(bg, auth);
    }
}
