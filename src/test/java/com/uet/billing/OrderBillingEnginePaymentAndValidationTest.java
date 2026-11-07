package com.uet.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OrderBillingEnginePaymentAndValidationTest {

    @TempDir
    Path tempDir;

    private OrderBillingEngine engine;
    private Path logFile;

    @BeforeEach
    void setUp() {
        engine = new OrderBillingEngine();
        logFile = tempDir.resolve("test_audit.log");
        engine.setLogFilePath(logFile.toString());
    }

    // ---------- Helper methods ----------

    private Map<String, Object> item(double price, int quantity) {
        Map<String, Object> item = new HashMap<>();
        item.put("price", price);
        item.put("quantity", quantity);
        return item;
    }

    private List<Map<String, Object>> oneItem(double price) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item(price, 1));
        return items;
    }

    // A valid base order (Cash on Delivery, 1000 PKR, REGULAR, PK)
    private Map<String, Object> validOrder() {
        Map<String, Object> order = new HashMap<>();
        order.put("customerEmail", "ali@example.com");
        order.put("customerType", "REGULAR");
        order.put("items", oneItem(1000.0));
        order.put("paymentMethod", "CASH_ON_DELIVERY");
        return order;
    }

    private String readLog() throws IOException {
        return Files.readString(logFile);
    }

    private double readPayableFromLog() throws IOException {
        String content = readLog();
        String marker = "Payable: PKR ";
        int start = content.indexOf(marker) + marker.length();
        int end = content.indexOf(" | Status", start);
        return Double.parseDouble(content.substring(start, end));
    }

    // ---------- CREDIT CARD ----------

    @Test
    void creditCard_valid16Digits_paid() throws IOException {
        Map<String, Object> order = validOrder();
        order.put("paymentMethod", "CREDIT_CARD");
        order.put("cardNumber", "1234567812345678");
        assertTrue(engine.processOrder(order));
        assertTrue(readLog().contains("Status: PAID"));
    }

    @Test
    void creditCard_tooShort_declined() {
        Map<String, Object> order = validOrder();
        order.put("paymentMethod", "CREDIT_CARD");
        order.put("cardNumber", "12345678");
        assertFalse(engine.processOrder(order));
    }

    @Test
    void creditCard_containsLetters_declined() {
        Map<String, Object> order = validOrder();
        order.put("paymentMethod", "CREDIT_CARD");
        order.put("cardNumber", "1234abcd12345678");
        assertFalse(engine.processOrder(order));
    }

    @Test
    void creditCard_missingCardNumber_declined() {
        Map<String, Object> order = validOrder();
        order.put("paymentMethod", "CREDIT_CARD");
        assertFalse(engine.processOrder(order));
    }

    // ---------- EASYPAISA ----------

    @Test
    void easyPaisa_validNumber_paid() throws IOException {
        Map<String, Object> order = validOrder();
        order.put("paymentMethod", "EASYPAISA");
        order.put("mobileNumber", "03001234567");
        assertTrue(engine.processOrder(order));
        assertTrue(readLog().contains("Status: PAID"));
    }

    @Test
    void easyPaisa_wrongPrefix_failed() {
        Map<String, Object> order = validOrder();
        order.put("paymentMethod", "EASYPAISA");
        order.put("mobileNumber", "04001234567");
        assertFalse(engine.processOrder(order));
    }

    @Test
    void easyPaisa_wrongLength_failed() {
        Map<String, Object> order = validOrder();
        order.put("paymentMethod", "EASYPAISA");
        order.put("mobileNumber", "0300123");
        assertFalse(engine.processOrder(order));
    }

    @Test
    void easyPaisa_missingNumber_failed() {
        Map<String, Object> order = validOrder();
        order.put("paymentMethod", "EASYPAISA");
        assertFalse(engine.processOrder(order));
    }

    // ---------- CASH ON DELIVERY ----------

    @Test
    void cashOnDelivery_smallOrder_pendingCash() throws IOException {
        assertTrue(engine.processOrder(validOrder()));
        assertTrue(readLog().contains("Status: PENDING_CASH"));
    }

    @Test
    void cashOnDelivery_payableAbove25000_rejected() {
        Map<String, Object> order = validOrder();
        order.put("items", oneItem(30000.0)); // (30000-500)*1.17 = 34515
        assertFalse(engine.processOrder(order));
    }

    @Test
    void cashOnDelivery_payableJustUnder25000_allowed() {
        Map<String, Object> order = validOrder();
        order.put("items", oneItem(21000.0)); // (21000-500)*1.17 = 23985
        assertTrue(engine.processOrder(order));
    }

    // ---------- UNSUPPORTED / DEFAULT PAYMENT ----------

    @Test
    void unsupportedPaymentMethod_rejected() {
        Map<String, Object> order = validOrder();
        order.put("paymentMethod", "BITCOIN");
        assertFalse(engine.processOrder(order));
    }

    @Test
    void missingPaymentMethod_defaultsToCash_accepted() throws IOException {
        Map<String, Object> order = validOrder();
        order.remove("paymentMethod");
        // legacy default is "CASH", which is NOT a supported method -> rejected
        assertFalse(engine.processOrder(order));
    }

    // ---------- TAX BY COUNTRY ----------

    @Test
    void tax_US_8Percent() throws IOException {
        Map<String, Object> order = validOrder();
        order.put("country", "US");
        assertTrue(engine.processOrder(order));
        assertEquals(1080.0, readPayableFromLog(), 0.01);
    }

    @Test
    void tax_UK_20Percent() throws IOException {
        Map<String, Object> order = validOrder();
        order.put("country", "UK");
        assertTrue(engine.processOrder(order));
        assertEquals(1200.0, readPayableFromLog(), 0.01);
    }

    @Test
    void tax_otherCountry_15PercentDefault() throws IOException {
        Map<String, Object> order = validOrder();
        order.put("country", "FR");
        assertTrue(engine.processOrder(order));
        assertEquals(1150.0, readPayableFromLog(), 0.01);
    }

    // ---------- INVALID INPUTS ----------

    @Test
    void nullOrder_rejected() {
        assertFalse(engine.processOrder(null));
    }

    @Test
    void emptyOrder_rejected() {
        assertFalse(engine.processOrder(new HashMap<>()));
    }

    @Test
    void invalidEmail_noAtSymbol_rejected() {
        Map<String, Object> order = validOrder();
        order.put("customerEmail", "aliexample.com");
        assertFalse(engine.processOrder(order));
    }

    @Test
    void missingEmail_rejected() {
        Map<String, Object> order = validOrder();
        order.remove("customerEmail");
        assertFalse(engine.processOrder(order));
    }

    @Test
    void emptyCart_rejected() {
        Map<String, Object> order = validOrder();
        order.put("items", new ArrayList<>());
        assertFalse(engine.processOrder(order));
    }

    @Test
    void missingItems_rejected() {
        Map<String, Object> order = validOrder();
        order.remove("items");
        assertFalse(engine.processOrder(order));
    }

    @Test
    void negativePrice_rejected() {
        Map<String, Object> order = validOrder();
        order.put("items", oneItem(-100.0));
        assertFalse(engine.processOrder(order));
    }

    @Test
    void zeroPrice_rejected() {
        Map<String, Object> order = validOrder();
        order.put("items", oneItem(0.0));
        assertFalse(engine.processOrder(order));
    }

    @Test
    void zeroQuantity_rejected() {
        Map<String, Object> order = validOrder();
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item(100.0, 0));
        order.put("items", items);
        assertFalse(engine.processOrder(order));
    }

    @Test
    void negativeQuantity_rejected() {
        Map<String, Object> order = validOrder();
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item(100.0, -2));
        order.put("items", items);
        assertFalse(engine.processOrder(order));
    }

    @Test
    void itemMissingPrice_rejected() {
        Map<String, Object> order = validOrder();
        Map<String, Object> badItem = new HashMap<>();
        badItem.put("quantity", 1);
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(badItem);
        order.put("items", items);
        assertFalse(engine.processOrder(order));
    }

    @Test
    void itemMissingQuantity_rejected() {
        Map<String, Object> order = validOrder();
        Map<String, Object> badItem = new HashMap<>();
        badItem.put("price", 100.0);
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(badItem);
        order.put("items", items);
        assertFalse(engine.processOrder(order));
    }

    // ---------- NOTIFICATIONS & LOGGING ----------

    @Test
    void smsNotification_orderStillSucceeds() {
        Map<String, Object> order = validOrder();
        order.put("notifyChannel", "SMS");
        order.put("phone", "03001234567");
        assertTrue(engine.processOrder(order));
    }

    @Test
    void unknownNotifyChannel_orderStillSucceeds() {
        Map<String, Object> order = validOrder();
        order.put("notifyChannel", "WHATSAPP");
        assertTrue(engine.processOrder(order));
    }

    @Test
    void successfulOrder_writesLogWithEmail() throws IOException {
        assertTrue(engine.processOrder(validOrder()));
        assertTrue(readLog().contains("Order for: ali@example.com"));
    }

    @Test
    void failedOrder_writesNoLog() {
        Map<String, Object> order = validOrder();
        order.put("customerEmail", "bad-email");
        assertFalse(engine.processOrder(order));
        assertFalse(Files.exists(logFile));
    }
}