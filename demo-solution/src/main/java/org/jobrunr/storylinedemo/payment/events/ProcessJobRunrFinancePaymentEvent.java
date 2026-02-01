package org.jobrunr.storylinedemo.payment.events;

import org.jobrunr.storylinedemo.payment.Payment;

public final class ProcessJobRunrFinancePaymentEvent extends ProcessPaymentEvent {

    public ProcessJobRunrFinancePaymentEvent(Payment payment) {
        super(payment);
    }
}
