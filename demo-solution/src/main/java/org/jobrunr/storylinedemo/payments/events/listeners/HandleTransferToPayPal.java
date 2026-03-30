package org.jobrunr.storylinedemo.payments.events.listeners;

import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.storylinedemo.payments.Payment;
import org.jobrunr.storylinedemo.payments.events.ProcessPayPalPaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class HandleTransferToPayPal implements ApplicationListener<ProcessPayPalPaymentEvent> {
    private static final Logger LOGGER = new JobRunrDashboardLogger(LoggerFactory.getLogger(HandleTransferToPayPal.class));

    @Override
    public void onApplicationEvent(ProcessPayPalPaymentEvent event) {
        Payment payment = event.getPayment();

        LOGGER.info("PayPal transfer to: {}", payment.getRecipient());
        simulateWork(800);
        LOGGER.info("PayPal transfer completed for payment #{}", payment.getId());
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
