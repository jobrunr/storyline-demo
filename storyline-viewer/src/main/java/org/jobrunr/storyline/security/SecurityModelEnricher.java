package org.jobrunr.storyline.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SecurityModelEnricher {

    @ModelAttribute
    public void addAuthInfo(Model model, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            model.addAttribute("isAuthenticated", false);
        } else {
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("currentUser", auth.getName());
        }

        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrf != null) {
            model.addAttribute("_csrf", csrf);
        }
    }
}
