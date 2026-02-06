package com.tfms.controller;

import com.tfms.model.*;
import com.tfms.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {
    
    private final LetterOfCreditService lcService;
    private final BankGuaranteeService bgService;
    private final TradeDocumentService documentService;
    private final RiskAssessmentService riskService;

    public DashboardController(LetterOfCreditService lcService,
                                BankGuaranteeService bgService,
                                TradeDocumentService documentService,
                                RiskAssessmentService riskService) {
        this.lcService = lcService;
        this.bgService = bgService;
        this.documentService = documentService;
        this.riskService = riskService;
    }
    
    @GetMapping("/")
    public String home(Authentication auth) {
        // If the current user is in the RISK role, send them to the risk dashboard directly
        boolean isRisk = auth != null && auth.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority() != null && g.getAuthority().contains("RISK"));
        if (isRisk) {
            return "redirect:/risk/dashboard";
        }
        return "redirect:/dashboard";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        String username = auth != null ? auth.getName() : "";
        boolean isOfficer = auth != null && auth.getAuthorities().stream()
                .anyMatch(g -> "ROLE_OFFICER".equals(g.getAuthority()));
        boolean isRisk = auth != null && auth.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority() != null && g.getAuthority().contains("RISK"));

        // If a risk user navigates to /dashboard, redirect them to the dedicated risk dashboard
        if (isRisk) {
            return "redirect:/risk/dashboard";
        }

        // Common statistics - for officer show totals, for others show scoped counts
        if (isOfficer) {
            model.addAttribute("totalLCs", lcService.findAll().size());
            model.addAttribute("totalGuarantees", bgService.findAll().size());
            model.addAttribute("totalDocuments", documentService.findAll().size());
        } else {
            model.addAttribute("totalLCs", lcService.findByCreatedBy(username).size());
            model.addAttribute("totalGuarantees", bgService.findByCreatedBy(username).size());
            model.addAttribute("totalDocuments", documentService.findByUploadedBy(username).size());
        }

        // Beneficiary-specific lists: show where the current user is beneficiary
        List<LetterOfCredit> beneficiaryLCs = lcService.findByBeneficiaryName(username);
        List<BankGuarantee> beneficiaryGuarantees = bgService.findAllByUserOrBeneficiary(username).stream()
                .filter(bg -> bg.getBeneficiaryName() != null && bg.getBeneficiaryName().toLowerCase().contains(username.toLowerCase()))
                .toList();

        model.addAttribute("beneficiaryLCs", beneficiaryLCs.stream().limit(5).toList());
        model.addAttribute("beneficiaryLCCount", beneficiaryLCs.size());
        model.addAttribute("beneficiaryGuarantees", beneficiaryGuarantees.stream().limit(5).toList());
        model.addAttribute("beneficiaryBGCount", beneficiaryGuarantees.size());

        // Role-specific data
        if (isOfficer) {
            // Officer sees pending approvals and recent items
            model.addAttribute("pendingLCs", lcService.findPendingApproval().size());
            model.addAttribute("pendingGuarantees", bgService.findPendingApproval().size());
            model.addAttribute("pendingDocuments", documentService.findPendingReview().size());
            model.addAttribute("recentLCs", lcService.findAll().stream().limit(5).toList());
            model.addAttribute("recentGuarantees", bgService.findAll().stream().limit(5).toList());
            // show number of LCs currently sent to risk (for officer awareness)
            model.addAttribute("lcsSentToRiskCount", riskService.countLcsSentToRisk());
        } else {
            // Customer sees their requests
            model.addAttribute("myLCs", lcService.findByCreatedBy(username).size());
            model.addAttribute("myGuarantees", bgService.findByCreatedBy(username).size());
            model.addAttribute("myDocuments", documentService.findAccessibleByUser(username, false).size());
            model.addAttribute("recentLCs", lcService.findByCreatedBy(username).stream().limit(5).toList());
            model.addAttribute("recentGuarantees", bgService.findByCreatedBy(username).stream().limit(5).toList());
        }
        
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("currentUser", username);
        model.addAttribute("userRole", auth != null && auth.getAuthorities().iterator().hasNext() ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "") : "");

        return "dashboard";
    }
    
    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("pageTitle", "Access Denied");
        return "error/access-denied";
    }
}
