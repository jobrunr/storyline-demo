package org.jobrunr.storylinedemo.creditcards;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class CreditCardStatementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreditCardStatementService.class);

    private final JobScheduler jobScheduler;
    private final CreditCardRepository creditCardRepository;

    public CreditCardStatementService(CreditCardRepository creditCardRepository, JobScheduler jobScheduler) {
        this.creditCardRepository = creditCardRepository;
        this.jobScheduler = jobScheduler;
    }

    // TODO Step 3: Advanced cron - Run on the LAST BUSINESS DAY of each month (not just last day!)
    // If the 31st is a Saturday, this runs on Friday the 29th instead
    public void generateMonthlyCreditCardStatements() {
        // TODO Step 7: Start a batch to process all statements atomically
        // TODO Step 8: Add onFailure to notify the team if something goes wrong

    }

    public void generateMonthlyExpensesForEachCreditCard() {
        // TODO enqueue a expense report generation job for each credit cards
    }

    // TODO Step 12: Mutex ensures only one PDF generation at a time (one printer!)
    public void generateExpenseReportFor(CreditCard creditCard) {
        generatePDF();
        LOGGER.info("Monthly expenses generated for {}", creditCard);
    }

    // TODO Step 16: Progress bar and logging for long-running jobs
    public void generateStatementsWithProgress(JobContext context) {
    }

    @Job(name = "Generate Summary Report", retries = 3)
    public void generateSummaryReport() {
        generatePDFThatSometimesFails();
        LOGGER.info("Summary Report generated");
    }

    public void notifyOpsTeam() {
        LOGGER.error("🚨 ALERT: Monthly statement generation failed! Notifying ops team via Slack...");
        // In production, this would send to Slack, PagerDuty, email, etc.
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
