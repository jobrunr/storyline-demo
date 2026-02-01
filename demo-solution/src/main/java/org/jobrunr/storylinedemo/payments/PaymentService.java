package org.jobrunr.storylinedemo.payments;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.jobrunr.storylinedemo.creditcards.CreditCardRepository;
import org.jobrunr.storylinedemo.exceptions.NonRetryableException;
import org.jobrunr.storylinedemo.payments.events.ProcessJobRunrFinancePaymentEvent;
import org.jobrunr.storylinedemo.payments.events.ProcessPayPalPaymentEvent;
import org.jobrunr.storylinedemo.payments.events.ProcessStripePaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.jobrunr.scheduling.JobBuilder.aJob;

@Service
public class PaymentService {

    private static final Logger LOGGER = new JobRunrDashboardLogger(LoggerFactory.getLogger(PaymentService.class));

    private final JobScheduler jobScheduler;
    private final PaymentRepository paymentRepository;
    private final CreditCardRepository creditCardRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final String serverTags;

    public PaymentService(JobScheduler jobScheduler,
                          PaymentRepository paymentRepository,
                          CreditCardRepository creditCardRepository,
                          ApplicationEventPublisher applicationEventPublisher,
                          @Value("${jobrunr.background-job-server.tags:}") String serverTags) {
        this.jobScheduler = jobScheduler;
        this.paymentRepository = paymentRepository;
        this.creditCardRepository = creditCardRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.serverTags = serverTags;
    }

    @Transactional
    public void submitPayment(Payment payment) {
        var savedPayment = paymentRepository.save(payment);
        createPaymentProcessingJob(savedPayment);
    }

    private void createPaymentProcessingJob(Payment payment) {
        CreditCard creditCard = creditCardRepository.findById(payment.getCreditCardId())
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found: " + payment.getCreditCardId()));

        var processPaymentJob = jobScheduler.create(aJob()
                .withQueue("high-prio")
                .withName("Process payment #" + payment.getId())
                .withLabels("cardType:" + creditCard.getType().name())
                .withServerTag(payment.getPlatform().getServerTag())
                .withRateLimiter(payment.getPlatform().isExternal() ? payment.getPlatform().name() : null)
                .withDetails(() -> processPayment(payment.getId(), JobContext.Null)));

        // Chain government reporting for large payments (> $10k)
        if (payment.requiresGovernmentReporting()) {
            jobScheduler.create(aJob()
                    .withName("Reporting big money transfer")
                    .withRateLimiter("REPORTING")
                    .runAfterSuccessOf(processPaymentJob.asUUID())
                    .withDetails(() -> reportToGovernment(payment.getId())));
        }
    }

    public void processPayment(Long paymentId, JobContext context) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        context.runStepOnce("payment-processing", () -> markPaymentAsProcessing(payment));

        context.runStepOnce("charge-card", () -> chargeCard(payment));

        context.runStepOnce("transfer-money", () -> processPlatformTransfer(payment));

        context.runStepOnce("payment-completed", () -> markPaymentAsCompleted(payment));

        LOGGER.info("Payment processed successfully: {}", payment);
    }

    public void markPaymentAsProcessing(Payment payment) {
        LOGGER.info("Updating payment status to PROCESSING: {}", payment);
        payment.setStatus(Payment.Status.PROCESSING);
        paymentRepository.save(payment);
    }

    public void chargeCard(Payment payment) {
        LOGGER.info("Charging card for payment: {}", payment);
        CreditCard creditCard = creditCardRepository.findById(payment.getCreditCardId())
                .orElseThrow(() -> new NonRetryableException("Credit card not found"));
        creditCard.deductBalance(payment.getAmount());
        creditCardRepository.save(creditCard);
    }

    private void processPlatformTransfer(Payment payment) {
        switch (payment.getPlatform()) {
            case JOBRUNR_FINANCE -> applicationEventPublisher.publishEvent(
                new ProcessJobRunrFinancePaymentEvent(payment));
            case PAYPAL -> {
                requireServerTag("external");
                applicationEventPublisher.publishEvent(new ProcessPayPalPaymentEvent(payment));
            }
            case STRIPE -> {
                requireServerTag("external");
                applicationEventPublisher.publishEvent(new ProcessStripePaymentEvent(payment));
            }
        }
    }

    private void requireServerTag(String required) {
        if (!serverTags.contains(required)) {
            throw new NonRetryableException(
                "Server missing required tag '" + required + "' (has: '" + serverTags + "')");
        }
    }

    private void markPaymentAsCompleted(Payment payment) {
        LOGGER.info("Sending confirmation for payment: {}", payment);
        payment.setStatus(Payment.Status.COMPLETED);
        paymentRepository.save(payment);
    }

    public void reportToGovernment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NonRetryableException("Payment not found: " + paymentId));

        LOGGER.info("Reporting payment > $10k to government: {}", payment);
        simulateWork(500);
        LOGGER.info("Government reporting verified: {} - response: {}", payment);
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
