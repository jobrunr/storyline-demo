package org.jobrunr.storylinedemo.creditcards.events.listeners;


import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.jobrunr.storylinedemo.creditcards.events.CreditCardRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static org.jobrunr.scheduling.JobBuilder.aJob;

@Component
public class ScheduleEmailOnCreditCardRegistered implements ApplicationListener<CreditCardRegisteredEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleEmailOnCreditCardRegistered.class);

    private final JobScheduler jobScheduler;

    public ScheduleEmailOnCreditCardRegistered(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    @Override
    public void onApplicationEvent(CreditCardRegisteredEvent event) {
        // We give the job a label that we can reuse to retrieve when we need to cancel it
        CreditCard creditCard = event.getCreditCard();
        jobScheduler.create(aJob()
                .scheduleAt(LocalDateTime.now().plusDays(7))
                .withLabels("customer: " + event.getCreditCard().getEmail())
                .withDetails(() -> sendActivationReminderEmail(creditCard)));

        // Alternatively we can give the job a deterministic identifier
        /*
        jobScheduler.create(aJob()
                .withId(JobId.fromIdentifier("credit-card-"+creditCard.getId()+"-activation-reminder"))
                .scheduleAt(LocalDateTime.now().plusDays(7))
                .withLabels("customer: " + event.getCreditCard().getEmail())
                .withDetails(() -> sendActivationReminderEmail(creditCard)));
        */
    }

    public void sendActivationReminderEmail(CreditCard creditCard) {
        LOGGER.info("Sending out reminder to: {}", creditCard.getEmail());
        // use SMTP service or Email SaaS like Mailgun or SendGrid
    }
}
