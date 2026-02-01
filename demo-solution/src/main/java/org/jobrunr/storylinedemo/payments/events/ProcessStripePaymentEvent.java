package org.jobrunr.storylinedemo.payments.events;

import org.jobrunr.storylinedemo.payments.Payment;

public final class ProcessStripePaymentEvent extends ProcessPaymentEvent {

    public ProcessStripePaymentEvent(Payment payment) {
        super(payment);
    }
}
