package org.jobrunr.storylinedemo.creditcards;

import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CreditCardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreditCardService.class);

    private final JobScheduler jobScheduler;
    private final CreditCardRepository creditCardRepository;

    public CreditCardService(JobScheduler jobScheduler, CreditCardRepository creditCardRepository) {
        this.jobScheduler = jobScheduler;
        this.creditCardRepository = creditCardRepository;
    }

    // TODO Step 1: Enqueue a background job for credit card creation
    public void processRegistration(CreditCard creditCard) {
        // Use enqueue to process the application in the background
        // The web request returns immediately while JobRunr handles the work
    }

    // TODO Step 20: Replace a pending job with updated customer info
    public void processRegistrationOrReplace(CreditCard creditCard) {
        // Create a unique job ID from the customer's email
        // If a job already exists for this customer, it will be replaced
    }

    public void processActivation(CreditCard creditCard) {
        // Activate the credit card
        CreditCard creditCardFromRepo = creditCardRepository.findByEmail(creditCard.getEmail());
        creditCardFromRepo.activate();
        creditCardRepository.save(creditCardFromRepo);

        // Step 2B: Cancel card activation reminder job
    }

    // TODO Nice name for the dashboard with customer info
    public void createNewCreditCard(CreditCard creditCard) {
        // Step 1: Save to repository
        var creditCardFromRepo = creditCardRepository.save(creditCard);
        LOGGER.info("Created new credit card: {}", creditCardFromRepo);

        // TODO Step 2A: Schedule card activation reminder job
    }

}
