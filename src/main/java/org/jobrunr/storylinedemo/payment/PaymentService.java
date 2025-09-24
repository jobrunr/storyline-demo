package org.jobrunr.storylinedemo.payment;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    @Recurring(cron = "0 3 * * *")
    @Job(queue = "high-prio")
    public void processPaymentsNightly() {
        System.out.println("Processing nightly payments");
    }

}
