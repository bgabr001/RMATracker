package com.harpenterprises.rmatracker.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RmaRecord {

    private String rmaNumber;
    private LocalDate dateSent;
    private LocalDate dateReceived;
    private Status status;
    private String outgoingTrackingNumber;
    private String returnTrackingNumber;
    private String notes;

    private List<RepairItem> repairItems;

    // Default Constructor
    public RmaRecord() {
        this.status = Status.RECEIVED;
        this.repairItems = new ArrayList<>();
    }

    // Constructor without repair items
    public RmaRecord(
            String rmaNumber,
            LocalDate dateSent,
            LocalDate dateReceived,
            Status status,
            String outgoingTrackingNumber,
            String returnTrackingNumber,
            String notes
    ) {
        this.rmaNumber = rmaNumber;
        this.dateSent = dateSent;
        this.dateReceived = dateReceived;
        this.status = status;
        this.outgoingTrackingNumber = outgoingTrackingNumber;
        this.returnTrackingNumber = returnTrackingNumber;
        this.notes = notes;
        this.repairItems = new ArrayList<>();
    }

    // Constructor with repair items
    public RmaRecord(
            String rmaNumber,
            LocalDate dateSent,
            LocalDate dateReceived,
            Status status,
            String outgoingTrackingNumber,
            String returnTrackingNumber,
            String notes,
            List<RepairItem> repairItems
    ) {
        this.rmaNumber = rmaNumber;
        this.dateSent = dateSent;
        this.dateReceived = dateReceived;
        this.status = status;
        this.outgoingTrackingNumber = outgoingTrackingNumber;
        this.returnTrackingNumber = returnTrackingNumber;
        this.notes = notes;

        if (repairItems == null) {
            this.repairItems = new ArrayList<>();
        } else {
            this.repairItems = new ArrayList<>(repairItems);
        }
    }

    // Getters and Setters

    public String getRmaNumber() {
        return rmaNumber;
    }

    public void setRmaNumber(String rmaNumber) {
        this.rmaNumber = rmaNumber;
    }

    public LocalDate getDateSent() {
        return dateSent;
    }

    public void setDateSent(LocalDate dateSent) {
        this.dateSent = dateSent;
    }

    public LocalDate getDateReceived() {
        return dateReceived;
    }

    public void setDateReceived(LocalDate dateReceived) {
        this.dateReceived = dateReceived;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getOutgoingTrackingNumber() {
        return outgoingTrackingNumber;
    }

    public void setOutgoingTrackingNumber(String outgoingTrackingNumber) {
        this.outgoingTrackingNumber = outgoingTrackingNumber;
    }

    public String getReturnTrackingNumber() {
        return returnTrackingNumber;
    }

    public void setReturnTrackingNumber(String returnTrackingNumber) {
        this.returnTrackingNumber = returnTrackingNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<RepairItem> getRepairItems() {
        return repairItems;
    }

    public void setRepairItems(List<RepairItem> repairItems) {
        if (repairItems == null) {
            this.repairItems = new ArrayList<>();
        } else {
            this.repairItems = new ArrayList<>(repairItems);
        }
    }

    // Helper Methods

    public void addRepairItem(RepairItem repairItem) {
        if (repairItem != null) {
            repairItems.add(repairItem);
        }
    }

    public boolean removeRepairItem(RepairItem repairItem) {
        return repairItems.remove(repairItem);
    }

    public int getItemCount() {
        return repairItems.size();
    }

    @Override
    public String toString() {
        return "RMA: " + rmaNumber +
                " | Status: " + status +
                " | Machines: " + repairItems.size();
    }
}