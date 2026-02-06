package com.tfms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.tfms.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tfms.model.BankGuarantee;
import com.tfms.model.Compliance;
import com.tfms.model.enums.ComplianceStatus;
import com.tfms.model.LetterOfCredit;
import com.tfms.model.RiskAssessment;
import com.tfms.model.TradeDocument;
import com.tfms.repository.BankGuaranteeRepository;
import com.tfms.repository.ComplianceRepository;
import com.tfms.repository.LetterOfCreditRepository;
import com.tfms.repository.RiskAssessmentRepository;
import com.tfms.repository.TradeDocumentRepository;

@Service
public class ComplianceService {

    @Autowired
    private ComplianceRepository complianceRepository;

    @Autowired
    private LetterOfCreditRepository lcRepository;

    @Autowired
    private BankGuaranteeRepository bgRepository;

    @Autowired
    private TradeDocumentRepository documentRepository;

    @Autowired
    private RiskAssessmentRepository riskRepository;

    // ========== CRUD Operations ==========

    public List<Compliance> getAllCompliances() {
        return complianceRepository.findAll();
    }

    public Compliance getComplianceById(Long id) {
        return complianceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance", "id", id));
    }

    public Compliance saveCompliance(Compliance compliance) {
        if (compliance.getComplianceId() != null) {
            Compliance existing = getComplianceById(compliance.getComplianceId());
            existing.setTransactionReference(compliance.getTransactionReference());
            existing.setComplianceStatus(compliance.getComplianceStatus());
            existing.setRemarks(compliance.getRemarks());
            existing.setReportDate(compliance.getReportDate());
            return complianceRepository.save(existing);
        } else {
            return complianceRepository.save(compliance);
        }
    }

    public void deleteCompliance(Long id) {
        complianceRepository.deleteById(id);
    }

    // ========== Dashboard Statistics ==========

    public long getCompliantCount() {
        return complianceRepository.countByComplianceStatus(ComplianceStatus.COMPLIANT);
    }

    public long getNonCompliantCount() {
        return complianceRepository.countByComplianceStatus(ComplianceStatus.NON_COMPLIANT);
    }

    public long getPendingReviewCount() {
        return complianceRepository.countByComplianceStatus(ComplianceStatus.PENDING);
    }

    public long getUnderReviewCount() {
        return complianceRepository.countByComplianceStatus(ComplianceStatus.UNDER_REVIEW);
    }

    // ========== Compliance Check Logic ==========

    /**
     * Generate compliance report for a transaction (LC or BG)
     * Performs comprehensive compliance checks and creates/updates compliance record
     */
    public Compliance generateComplianceReport(String transactionReference) {
        // Check if compliance report already exists
        Optional<Compliance> existing = complianceRepository.findByTransactionReference(transactionReference);

        Compliance compliance = existing.orElse(new Compliance());
        compliance.setTransactionReference(transactionReference);
        compliance.setReportDate(LocalDate.now());

        StringBuilder remarks = new StringBuilder();
        boolean isCompliant = true;

        // ===== CHECK 1: Validate Transaction Exists =====
        Object transaction = findTransaction(transactionReference);
        if (transaction == null) {
            compliance.setComplianceStatus(ComplianceStatus.NON_COMPLIANT);
            remarks.append("Transaction not found. ");
            return complianceRepository.save(compliance);
        }

        // Determine transaction type
        if (transaction instanceof LetterOfCredit) {
            compliance.setTransactionType("LC");
            isCompliant = checkLCCompliance((LetterOfCredit) transaction, remarks);
        } else if (transaction instanceof BankGuarantee) {
            compliance.setTransactionType("BG");
            isCompliant = checkBGCompliance((BankGuarantee) transaction, remarks);
        }

        // ===== CHECK 2: Validate Required Documents =====
        if (!checkDocumentsExist(transactionReference, remarks)) {
            isCompliant = false;
            compliance.setDocumentsValidated(false);
        } else {
            compliance.setDocumentsValidated(true);
        }

        // ===== CHECK 3: Check Risk Score =====
        if (!checkRiskScore(transactionReference, remarks)) {
            isCompliant = false;
            compliance.setRiskCheckPassed(false);
        } else {
            compliance.setRiskCheckPassed(true);
        }

        // ===== CHECK 4: Check Country Restrictions =====
        if (!checkCountryRestrictions(transactionReference, remarks)) {
            isCompliant = false;
            compliance.setCountryCheckPassed(false);
        } else {
            compliance.setCountryCheckPassed(true);
        }

        // Set final status based on all checks
        if (isCompliant) {
            compliance.setComplianceStatus(ComplianceStatus.COMPLIANT);
            if (remarks.length() == 0) {
                remarks.append("All compliance checks passed successfully.");
            } else {
                remarks.append(" All checks passed.");
            }
        } else {
            compliance.setComplianceStatus(ComplianceStatus.NON_COMPLIANT);
        }

        compliance.setRemarks(remarks.toString());
        // Mark that the automated compliance checks have been performed
        // Set reviewedBy/reviewDate so the UI shows the report has been generated
        // Only set if fields are not already set (don't override officer reviews)
        if (compliance.getReviewedBy() == null || compliance.getReviewedBy().trim().isEmpty()) {
            compliance.setReviewedBy("Automated Check");
        }
        if (compliance.getReviewDate() == null) {
            compliance.setReviewDate(LocalDate.now());
        }
        return complianceRepository.save(compliance);
    }

