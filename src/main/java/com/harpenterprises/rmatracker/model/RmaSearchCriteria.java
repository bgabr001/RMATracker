package com.harpenterprises.rmatracker.model;

import java.time.LocalDate;

/**
 * Stores all filters selected in the main window.
 */
public class RmaSearchCriteria {

    private final String searchText;
    private final Status status;
    private final String county;
    private final String machineType;
    private final LocalDate dateSentFrom;
    private final LocalDate dateSentTo;

    public RmaSearchCriteria(
            String searchText,
            Status status,
            String county,
            String machineType,
            LocalDate dateSentFrom,
            LocalDate dateSentTo
    ) {
        this.searchText = searchText;
        this.status = status;
        this.county = county;
        this.machineType = machineType;
        this.dateSentFrom = dateSentFrom;
        this.dateSentTo = dateSentTo;
    }

    public String getSearchText() {
        return searchText;
    }

    public Status getStatus() {
        return status;
    }

    public String getCounty() {
        return county;
    }

    public String getMachineType() {
        return machineType;
    }

    public LocalDate getDateSentFrom() {
        return dateSentFrom;
    }

    public LocalDate getDateSentTo() {
        return dateSentTo;
    }
}