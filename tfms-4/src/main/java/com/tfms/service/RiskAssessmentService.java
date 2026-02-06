package com.tfms.service;

import com.tfms.exception.ResourceNotFoundException;
import com.tfms.model.RiskAssessment;
import com.tfms.model.enums.RiskLevel;
import com.tfms.model.LetterOfCredit;
import com.tfms.model.BankGuarantee;
import com.tfms.model.enums.LCStatus;
import com.tfms.model.enums.GuaranteeStatus;
import com.tfms.repository.RiskAssessmentRepository;
import com.tfms.repository.LetterOfCreditRepository;
import com.tfms.repository.BankGuaranteeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class RiskAssessmentService {
    
    private final RiskAssessmentRepository riskRepository;
    private final LetterOfCreditRepository lcRepository;
    private final BankGuaranteeRepository bgRepository;
    
    public RiskAssessmentService(RiskAssessmentRepository riskRepository,
                                  LetterOfCreditRepository lcRepository,
                                  BankGuaranteeRepository bgRepository) {
        this.riskRepository = riskRepository;
        this.lcRepository = lcRepository;
        this.bgRepository = bgRepository;
    }
    
    /**
     * Analyze risk for a transaction with provided assessment data
     */
    public RiskAssessment analyzeRisk(RiskAssessment providedAssessment, String assessedBy) {
        BigDecimal riskScore;
        String riskFactors;
        String recommendations;
        
        // Check if manual risk factors and score were provided from the form
        if (providedAssessment.getRiskFactors() != null && !providedAssessment.getRiskFactors().trim().isEmpty()
                && providedAssessment.getRiskScore() != null) {
            // Use manually calculated values from the form
            riskScore = providedAssessment.getRiskScore();
            riskFactors = providedAssessment.getRiskFactors();
            recommendations = providedAssessment.getRemarks() != null ? providedAssessment.getRemarks() : "Manual assessment completed";
        } else {
            // Automatically calculate risk based on transaction type
            String transactionReference = providedAssessment.getTransactionReference();
            String transactionType = providedAssessment.getTransactionType();

            if ("LC".equalsIgnoreCase(transactionType) || transactionReference.startsWith("LC")) {
                Optional<LetterOfCredit> lcOpt = lcRepository.findByReferenceNumber(transactionReference);
                if (lcOpt.isPresent()) {
                    LetterOfCredit lc = lcOpt.get();
                    RiskAnalysisResult result = calculateLCRisk(lc);
                    riskScore = result.score;
                    riskFactors = result.factors;
                    recommendations = result.recommendations;
                } else {
                    riskScore = new BigDecimal("50.00");
                    riskFactors = "{\"transactionRisk\":2}";
                    recommendations = "Transaction not found - Default medium risk applied. Verify transaction details";
                }
            } else if ("BG".equalsIgnoreCase(transactionType) || transactionReference.startsWith("BG")) {
                Optional<BankGuarantee> bgOpt = bgRepository.findByReferenceNumber(transactionReference);
                if (bgOpt.isPresent()) {
                    BankGuarantee bg = bgOpt.get();
                    RiskAnalysisResult result = calculateBGRisk(bg);
                    riskScore = result.score;
                    riskFactors = result.factors;
                    recommendations = result.recommendations;
                } else {
                    riskScore = new BigDecimal("50.00");
                    riskFactors = "{\"transactionRisk\":2}";
                    recommendations = "Transaction not found - Default medium risk applied. Verify transaction details";
                }
            } else {
                riskScore = new BigDecimal("40.00");
                riskFactors = "{\"transactionRisk\":2}";
                recommendations = "General trade transaction - Standard due diligence recommended";
            }
        }
        
        RiskLevel riskLevel = determineRiskLevel(riskScore);
        
        RiskAssessment assessment = new RiskAssessment();
        assessment.setTransactionReference(providedAssessment.getTransactionReference());
        assessment.setTransactionType(providedAssessment.getTransactionType());
        assessment.setRiskScore(riskScore);
        assessment.setRiskLevel(riskLevel);
        assessment.setRiskFactors(riskFactors);
        assessment.setRecommendations(recommendations);
        assessment.setRemarks(providedAssessment.getRemarks());
        assessment.setAssessedBy(assessedBy);
        assessment.setAssessmentDate(LocalDate.now());
        
        RiskAssessment saved = riskRepository.save(assessment);

        // If the transaction is an LC and the LC was SENT_TO_RISK, update LC status back to officer review
        if ("LC".equalsIgnoreCase(providedAssessment.getTransactionType()) ||
                providedAssessment.getTransactionReference().startsWith("LC")) {
            Optional<LetterOfCredit> lcOpt = lcRepository.findByReferenceNumber(providedAssessment.getTransactionReference());
            lcOpt.ifPresent(lc -> {
                if (lc.getStatus() == LCStatus.SENT_TO_RISK) {
                    // move back to UNDER_VERIFICATION so officer can continue workflow
                    lc.setStatus(LCStatus.UNDER_VERIFICATION);
                    lcRepository.save(lc);
                }
            });
        }

        // If the transaction is a BG and the BG was SENT_TO_RISK, update BG status back to officer review
        if ("BG".equalsIgnoreCase(providedAssessment.getTransactionType()) ||
                providedAssessment.getTransactionReference().startsWith("BG")) {
            Optional<BankGuarantee> bgOpt = bgRepository.findByReferenceNumber(providedAssessment.getTransactionReference());
            bgOpt.ifPresent(bg -> {
                if (bg.getStatus() == GuaranteeStatus.SENT_TO_RISK) {
                    bg.setStatus(GuaranteeStatus.UNDER_REVIEW);
                    bgRepository.save(bg);
                }
            });
        }
        log.info("risk assessment ", saved);

        return saved;
    }
    


    /**
     * Calculate risk for Letter of Credit
     */
    private RiskAnalysisResult calculateLCRisk(LetterOfCredit lc) {
        BigDecimal score = BigDecimal.ZERO;
        StringBuilder recommendations = new StringBuilder();
        
        // Risk factor levels (1=Low, 2=Medium, 3=High)
        int amountRisk = 1;
        int durationRisk = 1;
        int currencyRisk = 1;
        int documentationRisk = 1;
        int counterpartyRisk = 2; // Default medium for LC

        // Amount-based risk (higher amounts = higher risk)
        if (lc.getAmount().compareTo(new BigDecimal("1000000")) > 0) {
            score = score.add(new BigDecimal("25"));
            amountRisk = 3;
            recommendations.append("Enhanced due diligence required; ");
        } else if (lc.getAmount().compareTo(new BigDecimal("100000")) > 0) {
            score = score.add(new BigDecimal("15"));
            amountRisk = 2;
        } else {
            score = score.add(new BigDecimal("5"));
            amountRisk = 1;
        }
        
        // Duration-based risk
        long daysToExpiry = ChronoUnit.DAYS.between(LocalDate.now(), lc.getExpiryDate());
        if (daysToExpiry > 365) {
            score = score.add(new BigDecimal("20"));
            durationRisk = 3;
            recommendations.append("Periodic review recommended; ");
        } else if (daysToExpiry > 180) {
            score = score.add(new BigDecimal("10"));
            durationRisk = 2;
        } else {
            score = score.add(new BigDecimal("5"));
            durationRisk = 1;
        }
        
        // Currency risk
        if (!"USD".equals(lc.getCurrency()) && !"EUR".equals(lc.getCurrency())) {
            score = score.add(new BigDecimal("15"));
            currencyRisk = 3;
            recommendations.append("Consider currency hedging; ");
        } else {
            score = score.add(new BigDecimal("5"));
            currencyRisk = 1;
        }

        // Documentation risk - check if documents are complete
        if (lc.getStatus() != null && lc.getStatus().toString().contains("DOCUMENT")) {
            documentationRisk = 2;
            score = score.add(new BigDecimal("5"));
        }
        
        // Add some baseline risk
        score = score.add(new BigDecimal("10"));

        // Build JSON format for risk factors
        String factorsJson = String.format(
            "{\"amountRisk\":%d,\"durationRisk\":%d,\"currencyRisk\":%d,\"documentationRisk\":%d,\"counterpartyRisk\":%d}",
            amountRisk, durationRisk, currencyRisk, documentationRisk, counterpartyRisk
        );

        return new RiskAnalysisResult(
                score.min(new BigDecimal("100")),
                factorsJson,
                recommendations.length() > 0 ? recommendations.toString() : "Standard monitoring recommended"
        );
    }
    
    /**
     * Calculate risk for Bank Guarantee
     */
    private RiskAnalysisResult calculateBGRisk(BankGuarantee bg) {
        BigDecimal score = BigDecimal.ZERO;
        StringBuilder recommendations = new StringBuilder();
        
        // Risk factor levels (1=Low, 2=Medium, 3=High)
        int amountRisk = 1;
        int durationRisk = 1;
        int guaranteeTypeRisk = 1;
        int counterpartyRisk = 2; // Default medium
        int documentationRisk = 1;

        // Amount-based risk
        if (bg.getGuaranteeAmount().compareTo(new BigDecimal("500000")) > 0) {
            score = score.add(new BigDecimal("25"));
            amountRisk = 3;
            recommendations.append("Senior approval required; ");
        } else if (bg.getGuaranteeAmount().compareTo(new BigDecimal("100000")) > 0) {
            score = score.add(new BigDecimal("15"));
            amountRisk = 2;
        } else {
            score = score.add(new BigDecimal("5"));
            amountRisk = 1;
        }
        
        // Guarantee type risk
        String gType = bg.getGuaranteeType();
        if (gType != null) {
            if (gType.contains("Performance") || gType.contains("Financial")) {
                score = score.add(new BigDecimal("20"));
                guaranteeTypeRisk = 3;
                recommendations.append("Thorough applicant assessment required; ");
            } else if (gType.contains("Bid") || gType.contains("Advance")) {
                score = score.add(new BigDecimal("15"));
                guaranteeTypeRisk = 2;
            } else {
                score = score.add(new BigDecimal("10"));
                guaranteeTypeRisk = 1;
            }
        }
        
        // Validity period risk
        long daysToExpiry = ChronoUnit.DAYS.between(LocalDate.now(), bg.getValidityPeriod());
        if (daysToExpiry > 365) {
            score = score.add(new BigDecimal("20"));
            durationRisk = 3;
            recommendations.append("Annual review required; ");
        } else if (daysToExpiry > 180) {
            score = score.add(new BigDecimal("10"));
            durationRisk = 2;
        } else {
            score = score.add(new BigDecimal("5"));
            durationRisk = 1;
        }
        
        // Add baseline
        score = score.add(new BigDecimal("10"));

        // Build JSON format for risk factors
        String factorsJson = String.format(
            "{\"amountRisk\":%d,\"durationRisk\":%d,\"guaranteeTypeRisk\":%d,\"counterpartyRisk\":%d,\"documentationRisk\":%d}",
            amountRisk, durationRisk, guaranteeTypeRisk, counterpartyRisk, documentationRisk
        );

        return new RiskAnalysisResult(
                score.min(new BigDecimal("100")),
                factorsJson,
                recommendations.length() > 0 ? recommendations.toString() : "Standard monitoring recommended"
        );
    }
    
    /**
     * Determine risk level based on score
     */
    private RiskLevel determineRiskLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("75")) >= 0) {
            return RiskLevel.CRITICAL;
        } else if (score.compareTo(new BigDecimal("60")) >= 0) {
            return RiskLevel.HIGH;
        } else if (score.compareTo(new BigDecimal("25")) >= 0) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }
    
    /**
     * Update risk assessment remarks
     */
    public RiskAssessment updateRemarks(Long riskId, String remarks) {
        RiskAssessment assessment = riskRepository.findById(riskId)
                .orElseThrow(() -> new ResourceNotFoundException("Risk Assessment not found"));
        assessment.setRemarks(remarks);
        return riskRepository.save(assessment);
    }
    
    // Query methods
    public Optional<RiskAssessment> findById(Long id) {
        return riskRepository.findById(id);
    }
    
    public Optional<RiskAssessment> findByTransactionReference(String reference) {
        return riskRepository.findFirstByTransactionReferenceOrderByAssessmentDateDesc(reference);
    }
    
    public List<RiskAssessment> findAll() {
        return riskRepository.findAllOrderByCreatedAtDesc();
    }
    
    public List<RiskAssessment> findByRiskLevel(RiskLevel level) {
        return riskRepository.findByRiskLevel(level);
    }
    
    public List<RiskAssessment> findHighRiskAssessments() {
        return riskRepository.findHighRiskAssessments();
    }
    

    public long countByRiskLevel(RiskLevel level) {
        return riskRepository.countByRiskLevel(level);
    }
    
    public Double getAverageRiskScore() {
        return riskRepository.findAverageRiskScore();
    }
    
    public void deleteAssessment(Long id) {
        riskRepository.deleteById(id);
    }
    
    /**
     * Find LCs that have been sent to Risk team
     */
    public List<LetterOfCredit> findLcsSentToRisk() {
        return lcRepository.findByStatus(LCStatus.SENT_TO_RISK);
    }

    /**
     * Count LCs sent to Risk
     */
    public long countLcsSentToRisk() {
        return lcRepository.countByStatus(LCStatus.SENT_TO_RISK);
    }

    /**
     * Find BGs that have been sent to Risk team
     */
    public List<BankGuarantee> findBgsSentToRisk() {
        return bgRepository.findByStatus(GuaranteeStatus.SENT_TO_RISK);
    }

    /**
     * Count BGs sent to Risk
     */
    public long countBgsSentToRisk() {
        return bgRepository.countByStatus(GuaranteeStatus.SENT_TO_RISK);
    }

    /**
     * Helper class for risk analysis results
     */
    private static class RiskAnalysisResult {
        BigDecimal score;
        String factors;
        String recommendations;
        
        RiskAnalysisResult(BigDecimal score, String factors, String recommendations) {
            this.score = score;
            this.factors = factors;
            this.recommendations = recommendations;
        }
    }
}