    /**
     * Check 1: Validate Letter of Credit
     */
    private boolean checkLCCompliance(LetterOfCredit lc, StringBuilder remarks) {
        boolean isValid = true;

        // Check LC is not expired
        if (lc.getExpiryDate() != null && lc.getExpiryDate().isBefore(LocalDate.now())) {
            remarks.append("LC has expired. ");
            isValid = false;
        }

        // Check amount is positive
        if (lc.getAmount() == null || lc.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            remarks.append("Invalid LC amount. ");
            isValid = false;
        }

        // Check beneficiary exists and is not null
        if (lc.getBeneficiaryName() == null || lc.getBeneficiaryName().trim().isEmpty()) {
            remarks.append("Beneficiary name is missing. ");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Check 1: Validate Bank Guarantee
     */
    private boolean checkBGCompliance(BankGuarantee bg, StringBuilder remarks) {
        boolean isValid = true;

        // Check validity period
        if (bg.getValidityPeriod() != null && bg.getValidityPeriod().isBefore(LocalDate.now())) {
            remarks.append("Guarantee validity period has expired. ");
            isValid = false;
        }

        // Check guarantee amount
        if (bg.getGuaranteeAmount() == null || bg.getGuaranteeAmount().compareTo(BigDecimal.ZERO) <= 0) {
            remarks.append("Invalid guarantee amount. ");
            isValid = false;
        }

        // Check beneficiary
        if (bg.getBeneficiaryName() == null || bg.getBeneficiaryName().trim().isEmpty()) {
            remarks.append("Beneficiary name is missing. ");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Check 2: Validate Required Documents Exist
     */
    private boolean checkDocumentsExist(String transactionReference, StringBuilder remarks) {
        // Fetch all documents for this transaction
        List<TradeDocument> documents = documentRepository.findByTradeReferenceNumber(transactionReference);

        if (documents == null || documents.isEmpty()) {
            remarks.append("No trade documents found for this transaction. ");
            return false;
        }

        boolean hasInvoice = false;
        boolean hasBOL = false; // Bill of Lading

        for (TradeDocument doc : documents) {
            String docType = doc.getDocumentType().toUpperCase();
            if (docType.contains("INVOICE")) {
                hasInvoice = true;
            }
            if (docType.contains("BILL OF LADING") || docType.contains("BOL")) {
                hasBOL = true;
            }
        }

        if (!hasInvoice) {
            remarks.append("Invoice document is missing. ");
            return false;
        }

        if (!hasBOL) {
            remarks.append("Bill of Lading is missing. ");
            return false;
        }

        remarks.append("Required documents present. ");
        return true;
    }

    /**
     * Check 3: Validate Risk Score
     */
    private boolean checkRiskScore(String transactionReference, StringBuilder remarks) {
        Optional<RiskAssessment> riskOpt = riskRepository.findFirstByTransactionReferenceOrderByAssessmentDateDesc(transactionReference);

        if (!riskOpt.isPresent()) {
            remarks.append("No risk assessment found. ");
            return true; // Allow if no risk assessment (can be optional)
        }

        RiskAssessment risk = riskOpt.get();
        BigDecimal riskScore = risk.getRiskScore();

        if (riskScore == null) {
            return true;
        }

        // If risk score > 70, mark as non-compliant or needs escalation
        if (riskScore.compareTo(new BigDecimal("70")) > 0) {
            remarks.append("High risk score detected (").append(riskScore).append("). Requires escalation. ");
            return false;
        }

        // Moderate risk (50-70) is acceptable but noted
        if (riskScore.compareTo(new BigDecimal("50")) > 0) {
            remarks.append("Moderate risk detected (").append(riskScore).append("). ");
        } else {
            remarks.append("Risk assessment passed. ");
        }

        return true;
    }

    /**
     * Check 4: Validate Country Restrictions
     */
    private boolean checkCountryRestrictions(String transactionReference, StringBuilder remarks) {
        Object transaction = findTransaction(transactionReference);
        String country = null;

        if (transaction instanceof LetterOfCredit) {
            // Could extract from beneficiary name or add country field
            country = "Unknown"; // In real system, would extract from beneficiary info
        } else if (transaction instanceof BankGuarantee) {
            country = "Unknown";
        }

        // Hardcoded restricted countries for demo
        String[] restrictedCountries = {"IRAN", "NORTH KOREA", "SYRIA"};

        if (country != null) {
            for (String restricted : restrictedCountries) {
                if (country.toUpperCase().contains(restricted)) {
                    remarks.append("Beneficiary country is in restricted list. ");
                    return false;
                }
            }
        }

        remarks.append("Country check passed. ");
        return true;
    }

    // ========== Helper Methods ==========

    /**
     * Find transaction by reference (LC or BG)
     */
    private Object findTransaction(String reference) {
        // Try to find as LC
        Optional<LetterOfCredit> lc = lcRepository.findByReferenceNumber(reference);
        if (lc.isPresent()) {
            return lc.get();
        }

        // Try to find as BG
        Optional<BankGuarantee> bg = bgRepository.findByReferenceNumber(reference);
        if (bg.isPresent()) {
            return bg.get();
        }

        return null;
    }

    /**
     * Submit regulatory report (updates submission status)
     */
    public Compliance submitRegulatoryReport(Long complianceId, String officerId) {
        Compliance compliance = getComplianceById(complianceId);
        compliance.setReviewedBy(officerId);
        compliance.setReviewDate(LocalDate.now());
        return complianceRepository.save(compliance);
    }

    /**
     * Legacy check method for backward compatibility
     */
    public Compliance checkCompliance(String transactionRef, String country, String goodsType) {
        Compliance compliance = new Compliance();
        compliance.setTransactionReference(transactionRef);
        compliance.setReportDate(LocalDate.now());

        if ("SANCTIONED_COUNTRY".equalsIgnoreCase(country)) {
            compliance.setComplianceStatus(ComplianceStatus.NON_COMPLIANT);
            compliance.setRemarks("Country is under sanctions");
        } else if ("RESTRICTED_GOODS".equalsIgnoreCase(goodsType)) {
            compliance.setComplianceStatus(ComplianceStatus.NON_COMPLIANT);
            compliance.setRemarks("Goods are restricted");
        } else {
            compliance.setComplianceStatus(ComplianceStatus.COMPLIANT);
            compliance.setRemarks("All regulatory checks passed");
        }

        return complianceRepository.save(compliance);
    }
}