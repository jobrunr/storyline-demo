package org.jobrunr.storyline.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

/**
 * Redirects mobile visitors of the desktop guide to the mobile-first card experience at /m.
 * An explicit {@code ?view=desktop} (persisted in a cookie) opts back into the desktop guide.
 */
public class MobileRedirectInterceptor implements HandlerInterceptor {

    private static final Pattern MOBILE_UA = Pattern.compile("Mobi|Android|iPhone|iPod", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("desktop".equals(request.getParameter("view"))) {
            Cookie cookie = new Cookie("view", "desktop");
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24 * 30);
            response.addCookie(cookie);
            return true;
        }
        if (hasDesktopCookie(request)) return true;

        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && MOBILE_UA.matcher(userAgent).find()) {
            String query = request.getQueryString();
            response.sendRedirect(query == null || query.isBlank() ? "/m" : "/m?" + query);
            return false;
        }
        return true;
    }

    private static boolean hasDesktopCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        for (Cookie cookie : cookies) {
            if ("view".equals(cookie.getName()) && "desktop".equals(cookie.getValue())) return true;
        }
        return false;
    }
}
