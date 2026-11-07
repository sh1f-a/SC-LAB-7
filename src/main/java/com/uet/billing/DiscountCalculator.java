package com.uet.billing;

public class DiscountCalculator {

    // VIP
    private static final double VIP_HIGH_THRESHOLD = 5000;
    private static final double VIP_HIGH_RATE = 0.15;
    private static final double VIP_STANDARD_RATE = 0.10;

    // Student
    private static final double STUDENT_RATE = 0.08;

    // Corporate
    private static final int CORPORATE_BULK_ITEM_COUNT = 10;
    private static final double CORPORATE_BULK_RATE = 0.20;
    private static final double CORPORATE_STANDARD_RATE = 0.05;

    // Regular
    private static final double REGULAR_FLAT_THRESHOLD = 10000;
    private static final double REGULAR_FLAT_DISCOUNT = 500.0;

    public double calculate(String customerType, double rawTotal, int itemCount) {
        switch (customerType) {
            case "VIP":
                return rawTotal > VIP_HIGH_THRESHOLD
                        ? rawTotal * VIP_HIGH_RATE
                        : rawTotal * VIP_STANDARD_RATE;
            case "STUDENT":
                return rawTotal * STUDENT_RATE;
            case "CORPORATE":
                return itemCount >= CORPORATE_BULK_ITEM_COUNT
                        ? rawTotal * CORPORATE_BULK_RATE
                        : rawTotal * CORPORATE_STANDARD_RATE;
            default:
                return rawTotal > REGULAR_FLAT_THRESHOLD ? REGULAR_FLAT_DISCOUNT : 0.0;
        }
    }
}