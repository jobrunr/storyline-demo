package org.jobrunr.storylinedemo.creditcards;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storylinedemo.creditcards.events.CreditCardActivatedEvent;
import org.jobrunr.storylinedemo.creditcards.events.CreditCardRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

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

    public void processRegistration(CreditCard creditCard) {
        // Enqueue creation of a card
        jobScheduler.enqueue(() -> createNewCreditCard(creditCard));
    }

    public void processActivation(CreditCard creditCard) {
        // Activate the credit card
        CreditCard creditCardFromRepo = creditCardRepository.findByEmail(creditCard.getEmail());
        creditCardFromRepo.activate();
        creditCardRepository.save(creditCardFromRepo);

        // Publish event
        applicationEventPublisher.publishEvent(new CreditCardActivatedEvent(creditCard));
    }

    @Job(name = "Create %0") // nice name for the dashboard
    public void createNewCreditCard(CreditCard creditCard) {
        // Step 1: Save to repository
        creditCardRepository.save(creditCard);
        LOGGER.info("Created new credit card: {}", creditCard);

        // Step 2: Publish event
        applicationEventPublisher.publishEvent(new CreditCardRegisteredEvent(creditCard));
    }

}
