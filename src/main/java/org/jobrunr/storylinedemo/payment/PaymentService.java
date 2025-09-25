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
    @Job(queue = "high-prio")
    public void processAllPaymentsNightly() {
        System.out.println("Processing all nightly payments");

        for(int i = 1; i <= 100; i++) {
            var customerType = CustomerType.random();
            var randomPayment = Payment.randomPayment(i);

            jobScheduler.create(aJob()
                    .withLabels("customer:" + customerType.name())
                    .withDetails(() -> processPayments(randomPayment)));
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

}
