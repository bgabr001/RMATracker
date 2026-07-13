package com.harpenterprises.rmatracker.model;

import java.time.LocalDateTime;

public class StatusHistory {

    private long id;
    private String rmaNumber;
    private Status oldStatus;
    private Status newStatus;
    private LocalDateTime changedAt;

    public StatusHistory() {
    }

    public StatusHistory(
            long id,
            String rmaNumber,
            Status oldStatus,
            Status newStatus,
            LocalDateTime changedAt
    ) {
        this.id = id;
        this.rmaNumber = rmaNumber;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRmaNumber() {
        return rmaNumber;
    }

    public void setRmaNumber(String rmaNumber) {
        this.rmaNumber = rmaNumber;
    }

    public Status getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(Status oldStatus) {
        this.oldStatus = oldStatus;
    }

    public Status getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(Status newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
