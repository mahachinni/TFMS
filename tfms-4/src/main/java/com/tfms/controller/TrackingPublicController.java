package com.tfms.controller;

import com.tfms.model.BankGuarantee;
import com.tfms.model.enums.DocumentStatus;
import com.tfms.model.enums.LCStatus;
import com.tfms.model.LetterOfCredit;
import com.tfms.model.TradeDocument;
import com.tfms.service.BankGuaranteeService;
import com.tfms.service.LetterOfCreditService;
import com.tfms.service.TradeDocumentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class TrackingPublicController {

    private final LetterOfCreditService lcService;
    private final BankGuaranteeService bgService;
    private final TradeDocumentService documentService;

    public TrackingPublicController(LetterOfCreditService lcService,
                                    BankGuaranteeService bgService,
                                    TradeDocumentService documentService) {
        this.lcService = lcService;
        this.bgService = bgService;
        this.documentService = documentService;
    }

    @GetMapping("/tracking")
    public String showTrackingPage(Model model) {
        model.addAttribute("pageTitle", "Track Request Status");
        return "tracking/status";
    }

    @GetMapping("/tracking/status")
    public String trackPublic(@RequestParam(name = "reference", required = false) String reference, Model model) {
        if (reference == null || reference.isBlank()) {
            model.addAttribute("pageTitle", "Track Request Status");
            return "tracking/status";
        }

        String referenceNumber = reference;
        model.addAttribute("reference", referenceNumber);

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
        return "tracking/status";
    }

    private List<TrackingController.TimelineItem> buildLCTimeline(LetterOfCredit lc) {
        List<TrackingController.TimelineItem> timeline = new ArrayList<>();
        LCStatus status = lc.getStatus();

        timeline.add(new TrackingController.TimelineItem("Created", "Draft LC created", true, lc.getCreatedAt() != null ? lc.getCreatedAt().toLocalDate().toString() : ""));
        timeline.add(new TrackingController.TimelineItem("Submitted", "Submitted for verification",
                status.ordinal() >= LCStatus.SUBMITTED.ordinal(), ""));
        timeline.add(new TrackingController.TimelineItem("Under Verification", "Being reviewed by bank officer",
                status.ordinal() >= LCStatus.UNDER_VERIFICATION.ordinal(), ""));
        timeline.add(new TrackingController.TimelineItem("Approved", "LC approved and issued",
                status == LCStatus.APPROVED || status == LCStatus.OPEN || status == LCStatus.CLOSED,
                lc.getIssueDate() != null ? lc.getIssueDate().toString() : ""));
        timeline.add(new TrackingController.TimelineItem("Active", "LC is active",
                status == LCStatus.OPEN, ""));
        timeline.add(new TrackingController.TimelineItem("Closed", "LC closed",
                status == LCStatus.CLOSED, ""));

        return timeline;
    }

    private List<TrackingController.TimelineItem> buildBGTimeline(BankGuarantee bg) {
        List<TrackingController.TimelineItem> timeline = new ArrayList<>();
        var status = bg.getStatus();

        timeline.add(new TrackingController.TimelineItem("Created", "Guarantee request created", true,
                bg.getCreatedAt() != null ? bg.getCreatedAt().toLocalDate().toString() : ""));
        timeline.add(new TrackingController.TimelineItem("Submitted", "Submitted for review",
                status.ordinal() >= bg.getStatus().SUBMITTED.ordinal(), ""));
        timeline.add(new TrackingController.TimelineItem("Under Review", "Being reviewed by bank",
                status.ordinal() >= bg.getStatus().UNDER_REVIEW.ordinal(), ""));
        timeline.add(new TrackingController.TimelineItem("Under Review", "Being reviewed by bank",
                status.ordinal() >= bg.getStatus().SENT_TO_RISK.ordinal(), ""));
        timeline.add(new TrackingController.TimelineItem("Issued", "Guarantee issued",
                status.ordinal() >= bg.getStatus().ISSUED.ordinal(),
                bg.getIssueDate() != null ? bg.getIssueDate().toString() : ""));
        timeline.add(new TrackingController.TimelineItem("Active", "Guarantee is active",
                status == bg.getStatus().ACTIVE, ""));
        timeline.add(new TrackingController.TimelineItem("Completed", "Guarantee period ended",
                status == bg.getStatus().EXPIRED || status == bg.getStatus().CLAIMED, ""));

        return timeline;
    }

    private List<TrackingController.TimelineItem> buildDocTimeline(TradeDocument doc) {
        List<TrackingController.TimelineItem> timeline = new ArrayList<>();
        DocumentStatus status = doc.getStatus();

        timeline.add(new TrackingController.TimelineItem("Uploaded", "Document uploaded", true,
                doc.getUploadDate() != null ? doc.getUploadDate().toString() : ""));
        timeline.add(new TrackingController.TimelineItem("Pending Review", "Awaiting review",
                status.ordinal() >= DocumentStatus.PENDING_REVIEW.ordinal(), ""));
        timeline.add(new TrackingController.TimelineItem("Reviewed", "Document reviewed",
                status == DocumentStatus.APPROVED || status == DocumentStatus.REJECTED, ""));
        timeline.add(new TrackingController.TimelineItem("Completed", "Process complete",
                status == DocumentStatus.APPROVED || status == DocumentStatus.ARCHIVED, ""));

        return timeline;
    }
}
