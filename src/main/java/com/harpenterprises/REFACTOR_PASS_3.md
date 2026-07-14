# Refactor Pass 3

This pass refactors the report-preview feature without changing its behavior.

## Changes

- Added `report/RmaReportHtmlBuilder.java` so report HTML generation is separate from the Swing dialog.
- Added `ui/RmaReportActionBar.java` so report action-button layout is isolated.
- Simplified `ui/RmaReportPreviewWindow.java` so it focuses on displaying and refreshing the report.
- Preserved the existing Print and Save as PDF placeholders for later Version 2 steps.
- Kept all existing RMA, machine, shipping, and report-preview content.
- Compile-checked every Java source file together.
