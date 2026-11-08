package com.uet.billing;

import java.util.List;
import java.util.Map;

/**
 * Coordinates the order flow: validate, total, discount, tax, pay, save, notify.
 * All the real work is done by small, focused classes.
 */
public class OrderBillingEngine {

    private String logFilePath = "order_audit.log";

    private final DiscountCalculator discountCalculator = new DiscountCalculator();
    private final TaxCalculator taxCalculator = new TaxCalculator();
    private final PaymentStrategyRegistry paymentRegistry = new PaymentStrategyRegistry();
    private final NotificationRegistry notificationRegistry = new NotificationRegistry();
    private OrderRepository orderRepository = new FileOrderRepository(logFilePath);

    public boolean processOrder(Map<String, Object> orderData) {
        if (!isValidOrder(orderData)) {
            return false;
        }

        String email = (String) orderData.get("customerEmail");
        List<Map<String, Object>> items = getItems(orderData);

        double rawTotal = calculateRawTotal(items);
        if (rawTotal < 0) {
            return false;
        }

        double payableAmount = calculatePayable(orderData, rawTotal, items.size());

        String paymentStatus = processPayment(orderData, payableAmount);
        if (paymentStatus == null) {
            return false;
        }

        orderRepository.save(email, payableAmount, paymentStatus);
        notifyCustomer(orderData, email, payableAmount);
        return true;
    }

    // ---------- Validation ----------

    private boolean isValidOrder(Map<String, Object> orderData) {
        if (orderData == null || orderData.isEmpty()) {
            System.out.println("Error: Order data is empty!");
            return false;
        }
        String email = (String) orderData.get("customerEmail");
        if (email == null || !email.contains("@")) {
            System.out.println("Error: Invalid customer email!");
            return false;
        }
        List<Map<String, Object>> items = getItems(orderData);
        if (items == null || items.isEmpty()) {
            System.out.println("Error: Cart has no items!");
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getItems(Map<String, Object> orderData) {
        return (List<Map<String, Object>>) orderData.get("items");
    }

    // ---------- Pricing ----------

    /** Returns the raw total, or -1 if any item is invalid. */
    private double calculateRawTotal(List<Map<String, Object>> items) {
        double rawTotal = 0.0;
        for (Map<String, Object> item : items) {
            if (!item.containsKey("price") || !item.containsKey("quantity")) {
                System.out.println("Error: Missing item price or quantity!");
                return -1;
            }
            double price = (double) item.get("price");
            int quantity = (int) item.get("quantity");
            if (price <= 0 || quantity <= 0) {
                System.out.println("Error: Invalid price or quantity for item!");
                return -1;
            }
            rawTotal += price * quantity;
        }
        return rawTotal;
    }

    private double calculatePayable(Map<String, Object> orderData, double rawTotal, int itemCount) {
        String customerType = (String) orderData.getOrDefault("customerType", "REGULAR");
        double discount = discountCalculator.calculate(customerType, rawTotal, itemCount);
        double netAmount = rawTotal - discount;

        String country = (String) orderData.getOrDefault("country", "PK");
        double tax = taxCalculator.calculateTax(netAmount, country);
        return netAmount + tax;
    }

    // ---------- Payment ----------

    /** Returns the payment status, or null if the payment was rejected. */
    private String processPayment(Map<String, Object> orderData, double payableAmount) {
        String paymentMethod = (String) orderData.getOrDefault("paymentMethod", "CASH");
        PaymentStrategy strategy = paymentRegistry.find(paymentMethod);
        if (strategy == null) {
            System.out.println("Error: Unsupported payment method: " + paymentMethod);
            return null;
        }
        return strategy.pay(orderData, payableAmount);
    }

    // ---------- Notification ----------

    private void notifyCustomer(Map<String, Object> orderData, String email, double payableAmount) {
        String channel = (String) orderData.getOrDefault("notifyChannel", "EMAIL");
        NotificationService notifier = notificationRegistry.find(channel);
        if (notifier != null) {
            notifier.send(orderData, email, payableAmount);
        }
    }

    // ---------- Test helper ----------

    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
        this.orderRepository = new FileOrderRepository(logFilePath);
    }

    public String getLogFilePath() {
        return logFilePath;
    }
}