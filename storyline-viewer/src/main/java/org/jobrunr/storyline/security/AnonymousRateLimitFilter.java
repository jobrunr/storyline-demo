package org.jobrunr.storyline.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.StringUtils.substringBefore;

/**
 * Keeps the endpoints that anyone may call without signing in from flooding the demo database.
 * Counts requests in fixed one-minute windows, per caller and per endpoint, plus one global window
 * so a botnet cannot simply spread itself over many addresses.
 */
public class AnonymousRateLimitFilter extends OncePerRequestFilter {

    private static final String GLOBAL_KEY = "*";
    private static final int MAX_TRACKED_CALLERS = 10_000;

    private final int perCallerPerMinute;
    private final int globalPerMinute;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AnonymousRateLimitFilter(int perCallerPerMinute, int globalPerMinute) {
        this(perCallerPerMinute, globalPerMinute, Clock.systemUTC());
    }

    AnonymousRateLimitFilter(int perCallerPerMinute, int globalPerMinute, Clock clock) {
        this.perCallerPerMinute = perCallerPerMinute;
        this.globalPerMinute = globalPerMinute;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long minute = clock.millis() / 60_000;
        evictStaleWindows(minute);

        boolean allowed = withinLimit(GLOBAL_KEY, globalPerMinute, minute)
                && withinLimit(callerOf(request) + " " + request.getRequestURI(), perCallerPerMinute, minute);
        if (!allowed) {
            rejectWithRetryAfter(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean withinLimit(String key, int limit, long minute) {
        Window window = windows.compute(key, (ignored, current) ->
                current != null && current.minute() == minute ? current : new Window(minute, new AtomicInteger()));
        return window.hits().incrementAndGet() <= limit;
    }

    private void evictStaleWindows(long minute) {
        if (windows.size() > MAX_TRACKED_CALLERS) {
            windows.values().removeIf(window -> window.minute() < minute);
        }
    }

    private static void rejectWithRetryAfter(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, "60");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"ok\":false,\"message\":\"That was a lot of demo traffic. Give it a minute and try again.\"}");
    }

    private static String callerOf(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : substringBefore(forwardedFor, ",").trim();
    }

    private record Window(long minute, AtomicInteger hits) {
    }
}
