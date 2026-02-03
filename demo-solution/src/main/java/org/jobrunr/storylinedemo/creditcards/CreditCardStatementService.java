package org.jobrunr.storylinedemo.creditcards;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobDashboardProgressBar;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storylinedemo.queues.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.stream.StreamSupport;

@Service
public class CreditCardStatementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreditCardStatementService.class);

    private final JobScheduler jobScheduler;
    private final CreditCardRepository creditCardRepository;

    public CreditCardStatementService(CreditCardRepository creditCardRepository, JobScheduler jobScheduler) {
        this.creditCardRepository = creditCardRepository;
        this.jobScheduler = jobScheduler;
    }

    // Step 3: Advanced cron - Run on the LAST BUSINESS DAY of each month (not just last day!)
    // If the 31st is a Saturday, this runs on Friday the 29th instead
    @Recurring(id = "monthly-statements", cron = "0 0 LW * *")
    @Job(name = "Crate BatchJob to Generate Monthly Credit Card Statements")
    public void generateMonthlyCreditCardStatements() {
        // Step 7: Start a batch to process all statements atomically
        jobScheduler
                .startBatch(this::generateMonthlyExpensesForEachCreditCard)
                .continueWith(this::generateSummaryReport)
                // Step 8: Add onFailure to notify the team if something goes wrong
                .onFailure(this::notifyOpsTeam);
    }

    @Job(name = "Generate Monthly Credit Card Statements for all Cardholders")
    public void generateMonthlyExpensesForEachCreditCard() {
        jobScheduler.enqueue(
            StreamSupport.stream(creditCardRepository.findAll().spliterator(), false),
            creditCard -> generateExpenseReportFor(creditCard)
        );
    }

    // Step 12: Mutex ensures only one PDF generation at a time (one printer!)
    @Job(name = "Generate Statement for %0", mutex = "pdf-printer")
    public void generateExpenseReportFor(CreditCard creditCard) {
        generatePDF();
        LOGGER.info("Monthly expenses generated for {}", creditCard);
    }

    // Step 16: Progress bar and logging for long-running jobs
    @Job(name = "Generate Bulk Statements with Progress")
    public void generateStatementsWithProgress(JobContext context) {
        List<CreditCard> cards = new java.util.ArrayList<>();
        creditCardRepository.findAll().forEach(cards::add);
        
        JobDashboardProgressBar progressBar = context.progressBar(cards.size());
        
        int progress = 0;
        for (CreditCard card : cards) {
            generatePDF();
            progress++;
            progressBar.setProgress(progress);
            context.logger().info("Generated statement for: " + card.getEmail());
        }
        
        context.logger().info("All statements generated successfully!");
    }

    @Job(name = "Generate Summary Report", queue = Priority.LOW, retries = 3)
    public void generateSummaryReport() {
        generatePDFThatSometimesFails();
        LOGGER.info("Summary Report generated");
    }

    // Step 8: Notification job that runs when the batch fails
    @Job(name = "Notify Ops Team of Failure")
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
