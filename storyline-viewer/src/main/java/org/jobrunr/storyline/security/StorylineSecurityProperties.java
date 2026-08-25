package org.jobrunr.storyline.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storyline.security")
public class StorylineSecurityProperties {

    private boolean enabled = false;
    private Mail mail = new Mail();
    private RateLimit rateLimit = new RateLimit();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Mail getMail() { return mail; }
    public void setMail(Mail mail) { this.mail = mail; }

    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }

    public static class Mail {
        /** Sender address used in magic link emails (e.g. noreply@yourcompany.com). */
        private String from;

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
    }

    /** Guards the endpoints the tour may call before anyone has signed in. */
    public static class RateLimit {
        /** Requests one caller may make to one pre-auth endpoint within a minute. */
        private int perCallerPerMinute = 6;

        /** Requests all callers together may make to the pre-auth endpoints within a minute. */
        private int globalPerMinute = 60;

        public int getPerCallerPerMinute() { return perCallerPerMinute; }
        public void setPerCallerPerMinute(int perCallerPerMinute) { this.perCallerPerMinute = perCallerPerMinute; }

        public int getGlobalPerMinute() { return globalPerMinute; }
        public void setGlobalPerMinute(int globalPerMinute) { this.globalPerMinute = globalPerMinute; }
    }
}
