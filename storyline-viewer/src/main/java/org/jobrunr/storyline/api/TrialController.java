package org.jobrunr.storyline.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Captures free-trial sign-ups from the mobile demo and forwards them to the n8n webhook
 * server-side (avoids browser CORS and keeps the webhook URL off the client).
 */
@Controller
public class TrialController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrialController.class);
    private static final String WEBHOOK_URL = "https://n8n.srv851199.hstgr.cloud/webhook/f7a5e38e-4b1d-4f5b-b534-e014ff6b80fe";

    private final RestClient restClient;

    public TrialController(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @PostMapping("/m/trial")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submit(@RequestBody TrialRequest request) {
        String email = request.email() == null ? "" : request.email().trim();
        if (email.isEmpty() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "invalid email"));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", email);
        payload.put("username", "");
        payload.put("form", "trial-demo-mobile");
        payload.put("utm_source", nullToEmpty(request.utm_source()));
        payload.put("utm_medium", nullToEmpty(request.utm_medium()));
        payload.put("utm_campaign", nullToEmpty(request.utm_campaign()));
        payload.put("utm_term", nullToEmpty(request.utm_term()));
        payload.put("utm_content", nullToEmpty(request.utm_content()));

        try {
            restClient.post()
                    .uri(WEBHOOK_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            LOGGER.warn("Failed to forward trial sign-up to webhook", e);
            return ResponseEntity.status(502).body(Map.of("ok", false, "error", "upstream failure"));
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record TrialRequest(
            String email,
            String utm_source,
            String utm_medium,
            String utm_campaign,
            String utm_term,
            String utm_content) {
    }
}
