package org.jobrunr.storylinedemo.creditcards.events.listeners;

import org.jobrunr.jobs.Job;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storylinedemo.creditcards.events.CreditCardActivatedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import static org.jobrunr.storage.JobSearchRequestBuilder.aJobSearchRequest;

@Component
public class CancelScheduledEmailOnCreditCardActivated implements ApplicationListener<CreditCardActivatedEvent> {

    private final StorageProvider storageProvider;


    public CancelScheduledEmailOnCreditCardActivated(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    @Override
    public void onApplicationEvent(CreditCardActivatedEvent event) {
        Job job = storageProvider.getJob(aJobSearchRequest().withLabel("customer: " + event.getCreditCard().getEmail()).build());
        job.delete("Credit Card was activated by customer");
        storageProvider.save(job);
    }
}
