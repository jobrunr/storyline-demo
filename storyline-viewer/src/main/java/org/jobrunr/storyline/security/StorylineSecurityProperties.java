package org.jobrunr.storyline.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storyline.security")
public class StorylineSecurityProperties {

    private boolean enabled = false;
    private Mail mail = new Mail();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Mail getMail() { return mail; }
    public void setMail(Mail mail) { this.mail = mail; }

    public static class Mail {
        /** Sender address used in magic link emails (e.g. noreply@yourcompany.com). */
        private String from;

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
    }
}
