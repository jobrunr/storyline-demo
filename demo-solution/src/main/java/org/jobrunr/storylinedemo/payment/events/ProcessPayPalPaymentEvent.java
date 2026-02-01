package org.jobrunr.storylinedemo.payment.events;

import org.jobrunr.storylinedemo.payment.Payment;

public final class ProcessPayPalPaymentEvent extends ProcessPaymentEvent {

    public ProcessPayPalPaymentEvent(Payment payment) {
        super(payment);
    }
}
