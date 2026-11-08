package com.uet.billing;

import java.util.Map;

public class EmailNotificationService implements NotificationService {

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    @Override
    public void send(Map<String, Object> orderData, String email, double payableAmount) {
        System.out.println("[EMAIL DISPATCHED] To: " + email + " | Your order of PKR "
                + String.format("%.2f", payableAmount) + " is confirmed!");
    }
}