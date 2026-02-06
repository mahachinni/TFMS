package com.tfms.service;

import com.tfms.exception.InvalidStateException;
import com.tfms.exception.ResourceNotFoundException;
import com.tfms.model.LetterOfCredit;
import com.tfms.model.enums.LCStatus;
import com.tfms.repository.LetterOfCreditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LetterOfCreditService {
    
    private final LetterOfCreditRepository lcRepository;

    public LetterOfCreditService(LetterOfCreditRepository lcRepository) {
        this.lcRepository = lcRepository;
    }
    
    /**
     * Create a new Letter of Credit
     */
    public LetterOfCredit createLetterOfCredit(LetterOfCredit lc, String createdBy) {
        lc.setCreatedBy(createdBy);
        lc.setReferenceNumber(generateReferenceNumber());
        lc.setStatus(LCStatus.DRAFT);
        return lcRepository.save(lc);
    }
    
    /**
     * Submit LC for verification
     */
    public LetterOfCredit submitForVerification(Long lcId) {
        LetterOfCredit lc = lcRepository.findById(lcId)
                .orElseThrow(() -> new  ResourceNotFoundException("LetterOfCredit", "id", lcId));
        if (lc.getStatus() == LCStatus.CLOSED) {
            throw new  InvalidStateException("LetterOfCredit", lc.getStatus().name(), "submit");
        }
        lc.setStatus(LCStatus.SUBMITTED);
        return lcRepository.save(lc);
    }
    
    /**
     * Start verification process
     */
    public LetterOfCredit startVerification(Long lcId) {
        LetterOfCredit lc = lcRepository.findById(lcId)
                .orElseThrow(() -> new  ResourceNotFoundException("LetterOfCredit", "id", lcId));
        if (lc.getStatus() != LCStatus.SUBMITTED) {
            throw new  InvalidStateException("LetterOfCredit", lc.getStatus() != null ? lc.getStatus().name() : "UNKNOWN", "start verification");
        }
        lc.setStatus(LCStatus.UNDER_VERIFICATION);
        return lcRepository.save(lc);
    }
    
    /**
     * Approve LC - Officer only
     */
    public LetterOfCredit approveLetterOfCredit(Long lcId) {
        LetterOfCredit lc = lcRepository.findById(lcId)
                .orElseThrow(() -> new  ResourceNotFoundException("LetterOfCredit", "id", lcId));
        if (!(lc.getStatus() == LCStatus.SUBMITTED || lc.getStatus() == LCStatus.UNDER_VERIFICATION)) {
            throw new  InvalidStateException("LetterOfCredit", lc.getStatus() != null ? lc.getStatus().name() : "UNKNOWN", "approve");
        }
        lc.setStatus(LCStatus.APPROVED);
        lc.setIssueDate(LocalDate.now());
        return lcRepository.save(lc);
    }
    
    /**
     * Reject LC - Officer only
     */
    public LetterOfCredit rejectLetterOfCredit(Long lcId, String reason) {
        LetterOfCredit lc = lcRepository.findById(lcId)
                .orElseThrow(() -> new  ResourceNotFoundException("LetterOfCredit", "id", lcId));
        if (lc.getStatus() == LCStatus.CLOSED) {
            throw new  InvalidStateException("LetterOfCredit", lc.getStatus().name(), "reject");
        }
        lc.setStatus(LCStatus.REJECTED);
        String prev = lc.getDescription() == null ? "" : lc.getDescription();
        lc.setDescription(prev + " | Rejection Reason: " + reason);
        return lcRepository.save(lc);
    }
    
    /**
     * Amend LC
     */
    public LetterOfCredit amendLetterOfCredit(Long lcId, LetterOfCredit updatedLc) {
        LetterOfCredit existingLc = lcRepository.findById(lcId)
                .orElseThrow(() -> new  ResourceNotFoundException("LetterOfCredit", "id", lcId));

        if (existingLc.getStatus() == LCStatus.CLOSED) {
            throw new  InvalidStateException("LetterOfCredit", existingLc.getStatus().name(), "amend");
        }
        // validate expiry date if provided
        if (updatedLc.getExpiryDate() != null && !updatedLc.getExpiryDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Expiry date must be a future date");
        }

        existingLc.setApplicantName(updatedLc.getApplicantName());
        existingLc.setBeneficiaryName(updatedLc.getBeneficiaryName());
        existingLc.setAmount(updatedLc.getAmount());
        existingLc.setCurrency(updatedLc.getCurrency());
        existingLc.setExpiryDate(updatedLc.getExpiryDate());
        existingLc.setDescription(updatedLc.getDescription());
        existingLc.setAdvisingBank(updatedLc.getAdvisingBank());
        existingLc.setStatus(LCStatus.AMENDED);
        
        return lcRepository.save(existingLc);
    }
    
    /**
     * Close LC
     */
    public LetterOfCredit closeLetterOfCredit(Long lcId) {
        LetterOfCredit lc = lcRepository.findById(lcId)
                .orElseThrow(() -> new  ResourceNotFoundException("LetterOfCredit", "id", lcId));
        if (lc.getStatus() == LCStatus.CLOSED) {
            throw new InvalidStateException("LetterOfCredit", lc.getStatus().name(), "close");
        }
        lc.setStatus(LCStatus.CLOSED);
        return lcRepository.save(lc);
    }
    
    /**
     * Open LC (set to active)
     */
    public LetterOfCredit openLetterOfCredit(Long lcId) {
        LetterOfCredit lc = lcRepository.findById(lcId)
                .orElseThrow(() -> new  ResourceNotFoundException("LetterOfCredit", "id", lcId));
        if (lc.getStatus() == LCStatus.CLOSED) {
            throw new  InvalidStateException("LetterOfCredit", lc.getStatus().name(), "open");
        }
        lc.setStatus(LCStatus.OPEN);
        return lcRepository.save(lc);
    }
    
    /**
     * Send LC to Risk team - Officer only
     */
    public LetterOfCredit sendToRiskTeam(Long lcId) {
        LetterOfCredit lc = lcRepository.findById(lcId)
                .orElseThrow(() -> new ResourceNotFoundException("LetterOfCredit", "id", lcId));
        if (!(lc.getStatus() == LCStatus.SUBMITTED || lc.getStatus() == LCStatus.UNDER_VERIFICATION)) {
            throw new InvalidStateException("LetterOfCredit", lc.getStatus() != null ? lc.getStatus().name() : "UNKNOWN", "send to risk");
        }
        if (lc.getStatus() == LCStatus.CLOSED) {
            throw new  InvalidStateException("LetterOfCredit", lc.getStatus().name(), "send to risk");
        }
        lc.setStatus(LCStatus.SENT_TO_RISK);
        return lcRepository.save(lc);
    }

    // Query methods
    public Optional<LetterOfCredit> findById(Long id) {
        return lcRepository.findById(id);
    }
    
    public Optional<LetterOfCredit> findByReferenceNumber(String referenceNumber) {
        return lcRepository.findByReferenceNumber(referenceNumber);
    }
    
    public List<LetterOfCredit> findAll() {
        return lcRepository.findAllOrderByCreatedAtDesc();
    }
    
    public List<LetterOfCredit> findByStatus(LCStatus status) {
        return lcRepository.findByStatus(status);
    }
    
    public List<LetterOfCredit> findByCreatedBy(String username) {
        return lcRepository.findByCreatedBy(username);
    }
    
    /**
     * Find letters of credit either created by the given username or where the beneficiary name matches (case-insensitive)
     */
    public List<LetterOfCredit> findAllByUserOrBeneficiary(String username) {
        if (username == null) return List.of();
        List<LetterOfCredit> result = new ArrayList<>();
        result.addAll(lcRepository.findByCreatedBy(username));
        List<LetterOfCredit> byBeneficiary = lcRepository.findByBeneficiaryNameContainingIgnoreCase(username);
        for (LetterOfCredit lc : byBeneficiary) {
            if (!result.contains(lc)) result.add(lc);
        }
        return result;
    }

    /**
     * Return LCs where the beneficiary name matches (case-insensitive).
     * This is used to show a user where they are listed as beneficiary (regardless of creator).
     */
    public List<LetterOfCredit> findByBeneficiaryName(String username) {
        if (username == null) return List.of();
        return lcRepository.findByBeneficiaryNameContainingIgnoreCase(username);
    }

    public List<LetterOfCredit> findPendingApproval() {
        return lcRepository.findByStatusIn(List.of(LCStatus.SUBMITTED, LCStatus.UNDER_VERIFICATION));
    }

    public long countByStatus(LCStatus status) {
        return lcRepository.countByStatus(status);
    }
    
    public void deleteLetterOfCredit(Long id) {
        lcRepository.deleteById(id);
    }
    
    /**
     * Generate unique reference number
     */
    private String generateReferenceNumber() {
        return "LC-" + System.currentTimeMillis();
    }
    
    /**
     * Update LC
     */
    public LetterOfCredit updateLetterOfCredit(LetterOfCredit lc) {
        return lcRepository.save(lc);
    }
}
