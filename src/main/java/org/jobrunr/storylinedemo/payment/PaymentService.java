package org.jobrunr.storylinedemo.payment;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Service;

import static org.jobrunr.scheduling.JobBuilder.aJob;

@Service
public class PaymentService {

    private final JobScheduler jobScheduler;

    public PaymentService(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
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
        System.out.println("Exporting payment to external system: " + payment);
    }

}
