package org.jobrunr.storylinedemo;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.jobrunr.storylinedemo.creditcards.CreditCardService;
import org.jobrunr.storylinedemo.creditcards.CreditCardStatementService;
import org.jobrunr.storylinedemo.creditcards.CreditScoreService;
import org.jobrunr.storylinedemo.payment.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.UUID;

@Controller
public class AdminController {

    private final CreditCardService creditCardService;
    private final CreditCardStatementService creditCardStatementService;
    private final CreditScoreService creditScoreService;
    private final PaymentService paymentService;
    private final JobScheduler jobScheduler;

    public AdminController(CreditCardService creditCardService, 
                          CreditCardStatementService creditCardStatementService,
                          CreditScoreService creditScoreService,
                          PaymentService paymentService,
                          JobScheduler jobScheduler) {
        this.creditCardService = creditCardService;
        this.creditCardStatementService = creditCardStatementService;
        this.creditScoreService = creditScoreService;
        this.paymentService = paymentService;
        this.jobScheduler = jobScheduler;
    }

    // ----- Bulk Operations for Demo -----
    
    @GetMapping({"/bulk-add-cards"})
    public String bulkAddCreditCards() {
        for (int i = 1; i <= 100; i++) {
            var creditCard = CreditCard.randomCreditCard(i);
            this.creditCardService.processRegistration(creditCard);
        }
        return "redirect:/";
    }

    @GetMapping({"/bulk-generate-expenses"})
    public String triggerMonthlyExpenses() {
        for (int i = 1; i <= 100; i++) {
            var creditCard = CreditCard.randomCreditCard(i);
            jobScheduler.enqueue(() -> creditCardStatementService.generateExpenseReportFor(creditCard));
        }
        return "redirect:/";
    }

    // ----- Step 16: Progress Bar Demo -----
    
    @GetMapping({"/bulk-generate-with-progress"})
    public String triggerExpensesWithProgress() {
        // This triggers a single job that processes all cards with a progress bar
        jobScheduler.enqueue(() -> creditCardStatementService.generateStatementsWithProgress(JobContext.Null));
        return "redirect:/";
    }

    // ----- Step 17: Credit Score API (Job Results Demo) -----
    
    @GetMapping("/credit-score/request/{customerId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> requestCreditScore(@PathVariable String customerId) {
        UUID jobId = creditScoreService.requestCreditScoreCalculation(customerId);
        return ResponseEntity.accepted().body(Map.of(
            "message", "Credit score calculation started",
            "jobId", jobId.toString(),
            "pollUrl", "/credit-score/result/" + jobId
        ));
    }

    @GetMapping("/credit-score/result/{jobId}")
    @ResponseBody
    public ResponseEntity<?> getCreditScoreResult(@PathVariable UUID jobId) {
        var result = jobScheduler.getJobResult(jobId);
        
        if (result.isAvailable()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.accepted().body(Map.of(
                "status", "processing",
                "message", "Credit score calculation in progress",
                "retryAfterSeconds", 5
            ));
        }
    }

    // ----- Step 14: Trigger Payments (Server Tags Demo) -----
    
    @GetMapping({"/trigger-payments"})
    public String triggerPayments() {
        paymentService.processAllPaymentsNightly();
        return "redirect:/";
    }
}
