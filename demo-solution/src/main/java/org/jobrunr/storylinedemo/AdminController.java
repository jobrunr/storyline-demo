package org.jobrunr.storylinedemo;

import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.jobrunr.storylinedemo.creditcards.CreditCardService;
import org.jobrunr.storylinedemo.creditcards.CreditCardStatementService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final CreditCardService creditCardService;
    private final CreditCardStatementService creditCardStatementService;
    private final JobScheduler jobScheduler;

    public AdminController(CreditCardService creditCardService, CreditCardStatementService creditCardStatementService, JobScheduler jobScheduler) {
        this.creditCardService = creditCardService;
        this.creditCardStatementService = creditCardStatementService;
        this.jobScheduler = jobScheduler;
    }

    @GetMapping({"/bulk-add-cards"})
    public String bulkAddCreditCards() {
        for (int i = 1; i <= 1000; i++) {
            var creditCard = CreditCard.randomCreditCard(i);
            this.creditCardService.processRegistration(creditCard);
        }

        return "redirect:/";
    }

    @GetMapping({"/bulk-generate-expenses"})
    public String triggerMonthlyExpenses() {
        for (int i = 1; i <= 1000; i++) {
            var creditCard = CreditCard.randomCreditCard(i);
            jobScheduler.enqueue(() -> creditCardStatementService.generateExpenseReportFor(creditCard));
        }

        return "redirect:/";
    }
}
