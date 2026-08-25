package org.jobrunr.storylinedemo;

import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.jobrunr.storylinedemo.creditcards.CreditCardService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The trigger the tour hands to visitors who have not signed in yet, so act 1 tells its story before
 * asking for anything. Deliberately separate from {@link AdminController} and {@link TourDemoController}:
 * this is a POST with a hardcoded volume, which keeps crawlers and link prefetchers from writing to the
 * live demo.
 */
@RestController
public class TourSimulationController {

    private static final int APPLICATIONS_PER_RUN = 25;

    private final CreditCardService creditCardService;

    public TourSimulationController(CreditCardService creditCardService) {
        this.creditCardService = creditCardService;
    }

    /** Step 1: a burst of card applications, each one enqueued as a background job. */
    @PostMapping("/simulate/credit-cards")
    public DemoResult simulateCreditCardApplications() {
        for (int i = 0; i < APPLICATIONS_PER_RUN; i++) {
            creditCardService.processRegistration(CreditCard.randomCreditCard(CreditCard.State.REQUESTED));
        }
        return DemoResult.ok(APPLICATIONS_PER_RUN + " applications are queued. There are more of them than there are workers, "
                + "so watch them wait their turn in Enqueued.");
    }
}
