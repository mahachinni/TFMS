package com.tfms.repository;

import com.tfms.model.BankGuarantee;
import com.tfms.model.enums.GuaranteeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankGuaranteeRepository extends JpaRepository<BankGuarantee, Long> {
    
    Optional<BankGuarantee> findByReferenceNumber(String referenceNumber);
    
    List<BankGuarantee> findByStatus(GuaranteeStatus status);
    
    List<BankGuarantee> findByCreatedBy(String createdBy);
    
    List<BankGuarantee> findByBeneficiaryNameIgnoreCase(String beneficiaryName);

    List<BankGuarantee> findByApplicantNameContainingIgnoreCase(String applicantName);
    
    List<BankGuarantee> findByGuaranteeType(String guaranteeType);
    
    @Query("SELECT bg FROM BankGuarantee bg WHERE bg.validityPeriod < :date AND bg.status NOT IN ('EXPIRED', 'CANCELLED')")
    List<BankGuarantee> findExpiringBefore(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(bg) FROM BankGuarantee bg WHERE bg.status = :status")
    long countByStatus(@Param("status") GuaranteeStatus status);
    
    List<BankGuarantee> findByStatusIn(List<GuaranteeStatus> statuses);
    
    @Query("SELECT bg FROM BankGuarantee bg ORDER BY bg.createdAt DESC")
    List<BankGuarantee> findAllOrderByCreatedAtDesc();
}
