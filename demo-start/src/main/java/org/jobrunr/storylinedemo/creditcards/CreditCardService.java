package org.jobrunr.storylinedemo.creditcards;

import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CreditCardService {

    private final JobScheduler jobScheduler;
    private final CreditCardRepository creditCardRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(CreditCardService.class);

    public CreditCardService(JobScheduler jobScheduler, CreditCardRepository creditCardRepository) {
        this.jobScheduler = jobScheduler;
        this.creditCardRepository = creditCardRepository;
    }

    public void processRegistration(CreditCard creditCard) {
        // TODO Step 1: enqueue creation of a card + schedule one in seven days to send a reminder email
        LOGGER.info("processRegistration: Implement me!");

        // TODO Step 5: filters by credit card type
    }

    public void sendReminderEmail(CreditCard creditCard) {
        LOGGER.info("Sending out reminder to: {}", creditCard.getEmail());
    }

    public void createNewCreditCard(CreditCard creditCard) {
        creditCardRepository.save(creditCard);
        LOGGER.info("Created new credit card: {}", creditCard);
    }

}
