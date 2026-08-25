package org.jobrunr.storyline.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SecurityModelEnricher {

    private final StorylineSecurityProperties securityProperties;
    private final Environment environment;

    public SecurityModelEnricher(StorylineSecurityProperties securityProperties, Environment environment) {
        this.securityProperties = securityProperties;
        this.environment = environment;
    }

    @ModelAttribute
    public void addAuthInfo(Model model, HttpServletRequest request) {
        model.addAttribute("securityEnabled", securityProperties.isEnabled());
        model.addAttribute("isLiveDemo", environment.matchesProfiles("prd"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            model.addAttribute("isAuthenticated", false);
        } else {
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("currentUser", auth.getName());
        }

        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrf != null) {
            model.addAttribute("_csrf", Csrf.of(csrf));
        }
    }

    /**
     * Spring Security hands out a package-private CsrfToken implementation, which template engines
     * cannot reflect into. Copying the three values templates actually use keeps
     * {@code {{ _csrf.parameterName }}} working without reaching into a class we do not own.
     */
    public record Csrf(String parameterName, String headerName, String token) {

        static Csrf of(CsrfToken csrfToken) {
            return new Csrf(csrfToken.getParameterName(), csrfToken.getHeaderName(), csrfToken.getToken());
        }
    }
}
