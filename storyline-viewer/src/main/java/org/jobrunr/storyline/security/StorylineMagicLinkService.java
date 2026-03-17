package org.jobrunr.storyline.security;

import jakarta.mail.MessagingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jobrunr.storyline.model.Storyline;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

public class StorylineMagicLinkService implements OneTimeTokenGenerationSuccessHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorylineMagicLinkService.class);

    private final OneTimeTokenService oneTimeTokenService;
    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String demoTitle;

    public StorylineMagicLinkService(OneTimeTokenService oneTimeTokenService,
            JavaMailSender mailSender, String fromEmail, Storyline storyline) {
        this.oneTimeTokenService = oneTimeTokenService;
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.demoTitle = storyline.title();
    }

    // Called by our controller: generates the token and sends the email
    public void sendMagicLink(HttpServletRequest request, String email) {
        var token = oneTimeTokenService.generate(new GenerateOneTimeTokenRequest(email));
        sendEmail(email, buildLink(request, token), buildLogoUrl(request));
    }

    // Called by Spring Security's GenerateOneTimeTokenFilter (POST /ott/generate)
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, OneTimeToken oneTimeToken)
            throws IOException, ServletException {
        sendEmail(oneTimeToken.getUsername(), buildLink(request, oneTimeToken), buildLogoUrl(request));
        response.sendRedirect(request.getContextPath() + "/login?sent=true");
    }

    private String buildLink(HttpServletRequest request, OneTimeToken token) {
        return UriComponentsBuilder.fromUriString(UrlUtils.buildFullRequestUrl(request))
                .replacePath(request.getContextPath())
                .replaceQuery(null)
                .fragment(null)
                .path("/login/ott")
                .queryParam("token", token.getTokenValue())
                .toUriString();
    }

    private String buildLogoUrl(HttpServletRequest request) {
        return UriComponentsBuilder.fromUriString(UrlUtils.buildFullRequestUrl(request))
                .replacePath(request.getContextPath())
                .replaceQuery(null)
                .fragment(null)
                .path("/jobrunr-logo-white.webp")
                .toUriString();
    }

    private void sendEmail(String to, String magicLink, String logoUrl) {
        try {
            var mime = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Your sign-in link for " + demoTitle);
            helper.setText(buildEmailBody(magicLink, logoUrl), true);
            mailSender.send(mime);
            LOGGER.info("Magic link sent to {}", to);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send magic link email", e);
        }
    }

    private String buildEmailBody(String magicLink, String logoUrl) {
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
                          <p style="margin:0 0 8px;font-size:18px;font-weight:bold;color:#18181b;font-family:Arial,Helvetica,sans-serif;">Sign in to %s</p>
                          <p style="margin:0 0 28px;font-size:14px;color:#71717a;line-height:1.6;font-family:Arial,Helvetica,sans-serif;">
                            Click the button below to sign in. This link expires in <strong>15&nbsp;minutes</strong>.
                          </p>
                          <!-- Button wrapped in table so bgcolor works in Outlook -->
                          <table cellpadding="0" cellspacing="0" role="presentation">
                            <tr>
                              <td bgcolor="#7952b3" style="background:#7952b3;">
                                <a href="%s"
                                   style="display:inline-block;color:#ffffff;text-decoration:none;
                                          padding:12px 28px;font-size:14px;font-weight:600;
                                          font-family:Arial,Helvetica,sans-serif;">
                                  Sign in to %s
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
                """.formatted(logoUrl, demoTitle, magicLink, demoTitle, magicLink, magicLink);
    }
}
