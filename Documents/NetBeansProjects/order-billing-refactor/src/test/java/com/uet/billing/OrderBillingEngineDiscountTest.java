package com.uet.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class OrderBillingEngineDiscountTest {

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

    private Map<String, Object> order(String customerType, List<Map<String, Object>> items) {
        Map<String, Object> order = new HashMap<>();
        order.put("customerEmail", "ali@example.com");
        order.put("customerType", customerType);
        order.put("items", items);
        order.put("paymentMethod", "CASH_ON_DELIVERY");
        return order;
    }

    private List<Map<String, Object>> oneItem(double price) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item(price, 1));
        return items;
    }

    // The engine only returns true/false, so we read the payable amount from the log file.
    private double readPayableFromLog() throws IOException {
        String content = Files.readString(logFile);
        String marker = "Payable: PKR ";
        int start = content.indexOf(marker) + marker.length();
        int end = content.indexOf(" | Status", start);
        return Double.parseDouble(content.substring(start, end));
    }

    // ---------- REGULAR customer ----------

    @Test
    void regularCustomer_smallOrder_noDiscount() throws IOException {
        assertTrue(engine.processOrder(order("REGULAR", oneItem(1000.0))));
        assertEquals(1170.0, readPayableFromLog(), 0.01);
    }

    @Test
    void regularCustomer_exactly10000_noDiscount() throws IOException {
        assertTrue(engine.processOrder(order("REGULAR", oneItem(10000.0))));
        assertEquals(11700.0, readPayableFromLog(), 0.01);
    }

    @Test
    void regularCustomer_above10000_flat500Discount() throws IOException {
        assertTrue(engine.processOrder(order("REGULAR", oneItem(12000.0))));
        assertEquals(13455.0, readPayableFromLog(), 0.01);
    }

    // ---------- VIP customer ----------

    @Test
    void vipCustomer_underOrEqual5000_10PercentDiscount() throws IOException {
        assertTrue(engine.processOrder(order("VIP", oneItem(1000.0))));
        assertEquals(1053.0, readPayableFromLog(), 0.01);
    }

    @Test
    void vipCustomer_exactly5000_stillGets10Percent() throws IOException {
        assertTrue(engine.processOrder(order("VIP", oneItem(5000.0))));
        assertEquals(5265.0, readPayableFromLog(), 0.01);
    }

    @Test
    void vipCustomer_over5000_15PercentDiscount() throws IOException {
        assertTrue(engine.processOrder(order("VIP", oneItem(6000.0))));
        assertEquals(5967.0, readPayableFromLog(), 0.01);
    }

    // ---------- STUDENT customer ----------

    @Test
    void studentCustomer_8PercentDiscount() throws IOException {
        assertTrue(engine.processOrder(order("STUDENT", oneItem(1000.0))));
        assertEquals(1076.4, readPayableFromLog(), 0.01);
    }

    // ---------- CORPORATE customer ----------

    @Test
    void corporateCustomer_fewItems_5PercentDiscount() throws IOException {
        assertTrue(engine.processOrder(order("CORPORATE", oneItem(1000.0))));
        assertEquals(1111.5, readPayableFromLog(), 0.01);
    }

    @Test
    void corporateCustomer_10OrMoreItems_20PercentDiscount() throws IOException {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            items.add(item(100.0, 1));
        }
        assertTrue(engine.processOrder(order("CORPORATE", items)));
        assertEquals(936.0, readPayableFromLog(), 0.01);
    }
}