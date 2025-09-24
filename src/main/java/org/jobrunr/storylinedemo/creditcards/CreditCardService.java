package org.jobrunr.storylinedemo.creditcards;

import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static org.jobrunr.scheduling.JobBuilder.aJob;

@Service
public class CreditCardService {

    private final JobScheduler jobScheduler;
    private final CreditCardRepository creditCardRepository;

    public CreditCardService(JobScheduler jobScheduler, CreditCardRepository creditCardRepository) {
        this.jobScheduler = jobScheduler;
        this.creditCardRepository = creditCardRepository;
    }

    public void processRegistration(CreditCard creditCard) {
        jobScheduler.create(aJob()
                .withLabels(creditCard.getType().name())
                .withDetails(() -> createNewCreditCard(creditCard)));
        jobScheduler.schedule(LocalDateTime.now().plusDays(7), () -> sendReminderEmail(creditCard));
    }

    public void sendReminderEmail(CreditCard creditCard) {
        System.out.println("Sending out reminder to: " + creditCard.getEmail());
    }

    public void createNewCreditCard(CreditCard creditCard) {
        creditCardRepository.save(creditCard);
        System.out.println("Created new credit card: " + creditCard);
    }

}
