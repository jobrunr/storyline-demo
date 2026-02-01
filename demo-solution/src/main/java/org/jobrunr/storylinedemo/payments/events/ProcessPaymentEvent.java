package org.jobrunr.storylinedemo.payments.events;

import org.jobrunr.storylinedemo.payments.Payment;
import org.springframework.context.ApplicationEvent;

public sealed abstract class ProcessPaymentEvent extends ApplicationEvent
    permits ProcessJobRunrFinancePaymentEvent, ProcessPayPalPaymentEvent, ProcessStripePaymentEvent {

    public ProcessPaymentEvent(Payment payment) {
        super(payment);
    }

    public Payment getPayment() {
        return (Payment) getSource();
    }
}
