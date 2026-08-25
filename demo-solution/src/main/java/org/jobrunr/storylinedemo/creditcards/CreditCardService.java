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

import java.time.Duration;
import java.util.UUID;

@Service
public class CreditCardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreditCardService.class);

    private static final Duration APPLICATION_PROCESSING_TIME = Duration.ofSeconds(6);

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

    @Transactional
    public void processActivation(String number) {
        CreditCard creditCardFromRepo = creditCardRepository.findByNumber(number)
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found: " + number));
        creditCardFromRepo.activate();
        creditCardRepository.save(creditCardFromRepo);

        // Publish event to cancel the scheduled reminder email
        applicationEventPublisher.publishEvent(new CreditCardActivatedEvent(creditCardFromRepo));
    }

    /**
     * Payments need a card that is active, and the tour only ever registers new ones. This lets a payment
     * demo stand on its own instead of dead-ending on "no active credit cards found".
     */
    public int activateWaitingCards(int max) {
        int activated = 0;
        for (CreditCard card : creditCardRepository.findByState(CreditCard.State.REQUESTED).stream().limit(max).toList()) {
            try {
                processActivation(card.getNumber());
                activated++;
            } catch (RuntimeException e) {
                // Its reminder job has already run or been cleaned up; the next card will do just as well.
                LOGGER.debug("Could not activate card {}", card.getNumber(), e);
            }
        }
        return activated;
    }

    @Job(name = "Create %0") // Nice name for the dashboard with customer info
    public void createNewCreditCard(CreditCard creditCard) {
        // Identity verification, fraud scoring and card provisioning: the seconds that do not fit in a web request
        runIdentityAndFraudChecks();

        // Step 1: Save to repository
        var creditCardFromRepo = creditCardRepository.save(creditCard);
        LOGGER.info("Created new credit card: {}", creditCardFromRepo);

        // Step 2: Publish event to schedule the reminder email
        applicationEventPublisher.publishEvent(new CreditCardRegisteredEvent(creditCardFromRepo));
    }

    private static void runIdentityAndFraudChecks() {
        try {
            // Not re-setting the interrupt flag: JobRunr interrupts this thread to cancel the job and
            // needs a working database connection right after to record that it did
            Thread.sleep(APPLICATION_PROCESSING_TIME);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
