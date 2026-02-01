package org.jobrunr.storylinedemo.payments.events;

import org.jobrunr.storylinedemo.payments.Payment;

public final class ProcessJobRunrFinancePaymentEvent extends ProcessPaymentEvent {

    public ProcessJobRunrFinancePaymentEvent(Payment payment) {
        super(payment);
    }
}
