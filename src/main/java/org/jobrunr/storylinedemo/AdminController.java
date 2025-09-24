package org.jobrunr.storylinedemo;

import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storylinedemo.creditcards.CardType;
import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.jobrunr.storylinedemo.creditcards.CreditCardService;
import org.jobrunr.storylinedemo.expenses.ExpensesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final CreditCardService creditCardService;
    private final ExpensesService expensesService;
    private final JobScheduler jobScheduler;

    public AdminController(CreditCardService creditCardService, ExpensesService expensesService, JobScheduler jobScheduler) {
        this.creditCardService = creditCardService;
        this.expensesService = expensesService;
        this.jobScheduler = jobScheduler;
    }

    @GetMapping({"/bulk-add-cards"})
    public String bulkAddCreditCards() {
        for(int i = 1; i <= 1000; i++) {
            var creditCard = new CreditCard("Random Name #" + i, "random.email" + i + "@gmail.com", i % 2 == 0 ? CardType.AMERICAN_EXPRESS : CardType.MASTERCARD);
            this.creditCardService.processRegistration(creditCard);
        }

        return "redirect:/";
    }

    @GetMapping({"/bulk-generate-expenses"})
    public String triggerMonthlyExpenses() {
        for(int i = 1; i <= 1000; i++) {
            var creditCard = new CreditCard("Random Name #" + i, "random.email" + i + "@gmail.com", i % 2 == 0 ? CardType.AMERICAN_EXPRESS : CardType.MASTERCARD);
            jobScheduler.enqueue(() -> expensesService.generateExpenseReportFor(creditCard));
        }

        return "redirect:/";
    }
}
