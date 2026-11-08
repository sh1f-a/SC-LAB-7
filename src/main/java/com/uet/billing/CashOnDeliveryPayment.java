package com.uet.billing;

import java.util.Map;

public class CashOnDeliveryPayment implements PaymentStrategy {

    private static final double MAX_COD_AMOUNT = 25000;

    @Override
    public String getMethodName() {
        return "CASH_ON_DELIVERY";
    }

    @Override
    public String pay(Map<String, Object> orderData, double payableAmount) {
        if (payableAmount > MAX_COD_AMOUNT) {
            System.out.println("Cash on delivery not allowed for orders above PKR 25,000.");
            return null;
        }
        return "PENDING_CASH";
    }
}
