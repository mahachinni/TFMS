package com.tfms.repository;

import com.tfms.model.RiskAssessment;
import com.tfms.model.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {
    List<RiskAssessment> findByTransactionReference(String transactionReference);
    Optional<RiskAssessment> findFirstByTransactionReferenceOrderByAssessmentDateDesc(String transactionReference);
    List<RiskAssessment> findByRiskLevel(RiskLevel riskLevel);
    List<RiskAssessment> findByAssessedBy(String assessedBy);
    long countByRiskLevel(RiskLevel riskLevel);
    
    @Query("SELECT r FROM RiskAssessment r ORDER BY r.createdAt DESC")
    List<RiskAssessment> findAllOrderByCreatedAtDesc();
    
    @Query("SELECT r FROM RiskAssessment r WHERE r.riskLevel = 'HIGH' ORDER BY r.createdAt DESC")
    List<RiskAssessment> findHighRiskAssessments();
    
    @Query("SELECT AVG(r.riskScore) FROM RiskAssessment r")
    Double findAverageRiskScore();
}

