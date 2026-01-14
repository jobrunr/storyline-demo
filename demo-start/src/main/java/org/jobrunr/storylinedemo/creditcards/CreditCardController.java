package org.jobrunr.storylinedemo.creditcards;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        return "credit-cards/index";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("creditCard") CreditCard creditCard, BindingResult bindingResult, Model model, HttpServletRequest request, HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "credit-cards/index";
        }

        creditCardService.processRegistration(creditCard);
        model.addAttribute("creditCard", creditCard);
        model.addAttribute("success", true);
        model.addAttribute("cardTypes", CreditCard.Type.values());
        return "credit-cards/index";
    }
}

