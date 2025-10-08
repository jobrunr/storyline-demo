package org.jobrunr.storylinedemo.payment;

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

    public PaymentService(JobScheduler jobScheduler, RestClient.Builder restClientBuilder) {
        this.jobScheduler = jobScheduler;
        this.restClient = restClientBuilder.baseUrl("http://localhost:8089").build();
    }

    @Recurring(cron = "0 3 * * *")
    // Step 7: payment jobs have higher prio
    @Job(queue = "high-prio")
    public void processAllPaymentsNightly() {
        System.out.println("Processing all nightly payments");

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
        System.out.println("Processing payment: " + payment);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void exportPaymentToExternalSystem(Payment payment) {
        var verified = this.restClient.get().uri("/verify").retrieve().body(String.class);
        LOGGER.info("Exported and verified: {} - verified: {}", payment, verified);
    }

}
