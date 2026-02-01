package org.jobrunr.storylinedemo.payment.events.listeners;

import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.storylinedemo.payment.Payment;
import org.jobrunr.storylinedemo.payment.events.ProcessStripePaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class HandleTransferToStripe implements ApplicationListener<ProcessStripePaymentEvent> {
    private static final Logger LOGGER = new JobRunrDashboardLogger(LoggerFactory.getLogger(HandleTransferToStripe.class));

    @Override
    public void onApplicationEvent(ProcessStripePaymentEvent event) {
        Payment payment = event.getPayment();

        LOGGER.info("Stripe transfer to: {}", payment.getRecipient());
        simulateWork(600);
        LOGGER.info("Stripe transfer completed for payment #{}", payment.getId());
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
