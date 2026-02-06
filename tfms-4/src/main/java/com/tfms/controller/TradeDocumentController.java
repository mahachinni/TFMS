package com.tfms.controller;

import com.tfms.exception.ResourceNotFoundException;
import com.tfms.exception.UnauthorizedAccessException;
import com.tfms.model.TradeDocument;
import com.tfms.model.LetterOfCredit;
import com.tfms.model.BankGuarantee;
import com.tfms.service.TradeDocumentService;
import com.tfms.service.LetterOfCreditService;
import com.tfms.service.BankGuaranteeService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

@Controller
@RequestMapping("/documents")
public class TradeDocumentController {
    
    private final TradeDocumentService documentService;
    private final LetterOfCreditService lcService;
    private final BankGuaranteeService bgService;

    public TradeDocumentController(TradeDocumentService documentService, LetterOfCreditService lcService, BankGuaranteeService bgService) {
        this.documentService = documentService;
        this.lcService = lcService;
        this.bgService = bgService;
    }
    
    /**
     * List all documents
     */
    @GetMapping
    public String listDocuments(Model model, Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        boolean isOfficer = isOfficer(auth);

        model.addAttribute("documents", documentService.findAccessibleByUser(username, isOfficer));
        model.addAttribute("pageTitle", "Trade Documents");
        return "documents/list";
    }
    
    /**
     * Show upload form
     * - Allow any authenticated user to access the upload page, but when a tradeRef is provided
     *   require that the user is permitted to upload for that trade (creator or beneficiary) or officer.
     */
    @GetMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public String showUploadForm(@RequestParam(value = "tradeRef", required = false) String tradeRef,
                                 Model model, Authentication auth) {
        // If uploading for a trade, make sure the user is allowed (beneficiary/creator/officer)
        if (tradeRef != null && !tradeRef.isBlank()) {
            if (!canUploadForTrade(tradeRef, auth)) {
                throw new  UnauthorizedAccessException(auth != null ? auth.getName() : "anonymous", "upload tradeRef:" + tradeRef);
            }
        } else {
            // No tradeRef: require CUSTOMER or OFFICER
            boolean hasCustomerOrOfficer = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority() != null && (a.getAuthority().contains("CUSTOMER") || a.getAuthority().contains("OFFICER")));
            if (!hasCustomerOrOfficer) {
                throw new  UnauthorizedAccessException(auth != null ? auth.getName() : "anonymous", "upload standalone document");
            }
        }

