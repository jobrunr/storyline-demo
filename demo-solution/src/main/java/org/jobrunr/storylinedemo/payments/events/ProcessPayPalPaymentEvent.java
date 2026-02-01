package org.jobrunr.storylinedemo.payments.events;

import org.jobrunr.storylinedemo.payments.Payment;

public final class ProcessPayPalPaymentEvent extends ProcessPaymentEvent {

    public ProcessPayPalPaymentEvent(Payment payment) {
        super(payment);
    }
}
