package com.uet.billing;

import java.util.HashMap;
import java.util.Map;

public class NotificationRegistry {

    private final Map<String, NotificationService> services = new HashMap<>();

    public NotificationRegistry() {
        register(new EmailNotificationService());
        register(new SmsNotificationService());
    }

    public final void register(NotificationService service) {
        services.put(service.getChannelName(), service);
    }

    public NotificationService find(String channelName) {
        return services.get(channelName);
    }
}
