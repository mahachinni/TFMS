package com.tfms.model;

import com.tfms.model.enums.DocumentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Trade Document Entity
 * Represents trade-related documents like invoices, bills of lading, etc.
 */
@Entity
@Table(name = "trade_document")
public class TradeDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;
    
    @NotBlank(message = "Document type is required")
    @Size(max = 50)
    private String documentType;
    
    @NotBlank(message = "Reference number is required")
    @Size(max = 50)
    private String referenceNumber;
    
    private String tradeReferenceNumber;
    
    @NotBlank(message = "File name is required")
    private String fileName;
    
    private String filePath;
    
    private String fileType;
    
    private Long fileSize;
    
    @Size(max = 50)
    private String uploadedBy;
    
    private LocalDate uploadDate;
    
    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.ACTIVE;
    
    @Size(max = 500)
    private String description;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        uploadDate = LocalDate.now();
        if (referenceNumber == null) {
            referenceNumber = "DOC" + System.currentTimeMillis();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public TradeDocument() {}
    
    // Getters and Setters
    public Long getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }
    
    public String getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    public String getReferenceNumber() {
        return referenceNumber;
    }
    
    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }
    
    public String getTradeReferenceNumber() {
        return tradeReferenceNumber;
    }
    
    public void setTradeReferenceNumber(String tradeReferenceNumber) {
        this.tradeReferenceNumber = tradeReferenceNumber;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getUploadedBy() {
        return uploadedBy;
    }
    
    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
    
    public LocalDate getUploadDate() {
        return uploadDate;
    }
    
    public void setUploadDate(LocalDate uploadDate) {
        this.uploadDate = uploadDate;
    }
    
    public DocumentStatus getStatus() {
        return status;
    }
    
    public void setStatus(DocumentStatus status) {
        this.status = status;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
