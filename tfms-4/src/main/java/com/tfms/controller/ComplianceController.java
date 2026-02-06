package com.tfms.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tfms.model.Compliance;
import com.tfms.repository.LetterOfCreditRepository;
import com.tfms.repository.BankGuaranteeRepository;
import com.tfms.service.ComplianceService;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/compliance")
public class ComplianceController {

    private final ComplianceService complianceService;
    private final LetterOfCreditRepository lcRepository;
    private final BankGuaranteeRepository bgRepository;

    public ComplianceController(ComplianceService complianceService,
            LetterOfCreditRepository lcRepository,
            BankGuaranteeRepository bgRepository) {
        this.complianceService = complianceService;
        this.lcRepository = lcRepository;
        this.bgRepository = bgRepository;
    }

    // ========== HOME / DASHBOARD ==========

    @GetMapping
    public String complianceHome() {
        return "redirect:/compliance/list";
    }

    @GetMapping("/dashboard")
    public String complianceDashboard(Model model) {
        model.addAttribute("pageTitle", "Compliance Dashboard");
        model.addAttribute("compliantCount", complianceService.getCompliantCount());
        model.addAttribute("nonCompliantCount", complianceService.getNonCompliantCount());
        model.addAttribute("pendingReviewCount", complianceService.getPendingReviewCount());
        model.addAttribute("underReviewCount", complianceService.getUnderReviewCount());
        return "compliance/dashboard";
    }

    // ========== LIST ALL COMPLIANCE REPORTS ==========

    @GetMapping("/list")
    public String listCompliance(Model model) {
        model.addAttribute("pageTitle", "Compliance Reports");
        model.addAttribute("complianceList", complianceService.getAllCompliances());
        model.addAttribute("compliantCount", complianceService.getCompliantCount());
        model.addAttribute("nonCompliantCount", complianceService.getNonCompliantCount());
        model.addAttribute("pendingReviewCount", complianceService.getPendingReviewCount());
        model.addAttribute("underReviewCount", complianceService.getUnderReviewCount());
        return "compliance/list";
    }

    // ========== GENERATE COMPLIANCE REPORT ==========

    @GetMapping("/generate")
    public String showGenerateForm(Model model) {
        model.addAttribute("pageTitle", "Generate Compliance Report");

        // Collect all LC and BG references for selection
        List<String> transactionReferences = new ArrayList<>();

        // Add LC references
        lcRepository.findAll().forEach(lc -> {
            if (lc.getReferenceNumber() != null) {
                transactionReferences.add(lc.getReferenceNumber() + " (LC)");
            }
        });

        // Add BG references
        bgRepository.findAll().forEach(bg -> {
            if (bg.getReferenceNumber() != null) {
                transactionReferences.add(bg.getReferenceNumber() + " (BG)");
            }
        });

        model.addAttribute("transactions", transactionReferences);
        return "compliance/generate";
    }

    @PostMapping("/generate")
    public String generateReport(
            @RequestParam String transactionReference,
            RedirectAttributes redirectAttributes) {

        try {
            // Remove the transaction type suffix if present
            String cleanRef = transactionReference.contains(" (")
                    ? transactionReference.substring(0, transactionReference.lastIndexOf(" ("))
                    : transactionReference;

            Compliance compliance = complianceService.generateComplianceReport(cleanRef);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Compliance report generated successfully.");
            return "redirect:/compliance/view/" + compliance.getComplianceId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error generating compliance report: " + e.getMessage());
            return "redirect:/compliance/generate";
        }
    }

    // ========== VIEW COMPLIANCE REPORT ==========

    @GetMapping("/view/{id}")
    public String viewCompliance(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Compliance Report");
        model.addAttribute("compliance", complianceService.getComplianceById(id));
        return "compliance/view";
    }

    // ========== SUBMIT REGULATORY REPORT ==========

    @PostMapping("/submit/{id}")
    public String submitReport(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            String officerId = authentication != null ? authentication.getName() : "Unknown Officer";
            complianceService.submitRegulatoryReport(id, officerId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Compliance report submitted to regulator successfully.");
            return "redirect:/compliance/view/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error submitting report: " + e.getMessage());
            return "redirect:/compliance/view/" + id;
        }
    }

    // ========== EDIT COMPLIANCE REPORT (Legacy) ==========

    @GetMapping("/edit/{id}")
    public String editCompliance(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Edit Compliance Report");
        model.addAttribute("compliance", complianceService.getComplianceById(id));
        return "compliance/compliance_form";
    }

    @PostMapping("/save")
    public String saveCompliance(
            @ModelAttribute("compliance") Compliance compliance,
            RedirectAttributes redirectAttributes) {

        complianceService.saveCompliance(compliance);
        redirectAttributes.addFlashAttribute("successMessage", "Compliance report saved successfully.");
        return "redirect:/compliance/list";
    }

    // ========== DELETE COMPLIANCE REPORT ==========

    @GetMapping("/delete/{id}")
    public String deleteCompliance(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        complianceService.deleteCompliance(id);
        redirectAttributes.addFlashAttribute("successMessage", "Compliance report deleted successfully.");
        return "redirect:/compliance/list";
    }



}