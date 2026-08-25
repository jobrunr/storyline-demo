package org.jobrunr.storyline.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Sends a freshly signed-in visitor back to where they asked to sign in, instead of dropping everyone
 * on the guide's welcome screen. The tour writes its own location into the {@value #COOKIE} cookie
 * before it asks for an email, so the magic link lands on the exact step that was gated.
 */
public class StorylineAuthSuccessHandler implements AuthenticationSuccessHandler {

    public static final String COOKIE = "tour-return";

    private static final Pattern SAFE_RETURN = Pattern.compile("^/(tour|storyline)(/step/\\d{1,2})?$");
    private static final String DEFAULT_TARGET = "/storyline";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        String target = returnTarget(request).orElse(DEFAULT_TARGET);
        clearCookie(request, response);
        response.sendRedirect(request.getContextPath() + target);
    }

    private static Optional<String> returnTarget(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(cookie -> COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && SAFE_RETURN.matcher(value).matches())
                .findFirst();
    }

    private static void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie expired = new Cookie(COOKIE, "");
        expired.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        expired.setMaxAge(0);
        response.addCookie(expired);
    }
}
