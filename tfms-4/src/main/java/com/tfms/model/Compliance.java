package com.tfms.model;

import com.tfms.model.enums.ComplianceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Compliance Entity
 * Represents compliance and regulatory checks for trade transactions
 */
@Entity
@Table(name = "compliance")
public class Compliance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long complianceId;

    @NotBlank(message = "Transaction reference is required")
    @Size(max = 50)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    private ComplianceStatus complianceStatus = ComplianceStatus.PENDING;

    @Size(max = 2000)
    @Column(columnDefinition = "TEXT")
    private String remarks;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDate;

    private String reviewedBy;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate reviewDate;

    private String transactionType; // LC or BG

    private Boolean documentsValidated = false;
    private Boolean riskCheckPassed = false;
    private Boolean partyCheckPassed = false;
    private Boolean countryCheckPassed = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (reportDate == null) {
            reportDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Compliance() {}

    public Compliance(String transactionReference, ComplianceStatus status, String remarks) {
        this.transactionReference = transactionReference;
        this.complianceStatus = status;
        this.remarks = remarks;
        this.reportDate = LocalDate.now();
    }

    // Getters & Setters
    public Long getComplianceId() {
        return complianceId;
    }

    public void setComplianceId(Long complianceId) {
        this.complianceId = complianceId;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public ComplianceStatus getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(ComplianceStatus complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDate getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(LocalDate reviewDate) {
        this.reviewDate = reviewDate;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Boolean getDocumentsValidated() {
        return documentsValidated;
    }

    public void setDocumentsValidated(Boolean documentsValidated) {
        this.documentsValidated = documentsValidated;
    }

    public Boolean getRiskCheckPassed() {
        return riskCheckPassed;
    }

    public void setRiskCheckPassed(Boolean riskCheckPassed) {
        this.riskCheckPassed = riskCheckPassed;
    }

    public Boolean getPartyCheckPassed() {
        return partyCheckPassed;
    }

    public void setPartyCheckPassed(Boolean partyCheckPassed) {
        this.partyCheckPassed = partyCheckPassed;
    }

    public Boolean getCountryCheckPassed() {
        return countryCheckPassed;
    }

    public void setCountryCheckPassed(Boolean countryCheckPassed) {
        this.countryCheckPassed = countryCheckPassed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
