package org.jobrunr.storylinedemo.payment;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import static org.jobrunr.scheduling.JobBuilder.aJob;

@Service
public class PaymentService {

    private final JobScheduler jobScheduler;
    private final RestClient restClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);

    public PaymentService(JobScheduler jobScheduler, RestClient.Builder restClientBuilder) {
        this.jobScheduler = jobScheduler;
        this.restClient = restClientBuilder.baseUrl("http://localhost:8089").build();
    }

    // Step 10: Payment jobs have higher priority than reports
    @Recurring(id = "nightly-payments", cron = "0 3 * * *")
    @Job(name = "Process All Nightly Payments", queue = "high-prio")
    public void processAllPaymentsNightly() {
        LOGGER.info("Processing all nightly payments");

        for (int i = 1; i <= 100; i++) {
            var customerType = CustomerType.random();
            var randomPayment = Payment.randomPayment(i);

            // Step 11: Dynamic queues by customer type (Enterprise vs Pro)
            // Step 14: Server tags for international vs national payments
            var job = jobScheduler.create(aJob()
                    .withLabels("customer:" + customerType.name())
                    .withServerTag(randomPayment.getRegion())
                    .withDetails(() -> processPayment(randomPayment)));

            // Step 15: Rate limit exports to government API (max 3 concurrent)
            if (randomPayment.international()) {
                jobScheduler.create(aJob()
                        .withRateLimiter("government-api")
                        .runAfterSuccessOf(job.asUUID())
                        .withDetails(() -> exportPaymentToExternalSystem(randomPayment)));
            }
        }
    }

    // Step 6: Idempotent payment processing - safe to retry!
    @Job(name = "Process Payment #%0")
    public void processPayment(Payment payment, JobContext context) {
        // Each step is executed ONLY ONCE, even on retry (unless step fails)
        // If the job fails after "charge-card", retry will skip it!
        
        context.runStepOnce("charge-card", () -> {
            LOGGER.info("💳 Charging card for payment: {}", payment);
            simulateWork(500);
        });
        
        context.runStepOnce("send-receipt", () -> {
            LOGGER.info("📧 Sending receipt for payment: {}", payment);
            simulateWork(300);
        });
        
        context.runStepOnce("update-ledger", () -> {
            LOGGER.info("📊 Updating ledger for payment: {}", payment);
            simulateWork(200);
        });
        
        LOGGER.info("✅ Payment processed successfully: {}", payment);
    }

    // Overload for backward compatibility (without JobContext)
    public void processPayment(Payment payment) {
        LOGGER.info("Processing payment: {}", payment);
        simulateWork(1000);
    }

    // Step 13: Job timeout - fail if external API takes too long
    @Job(name = "Export Payment to Government", processTimeOut = "PT30S")
    public void exportPaymentToExternalSystem(Payment payment) {
        LOGGER.info("🌍 Exporting payment to government API: {}", payment);
        var verified = this.restClient.get().uri("/verify").retrieve().body(String.class);
        LOGGER.info("✅ Export verified: {} - response: {}", payment, verified);
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
