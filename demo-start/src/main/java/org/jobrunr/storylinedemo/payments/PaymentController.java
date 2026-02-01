package org.jobrunr.storylinedemo.payments;

import jakarta.validation.Valid;
import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.jobrunr.storylinedemo.creditcards.CreditCardRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final CreditCardRepository creditCardRepository;

    public PaymentController(PaymentService paymentService, CreditCardRepository creditCardRepository) {
        this.paymentService = paymentService;
        this.creditCardRepository = creditCardRepository;
    }

    @GetMapping("/new")
    public String showPaymentForm(Model model) {
        model.addAttribute("payment", new Payment());
        populateFormData(model);
        return "payments/new";
    }

    @PostMapping("/new")
    public String processPaymentForm(@Valid @ModelAttribute("payment") Payment payment,
                                     BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            populateFormData(model);
            return "payments/new";
        }

        paymentService.submitPayment(payment);
        model.addAttribute("payment", new Payment());
        model.addAttribute("success", true);
        populateFormData(model);
        return "payments/new";
    }

    private void populateFormData(Model model) {
        List<CreditCard> activeCards = creditCardRepository.findByState(CreditCard.State.ACTIVE);
        model.addAttribute("creditCards", activeCards);
        model.addAttribute("platforms", PaymentPlatform.values());
    }
}
