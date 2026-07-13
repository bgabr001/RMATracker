package com.harpenterprises.rmatracker.model;

import java.util.Objects;

public class ShippingInfo {
    private String carrier;
    private String trackingNumber;
    private ShippingDirection direction;

    public ShippingInfo() {
        this("", "", ShippingDirection.SENT_TO_REPAIR);
    }

    public ShippingInfo(
            String carrier,
            String trackingNumber,
            ShippingDirection direction
    ) {
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.direction = direction == null
                ? ShippingDirection.SENT_TO_REPAIR
                : direction;
    }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public ShippingDirection getDirection() { return direction; }
    public void setDirection(ShippingDirection direction) { this.direction = direction; }

    @Override
    public String toString() {
        return direction + " | " + carrier + " | " + trackingNumber;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ShippingInfo other)) return false;
        return Objects.equals(carrier, other.carrier)
                && Objects.equals(trackingNumber, other.trackingNumber)
                && direction == other.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(carrier, trackingNumber, direction);
    }
}
