package org.jobrunr.storylinedemo.payment;

import java.util.Random;

public record Payment(double amount, String description, boolean international, CustomerType customerType) {

    public static Payment randomPayment(int index) {
        return new Payment(new Random().nextLong(99999), "random payment #" + index , new Random().nextBoolean(), CustomerType.random());
    }
}
