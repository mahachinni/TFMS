package com.tfms.model;

import com.tfms.model.enums.RiskLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Risk Assessment Entity
 * Represents risk evaluation of trade finance transactions
 */
@Entity
@Table(name = "risk_assessment")
public class RiskAssessment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long riskId;
    
    @NotBlank(message = "Transaction reference is required")
    @Size(max = 50)
    private String transactionReference;
    
    private String transactionType;
    
    @Column(columnDefinition = "TEXT")
    private String riskFactors;
    
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    @Column(precision = 5, scale = 2)
    private BigDecimal riskScore;
    
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;
    
    private LocalDate assessmentDate;
    
    @Size(max = 1000)
    private String remarks;
    
    @Size(max = 1000)
    private String recommendations;
    
    private String assessedBy;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        assessmentDate = LocalDate.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public RiskAssessment() {}
    
    // Getters and Setters
    public Long getRiskId() {
        return riskId;
    }
    
    public void setRiskId(Long riskId) {
        this.riskId = riskId;
    }
    
    public String getTransactionReference() {
        return transactionReference;
    }
    
    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    
    public String getRiskFactors() {
        return riskFactors;
    }
    
    public void setRiskFactors(String riskFactors) {
        this.riskFactors = riskFactors;
    }
    
    public BigDecimal getRiskScore() {
        return riskScore;
    }
    
    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }
    
    public RiskLevel getRiskLevel() {
        return riskLevel;
    }
    
    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }
    
    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }
    
    public void setAssessmentDate(LocalDate assessmentDate) {
        this.assessmentDate = assessmentDate;
    }
    
    public String getRemarks() {
        return remarks;
    }
    
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
    
    public String getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }
    
    public String getAssessedBy() {
        return assessedBy;
    }
    
    public void setAssessedBy(String assessedBy) {
        this.assessedBy = assessedBy;
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
