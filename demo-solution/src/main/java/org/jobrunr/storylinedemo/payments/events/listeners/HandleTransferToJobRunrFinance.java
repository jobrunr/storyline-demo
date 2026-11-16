package org.jobrunr.storylinedemo.payments.events.listeners;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.jobrunr.storylinedemo.creditcards.CreditCardRepository;
import org.jobrunr.storylinedemo.exceptions.NonRetryableException;
import org.jobrunr.storylinedemo.exceptions.TransferTimeoutException;
import org.jobrunr.storylinedemo.payments.Payment;
import org.jobrunr.storylinedemo.payments.events.ProcessJobRunrFinancePaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class HandleTransferToJobRunrFinance implements ApplicationListener<ProcessJobRunrFinancePaymentEvent> {
    private static final Logger LOGGER = new JobRunrDashboardLogger(LoggerFactory.getLogger(HandleTransferToJobRunrFinance.class));

    /** Payments carrying this label time out on their first transfer attempt, which is what the tour's step 6 shows. */
    public static final String TIMEOUT_ONCE_LABEL = "transfer:times-out-once";

    private final CreditCardRepository creditCardRepository;

    public HandleTransferToJobRunrFinance(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = creditCardRepository;
    }

    @Override
    public void onApplicationEvent(ProcessJobRunrFinancePaymentEvent event) {
        Payment payment = event.getPayment();
        timeOutOnFirstAttemptIfAsked(payment);

        LOGGER.info("Internal transfer to JobRunr Finance account: {}", payment.getRecipient());
        CreditCard recipientCard = creditCardRepository.findByNumber(payment.getRecipient())
            .orElseThrow(() -> new NonRetryableException("Could not find the target credit card " + payment.getRecipient()));
        recipientCard.addBalance(payment.getAmount());
        creditCardRepository.save(recipientCard);
        LOGGER.info("Credited {} to recipient card {}", payment.getAmount(), payment.getRecipient());
    }

    private static void timeOutOnFirstAttemptIfAsked(Payment payment) {
        if (!ThreadLocalJobContext.hasJobContext()) return;
        JobContext job = ThreadLocalJobContext.getJobContext();
        if (job.getJobLabels().contains(TIMEOUT_ONCE_LABEL) && job.amountOfFailures() == 0) {
            throw new TransferTimeoutException("The JobRunr Finance ledger did not answer in time for payment #" + payment.getId());
        }
    }
}
