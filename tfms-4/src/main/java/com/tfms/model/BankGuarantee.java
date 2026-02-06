package com.tfms.model;

import com.tfms.model.enums.GuaranteeStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bank Guarantee Entity
 * Represents a bank guarantee in the trade finance system
 */
@Entity
@Table(name = "bank_guarantee")
public class BankGuarantee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long guaranteeId;
    
    @Column(unique = true)
    private String referenceNumber;
    
    @NotBlank(message = "Applicant name is required")
    @Size(max = 100)
    private String applicantName;
    
    @NotBlank(message = "Beneficiary name is required")
    @Size(max = 100)
    private String beneficiaryName;
    
    @NotNull(message = "Guarantee amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(precision = 15, scale = 2)
    private BigDecimal guaranteeAmount;
    
    @NotBlank(message = "Currency is required")
    @Size(max = 10)
    private String currency;
    
    @NotBlank(message = "Guarantee type is required")
    private String guaranteeType;
    
    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;
    
    @NotNull(message = "Validity period end date is required")
    private LocalDate validityPeriod;
    
    @Enumerated(EnumType.STRING)
    private GuaranteeStatus status = GuaranteeStatus.DRAFT;
    
    @Size(max = 500)
    private String purpose;
    
    @Size(max = 200)
    private String issuingBank;
    
    private String createdBy;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createdAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (referenceNumber == null) {
            referenceNumber = "BG" + System.currentTimeMillis();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public BankGuarantee() {}
    
    // Getters and Setters
    public Long getGuaranteeId() {
        return guaranteeId;
    }
    
    public void setGuaranteeId(Long guaranteeId) {
        this.guaranteeId = guaranteeId;
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
    
    public BigDecimal getGuaranteeAmount() {
        return guaranteeAmount;
    }
    
    public void setGuaranteeAmount(BigDecimal guaranteeAmount) {
        this.guaranteeAmount = guaranteeAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getGuaranteeType() {
        return guaranteeType;
    }
    
    public void setGuaranteeType(String guaranteeType) {
        this.guaranteeType = guaranteeType;
    }
    
    public LocalDate getIssueDate() {
        return issueDate;
    }
    
    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }
    
    public LocalDate getValidityPeriod() {
        return validityPeriod;
    }
    
    public void setValidityPeriod(LocalDate validityPeriod) {
        this.validityPeriod = validityPeriod;
    }
    
    public GuaranteeStatus getStatus() {
        return status;
    }
    
    public void setStatus(GuaranteeStatus status) {
        this.status = status;
    }
    
    public String getPurpose() {
        return purpose;
    }
    
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
    
    public String getIssuingBank() {
        return issuingBank;
    }
    
    public void setIssuingBank(String issuingBank) {
        this.issuingBank = issuingBank;
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
