# Refactor Pass 2

This pass focuses on `RmaFormDialog` without changing its behavior.

## Changes

- Extracted the RMA information layout into `RmaFormInformationPanel`.
- Extracted shipping layout into `RmaFormShippingPanel`.
- Extracted repair-item layout into `RmaFormRepairItemsPanel`.
- Extracted Save/Cancel layout into `RmaFormActionBar`.
- Kept validation, repository access, keyboard shortcuts, change tracking, and save logic in `RmaFormDialog`.
- Removed the obsolete layout-building methods from `RmaFormDialog`.

## Placement

All new files belong in:

`rmatracker/ui/`
