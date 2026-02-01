package org.jobrunr.storylinedemo.payment.events.listeners;

import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.jobrunr.storylinedemo.creditcards.CreditCardRepository;
import org.jobrunr.storylinedemo.exceptions.NonRetryableException;
import org.jobrunr.storylinedemo.payment.Payment;
import org.jobrunr.storylinedemo.payment.events.ProcessJobRunrFinancePaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class HandleTransferToJobRunrFinance implements ApplicationListener<ProcessJobRunrFinancePaymentEvent> {
    private static final Logger LOGGER = new JobRunrDashboardLogger(LoggerFactory.getLogger(HandleTransferToJobRunrFinance.class));

    private final CreditCardRepository creditCardRepository;

    public HandleTransferToJobRunrFinance(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = creditCardRepository;
    }

    @Override
    public void onApplicationEvent(ProcessJobRunrFinancePaymentEvent event) {
        Payment payment = event.getPayment();

        LOGGER.info("Internal transfer to JobRunr Finance account: {}", payment.getRecipient());
        CreditCard recipientCard = creditCardRepository.findByNumber(payment.getRecipient())
            .orElseThrow(() -> new NonRetryableException("Could not find the target credit card " + payment.getRecipient()));
        recipientCard.addBalance(payment.getAmount());
        creditCardRepository.save(recipientCard);
        LOGGER.info("Credited {} to recipient card {}", payment.getAmount(), payment.getRecipient());
    }
}
