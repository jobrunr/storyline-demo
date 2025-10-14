package org.jobrunr.storylinedemo.expenses;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.jobrunr.storylinedemo.creditcards.CreditCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class ExpensesService {

    private final JobScheduler jobScheduler;
    private final CreditCardRepository creditCardRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpensesService.class);

    public ExpensesService(CreditCardRepository creditCardRepository, JobScheduler jobScheduler) {
        this.creditCardRepository = creditCardRepository;
        this.jobScheduler = jobScheduler;
    }

    // Step 2: at 00:00 on day-of-month 1, create a recurring job to start processing monthly expenses
    @Recurring(cron = "0 0 1 * *")
    @Job
    public void startGenerateMonthlyExpensesJob() {
        jobScheduler
                .startBatch(this::generateMonthlyExpensesForEachCreditCardUser)
                // Step 3: continue with a summary report by batching
                .continueWith(this::generateSummaryReport);
    }

    public void generateMonthlyExpensesForEachCreditCardUser() {
        creditCardRepository.findAll().forEach(creditCard -> {
            jobScheduler.enqueue(() -> generateExpenseReportFor(creditCard));
        });
    }

    public void generateSummaryReport() {
        generatePDFThatSometimesFails();
        LOGGER.info("Summary Report generated");
    }

    public void generateExpenseReportFor(CreditCard creditCard) {
        generatePDF();
        LOGGER.info("Monthly expenses generated for {}", creditCard);
    }

    private static void generatePDFThatSometimesFails() {
        if(new Random().nextBoolean()) {
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
