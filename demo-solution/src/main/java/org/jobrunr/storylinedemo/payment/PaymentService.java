package org.jobrunr.storylinedemo.payment;

import io.opentelemetry.api.trace.Tracer;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
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
    private final Tracer tracer;

    public PaymentService(JobScheduler jobScheduler, RestClient.Builder restClientBuilder, Tracer tracer) {
        this.jobScheduler = jobScheduler;
        this.restClient = restClientBuilder.baseUrl("http://localhost:8089").build();
        this.tracer = tracer;
    }

    @Recurring(cron = "0 3 * * *")
    // Step 7: payment jobs have higher prio
    @Job(queue = "high-prio")
    public void processAllPaymentsNightly() {
        LOGGER.info("Processing all nightly payments");

        for(int i = 1; i <= 100; i++) {
            var customerType = CustomerType.random();
            var randomPayment = Payment.randomPayment(i);

            var job = jobScheduler.create(aJob()
                    // Step 8: configure queues by customer type
                    .withLabels("customer:" + customerType.name())
                    // Step 9: international payments on another server
                    .withServerTag(randomPayment.getRegion())
                    .withDetails(() -> processPayments(randomPayment)));

            // Step 10: export payment to external system but use rate limiting
            if(randomPayment.international()) {
                jobScheduler.create(aJob()
                        .withRateLimiter("external")
                        .runAfterSuccessOf(job.asUUID())
                        .withDetails(() -> exportPaymentToExternalSystem(randomPayment)));
            }
        }
    }

    public void processPayments(Payment payment) {
        LOGGER.info("Processing payment: {}", payment);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void exportPaymentToExternalSystem(Payment payment) {
        // Optional: you can add custom trace metadata inside the job span or add a new span that will show up in Jaeger
        var span = tracer.spanBuilder("exportPaymentToExternalSystem").startSpan();
        span.setAttribute("payment.amount", payment.amount());
        span.setAttribute("payment.description", payment.description());

        try {
            var verified = this.restClient.get().uri("/verify").retrieve().body(String.class);
            LOGGER.info("Exported and verified: {} - verified: {}", payment, verified);
        } finally {
            span.end();
        }
    }

}
