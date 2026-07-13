package com.harpenterprises.rmatracker.model;

public enum ShippingDirection {
    SENT_TO_REPAIR("Sent to Repair"),
    RETURNED_FROM_REPAIR("Returned from Repair");

    private final String displayName;

    ShippingDirection(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
