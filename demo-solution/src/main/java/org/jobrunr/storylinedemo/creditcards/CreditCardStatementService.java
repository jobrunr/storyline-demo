package org.jobrunr.storylinedemo.creditcards;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class CreditCardStatementService {

    private final JobScheduler jobScheduler;
    private final CreditCardRepository creditCardRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(CreditCardStatementService.class);

    public CreditCardStatementService(CreditCardRepository creditCardRepository, JobScheduler jobScheduler) {
        this.creditCardRepository = creditCardRepository;
        this.jobScheduler = jobScheduler;
    }

    // At 00:00 on day-of-month 1, create a recurring job to start processing monthly expenses
    @Recurring(cron = "0 0 1 * *")
    @Job(name = "Generate Monthly Credit Card Statements for all cardholders")
    public void generateMonthlyCreditCardStatements() {
        // Step 1: start a batch job to process monthly expenses for all cardholders
        jobScheduler
                .startBatch(this::generateMonthlyExpensesForAllCreditCardHolders)
                // Step 2: continue with a summary report when the batch ended
                .continueWith(this::generateSummaryReport);
    }

    public void generateMonthlyExpensesForAllCreditCardHolders() {
        creditCardRepository.findAll().forEach(creditCard -> {
            jobScheduler.enqueue(() -> generateExpenseReportFor(creditCard));
        });
    }

    public void generateExpenseReportFor(CreditCard creditCard) {
        generatePDF();
        LOGGER.info("Monthly expenses generated for {}", creditCard);
    }

    public void generateSummaryReport() {
        generatePDFThatSometimesFails();
        LOGGER.info("Summary Report generated");
    }

    private static void generatePDFThatSometimesFails() {
        if (new Random().nextBoolean()) {
            throw new RuntimeException("Something went wrong while generating pdf");
        }

        generatePDF();
    }

    private static void generatePDF() {
        // Pretend PDFs are being generated
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
