package org.jobrunr.storylinedemo.creditcards.events;

import org.jobrunr.storylinedemo.creditcards.CreditCard;
import org.springframework.context.ApplicationEvent;

public class CreditCardActivatedEvent extends ApplicationEvent {

    public CreditCardActivatedEvent(CreditCard creditCard) {
        super(creditCard);
    }

    public CreditCard getCreditCard() {
        return (CreditCard) getSource();
    }
}
