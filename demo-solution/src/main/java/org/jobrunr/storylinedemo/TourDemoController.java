package org.jobrunr.storylinedemo;

import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.jobrunr.storylinedemo.creditcards.CreditCardRepository;
import org.jobrunr.storylinedemo.creditcards.CreditCardService;
import org.jobrunr.storylinedemo.creditcards.FraudReviewService;
import org.jobrunr.storylinedemo.payments.Payment;
import org.jobrunr.storylinedemo.payments.PaymentPlatform;
import org.jobrunr.storylinedemo.payments.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * The one-click triggers the tour hands to signed-in visitors. Everything here writes to the bank, so
 * it stays out of the anonymous {@code /simulate/*} namespace: Spring Security asks for an email first
 * and the tour turns that into its own inline sign-in form. GET, like the {@code /bulk-*} endpoints next
 * door, because the tour fetches without a CSRF token.
 */
@RestController
public class TourDemoController {

    private static final String REPLACE_DEMO_NAME = "Ada Lovelace";
    private static final String REPLACE_DEMO_EMAIL = "ada.lovelace@jobrunr.io";

    private final CreditCardService creditCardService;
    private final CreditCardRepository creditCardRepository;
    private final FraudReviewService fraudReviewService;
    private final PaymentService paymentService;

    public TourDemoController(CreditCardService creditCardService,
                              CreditCardRepository creditCardRepository,
                              FraudReviewService fraudReviewService,
                              PaymentService paymentService) {
        this.creditCardService = creditCardService;
        this.creditCardRepository = creditCardRepository;
        this.fraudReviewService = fraudReviewService;
        this.paymentService = paymentService;
    }

    /** Step 6: one payment whose job records every completed step, so a requeue repeats none of them. */
    @GetMapping("/demo/payment")
    public DemoResult payment() {
        List<CreditCard> cards = activeCards();
        CreditCard payer = cards.getFirst();
        BigDecimal amount = BigDecimal.valueOf(new Random().nextInt(50, 4000));

        paymentService.submitPayment(
                new Payment(payer.getId(), amount, PaymentPlatform.JOBRUNR_FINANCE, cards.getLast().getNumber()));

        return DemoResult.ok(payer.getName() + " just paid " + amount.intValue()
                + " euro in four recorded steps. Open the Process payment job and press Requeue: every step gets skipped.");
    }

    /** Step 10: one payment on the High queue, to overtake whatever is piling up below it. */
    @GetMapping("/demo/priority-payment")
    public DemoResult priorityPayment() {
        List<CreditCard> cards = activeCards();
        CreditCard payer = cards.getFirst();
        BigDecimal amount = BigDecimal.valueOf(new Random().nextInt(50, 4000));

        paymentService.submitPayment(
                new Payment(payer.getId(), amount, PaymentPlatform.JOBRUNR_FINANCE, cards.getLast().getNumber()));

        return DemoResult.ok(payer.getName() + " just paid " + amount.intValue()
                + " euro. It is on the High queue, so it goes first.");
    }

    /** Step 18: hand an application to the risk cluster and let JobRunr wait for the verdict. */
    @GetMapping("/demo/fraud-review")
    public DemoResult sendForFraudReview() {
        CreditCard applicant = creditCardRepository.findRandomCards(1).stream()
                .findFirst()
                .orElseGet(CreditCard::randomCreditCard);

        fraudReviewService.requestReview(applicant);
        return DemoResult.ok("The risk cluster has the application of " + applicant.getEmail()
                + ". Its job is now waiting in PROCESSED.");
    }

    /** Step 18: the callback the GPU cluster would make once its model is done. */
    @GetMapping("/demo/fraud-review/verdict")
    public DemoResult reportFraudVerdict() {
        return fraudReviewService.reviewWaitingForVerdict()
                .map(jobId -> {
                    fraudReviewService.verdictReceived(jobId, true);
                    return DemoResult.ok("Verdict in: approved. The job left PROCESSED with the model's answer attached.");
                })
                .orElseGet(() -> DemoResult.nothingToDo("No review is waiting for a verdict. Send one to the cluster first."));
    }

    /** Step 21: the same customer, twice, so the second application replaces the first one's job. */
    @GetMapping("/demo/application")
    public DemoResult application(@RequestParam CreditCard.Type cardType) {
        creditCardService.processRegistrationOrReplace(
                new CreditCard(REPLACE_DEMO_NAME, REPLACE_DEMO_EMAIL, cardType));

        String card = cardType == CreditCard.Type.AMERICAN_EXPRESS ? "an American Express" : "a Mastercard";
        return DemoResult.ok(REPLACE_DEMO_NAME + " applied for " + card
                + ". Her job id comes from her email address, so there is only ever one.");
    }

    private List<CreditCard> activeCards() {
        List<CreditCard> active = creditCardRepository.findRandomActiveCards(2);
        if (active.isEmpty()) {
            creditCardService.activateWaitingCards(3);
            active = creditCardRepository.findRandomActiveCards(2);
        }
        if (active.isEmpty()) {
            throw new IllegalStateException("There is no card to pay with yet. Run step 1 and give the applications a few seconds.");
        }
        return active;
    }
}
