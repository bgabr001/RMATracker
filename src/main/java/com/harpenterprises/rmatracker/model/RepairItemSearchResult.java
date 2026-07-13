package com.harpenterprises.rmatracker.model;

/**
 * Connects a RepairItem to the RMA that contains it.
 *
 * This lets the repair-item search table know which
 * complete RMA should open when the item is double-clicked.
 */
public class RepairItemSearchResult {

    private final String rmaNumber;
    private final RepairItem repairItem;

    public RepairItemSearchResult(
            String rmaNumber,
            RepairItem repairItem
    ) {
        this.rmaNumber = rmaNumber;
        this.repairItem = repairItem;
    }

    public String getRmaNumber() {
        return rmaNumber;
    }

    public RepairItem getRepairItem() {
        return repairItem;
    }
}