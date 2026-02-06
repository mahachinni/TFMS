package com.tfms.config;

import com.tfms.model.*;
import com.tfms.model.enums.*;
import com.tfms.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final LetterOfCreditRepository lcRepository;
    private final BankGuaranteeRepository bgRepository;
    private final TradeDocumentRepository documentRepository;
    private final RiskAssessmentRepository riskRepository;
//    private final ComplianceRepository complianceRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, LetterOfCreditRepository lcRepository,
                          BankGuaranteeRepository bgRepository, TradeDocumentRepository documentRepository,
                          RiskAssessmentRepository riskRepository, ComplianceRepository complianceRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.lcRepository = lcRepository;
        this.bgRepository = bgRepository;
        this.documentRepository = documentRepository;
        this.riskRepository = riskRepository;
//        this.complianceRepository = complianceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            initializeUsers();
            initializeSampleData();
        }
    }

    private void initializeUsers() {
        // Customer - Importer
        User importer = new User();
        importer.setUsername("importer");
        importer.setPassword(passwordEncoder.encode("password123"));
        importer.setFullName("John Importer");
        importer.setEmail("importer@tfms.com");
        importer.setRole(Role.CUSTOMER);
        importer.setBusinessRole(BusinessRole.IMPORTER);
        importer.setEnabled(true);
        userRepository.save(importer);

        // Customer - Exporter
        User exporter = new User();
        exporter.setUsername("exporter");
        exporter.setPassword(passwordEncoder.encode("password123"));
        exporter.setFullName("Jane Exporter");
        exporter.setEmail("exporter@tfms.com");
        exporter.setRole(Role.CUSTOMER);
        exporter.setBusinessRole(BusinessRole.EXPORTER);
        exporter.setEnabled(true);
        userRepository.save(exporter);

        // Bank Officer
        User officer = new User();
        officer.setUsername("officer");
        officer.setPassword(passwordEncoder.encode("password123"));
        officer.setFullName("Bob Officer");
        officer.setEmail("officer@tfms.com");
        officer.setRole(Role.OFFICER);
        officer.setBusinessRole(BusinessRole.BANK_STAFF);
        officer.setEnabled(true);
        userRepository.save(officer);

        // Risk Analyst
        User analyst = new User();
        analyst.setUsername("analyst");
        analyst.setPassword(passwordEncoder.encode("password123"));
        analyst.setFullName("Alice Analyst");
        analyst.setEmail("analyst@tfms.com");
        analyst.setRole(Role.RISK);
        analyst.setBusinessRole(BusinessRole.RISK_ANALYST);
        analyst.setEnabled(true);
        userRepository.save(analyst);

        System.out.println("=== Sample Users Created ===");
        System.out.println("importer / password123 (CUSTOMER - Importer)");
        System.out.println("exporter / password123 (CUSTOMER - Exporter)");
        System.out.println("officer / password123 (OFFICER - Bank Staff)");
        System.out.println("analyst / password123 (RISK - Risk Analyst)");
        System.out.println("============================");
    }

    private void initializeSampleData() {
        // Sample Letter of Credit
        LetterOfCredit lc1 = new LetterOfCredit();
        lc1.setReferenceNumber("LC-2024-001");
        lc1.setApplicantName("ABC Trading Co.");
        lc1.setBeneficiaryName("XYZ Exports Ltd.");
        lc1.setAmount(new BigDecimal("150000.00"));
        lc1.setCurrency("USD");
        lc1.setIssueDate(LocalDate.now().minusDays(10));
        lc1.setExpiryDate(LocalDate.now().plusMonths(3));
        lc1.setStatus(LCStatus.APPROVED);
        lc1.setCreatedBy("importer");
        lc1.setCreatedAt(LocalDateTime.now().minusDays(10));
        lc1.setIssuingBank("Global Trade Bank");
        lc1.setAdvisingBank("International Commerce Bank");
        lcRepository.save(lc1);

        LetterOfCredit lc2 = new LetterOfCredit();
        lc2.setReferenceNumber("LC-2024-002");
        lc2.setApplicantName("Tech Imports Inc.");
        lc2.setBeneficiaryName("Electronic Suppliers Co.");
        lc2.setAmount(new BigDecimal("75000.00"));
        lc2.setCurrency("EUR");
        lc2.setIssueDate(LocalDate.now().minusDays(3));
        lc2.setExpiryDate(LocalDate.now().plusMonths(2));
        lc2.setStatus(LCStatus.SUBMITTED);
        lc2.setCreatedBy("importer");
        lc2.setCreatedAt(LocalDateTime.now().minusDays(3));
        lcRepository.save(lc2);

        // Sample Bank Guarantee
        BankGuarantee bg1 = new BankGuarantee();
        bg1.setReferenceNumber("BG-2024-001");
        bg1.setApplicantName("Construction Corp.");
        bg1.setBeneficiaryName("Government Works Dept.");
        bg1.setGuaranteeAmount(new BigDecimal("500000.00"));
        bg1.setCurrency("USD");
        bg1.setGuaranteeType("Performance");
        bg1.setValidityPeriod(LocalDate.now().plusYears(1));
        bg1.setStatus(GuaranteeStatus.ACTIVE);
        bg1.setIssuingBank("Global Trade Bank");
        bg1.setIssueDate(LocalDate.now().minusDays(30));
        bg1.setCreatedBy("importer");
        bg1.setCreatedAt(LocalDateTime.now().minusDays(35));
        bgRepository.save(bg1);

        // Sample Documents
        TradeDocument doc1 = new TradeDocument();
        doc1.setDocumentType("Invoice");
        doc1.setReferenceNumber("LC-2024-001");
        doc1.setUploadedBy("importer");
        doc1.setUploadDate(LocalDate.now().minusDays(5));
        doc1.setStatus(DocumentStatus.APPROVED);
        doc1.setFileName("commercial_invoice.pdf");
        documentRepository.save(doc1);

        TradeDocument doc2 = new TradeDocument();
        doc2.setDocumentType("Bill of Lading");
        doc2.setReferenceNumber("LC-2024-001");
        doc2.setUploadedBy("exporter");
        doc2.setUploadDate(LocalDate.now().minusDays(2));
        doc2.setStatus(DocumentStatus.PENDING_REVIEW);
        doc2.setFileName("bill_of_lading.pdf");
        documentRepository.save(doc2);

        // Sample Risk Assessment
        RiskAssessment risk1 = new RiskAssessment();
        risk1.setTransactionReference("LC-2024-001");
        risk1.setTransactionType("LC");
        risk1.setRiskScore(new BigDecimal("35.00"));
        risk1.setRiskLevel(RiskLevel.MEDIUM);
        risk1.setRiskFactors("{\"countryRisk\":{\"level\":1,\"weight\":20},\"counterpartyRisk\":{\"level\":1,\"weight\":25}}");
        risk1.setAssessedBy("analyst");
        risk1.setAssessmentDate(LocalDate.now().minusDays(8));
        risk1.setRemarks("Standard transaction with established counterparty");
        risk1.setRecommendations("Approve transaction. Proceed with standard verification procedures.");
        riskRepository.save(risk1);

        // Sample Compliance Report
//        Compliance comp1 = new Compliance();
//        comp1.setTransactionReference("LC-2024-001");
//        comp1.setComplianceStatus(ComplianceStatus.COMPLIANT);
//        comp1.setReviewedBy("officer");
//        comp1.setRemarks("All compliance checks passed. KYC verified, no sanctions matches.");
//        comp1.setCreatedAt(LocalDateTime.now().minusDays(9));
//        complianceRepository.save(comp1);

        System.out.println("Sample data initialized successfully!");
    }
}
