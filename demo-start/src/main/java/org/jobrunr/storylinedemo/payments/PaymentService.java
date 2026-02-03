package org.jobrunr.storylinedemo.payments;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.jobrunr.storylinedemo.creditcards.CreditCardRepository;
import org.jobrunr.storylinedemo.exceptions.NonRetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.jobrunr.scheduling.JobBuilder.aJob;

@Service
public class PaymentService {

    private static final Logger LOGGER = new JobRunrDashboardLogger(LoggerFactory.getLogger(PaymentService.class));

    private final JobScheduler jobScheduler;
    private final PaymentRepository paymentRepository;
    private final CreditCardRepository creditCardRepository;
    private final String serverTags;

    public PaymentService(JobScheduler jobScheduler,
                          PaymentRepository paymentRepository,
                          CreditCardRepository creditCardRepository,
                          @Value("${jobrunr.background-job-server.tags:}") String serverTags) {
        this.jobScheduler = jobScheduler;
        this.paymentRepository = paymentRepository;
        this.creditCardRepository = creditCardRepository;
        this.serverTags = serverTags;
    }

    @Transactional
    public void submitPayment(Payment payment) {
        var savedPayment = paymentRepository.save(payment);
        createPaymentProcessingJob(savedPayment);
    }

    private void createPaymentProcessingJob(Payment payment) {
        jobScheduler.create(aJob()
                .withDetails(() -> processPayment(payment.getId(), JobContext.Null)));

        // TODO Step 10: Payments are high priority!
        // TODO Step 11: Process more payments on average for premium cards
        // TODO Step 13: Report high transfer to the government and Timeout if when HTTP request are taking too long!
        // TODO Step 14: Payments to Stripe or Paypal can only be processed on dedicated servers
        // TODO Step 15A: Payments to Stripe or Paypal are risky if the number of requests are not limited
        // TODO Step 15B: The government app is easily DDoSable, rate-limiting to the rescue!
    }

    public void processPayment(Long paymentId, JobContext context) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        // TODO Step 6: use context.runStepOnce to avoid double charging and double paying in case of a retry
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
            case JOBRUNR_FINANCE -> {
                // TODO update the balance of the receiver
            }
            case PAYPAL -> {
                requireServerTag("external");
                simulateWork(500);
            }
            case STRIPE -> {
                requireServerTag("external");
                simulateWork(500);
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
