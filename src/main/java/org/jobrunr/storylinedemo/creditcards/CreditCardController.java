package org.jobrunr.storylinedemo.creditcards;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CreditCardController {

    private final CreditCardService creditCardService;

    public CreditCardController(CreditCardService creditCardService) {
        this.creditCardService = creditCardService;
    }

    @GetMapping({"/", "/register"})
    public String showRegistrationForm(Model model) {
        model.addAttribute("creditCard", new CreditCard());
        return "index";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("creditCard") CreditCard creditCard, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "index";
        }

        creditCardService.processRegistration(creditCard);
        return "redirect:/register?success";
    }
}

