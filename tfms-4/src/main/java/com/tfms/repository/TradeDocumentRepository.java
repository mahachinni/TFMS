package com.tfms.repository;

import com.tfms.model.TradeDocument;
import com.tfms.model.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeDocumentRepository extends JpaRepository<TradeDocument, Long> {
    
    Optional<TradeDocument> findByReferenceNumber(String referenceNumber);
    
    List<TradeDocument> findByStatus(DocumentStatus status);
    
    List<TradeDocument> findByUploadedBy(String uploadedBy);
    
    List<TradeDocument> findByDocumentType(String documentType);
    
    List<TradeDocument> findByTradeReferenceNumber(String tradeReferenceNumber);

    // Find documents uploaded by user OR whose trade reference is in the provided list
    List<TradeDocument> findByUploadedByOrTradeReferenceNumberIn(String uploadedBy, List<String> tradeReferenceNumbers);

    @Query("SELECT COUNT(td) FROM TradeDocument td WHERE td.status = :status")
    long countByStatus(@Param("status") DocumentStatus status);
    
    List<TradeDocument> findByStatusIn(List<DocumentStatus> statuses);
    
    @Query("SELECT td FROM TradeDocument td ORDER BY td.createdAt DESC")
    List<TradeDocument> findAllOrderByCreatedAtDesc();
    
    @Query("SELECT DISTINCT td.documentType FROM TradeDocument td")
    List<String> findDistinctDocumentTypes();
}
