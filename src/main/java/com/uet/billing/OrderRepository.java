package com.uet.billing;

public interface OrderRepository {

    void save(String email, double payableAmount, String paymentStatus);
}