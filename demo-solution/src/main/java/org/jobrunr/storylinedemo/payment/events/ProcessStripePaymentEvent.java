package org.jobrunr.storylinedemo.payment.events;

import org.jobrunr.storylinedemo.payment.Payment;

public final class ProcessStripePaymentEvent extends ProcessPaymentEvent {

    public ProcessStripePaymentEvent(Payment payment) {
        super(payment);
    }
}
