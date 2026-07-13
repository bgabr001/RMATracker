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
    private List<ShippingInfo> shippingInformation;

    public RmaRecord() {
        repairItems = new ArrayList<>();
        shippingInformation = new ArrayList<>();
    }

    public RmaRecord(
            String rmaNumber,
            LocalDate dateSent,
            LocalDate dateReceived,
            Status status,
            String outgoingTrackingNumber,
            String returnTrackingNumber,
            String notes
    ) {
        this(rmaNumber, dateSent, dateReceived, status,
                outgoingTrackingNumber, returnTrackingNumber, notes,
                new ArrayList<>(), new ArrayList<>());
    }

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
        this(rmaNumber, dateSent, dateReceived, status,
                outgoingTrackingNumber, returnTrackingNumber, notes,
                repairItems, new ArrayList<>());
    }

    public RmaRecord(
            String rmaNumber,
            LocalDate dateSent,
            LocalDate dateReceived,
            Status status,
            String outgoingTrackingNumber,
            String returnTrackingNumber,
            String notes,
            List<RepairItem> repairItems,
            List<ShippingInfo> shippingInformation
    ) {
        this.rmaNumber = rmaNumber;
        this.dateSent = dateSent;
        this.dateReceived = dateReceived;
        this.status = status;
        this.outgoingTrackingNumber = outgoingTrackingNumber;
        this.returnTrackingNumber = returnTrackingNumber;
        this.notes = notes;
        this.repairItems = repairItems == null
                ? new ArrayList<>()
                : new ArrayList<>(repairItems);
        this.shippingInformation = shippingInformation == null
                ? new ArrayList<>()
                : new ArrayList<>(shippingInformation);
    }

    public String getRmaNumber() { return rmaNumber; }
    public void setRmaNumber(String rmaNumber) { this.rmaNumber = rmaNumber; }
    public LocalDate getDateSent() { return dateSent; }
    public void setDateSent(LocalDate dateSent) { this.dateSent = dateSent; }
    public LocalDate getDateReceived() { return dateReceived; }
    public void setDateReceived(LocalDate dateReceived) { this.dateReceived = dateReceived; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getOutgoingTrackingNumber() { return outgoingTrackingNumber; }
    public void setOutgoingTrackingNumber(String value) { outgoingTrackingNumber = value; }
    public String getReturnTrackingNumber() { return returnTrackingNumber; }
    public void setReturnTrackingNumber(String value) { returnTrackingNumber = value; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<RepairItem> getRepairItems() { return repairItems; }
    public void setRepairItems(List<RepairItem> repairItems) {
        this.repairItems = repairItems == null ? new ArrayList<>() : new ArrayList<>(repairItems);
    }
    public void addRepairItem(RepairItem repairItem) { repairItems.add(repairItem); }
    public boolean removeRepairItem(RepairItem repairItem) { return repairItems.remove(repairItem); }
    public List<ShippingInfo> getShippingInformation() { return shippingInformation; }
    public void setShippingInformation(List<ShippingInfo> shippingInformation) {
        this.shippingInformation = shippingInformation == null
                ? new ArrayList<>()
                : new ArrayList<>(shippingInformation);
    }
    public void addShippingInfo(ShippingInfo shippingInfo) { shippingInformation.add(shippingInfo); }
    public boolean removeShippingInfo(ShippingInfo shippingInfo) { return shippingInformation.remove(shippingInfo); }
}
