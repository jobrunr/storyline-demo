package org.jobrunr.storylinedemo.expenses;

import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.jobrunr.storylinedemo.creditcards.CreditCardRepository;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class ExpensesService {

    private final JobScheduler jobScheduler;
    private final CreditCardRepository creditCardRepository;

    public ExpensesService(CreditCardRepository creditCardRepository, JobScheduler jobScheduler) {
        this.creditCardRepository = creditCardRepository;
        this.jobScheduler = jobScheduler;
    }

    // TODO Step 2: at 00:00 on day-of-month 1, create a recurring job to start processing monthly expenses
    public void startGenerateMonthlyExpensesJob() {
        // TODO Step 3: continue with a summary report by batching
    }

    public void generateMonthlyExpensesForEachCreditCardUser() {
        creditCardRepository.findAll().forEach(creditCard -> {
            generateExpenseReportFor(creditCard);
        });
    }

    public void generateSummaryReport() {
        generatePDFThatSometimesFails();
        System.out.println("Summary Report generated");
    }

    public void generateExpenseReportFor(CreditCard creditCard) {
        generatePDF();
        System.out.println("Monthly expenses generated for " + creditCard);
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
