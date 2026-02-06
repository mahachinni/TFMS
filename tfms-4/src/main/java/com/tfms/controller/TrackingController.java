package com.tfms.controller;

import com.tfms.model.*;
import com.tfms.model.enums.DocumentStatus;
import com.tfms.model.enums.GuaranteeStatus;
import com.tfms.model.enums.LCStatus;
import com.tfms.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/track")
public class TrackingController {
    
    private final LetterOfCreditService lcService;
    private final BankGuaranteeService bgService;
    private final TradeDocumentService documentService;
    
    public TrackingController(LetterOfCreditService lcService,
                               BankGuaranteeService bgService,
                               TradeDocumentService documentService) {
        this.lcService = lcService;
        this.bgService = bgService;
        this.documentService = documentService;
    }
    
    @GetMapping
    public String showTrackingForm(Model model) {
        model.addAttribute("pageTitle", "Track Request Status");
        return "track/search";
    }
    
    @GetMapping("/search")
    public String trackRequest(@RequestParam("referenceNumber") String referenceNumber, Model model) {
        model.addAttribute("referenceNumber", referenceNumber);
        
        // Try to find LC
        if (referenceNumber.startsWith("LC")) {
            lcService.findByReferenceNumber(referenceNumber).ifPresent(lc -> {
                model.addAttribute("transactionType", "Letter of Credit");
                model.addAttribute("transaction", lc);
                model.addAttribute("status", lc.getStatus().getDisplayName());
                model.addAttribute("timeline", buildLCTimeline(lc));
                // normalized view model attributes
                model.addAttribute("applicant", lc.getApplicantName());
                model.addAttribute("beneficiary", lc.getBeneficiaryName());
                model.addAttribute("currency", lc.getCurrency());
                model.addAttribute("amount", lc.getAmount());
                model.addAttribute("createdDate", lc.getCreatedAt());
                model.addAttribute("expiryDate", lc.getExpiryDate());
                model.addAttribute("id", lc.getLcId());
            });
        }
        // Try to find BG
        else if (referenceNumber.startsWith("BG")) {
            bgService.findByReferenceNumber(referenceNumber).ifPresent(bg -> {
                model.addAttribute("transactionType", "Bank Guarantee");
                model.addAttribute("transaction", bg);
                model.addAttribute("status", bg.getStatus().getDisplayName());
                model.addAttribute("timeline", buildBGTimeline(bg));
                // normalized view model attributes
                model.addAttribute("applicant", bg.getApplicantName());
                model.addAttribute("beneficiary", bg.getBeneficiaryName());
                model.addAttribute("currency", bg.getCurrency());
                model.addAttribute("amount", bg.getGuaranteeAmount());
                model.addAttribute("createdDate", bg.getCreatedAt());
                model.addAttribute("expiryDate", bg.getValidityPeriod());
                model.addAttribute("id", bg.getGuaranteeId());
            });
        }
        // Try to find Document
        else if (referenceNumber.startsWith("DOC")) {
            documentService.findByReferenceNumber(referenceNumber).ifPresent(doc -> {
                model.addAttribute("transactionType", "Trade Document");
                model.addAttribute("transaction", doc);
                model.addAttribute("status", doc.getStatus().getDisplayName());
                model.addAttribute("timeline", buildDocTimeline(doc));
                // normalized view model attributes
                model.addAttribute("applicant", doc.getUploadedBy());
                model.addAttribute("beneficiary", "");
                model.addAttribute("currency", "");
                model.addAttribute("amount", null);
                model.addAttribute("createdDate", doc.getUploadDate());
                model.addAttribute("expiryDate", null);
                model.addAttribute("id", doc.getDocumentId());
            });
        }
        
        if (!model.containsAttribute("transaction")) {
            model.addAttribute("errorMessage", "No transaction found with reference: " + referenceNumber);
        }
        
        model.addAttribute("pageTitle", "Track Status - " + referenceNumber);
        return "track/result";
    }
    
