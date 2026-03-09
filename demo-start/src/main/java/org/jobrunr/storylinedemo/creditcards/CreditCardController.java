package org.jobrunr.storylinedemo.creditcards;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.stream.Collectors;

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
        model.addAttribute("cardTypes", CreditCard.Type.values());
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", fieldErrors(bindingResult));
            return "credit-cards/register";
        }

        creditCardService.processRegistration(creditCard);
        model.addAttribute("creditCard", creditCard);
        model.addAttribute("success", true);
        return "credit-cards/register";
    }

    @GetMapping("/activate")
    public String showActivationForm() {
        return "credit-cards/activate";
    }

    @PostMapping("/activate")
    public String processActivationForm(@RequestParam String number, Model model) {
        creditCardService.processActivation(number);
        model.addAttribute("success", true);
        return "credit-cards/activate";
    }

    private Map<String, String> fieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage(),
                        (a, b) -> a));
    }
}