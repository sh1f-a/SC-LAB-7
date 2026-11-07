package com.uet.billing;

public class TaxCalculator {

    private static final double DEFAULT_RATE = 0.15;
    private static final double PK_RATE = 0.17;
    private static final double US_RATE = 0.08;
    private static final double UK_RATE = 0.20;

    public double getTaxRate(String country) {
        switch (country) {
            case "PK":
                return PK_RATE;
            case "US":
                return US_RATE;
            case "UK":
                return UK_RATE;
            default:
                return DEFAULT_RATE;
        }
    }

    public double calculateTax(double netAmount, String country) {
        return netAmount * getTaxRate(country);
    }
}
