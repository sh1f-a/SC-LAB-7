package com.uet.billing;

import java.util.HashMap;
import java.util.Map;

public class PaymentStrategyRegistry {

    private final Map<String, PaymentStrategy> strategies = new HashMap<>();

    public PaymentStrategyRegistry() {
        register(new CreditCardPayment());
        register(new EasyPaisaPayment());
        register(new CashOnDeliveryPayment());
    }

    public final void register(PaymentStrategy strategy) {
        strategies.put(strategy.getMethodName(), strategy);
    }

    public PaymentStrategy find(String methodName) {
        return strategies.get(methodName);
    }
}