        model.addAttribute("documentTypes", Arrays.asList(
                "Invoice",
                "Bill of Lading",
                "Packing List",
                "Certificate of Origin",
                "Insurance Certificate",
                "Purchase Order",
                "Shipping Documents",
                "Inspection Certificate",
                "Weight Certificate",
                "Other"
        ));
        model.addAttribute("tradeRef", tradeRef);
        model.addAttribute("pageTitle", "Upload Document");
        return "documents/upload";
    }
    
    /**
     * Process document upload
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public String uploadDocument(@RequestParam("file") MultipartFile file,
                                  @RequestParam("documentType") String documentType,
                                  @RequestParam(value = "tradeReferenceNumber", required = false) String tradeRef,
                                  @RequestParam(value = "description", required = false) String description,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload.");
                return "redirect:/documents/upload" + (tradeRef != null ? "?tradeRef=" + tradeRef : "");
            }

            // If tradeRef provided, ensure user has right to upload for that trade
            if (tradeRef != null && !tradeRef.isBlank()) {
                if (!canUploadForTrade(tradeRef, auth)) {
                    redirectAttributes.addFlashAttribute("errorMessage", "You don't have access to upload documents for this trade reference.");
                    return "redirect:/documents";
                }
            } else {
                // No tradeRef: require CUSTOMER or OFFICER
                boolean hasCustomerOrOfficer = auth != null && auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority() != null && (a.getAuthority().contains("CUSTOMER") || a.getAuthority().contains("OFFICER")));
                if (!hasCustomerOrOfficer) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Only customers and officers can upload standalone documents.");
                    return "redirect:/documents";
                }
            }

            TradeDocument document = documentService.uploadDocument(
                    file, documentType, tradeRef, description, auth.getName());

            String successMsg = "Document uploaded successfully. Reference: " + document.getReferenceNumber();
            if (tradeRef != null && !tradeRef.isEmpty()) {
                successMsg += " | Linked to: " + tradeRef;
            }
            redirectAttributes.addFlashAttribute("successMessage", successMsg);
            return "redirect:/documents/view/" + document.getDocumentId();
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload document: " + e.getMessage());
            return "redirect:/documents/upload" + (tradeRef != null ? "?tradeRef=" + tradeRef : "");
        }
    }
    
    /**
     * View document details
     */
    @GetMapping("/view/{id}")
    public String viewDocument(@PathVariable Long id, Model model, Authentication auth) {
        TradeDocument document = documentService.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("TradeDocument", "id", id));

        if (!canAccessDocument(document, auth)) {
            throw new UnauthorizedAccessException(auth != null ? auth.getName() : "anonymous", "view document:" + id);
        }
        model.addAttribute("document", document);
        model.addAttribute("pageTitle", "Document - " + document.getReferenceNumber());
        return "documents/view";
    }
    
    /**
     * Download document
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, Authentication auth) throws IOException {
        TradeDocument document = documentService.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("TradeDocument", "id", id));

        if (!canAccessDocument(document, auth)) {
            throw new UnauthorizedAccessException(auth != null ? auth.getName() : "anonymous", "download document:" + id);
        }

        Path filePath = documentService.getDocumentPath(id);
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            throw new ResourceNotFoundException("File not found for document id " + id);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + document.getFileName() + "\"")
                .body(resource);
    }
    
    /**
     * Show edit form
     */
    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public String showEditForm(@PathVariable Long id, Model model) {
        TradeDocument document = documentService.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("TradeDocument", "id", id));
        model.addAttribute("document", document);
        model.addAttribute("documentTypes", Arrays.asList(
                "Invoice", "Bill of Lading", "Packing List", "Certificate of Origin",
                "Insurance Certificate", "Purchase Order", "Shipping Documents",
                "Inspection Certificate", "Weight Certificate", "Other"
        ));
        model.addAttribute("pageTitle", "Edit Document");
        return "documents/edit";
    }
    
    /**
     * Update document details
     */
    @PostMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public String updateDocument(@PathVariable Long id,
                                  @RequestParam("documentType") String documentType,
                                  @RequestParam(value = "description", required = false) String description,
                                  RedirectAttributes redirectAttributes) {
        documentService.updateDocumentDetails(id, documentType, description);
        redirectAttributes.addFlashAttribute("successMessage", "Document updated successfully.");
        return "redirect:/documents/view/" + id;
    }
    
    /**
     * Submit for review
     */
    @PostMapping("/submit/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OFFICER')")
    public String submitForReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        documentService.submitForReview(id);
        redirectAttributes.addFlashAttribute("successMessage", "Document submitted for review.");
        return "redirect:/documents/view/" + id;
    }
    
    /**
     * Approve document - Officer only
     */
    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String approveDocument(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        documentService.approveDocument(id);
        redirectAttributes.addFlashAttribute("successMessage", "Document approved successfully.");
        return "redirect:/documents/view/" + id;
    }
    
    /**
     * Reject document - Officer only
     */
    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String rejectDocument(@PathVariable Long id,
                                  @RequestParam(required = false) String reason,
                                  RedirectAttributes redirectAttributes) {
        documentService.rejectDocument(id, reason != null ? reason : "Rejected by officer");
        redirectAttributes.addFlashAttribute("warningMessage", "Document rejected.");
        return "redirect:/documents/view/" + id;
    }
    
    /**
     * Archive document
     */
    @PostMapping("/archive/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String archiveDocument(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        documentService.archiveDocument(id);
        redirectAttributes.addFlashAttribute("successMessage", "Document archived successfully.");
        return "redirect:/documents/view/" + id;
    }
    
    /**
     * List pending review documents - Officer only
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('OFFICER')")
    public String listPendingReview(Model model) {
        model.addAttribute("documents", documentService.findPendingReview());
        model.addAttribute("pageTitle", "Documents Pending Review");
        return "documents/pending";
    }
    
    /**
     * Delete document
     */
    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('OFFICER')")
    public String deleteDocument(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            documentService.deleteDocument(id);
            redirectAttributes.addFlashAttribute("successMessage", "Document deleted successfully.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete document: " + e.getMessage());
        }
        return "redirect:/documents";
    }
    
    /**
     * Find documents by trade reference
     */
    @GetMapping("/by-trade/{tradeRef}")
    public String findByTradeReference(@PathVariable String tradeRef, Model model) {
        model.addAttribute("documents", documentService.findByTradeReference(tradeRef));
        model.addAttribute("tradeReference", tradeRef);
        model.addAttribute("pageTitle", "Documents for " + tradeRef);
        return "documents/by-trade";
    }

    /**
     * Helper to determine if current user is an officer
     */
    private boolean isOfficer(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority() != null && a.getAuthority().toUpperCase().contains("OFFICER"));
    }
    private boolean isRiskAnalyst(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority() != null && a.getAuthority().toUpperCase().contains("RISK"));
    }
    /**
     * Check whether authenticated user can upload for a given trade reference (LC or BG)
     */
    private boolean canUploadForTrade(String tradeRef, Authentication auth) {
        if (auth == null) return false;
        if (isOfficer(auth)) return true;
        String username = auth.getName();
        // Check LC
        var lcOpt = lcService.findByReferenceNumber(tradeRef);
        if (lcOpt.isPresent()) {
            LetterOfCredit lc = lcOpt.get();
            if (username != null && username.equalsIgnoreCase(lc.getCreatedBy())) return true;
            if (lc.getBeneficiaryName() != null && lc.getBeneficiaryName().equalsIgnoreCase(username)) return true;
            // check principal fullName/email if available
            Object principal = auth.getPrincipal();
            if (principal instanceof com.tfms.security.CustomUserDetailsService.CustomUserDetails) {
                var cud = (com.tfms.security.CustomUserDetailsService.CustomUserDetails) principal;
                if (cud.getUser() != null) {
                    var user = cud.getUser();
                    if (user.getFullName() != null && user.getFullName().equalsIgnoreCase(lc.getBeneficiaryName())) return true;
                    if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(lc.getBeneficiaryName())) return true;
                }
            }
            return false;
        }

        // Check BG
        var bgOpt = bgService.findByReferenceNumber(tradeRef);
        if (bgOpt.isPresent()) {
            BankGuarantee bg = bgOpt.get();
            if (username != null && username.equalsIgnoreCase(bg.getCreatedBy())) return true;
            if (bg.getBeneficiaryName() != null && bg.getBeneficiaryName().equalsIgnoreCase(username)) return true;
            Object principal = auth.getPrincipal();
            if (principal instanceof com.tfms.security.CustomUserDetailsService.CustomUserDetails) {
                var cud = (com.tfms.security.CustomUserDetailsService.CustomUserDetails) principal;
                if (cud.getUser() != null) {
                    var user = cud.getUser();
                    if (user.getFullName() != null && user.getFullName().equalsIgnoreCase(bg.getBeneficiaryName())) return true;
                    if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(bg.getBeneficiaryName())) return true;
                }
            }
            return false;
        }

        // Trade reference doesn't correspond to LC or BG -> deny
        return false;
    }

    /**
     * Check whether authenticated user can access the document
     */
    private boolean canAccessDocument(TradeDocument document, Authentication auth) {
        if (auth == null) return false;
        if (isOfficer(auth) || isRiskAnalyst(auth)) return true;
        String username = auth.getName();
        if (username != null && username.equalsIgnoreCase(document.getUploadedBy())) return true;
        String tradeRef = document.getTradeReferenceNumber();
        if (tradeRef != null && !tradeRef.isBlank()) {
            return canUploadForTrade(tradeRef, auth);
        }
        return false;
    }
}
