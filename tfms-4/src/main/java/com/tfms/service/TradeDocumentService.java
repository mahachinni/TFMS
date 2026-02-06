package com.tfms.service;

import com.tfms.exception.ResourceNotFoundException;
import com.tfms.model.TradeDocument;
import com.tfms.model.enums.DocumentStatus;
import com.tfms.model.LetterOfCredit;
import com.tfms.model.BankGuarantee;
import com.tfms.repository.TradeDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TradeDocumentService {
    
    private final TradeDocumentRepository documentRepository;
    private final LetterOfCreditService lcService;
    private final BankGuaranteeService bgService;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;
    
    public TradeDocumentService(TradeDocumentRepository documentRepository, LetterOfCreditService lcService, BankGuaranteeService bgService) {
        this.documentRepository = documentRepository;
        this.lcService = lcService;
        this.bgService = bgService;
    }
    
    /**
     * Upload a new document
     */
    public TradeDocument uploadDocument(MultipartFile file, String documentType, 
                                         String tradeReferenceNumber, String description,
                                         String uploadedBy) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        // Save file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Create document record
        TradeDocument document = new TradeDocument();
        document.setReferenceNumber(generateReferenceNumber());
        document.setDocumentType(documentType);
        document.setTradeReferenceNumber(tradeReferenceNumber);
        document.setFileName(originalFilename);
        document.setFilePath(filePath.toString());
        document.setFileType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setUploadedBy(uploadedBy);
        document.setUploadDate(LocalDate.now());
        document.setDescription(description);
        document.setStatus(DocumentStatus.ACTIVE);
        
        return documentRepository.save(document);
    }
    
    /**
     * Update document details
     */
    public TradeDocument updateDocumentDetails(Long documentId, String documentType, 
                                                String description) {
        TradeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("TradeDocument", "id", documentId));

        document.setDocumentType(documentType);
        document.setDescription(description);
        
        return documentRepository.save(document);
    }
    
    /**
     * Approve document - Officer only
     */
    public TradeDocument approveDocument(Long documentId) {
        TradeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new  ResourceNotFoundException("TradeDocument", "id", documentId));
        document.setStatus(DocumentStatus.APPROVED);
        return documentRepository.save(document);
    }
    
    /**
     * Reject document - Officer only
     */
    public TradeDocument rejectDocument(Long documentId, String reason) {
        TradeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new  ResourceNotFoundException("TradeDocument", "id", documentId));
        document.setStatus(DocumentStatus.REJECTED);
        document.setDescription(document.getDescription() + " | Rejection: " + reason);
        return documentRepository.save(document);
    }
    
    /**
     * Archive document
     */
    public TradeDocument archiveDocument(Long documentId) {
        TradeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new  ResourceNotFoundException("TradeDocument", "id", documentId));
        document.setStatus(DocumentStatus.ARCHIVED);
        return documentRepository.save(document);
    }
    
    /**
     * Submit for review
     */
    public TradeDocument submitForReview(Long documentId) {
        TradeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new  ResourceNotFoundException("TradeDocument", "id", documentId));
        document.setStatus(DocumentStatus.PENDING_REVIEW);
        return documentRepository.save(document);
    }
    
    /**
     * Get document file path
     */
    public Path getDocumentPath(Long documentId) {
        TradeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new  ResourceNotFoundException("TradeDocument", "id", documentId));
        return Paths.get(document.getFilePath());
    }
    
    // Query methods
    public Optional<TradeDocument> findById(Long id) {
        return documentRepository.findById(id);
    }
    
    public Optional<TradeDocument> findByReferenceNumber(String referenceNumber) {
        return documentRepository.findByReferenceNumber(referenceNumber);
    }
    
    public List<TradeDocument> findAll() {
        return documentRepository.findAllOrderByCreatedAtDesc();
    }
    
    public List<TradeDocument> findByStatus(DocumentStatus status) {
        return documentRepository.findByStatus(status);
    }
    
    public List<TradeDocument> findByUploadedBy(String username) {
        return documentRepository.findByUploadedBy(username);
    }
    
    /**
     * Find documents accessible by user based on role
     * - Officers see all documents
     * - Regular users see only their uploaded documents OR documents linked to LC/BG where they are beneficiary
     */
    public List<TradeDocument> findAccessibleByUser(String username, boolean isOfficer) {
        if (isOfficer) {
            return documentRepository.findAllOrderByCreatedAtDesc();
        } else {
            if (username == null) return List.of();
            // find LCs where user is creator or beneficiary
            List<String> tradeRefs = new ArrayList<>();
            var createdLcs = lcService.findByCreatedBy(username);
            for (LetterOfCredit lc : createdLcs) tradeRefs.add(lc.getReferenceNumber());
            var beneficiaryLcs = lcService.findByBeneficiaryName(username);
            for (LetterOfCredit lc : beneficiaryLcs) if (!tradeRefs.contains(lc.getReferenceNumber())) tradeRefs.add(lc.getReferenceNumber());
            // BGs
            var createdBgs = bgService.findByCreatedBy(username);
            for (BankGuarantee bg : createdBgs) if (!tradeRefs.contains(bg.getReferenceNumber())) tradeRefs.add(bg.getReferenceNumber());
            var beneficiaryBgs = bgService.findAllByUserOrBeneficiary(username);
            for (BankGuarantee bg : beneficiaryBgs) if (!tradeRefs.contains(bg.getReferenceNumber())) tradeRefs.add(bg.getReferenceNumber());

            return documentRepository.findByUploadedByOrTradeReferenceNumberIn(username, tradeRefs);
        }
    }

    public List<TradeDocument> findByTradeReference(String tradeReferenceNumber) {
        return documentRepository.findByTradeReferenceNumber(tradeReferenceNumber);
    }
    
    public List<TradeDocument> findPendingReview() {
        return documentRepository.findByStatus(DocumentStatus.PENDING_REVIEW);
    }
    
    public long countByStatus(DocumentStatus status) {
        return documentRepository.countByStatus(status);
    }
    
    public void deleteDocument(Long id) throws IOException {
        TradeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new  ResourceNotFoundException("TradeDocument", "id", id));

        // Delete file from filesystem
        Path filePath = Paths.get(document.getFilePath());
        Files.deleteIfExists(filePath);
        
        // Delete record
        documentRepository.deleteById(id);
    }
    
    /**
     * Generate unique reference number
     */
    private String generateReferenceNumber() {
        return "DOC" + System.currentTimeMillis();
    }
}
