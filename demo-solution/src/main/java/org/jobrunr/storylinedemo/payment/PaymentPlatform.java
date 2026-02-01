package org.jobrunr.storylinedemo.payment;

public enum PaymentPlatform {
    JOBRUNR_FINANCE(null),
    PAYPAL("external"),
    STRIPE("external");

    private final String serverTag;

    PaymentPlatform(String serverTag) {
        this.serverTag = serverTag;
    }

    public String getServerTag() {
        return serverTag;
    }

    public String getRateLimiter() {
        return this != JOBRUNR_FINANCE ? this.name() : null;
    }

    public boolean isExternal() {
        return "external".equals(serverTag);
    }
}
