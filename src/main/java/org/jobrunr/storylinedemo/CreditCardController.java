package org.jobrunr.storylinedemo;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CreditCardController {

    @GetMapping({"/", "/register"})
    public String showRegistrationForm(Model model) {
        model.addAttribute("creditCard", new CreditCard());
        return "index";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("creditCard") CreditCard creditCard, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "index";
        }

        redirectAttributes.addFlashAttribute("message", "Application submitted successfully! Check your inbox.");
        return "redirect:/register";
    }
}

