package com.uet.billing;

import java.util.Map;

public interface NotificationService {

    /** The channel name used in orderData, e.g. "EMAIL". */
    String getChannelName();

    void send(Map<String, Object> orderData, String email, double payableAmount);
}
