package com.uet.billing;

import java.util.Map;

public interface PaymentStrategy {

    /** The key used in orderData, e.g. "CREDIT_CARD". */
    String getMethodName();

    /**
     * Tries to process the payment.
     * @return the payment status ("PAID" or "PENDING_CASH"), or null if the payment is rejected.
     */
    String pay(Map<String, Object> orderData, double payableAmount);
}
