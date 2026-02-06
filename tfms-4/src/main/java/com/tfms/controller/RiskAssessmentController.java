package com.tfms.controller;

import com.tfms.exception.ResourceNotFoundException;
import com.tfms.model.RiskAssessment;
import com.tfms.model.enums.RiskLevel;
import com.tfms.service.RiskAssessmentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;

import java.util.Arrays;

@Controller
@RequestMapping("/risk")
public class RiskAssessmentController {
    
    private final RiskAssessmentService riskService;
    
    public RiskAssessmentController(RiskAssessmentService riskService) {
        this.riskService = riskService;
    }

    /**
     * List all risk assessments
     */
    @GetMapping
    public String listRiskAssessments(Model model) {
        // Redirect root /risk to the dashboard which includes the queue preview
        return "redirect:/risk/dashboard";
    }

    @GetMapping("/update/{id}")
    public String showUpdateForm(@PathVariable("id") Long id, Model model) {
        var riskAssessment = riskService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Risk with id "+id+" doesn't exist"));
        model.addAttribute("riskAssessment", riskAssessment);
        return "risk/update"; // returns src/main/resources/templates/risk/update.html
    }
    /**
     * Show risk analysis form
     */
    @GetMapping("/analyze")
    public String showAnalyzeForm(Model model,
                                  @RequestParam(value = "transactionReference", required = false) String transactionReference,
                                  @RequestParam(value = "transactionType", required = false) String transactionType) {
        // ensure a target object is present for Thymeleaf form binding
        RiskAssessment ra = new RiskAssessment();
        if (transactionReference != null) ra.setTransactionReference(transactionReference);
        if (transactionType != null) ra.setTransactionType(transactionType);
        model.addAttribute("riskAssessment", ra);
        model.addAttribute("transactionTypes", Arrays.asList("LC", "BG", "Trade Document"));
        model.addAttribute("pageTitle", "Analyze Risk");
        return "risk/analyze";
    }
    
    /**
     * Process risk analysis
     */
    @PostMapping("/analyze")
    public String analyzeRisk(@ModelAttribute RiskAssessment riskAssessment,
                               BindingResult bindingResult,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            if (bindingResult.hasErrors()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please correct the form errors.");
                return "redirect:/risk/analyze";
            }

            RiskAssessment assessment = riskService.analyzeRisk(
                    riskAssessment,
                    auth.getName());

            redirectAttributes.addFlashAttribute("successMessage", 
                    "Risk analysis completed. Risk Level: " + assessment.getRiskLevel().getDisplayName());
            return "redirect:/risk/view/" + assessment.getRiskId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Failed to analyze risk: " + e.getMessage());
            return "redirect:/risk/analyze";
        }
    }
    
    /**
     * View risk assessment details
     */
    @GetMapping("/view/{id}")
    public String viewRiskAssessment(@PathVariable Long id, Model model) {
        RiskAssessment assessment = riskService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RiskAssessment", "id", id));
        model.addAttribute("assessment", assessment);
        // add a synonym attribute expected by the Thymeleaf template
        model.addAttribute("riskAssessment", assessment);
        model.addAttribute("pageTitle", "Risk Assessment - " + assessment.getTransactionReference());
        return "risk/view";
    }
    
    /**
     * Get risk score for a transaction (for display in other views)
     */
    @GetMapping("/score/{transactionReference}")
    public String getRiskScore(@PathVariable String transactionReference, Model model) {
        RiskAssessment assessment = riskService.findByTransactionReference(transactionReference)
                .orElse(null);
        model.addAttribute("assessment", assessment);
        model.addAttribute("transactionReference", transactionReference);
        model.addAttribute("pageTitle", "Risk Score - " + transactionReference);
        return "risk/score";
    }
    
    /**
     * Update remarks
     */
    @PostMapping("/remarks/{id}")
    public String updateRemarks(@PathVariable Long id,
                                 @RequestParam("remarks") String remarks,
                                 RedirectAttributes redirectAttributes) {
        riskService.updateRemarks(id, remarks);
        redirectAttributes.addFlashAttribute("successMessage", "Remarks updated successfully.");
        return "redirect:/risk/view/" + id;
    }
    
    /**
     * List high risk assessments
     */
    @GetMapping("/high-risk")
    public String listHighRiskAssessments(Model model) {
        model.addAttribute("assessments", riskService.findHighRiskAssessments());
        model.addAttribute("pageTitle", "High Risk Assessments");
        return "risk/high-risk";
    }
    
    /**
     * Filter by risk level
     */
    @GetMapping("/level/{level}")
    public String filterByLevel(@PathVariable String level, Model model) {
        try {
            RiskLevel riskLevel = RiskLevel.valueOf(level.toUpperCase());
            model.addAttribute("assessments", riskService.findByRiskLevel(riskLevel));
            model.addAttribute("selectedLevel", riskLevel);
            model.addAttribute("pageTitle", riskLevel.getDisplayName() + " Risk Assessments");
        } catch (IllegalArgumentException e) {
            model.addAttribute("assessments", riskService.findAll());
            model.addAttribute("pageTitle", "All Risk Assessments");
        }
        return "risk/list";
    }
    
    /**
     * Risk dashboard with statistics
     */
    @GetMapping("/dashboard")
    public String riskDashboard(Model model) {
        model.addAttribute("totalAssessments", riskService.findAll().size());
        model.addAttribute("lowRiskCount", riskService.countByRiskLevel(RiskLevel.LOW));
        model.addAttribute("mediumRiskCount", riskService.countByRiskLevel(RiskLevel.MEDIUM));
        model.addAttribute("highRiskCount", riskService.countByRiskLevel(RiskLevel.HIGH));
        model.addAttribute("criticalRiskCount", riskService.countByRiskLevel(RiskLevel.CRITICAL));
        model.addAttribute("averageRiskScore", riskService.getAverageRiskScore());
        model.addAttribute("riskAssessments", riskService.findAll());
         // Add LCs sent to risk info
         model.addAttribute("lcsSentToRiskCount", riskService.countLcsSentToRisk());
         // show a short list of LCs in the risk queue for quick access
         model.addAttribute("recentLcsSentToRisk", riskService.findLcsSentToRisk().stream().limit(5).toList());
         // Add BGs sent to risk info
         model.addAttribute("bgsSentToRiskCount", riskService.countBgsSentToRisk());
         model.addAttribute("recentBgsSentToRisk", riskService.findBgsSentToRisk().stream().limit(5).toList());
         model.addAttribute("pageTitle", "Risk Dashboard");
         return "risk/dashboard";
     }

     /**
      * List LC queue that have been sent to Risk team
      */
     @GetMapping("/queue")
     public String riskQueue(Model model) {
         model.addAttribute("lcs", riskService.findLcsSentToRisk());
         model.addAttribute("pageTitle", "LCs Sent to Risk");
         return "risk/queue";
     }

    /**
     * List BG queue that have been sent to Risk team
     */
    @GetMapping("/queue-bg")
    public String riskQueueBg(Model model) {
        model.addAttribute("bgs", riskService.findBgsSentToRisk());
        model.addAttribute("pageTitle", "BGs Sent to Risk");
        return "risk/queue-bg";
    }

     /**
      * Delete risk assessment
      */
     @PostMapping("/delete/{id}")
     public String deleteAssessment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
         riskService.deleteAssessment(id);
         redirectAttributes.addFlashAttribute("successMessage", "Risk assessment deleted successfully.");
         return "redirect:/risk";
     }
}
