package org.jobrunr.storyline.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The sign-in loop of the tour, in two calls. The popup asks for an email, the visitor clicks the
 * magic link in a second tab, and this tab notices because {@code /tour/me} starts saying yes.
 * Nobody ever leaves the tour.
 */
@RestController
public class TourAuthController {

    private final StorylineUserRepository userRepository;
    private final StorylineMagicLinkService magicLinkService;

    public TourAuthController(StorylineUserRepository userRepository, StorylineMagicLinkService magicLinkService) {
        this.userRepository = userRepository;
        this.magicLinkService = magicLinkService;
    }

    @GetMapping("/tour/me")
    public Identity me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
        return new Identity(authenticated, authenticated ? auth.getName() : null);
    }

    @PostMapping("/tour/magic-link")
    public ResponseEntity<MagicLinkResult> magicLink(@RequestBody MagicLinkRequest body, HttpServletRequest request) {
        String email = body.email() == null ? "" : body.email().trim();
        if (!isPlausibleEmail(email)) {
            return ResponseEntity.badRequest().body(MagicLinkResult.INVALID_EMAIL);
        }

        if (!userRepository.existsByEmail(email)) {
            String name = body.name() == null ? "" : body.name().trim();
            if (name.isEmpty()) {
                return ResponseEntity.ok(MagicLinkResult.NEEDS_REGISTRATION);
            }
            userRepository.save(StorylineUser.newUser(email, name, body.company()));
        }

        magicLinkService.sendMagicLink(request, email);
        return ResponseEntity.ok(MagicLinkResult.SENT);
    }

    private static boolean isPlausibleEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 && email.indexOf('.', at) > at + 1 && !email.endsWith(".") && !email.contains(" ");
    }

    public record Identity(boolean authenticated, String email) {
    }

    public record MagicLinkRequest(String email, String name, String company) {
    }

    public record MagicLinkResult(String status, String message) {

        static final MagicLinkResult INVALID_EMAIL = new MagicLinkResult("invalid-email", "That does not look like an email address.");
        static final MagicLinkResult NEEDS_REGISTRATION = new MagicLinkResult("needs-registration", "First time here? Tell us who you are.");
        static final MagicLinkResult SENT = new MagicLinkResult("sent", "Check your inbox for the sign-in link.");
    }
}
