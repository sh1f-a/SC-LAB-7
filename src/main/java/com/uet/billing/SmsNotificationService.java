package com.uet.billing;

import java.util.Map;

public class SmsNotificationService implements NotificationService {

    @Override
    public String getChannelName() {
        return "SMS";
    }

    @Override
    public void send(Map<String, Object> orderData, String email, double payableAmount) {
        String phone = (String) orderData.getOrDefault("phone", "0300-0000000");
        System.out.println("[SMS DISPATCHED] To: " + phone + " | Order confirmed! Paid: PKR "
                + String.format("%.2f", payableAmount));
    }
}
