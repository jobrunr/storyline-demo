package org.jobrunr.guide;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("/credit-card-form")
    public String getCreditCardForm(Model model) {
        return "fragments/credit-card-form";
    }

    @PostMapping("/credit-card-submit")
    public String submitCreditCard(
        @RequestParam String name,
        @RequestParam String email,
        @RequestParam String cardType,
        Model model
    ) {
        // Simulate job creation
        model.addAttribute("success", true);
        model.addAttribute("message", String.format(
            "Credit card application submitted for %s! Check the JobRunr dashboard to see the background jobs.",
            name
        ));
        return "fragments/success-message";
    }

    @GetMapping("/expense-trigger")
    public String getExpenseTrigger(Model model) {
        return "fragments/expense-trigger";
    }

    @PostMapping("/expense-trigger")
    public String triggerExpense(@RequestParam(required = false) String count, Model model) {
        int numExpenses = count != null ? Integer.parseInt(count) : 1;
        model.addAttribute("success", true);
        model.addAttribute("message", String.format(
            "Triggered %d expense report%s! Check the dashboard.",
            numExpenses,
            numExpenses > 1 ? "s" : ""
        ));
        return "fragments/success-message";
    }

    @GetMapping("/payment-form")
    public String getPaymentForm(Model model) {
        return "fragments/payment-form";
    }

    @PostMapping("/payment-submit")
    public String submitPayment(
        @RequestParam String amount,
        @RequestParam String recipient,
        @RequestParam(required = false) String priority,
        @RequestParam(required = false) String international,
        Model model
    ) {
        model.addAttribute("success", true);
        String paymentType = "true".equals(international) ? "international" : "domestic";
        String priorityText = "high".equals(priority) ? " high-priority" : "";
        model.addAttribute("message", String.format(
            "Payment of $%s to %s queued as%s %s payment! Check the dashboard.",
            amount, recipient, priorityText, paymentType
        ));
        return "fragments/success-message";
    }
}