    private List<TimelineItem> buildLCTimeline(LetterOfCredit lc) {
        List<TimelineItem> timeline = new ArrayList<>();
        LCStatus status = lc.getStatus();
        
        timeline.add(new TimelineItem("Created", "Draft LC created", true, lc.getCreatedAt() != null ? lc.getCreatedAt().toLocalDate().toString() : ""));
        timeline.add(new TimelineItem("Submitted", "Submitted for verification", 
                status.ordinal() >= LCStatus.SUBMITTED.ordinal(), ""));
        timeline.add(new TimelineItem("Under Verification", "Being reviewed by bank officer", 
                status.ordinal() >= LCStatus.UNDER_VERIFICATION.ordinal(), ""));
        timeline.add(new TimelineItem("Approved", "LC approved and issued", 
                status == LCStatus.APPROVED || status == LCStatus.OPEN || status == LCStatus.CLOSED, 
                lc.getIssueDate() != null ? lc.getIssueDate().toString() : ""));
        timeline.add(new TimelineItem("Active", "LC is active", 
                status == LCStatus.OPEN, ""));
        timeline.add(new TimelineItem("Closed", "LC closed", 
                status == LCStatus.CLOSED, ""));
        
        return timeline;
    }
    
    private List<TimelineItem> buildBGTimeline(BankGuarantee bg) {
        List<TimelineItem> timeline = new ArrayList<>();
        GuaranteeStatus status = bg.getStatus();
        
        timeline.add(new TimelineItem("Created", "Guarantee request created", true, 
                bg.getCreatedAt() != null ? bg.getCreatedAt().toLocalDate().toString() : ""));
        timeline.add(new TimelineItem("Submitted", "Submitted for review", 
                status.ordinal() >= GuaranteeStatus.SUBMITTED.ordinal(), ""));
        timeline.add(new TimelineItem("Under Review", "Being reviewed by bank", 
                status.ordinal() >= GuaranteeStatus.UNDER_REVIEW.ordinal(), ""));
        timeline.add(new TimelineItem("Issued", "Guarantee issued", 
                status.ordinal() >= GuaranteeStatus.ISSUED.ordinal(), 
                bg.getIssueDate() != null ? bg.getIssueDate().toString() : ""));
        timeline.add(new TimelineItem("Active", "Guarantee is active", 
                status == GuaranteeStatus.ACTIVE, ""));
        timeline.add(new TimelineItem("Completed", "Guarantee period ended", 
                status == GuaranteeStatus.EXPIRED || status == GuaranteeStatus.CLAIMED, ""));
        
        return timeline;
    }
    
    private List<TimelineItem> buildDocTimeline(TradeDocument doc) {
        List<TimelineItem> timeline = new ArrayList<>();
        DocumentStatus status = doc.getStatus();
        
        timeline.add(new TimelineItem("Uploaded", "Document uploaded", true, 
                doc.getUploadDate() != null ? doc.getUploadDate().toString() : ""));
        timeline.add(new TimelineItem("Pending Review", "Awaiting review", 
                status.ordinal() >= DocumentStatus.PENDING_REVIEW.ordinal(), ""));
        timeline.add(new TimelineItem("Reviewed", "Document reviewed", 
                status == DocumentStatus.APPROVED || status == DocumentStatus.REJECTED, ""));
        timeline.add(new TimelineItem("Completed", "Process complete", 
                status == DocumentStatus.APPROVED || status == DocumentStatus.ARCHIVED, ""));
        
        return timeline;
    }
    
    /**
     * Timeline item for status tracking
     */
    public static class TimelineItem {
        private String title;
        private String description;
        private boolean completed;
        private String date;
        // new field to match Thymeleaf template expectation of 'current'
        private boolean current;

        public TimelineItem(String title, String description, boolean completed, String date) {
            this.title = title;
            this.description = description;
            this.completed = completed;
            this.date = date;
            this.current = false; // default to false so existing call sites remain unchanged
        }
        
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public boolean isCompleted() { return completed; }
        public String getDate() { return date; }
        // public getter for 'current' property for SpringEL / Thymeleaf
        public boolean isCurrent() { return current; }
        // provide a setter in case callers want to mark an item as current
        public void setCurrent(boolean current) { this.current = current; }
    }
}
