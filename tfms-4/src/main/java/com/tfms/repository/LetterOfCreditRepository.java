package com.tfms.repository;

import com.tfms.model.LetterOfCredit;
import com.tfms.model.enums.LCStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LetterOfCreditRepository extends JpaRepository<LetterOfCredit, Long> {
    
    Optional<LetterOfCredit> findByReferenceNumber(String referenceNumber);
    
    List<LetterOfCredit> findByStatus(LCStatus status);
    
    List<LetterOfCredit> findByCreatedBy(String createdBy);
    
    List<LetterOfCredit> findByApplicantNameContainingIgnoreCase(String applicantName);
    
    List<LetterOfCredit> findByBeneficiaryNameContainingIgnoreCase(String beneficiaryName);
    
    @Query("SELECT lc FROM LetterOfCredit lc WHERE lc.expiryDate < :date AND lc.status NOT IN ('CLOSED', 'EXPIRED')")
    List<LetterOfCredit> findExpiringBefore(@Param("date") LocalDate date);
    
    @Query("SELECT lc FROM LetterOfCredit lc WHERE lc.createdBy = :username OR lc.applicantName LIKE %:username% OR lc.beneficiaryName LIKE %:username%")
    List<LetterOfCredit> findByUserInvolved(@Param("username") String username);
    
    @Query("SELECT COUNT(lc) FROM LetterOfCredit lc WHERE lc.status = :status")
    long countByStatus(@Param("status") LCStatus status);
    
    List<LetterOfCredit> findByStatusIn(List<LCStatus> statuses);
    
    @Query("SELECT lc FROM LetterOfCredit lc ORDER BY lc.createdAt DESC")
    List<LetterOfCredit> findAllOrderByCreatedAtDesc();
}
