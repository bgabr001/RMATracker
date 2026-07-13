package com.harpenterprises.rmatracker.service;

import com.harpenterprises.rmatracker.model.RepairItem;
import com.harpenterprises.rmatracker.model.RmaRecord;
import com.harpenterprises.rmatracker.model.RmaSearchCriteria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Searches and filters RMA records.
 */
public class RmaSearchService {

    public List<RmaRecord> search(
            List<RmaRecord> records,
            RmaSearchCriteria criteria
    ) {
        List<RmaRecord> matchingRecords =
                new ArrayList<>();

        if (records == null || criteria == null) {
            return matchingRecords;
        }

        for (RmaRecord record : records) {
            if (matchesAllFilters(record, criteria)) {
                matchingRecords.add(record);
            }
        }

        return matchingRecords;
    }

    private boolean matchesAllFilters(
            RmaRecord record,
            RmaSearchCriteria criteria
    ) {
        return matchesStatus(record, criteria)
                && matchesDateRange(record, criteria)
                && matchesCounty(record, criteria)
                && matchesMachineType(record, criteria)
                && matchesSearchText(record, criteria);
    }

    private boolean matchesStatus(
            RmaRecord record,
            RmaSearchCriteria criteria
    ) {
        if (criteria.getStatus() == null) {
            return true;
        }

        return record.getStatus() == criteria.getStatus();
    }

    private boolean matchesDateRange(
            RmaRecord record,
            RmaSearchCriteria criteria
    ) {
        LocalDate dateSent =
                record.getDateSent();

        LocalDate dateFrom =
                criteria.getDateSentFrom();

        LocalDate dateTo =
                criteria.getDateSentTo();

        if (dateFrom == null && dateTo == null) {
            return true;
        }

        if (dateSent == null) {
            return false;
        }

        if (dateFrom != null
                && dateSent.isBefore(dateFrom)) {

            return false;
        }

        if (dateTo != null
                && dateSent.isAfter(dateTo)) {

            return false;
        }

        return true;
    }

    private boolean matchesCounty(
            RmaRecord record,
            RmaSearchCriteria criteria
    ) {
        String selectedCounty =
                normalize(criteria.getCounty());

        if (selectedCounty.isEmpty()) {
            return true;
        }

        List<RepairItem> repairItems =
                record.getRepairItems();

        if (repairItems == null) {
            return false;
        }

        for (RepairItem item : repairItems) {
            if (normalize(item.getCounty())
                    .equals(selectedCounty)) {

                return true;
            }
        }

        return false;
    }

    private boolean matchesMachineType(
            RmaRecord record,
            RmaSearchCriteria criteria
    ) {
        String selectedMachineType =
                normalize(criteria.getMachineType());

        if (selectedMachineType.isEmpty()) {
            return true;
        }

        List<RepairItem> repairItems =
                record.getRepairItems();

        if (repairItems == null) {
            return false;
        }

        for (RepairItem item : repairItems) {
            if (normalize(item.getMachineType())
                    .equals(selectedMachineType)) {

                return true;
            }
        }

        return false;
    }

    private boolean matchesSearchText(
            RmaRecord record,
            RmaSearchCriteria criteria
    ) {
        String searchText =
                normalize(criteria.getSearchText());

        if (searchText.isEmpty()) {
            return true;
        }

        String searchableText =
                buildCompleteSearchText(record);

        String[] searchWords =
                searchText.split("\\s+");

        /*
         * Every entered word must occur somewhere
         * in the RMA or one of its repair items.
         */
        for (String word : searchWords) {
            if (!searchableText.contains(word)) {
                return false;
            }
        }

        return true;
    }

    private String buildCompleteSearchText(
            RmaRecord record
    ) {
        StringBuilder text =
                new StringBuilder();

        appendValue(text, record.getRmaNumber());
        appendValue(text, record.getDateSent());
        appendValue(text, record.getDateReceived());
        appendValue(text, record.getStatus());

        appendValue(
                text,
                record.getOutgoingTrackingNumber()
        );

        appendValue(
                text,
                record.getReturnTrackingNumber()
        );

        appendValue(text, record.getNotes());

        List<RepairItem> repairItems =
                record.getRepairItems();

        if (repairItems != null) {
            for (RepairItem item : repairItems) {
                appendValue(text, item.getCounty());
                appendValue(text, item.getMachineType());
                appendValue(text, item.getSerialNumber());
                appendValue(text, item.getVersion());

                appendValue(
                        text,
                        item.getProblemDescription()
                );

                appendValue(
                        text,
                        item.getRepairDescription()
                );
            }
        }

        return normalize(text.toString());
    }

    private void appendValue(
            StringBuilder builder,
            Object value
    ) {
        if (value == null) {
            return;
        }

        builder.append(value)
                .append(' ');
    }

    private String normalize(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}