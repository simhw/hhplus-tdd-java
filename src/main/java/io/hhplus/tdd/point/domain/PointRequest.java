package io.hhplus.tdd.point.domain;

import lombok.Data;

@Data
public class PointRequest {

    private long id;
    private long amount;

    public PointRequest() {
    }

    public PointRequest(long id, long amount) {
        this.id = id;
        this.amount = amount;
    }

    void valid() {
        if (amount <= 0 || amount > Integer.MAX_VALUE) {
            throw new RuntimeException("amount is less than 0 or equal to max value");
        }
    }
}
