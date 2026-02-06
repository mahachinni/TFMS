package com.tfms.repository;

import com.tfms.model.Compliance;
import com.tfms.model.enums.ComplianceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceRepository extends JpaRepository<Compliance, Long> {

    // Find by transaction reference
    Optional<Compliance> findByTransactionReference(String transactionReference);

    // Find all by status
    List<Compliance> findByComplianceStatus(ComplianceStatus status);

    // Find all by transaction type
    List<Compliance> findByTransactionType(String transactionType);

    // Count by status
    long countByComplianceStatus(ComplianceStatus status);

    // Find all compliant records
    @Query("SELECT c FROM Compliance c WHERE c.complianceStatus = 'COMPLIANT'")
    List<Compliance> findAllCompliant();

    // Find all non-compliant records
    @Query("SELECT c FROM Compliance c WHERE c.complianceStatus = 'NON_COMPLIANT'")
    List<Compliance> findAllNonCompliant();

    // Find pending review
    @Query("SELECT c FROM Compliance c WHERE c.complianceStatus = 'PENDING'")
    List<Compliance> findAllPending();

    // Find by date range
    @Query("SELECT c FROM Compliance c WHERE c.reportDate BETWEEN :startDate AND :endDate")
    List<Compliance> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}


