package org.jobrunr.storylinedemo.creditcards;

import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storylinedemo.creditcards.events.CreditCardActivatedEvent;
import org.jobrunr.storylinedemo.creditcards.events.CreditCardRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreditCardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreditCardService.class);

    private final JobScheduler jobScheduler;
    private final CreditCardRepository creditCardRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public CreditCardService(JobScheduler jobScheduler, CreditCardRepository creditCardRepository, ApplicationEventPublisher applicationEventPublisher) {
        this.jobScheduler = jobScheduler;
        this.creditCardRepository = creditCardRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    // Step 1: Enqueue a background job for credit card creation
    public void processRegistration(CreditCard creditCard) {
        // Use enqueue to process the application in the background
        // The web request returns immediately while JobRunr handles the work
        jobScheduler.enqueue(() -> createNewCreditCard(creditCard));
    }

    // Step 20: Replace a pending job with updated customer info
    public void processRegistrationOrReplace(CreditCard creditCard) {
        // Create a unique job ID from the customer's email
        // If a job already exists for this customer, it will be replaced
        UUID jobId = JobId.fromIdentifier("credit-card:" + creditCard.getEmail());
        jobScheduler.enqueueOrReplace(jobId, () -> createNewCreditCard(creditCard));
    }

    public void processActivation(CreditCard creditCard) {
        // Activate the credit card
        CreditCard creditCardFromRepo = creditCardRepository.findByEmail(creditCard.getEmail());
        creditCardFromRepo.activate();
        creditCardRepository.save(creditCardFromRepo);

        // Publish event to cancel the scheduled reminder email
        applicationEventPublisher.publishEvent(new CreditCardActivatedEvent(creditCardFromRepo));
    }

    @Transactional
    @Job(name = "Create %0") // Nice name for the dashboard with customer info
    public void createNewCreditCard(CreditCard creditCard) {
        // Step 1: Save to repository
        var creditCardFromRepo = creditCardRepository.save(creditCard);
        LOGGER.info("Created new credit card: {}", creditCardFromRepo);

        // Step 2: Publish event to schedule the reminder email
        applicationEventPublisher.publishEvent(new CreditCardRegisteredEvent(creditCardFromRepo));
    }

}
