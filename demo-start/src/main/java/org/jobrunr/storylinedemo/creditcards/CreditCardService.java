package org.jobrunr.storylinedemo.creditcards;

import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static org.jobrunr.scheduling.JobBuilder.aJob;

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
        // Step 1: enqueue creation of a card
        jobScheduler.create(aJob()
                .withDetails(() -> createNewCreditCard(creditCard)));
        // Step 2: schedule reminder email in 7 days if not activated by then
        jobScheduler.schedule(LocalDateTime.now().plusDays(7), () -> sendActivationReminderEmail(creditCard));
    }

    public void createNewCreditCard(CreditCard creditCard) {
        creditCardRepository.save(creditCard);
        LOGGER.info("Created new credit card: {}", creditCard);
    }

    public void sendActivationReminderEmail(CreditCard creditCard) {
        LOGGER.info("Sending out reminder to: {}", creditCard.getEmail());
    }

}
