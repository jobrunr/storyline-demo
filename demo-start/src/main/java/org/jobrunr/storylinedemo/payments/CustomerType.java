package org.jobrunr.storylinedemo.payments;

import java.util.Random;

public enum CustomerType {
    ENTERPRISE,
    PRO;

    public static CustomerType random() {
        return new Random().nextBoolean() ? CustomerType.ENTERPRISE : CustomerType.PRO;
    }
}
