package com.tfms.service;

import com.tfms.exception.InvalidStateException;
import com.tfms.exception.ResourceNotFoundException;
import com.tfms.model.BankGuarantee;
import com.tfms.model.enums.GuaranteeStatus;
import com.tfms.repository.BankGuaranteeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BankGuaranteeService {
    
    private final BankGuaranteeRepository bgRepository;

    public BankGuaranteeService(BankGuaranteeRepository bgRepository) {
        this.bgRepository = bgRepository;
    }
    

    /**
     * Request a new Bank Guarantee
     */
    public BankGuarantee requestGuarantee(BankGuarantee guarantee, String createdBy) {
        guarantee.setCreatedBy(createdBy);
        guarantee.setReferenceNumber(generateReferenceNumber());
        guarantee.setStatus(GuaranteeStatus.DRAFT);
        return bgRepository.save(guarantee);
    }
    
    /**
     * Submit guarantee for review
     */
    public BankGuarantee submitForReview(Long guaranteeId) {
        BankGuarantee bg = bgRepository.findById(guaranteeId)
                .orElseThrow(() -> new  ResourceNotFoundException("BankGuarantee", "id", guaranteeId));
        bg.setStatus(GuaranteeStatus.SUBMITTED);
        return bgRepository.save(bg);
    }

    /**
      * Issue Guarantee - Officer only
      */
    public BankGuarantee issueGuarantee(Long guaranteeId) {
        BankGuarantee bg = bgRepository.findById(guaranteeId)
                .orElseThrow(() -> new  ResourceNotFoundException("BankGuarantee", "id", guaranteeId));
        bg.setStatus(GuaranteeStatus.ISSUED);
        bg.setIssueDate(LocalDate.now());
        return bgRepository.save(bg);
    }

     /**
      * Activate Guarantee
      */
    public BankGuarantee activateGuarantee(Long guaranteeId) {
        BankGuarantee bg = bgRepository.findById(guaranteeId)
                .orElseThrow(() -> new  ResourceNotFoundException("BankGuarantee", "id", guaranteeId));
        bg.setStatus(GuaranteeStatus.ACTIVE);
        return bgRepository.save(bg);
    }

    /**
      * Cancel Guarantee
      */
    public BankGuarantee cancelGuarantee(Long guaranteeId, String reason) {
        BankGuarantee bg = bgRepository.findById(guaranteeId)
                .orElseThrow(() -> new  ResourceNotFoundException("BankGuarantee", "id", guaranteeId));
        bg.setStatus(GuaranteeStatus.CANCELLED);
        bg.setPurpose(bg.getPurpose() + " | Cancellation Reason: " + reason);
        return bgRepository.save(bg);
    }

     /**
      * Claim Guarantee
      */
    public BankGuarantee claimGuarantee(Long guaranteeId) {
        BankGuarantee bg = bgRepository.findById(guaranteeId)
                .orElseThrow(() -> new  ResourceNotFoundException("BankGuarantee", "id", guaranteeId));
        bg.setStatus(GuaranteeStatus.CLAIMED);
        return bgRepository.save(bg);
    }
    
    /**
     * Update Guarantee
     */
    public BankGuarantee updateGuarantee(Long guaranteeId, BankGuarantee updatedBg) {
        BankGuarantee existingBg = bgRepository.findById(guaranteeId)
                .orElseThrow(() -> new  ResourceNotFoundException("BankGuarantee", "id", guaranteeId));

        existingBg.setApplicantName(updatedBg.getApplicantName());
        existingBg.setBeneficiaryName(updatedBg.getBeneficiaryName());
        existingBg.setGuaranteeAmount(updatedBg.getGuaranteeAmount());
        existingBg.setCurrency(updatedBg.getCurrency());
        existingBg.setGuaranteeType(updatedBg.getGuaranteeType());
        existingBg.setValidityPeriod(updatedBg.getValidityPeriod());
        existingBg.setPurpose(updatedBg.getPurpose());
        
        return bgRepository.save(existingBg);
    }
    
    // Query methods
    public Optional<BankGuarantee> findById(Long id) {
        return bgRepository.findById(id);
    }
    
    public Optional<BankGuarantee> findByReferenceNumber(String referenceNumber) {
        return bgRepository.findByReferenceNumber(referenceNumber);
    }
    
    public List<BankGuarantee> findAll() {
        return bgRepository.findAllOrderByCreatedAtDesc();
    }

    public List<BankGuarantee> findByCreatedBy(String username) {
        return bgRepository.findByCreatedBy(username);
    }
    
    public List<BankGuarantee> findPendingApproval() {
        return bgRepository.findByStatusIn(List.of(GuaranteeStatus.SUBMITTED, GuaranteeStatus.UNDER_REVIEW, GuaranteeStatus.PENDING));
    }

    /**
     * Send BG to Risk team - Officer action
     */
    public BankGuarantee sendToRiskTeam(Long guaranteeId) {
        BankGuarantee bg = bgRepository.findById(guaranteeId)
                .orElseThrow(() -> new  ResourceNotFoundException("BankGuarantee", "id", guaranteeId));
        // Only allow sending to risk from Submitted/Under Review/Pending
        if (bg.getStatus() == GuaranteeStatus.SUBMITTED || bg.getStatus() == GuaranteeStatus.UNDER_REVIEW || bg.getStatus() == GuaranteeStatus.PENDING) {
            bg.setStatus(GuaranteeStatus.SENT_TO_RISK);
            return bgRepository.save(bg);
        }
        throw new InvalidStateException("BankGuarantee", bg.getStatus() != null ? bg.getStatus().name() : "UNKNOWN", "send to risk");
    }

    /**
     * Return BG from Risk back to Officer for further processing
     */
    public BankGuarantee returnToOfficer(Long guaranteeId) {
        BankGuarantee bg = bgRepository.findById(guaranteeId)
                .orElseThrow(() -> new ResourceNotFoundException("BankGuarantee", "id", guaranteeId));
        if (bg.getStatus() == GuaranteeStatus.SENT_TO_RISK) {
            bg.setStatus(GuaranteeStatus.UNDER_REVIEW);
            return bgRepository.save(bg);
        }
        throw new  InvalidStateException("BankGuarantee", bg.getStatus() != null ? bg.getStatus().name() : "UNKNOWN", "return to officer");
    }

    public void deleteGuarantee(Long id) {
        bgRepository.deleteById(id);
    }
    
    /**
     * Generate unique reference number
     */
    private String generateReferenceNumber() {
        return "BG-" + System.currentTimeMillis();
    }

    /**
     * Find bank guarantees either created by the given username or where the beneficiary name matches (case-insensitive)
     */
    public List<BankGuarantee> findAllByUserOrBeneficiary(String username) {
        if (username == null) return List.of();
        List<BankGuarantee> result = new ArrayList<>();
        // created by
        result.addAll(bgRepository.findByCreatedBy(username));
        // beneficiary match ignoring case
        List<BankGuarantee> byBeneficiary = bgRepository.findByBeneficiaryNameIgnoreCase(username);
        for (BankGuarantee bg : byBeneficiary) {
            if (!result.contains(bg)) result.add(bg);
        }
        return result;
    }
}
