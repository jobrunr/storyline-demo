package org.jobrunr.storylinedemo.creditcards;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/credit-cards")
public class CreditCardController {

    private final CreditCardService creditCardService;

    public CreditCardController(CreditCardService creditCardService) {
        this.creditCardService = creditCardService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("creditCard", new CreditCard());
        model.addAttribute("cardTypes", CreditCard.Type.values());
        return "credit-cards/register";
    }

    @PostMapping("/register")
    public String processRegistrationForm(@Valid @ModelAttribute("creditCard") CreditCard creditCard, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "credit-cards/register";
        }

        creditCardService.processRegistration(creditCard);
        model.addAttribute("creditCard", creditCard);
        model.addAttribute("success", true);
        model.addAttribute("cardTypes", CreditCard.Type.values());
        return "credit-cards/register";
    }

    @GetMapping("/activate")
    public String showActivationForm(Model model) {
        return "credit-cards/activate";
    }

    @PostMapping("/activate")
    public String processActivationForm(@Valid @ModelAttribute("creditCard") CreditCard creditCard, BindingResult bindingResult, Model model) {
        creditCardService.processActivation(creditCard);
        return "credit-cards/activate";
    }
}