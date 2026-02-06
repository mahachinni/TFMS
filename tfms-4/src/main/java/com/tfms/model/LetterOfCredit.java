package com.tfms.model;

import com.tfms.model.enums.LCStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Letter of Credit Entity
 * Represents a letter of credit in the trade finance system
 */
@Entity
@Table(name = "letter_of_credit")
public class LetterOfCredit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lcId;
    
    @Column(unique = true)
    private String referenceNumber;
    
    @NotBlank(message = "Applicant name is required")
    @Size(max = 100)
    private String applicantName;
    
    @NotBlank(message = "Beneficiary name is required")
    @Size(max = 100)
    private String beneficiaryName;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(precision = 15, scale = 2)
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(max = 10)
    private String currency;
    
    @NotNull(message = "Issue date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;
    
    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;
    
    @Enumerated(EnumType.STRING)
    private LCStatus status = LCStatus.DRAFT;
    
    @Size(max = 500)
    private String description;
    
    @Size(max = 200)
    private String issuingBank;
    
    @Size(max = 200)
    private String advisingBank;
    
    private String createdBy;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (referenceNumber == null) {
            referenceNumber = "LC" + System.currentTimeMillis();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public LetterOfCredit() {}

    // Getters and Setters
    public Long getLcId() {
        return lcId;
    }
    
    public void setLcId(Long lcId) {
        this.lcId = lcId;
    }
    
    public String getReferenceNumber() {
        return referenceNumber;
    }
    
    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }
    
    public String getApplicantName() {
        return applicantName;
    }
    
    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }
    
    public String getBeneficiaryName() {
        return beneficiaryName;
    }
    
    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public LocalDate getIssueDate() {
        return issueDate;
    }
    
    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }
    
    public LocalDate getExpiryDate() {
        return expiryDate;
    }
    
    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public LCStatus getStatus() {
        return status;
    }
    
    public void setStatus(LCStatus status) {
        this.status = status;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getIssuingBank() {
        return issuingBank;
    }
    
    public void setIssuingBank(String issuingBank) {
        this.issuingBank = issuingBank;
    }
    
    public String getAdvisingBank() {
        return advisingBank;
    }
    
    public void setAdvisingBank(String advisingBank) {
        this.advisingBank = advisingBank;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
