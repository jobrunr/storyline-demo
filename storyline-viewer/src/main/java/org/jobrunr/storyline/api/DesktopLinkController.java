package org.jobrunr.storyline.api;

import jakarta.servlet.http.HttpServletRequest;
import org.jobrunr.storyline.security.StorylineSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Emails mobile visitors a permanent link to the guided tour so they can pick it up on a desktop
 * (the magic-link email is a 15-minute sign-in token, so it is the wrong tool for "later today").
 * The address also goes to the same n8n webhook as the trial forms, as {@code form: tour_mobile}.
 */
@Controller
public class DesktopLinkController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DesktopLinkController.class);

    private final RestClient restClient;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final StorylineSecurityProperties securityProperties;

    public DesktopLinkController(RestClient.Builder restClientBuilder, ObjectProvider<JavaMailSender> mailSender,
            StorylineSecurityProperties securityProperties) {
        this.restClient = restClientBuilder.build();
        this.mailSender = mailSender;
        this.securityProperties = securityProperties;
    }

    @PostMapping("/m/desktop-link")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendDesktopLink(@RequestBody DesktopLinkRequest body, HttpServletRequest request) {
        String email = body.email() == null ? "" : body.email().trim();
        if (!isPlausibleEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "invalid email"));
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            return ResponseEntity.status(503).body(Map.of("ok", false, "error", "mail not configured"));
        }

        try {
            sendEmail(sender, email, buildTourLink(request), buildLogoUrl(request));
        } catch (Exception e) {
            LOGGER.warn("Failed to send desktop link email", e);
            return ResponseEntity.status(502).body(Map.of("ok", false, "error", "mail failure"));
        }
        forwardToWebhook(email, body);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private void sendEmail(JavaMailSender sender, String to, String tourLink, String logoUrl) throws Exception {
        var mime = sender.createMimeMessage();
        var helper = new MimeMessageHelper(mime, "UTF-8");
        helper.setFrom(securityProperties.getMail().getFrom());
        helper.setTo(to);
        helper.setSubject("Your link to the JobRunr Pro guided tour");
        helper.setText(buildEmailBody(tourLink, logoUrl), true);
        sender.send(mime);
        LOGGER.info("Desktop link sent to {}", to);
    }

    private void forwardToWebhook(String email, DesktopLinkRequest body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", email);
        payload.put("username", "");
        payload.put("company", "");
        payload.put("form", "tour_mobile");
        payload.put("utm_source", nullToEmpty(body.utm_source()));
        payload.put("utm_medium", nullToEmpty(body.utm_medium()));
        payload.put("utm_campaign", nullToEmpty(body.utm_campaign()));
        payload.put("utm_term", nullToEmpty(body.utm_term()));
        payload.put("utm_content", nullToEmpty(body.utm_content()));
        try {
            restClient.post()
                    .uri(TrialController.WEBHOOK_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // The visitor got their email; losing the lead is not worth failing the request over.
            LOGGER.warn("Failed to forward desktop-link sign-up to webhook", e);
        }
    }

    private static String buildTourLink(HttpServletRequest request) {
        return baseUrl(request)
                .path("/tour")
                .queryParam("utm_source", "demo-mobile")
                .queryParam("utm_medium", "email")
                .queryParam("utm_campaign", "continue-on-desktop")
                .toUriString();
    }

    private static String buildLogoUrl(HttpServletRequest request) {
        return baseUrl(request).path("/jobrunr-logo-png.png").toUriString();
    }

    private static UriComponentsBuilder baseUrl(HttpServletRequest request) {
        return UriComponentsBuilder.fromUriString(request.getRequestURL().toString())
                .replacePath(request.getContextPath())
                .replaceQuery(null)
                .fragment(null);
    }

    private static boolean isPlausibleEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 && email.indexOf('.', at) > at + 1 && !email.endsWith(".") && !email.contains(" ");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record DesktopLinkRequest(
            String email,
            String utm_source,
            String utm_medium,
            String utm_campaign,
            String utm_term,
            String utm_content) {
    }

    private String buildEmailBody(String tourLink, String logoUrl) {
        return """
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <meta name="x-apple-disable-message-reformatting">
                </head>
                <body style="margin:0;padding:0;background:#f4f4f5;font-family:Arial,Helvetica,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" role="presentation">
                  <tr><td align="center" bgcolor="#f4f4f5" style="padding:32px 16px;">
                    <table width="520" cellpadding="0" cellspacing="0" role="presentation" bgcolor="#ffffff" style="background:#ffffff;">

                      <!-- Header -->
                      <tr>
                        <td bgcolor="#7952b3" style="background:#7952b3;padding:24px 32px;">
                          <img src="%s" alt="JobRunr" height="32"
                               style="display:block;border:0;height:32px;">
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding:36px 32px 24px;">
                          <p style="margin:0 0 8px;font-size:18px;font-weight:bold;color:#18181b;font-family:Arial,Helvetica,sans-serif;">Back on a proper screen?</p>
                          <p style="margin:0 0 28px;font-size:14px;color:#71717a;line-height:1.6;font-family:Arial,Helvetica,sans-serif;">
                            You were browsing the JobRunr Pro demo on your phone and asked for the desktop link.
                            Here it is: <strong>JobRunr Finance</strong>, a neobank where the money is imaginary and the
                            background jobs are very real. Twenty-one short steps, one live dashboard, zero slides.
                          </p>
                          <!-- Button wrapped in table so bgcolor works in Outlook -->
                          <table cellpadding="0" cellspacing="0" role="presentation">
                            <tr>
                              <td bgcolor="#7952b3" style="background:#7952b3;">
                                <a href="%s"
                                   style="display:inline-block;color:#ffffff;text-decoration:none;
                                          padding:12px 28px;font-size:14px;font-weight:600;
                                          font-family:Arial,Helvetica,sans-serif;">
                                  Open the guided tour
                                </a>
                              </td>
                            </tr>
                          </table>
                          <p style="margin:28px 0 0;font-size:12px;color:#a1a1aa;line-height:1.6;font-family:Arial,Helvetica,sans-serif;">
                            Or copy this link into your browser:<br>
                            <a href="%s" style="color:#7952b3;word-break:break-word;">%s</a>
                          </p>
                          <p style="margin:16px 0 0;font-size:12px;color:#a1a1aa;font-family:Arial,Helvetica,sans-serif;">
                            If you didn't request this, you can safely ignore this email.
                          </p>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="padding:16px 32px;border-top:1px solid #f4f4f5;">
                          <p style="margin:0;font-size:11px;color:#a1a1aa;text-align:center;font-family:Arial,Helvetica,sans-serif;">
                            © JobRunr &nbsp;·&nbsp; Background job processing for the JVM
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(logoUrl, tourLink, tourLink, tourLink);
    }
}
