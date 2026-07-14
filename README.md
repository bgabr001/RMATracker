# RMA Repair Tracker 

A Java desktop application for tracking equipment sent out for repair.

The application will track:

* RMA numbers
* Counties connected to each RMA
* Machines included in each shipment
* Machine types
* Serial numbers
* Repair status
* Dates sent and received
* Tracking numbers
* Repair notes
* Completed and active RMAs

One RMA can contain multiple counties, and each county can contain multiple machines.

---

# Project Goals

The main goal is to replace manual RMA tracking with a searchable desktop application.

The finished program should allow the user to:

* Create a new RMA
* Add multiple counties to an RMA
* Add multiple machines to each county
* Edit existing RMAs
* Update repair statuses
* Search by RMA number, county, serial number, machine type, or status
* Track sent and returned equipment
* Store all information in a database
* View active and completed repairs
* Run the program on Windows and macOS
* Add optional Excel and server features later

---

# Technology

* Java
* Gradle
* IntelliJ IDEA
* Java Swing for the desktop interface
* SQLite for the database
* JDBC for database communication
* JUnit for testing

---

# Development Roadmap

## Step 1: Create the Project

Set up the Java project in IntelliJ.

Tasks:

* Create a new Gradle Java project
* Select a supported Java version
* Configure the Gradle build file
* Create the main package
* Create the main application class
* Confirm the program builds and runs

Example package:

```text
com.harpenterprises.rmatracker
```

Expected result:

```text
RMA Tracker Started
```

Status:

```text
Complete
```

---

## Step 2: Create the Project Structure

Create packages that keep the application organized.

Suggested structure:

```text
src
└── main
    └── java
        └── com
            └── harpenterprises
                └── rmatracker
                    ├── Main.java
                    ├── model
                    ├── database
                    ├── repository
                    ├── service
                    ├── ui
                    └── util
```

Package purposes:

```text
model
```

Stores application objects such as RMAs, counties, and machines.

```text
database
```

Handles the SQLite connection and database setup.

```text
repository
```

Contains code for saving, loading, updating, and deleting database records.

```text
service
```

Contains application rules and business logic.

```text
ui
```

Contains the Swing windows, forms, tables, and dialog boxes.

```text
util
```

Contains helper classes such as date formatting and validation.

Status:

```text
Complete
```

---

## Step 3: Create the Core Model Classes

Create the Java classes that represent the information in the application.

Main model classes:

### Rma

Suggested fields:

```text
id
rmaNumber
dateCreated
dateSent
dateCompleted
status
outboundTrackingNumber
returnTrackingNumber
notes
```

### RmaCounty

Represents a county connected to an RMA.

Suggested fields:

```text
id
rmaId
countyName
notes
```

### Machine

Represents an individual machine sent for repair.

Suggested fields:

```text
id
rmaCountyId
machineType
serialNumber
dateSent
dateReceived
repairResult
notes
```

### RmaStatus

Suggested statuses:

```text
DRAFT
READY_TO_SEND
SENT
IN_PROGRESS
WAITING_ON_REPAIR
RETURN_SHIPPED
PARTIALLY_RECEIVED
COMPLETED
CANCELLED
```

The exact status names can be adjusted later.

Status:

```text
Complete
```

---

## Step 4: Build the Main Application Window

Create the main desktop window using Java Swing.

The main window should contain:

* Application title
* RMA table
* Search field
* Status filter
* New RMA button
* Open/Edit button
* Delete button
* Refresh button
* Completed RMA filter
* Summary information

Suggested RMA table columns:

```text
RMA Number
Date Sent
Status
Counties
Machine Count
Tracking Number
Last Updated
```

Suggested summary information:

```text
Total RMAs
Active RMAs
Waiting for Repair
Return Shipped
Completed RMAs
```

Double-clicking a row should open the selected RMA.

Expected result:

The application opens to a usable dashboard, even before the database is connected.

Status:

```text
Not Started
```

---

## Step 5: Build the New and Edit RMA Form

Create a form for entering and editing RMA information.

The form should contain:

### Basic RMA Information

* RMA number
* Status
* Date created
* Date sent
* Date completed
* Outbound tracking number
* Return tracking number
* General notes

### County Section

The user should be able to:

* Add a county
* Remove a county
* Edit a county
* Add multiple counties to one RMA

Example:

```text
RMA 2026-015
├── Fayette County
├── Scott County
└── Clark County
```

### Machine Section

Each county should allow multiple machines.

Machine fields:

* Machine type
* Serial number
* Date sent
* Date received
* Repair result
* Notes

Example:

```text
Fayette County
├── Scanner - Serial 100245
├── ADA Machine - Serial 100711
└── Printer - Serial 209844
```

Buttons:

* Save
* Cancel
* Add County
* Remove County
* Add Machine
* Remove Machine

Expected result:

The user can completely enter an RMA before database storage is added.

Status:

```text
Not Started
```

---

## Step 6: Add the SQLite Database

Add permanent data storage using SQLite.

The application should create a local database file such as:

```text
data/rma_tracker.db
```

SQLite is appropriate because:

* It does not require a database server
* The database is stored in one file
* It works on Windows and macOS
* It is easy to back up
* It can later be migrated to a server database

Add the SQLite JDBC dependency to Gradle.

Create a database connection class.

Suggested class:

```text
DatabaseManager
```

Responsibilities:

* Open the database connection
* Create the database file
* Create missing tables
* Close database connections safely
* Handle database errors

Status:

```text
Not Started
```

---

## Step 7: Create the Database Tables

Create tables for RMAs, counties, and machines.

Suggested tables:

### rmas

```text
id
rma_number
date_created
date_sent
date_completed
status
outbound_tracking_number
return_tracking_number
notes
created_at
updated_at
```

### rma_counties

```text
id
rma_id
county_name
notes
```

### machines

```text
id
rma_county_id
machine_type
serial_number
date_sent
date_received
repair_result
notes
```

Relationships:

```text
One RMA
    has many counties

One county entry
    belongs to one RMA

One county entry
    has many machines

One machine
    belongs to one county entry
```

The database should use foreign keys so counties and machines remain connected to the correct RMA.

Status:

```text
Not Started
```

---

## Step 8: Create the Repository Classes

Create repository classes that communicate with the database.

Suggested repositories:

```text
RmaRepository
RmaCountyRepository
MachineRepository
```

Required functions:

### RMA Functions

* Create an RMA
* Find an RMA by ID
* Find an RMA by RMA number
* Load all RMAs
* Update an RMA
* Delete an RMA

### County Functions

* Add a county to an RMA
* Load counties for an RMA
* Update a county
* Remove a county

### Machine Functions

* Add a machine to a county
* Load machines for a county
* Update a machine
* Remove a machine
* Find a machine by serial number

Expected result:

The Java model objects can be saved to and loaded from SQLite.

Status:

```text
Not Started
```

---

## Step 9: Connect the Forms to the Database

Connect the main window and RMA form to the repository classes.

Required behavior:

* Clicking Save inserts a new RMA
* Editing an RMA updates the existing record
* Counties are saved with the correct RMA
* Machines are saved with the correct county
* The main table loads records from SQLite
* Refresh reloads the latest records
* Deleting an RMA removes its counties and machines
* Opening an RMA loads all saved information

Validation rules:

* RMA number cannot be blank
* RMA number must be unique
* County name cannot be blank
* Machine serial number cannot be blank when required
* Dates must be valid
* Completion date should not be before the sent date

Status:

```text
Not Started
```

---

## Step 10: Add Search and Filtering

Add search tools to the main dashboard.

The user should be able to search by:

* RMA number
* County name
* Machine serial number
* Machine type
* Tracking number
* Status
* Date sent
* Date completed

When searching by county, every RMA containing that county should appear.

Example:

Searching for:

```text
Fayette
```

Could return:

```text
RMA 2026-015
RMA 2026-022
RMA 2026-031
```

Filters should include:

* All RMAs
* Active RMAs
* Sent
* In progress
* Waiting for repair
* Return shipped
* Completed
* Cancelled

Status:

```text
Not Started
```

---

## Step 11: Improve Status Tracking

Add better status management for each repair.

The RMA should have an overall status.

Individual machines may also have their own status.

Example:

```text
RMA 2026-015
Overall Status: Partially Received

Scanner 100245: Received
ADA Machine 100711: Still in Repair
Printer 209844: Return Shipped
```

Possible machine statuses:

```text
NOT_SENT
SENT
RECEIVED_FOR_REPAIR
IN_REPAIR
WAITING_ON_PARTS
REPAIRED
RETURN_SHIPPED
RECEIVED
NOT_REPAIRABLE
REPLACED
```

The overall RMA status may be calculated from the machine statuses or selected manually.

Status:

```text
Not Started
```

---

## Step 12: Add Status History

Create a history table so important changes can be reviewed later.

Suggested table:

```text
status_history
```

Suggested fields:

```text
id
rma_id
machine_id
old_status
new_status
change_date
notes
```

Example history:

```text
July 10, 2026 - RMA created
July 11, 2026 - Status changed to Sent
July 14, 2026 - Status changed to In Repair
July 22, 2026 - Return tracking number added
July 25, 2026 - Status changed to Completed
```

Status:

```text
Not Started
```

---

## Step 13: Add Confirmation and Error Messages

Add clear messages for user actions.

Examples:

```text
RMA saved successfully.
```

```text
RMA number already exists.
```

```text
Are you sure you want to delete this RMA?
```

```text
The database could not be opened.
```

```text
Please enter a county name.
```

The program should not crash because of invalid form input.

Status:

```text
Not Started
```

---

## Step 14: Add Testing

Create automated tests using JUnit.

Suggested test areas:

* RMA creation
* Duplicate RMA detection
* County creation
* Machine creation
* Database inserts
* Database updates
* Database deletes
* Search by county
* Search by serial number
* Status changes
* Date validation

Also perform manual testing for the Swing interface.

Status:

```text
Not Started
```

---

## Step 15: Improve the Application Design

Polish the graphical interface.

Possible improvements:

* Harp Enterprises logo
* Application icon
* Consistent fonts
* Improved spacing
* Better button layout
* Table row highlighting
* Status colors
* Menu bar
* Keyboard shortcuts
* Confirmation dialogs
* Tooltips
* Resizable windows

Possible status colors:

```text
Draft - Gray
Sent - Blue
In Progress - Orange
Return Shipped - Purple
Completed - Green
Cancelled - Red
```

The colors should support the text, not replace it.

Status:

```text
Not Started
```

---

## Step 16: Add Database Backup and Restore

Add a safe way to back up the SQLite database.

Possible features:

* Create Backup button
* Restore Backup button
* Automatic backup when the program closes
* Date and time in backup file names

Example:

```text
backups/rma_tracker_backup_2026-07-11_2300.db
```

Before restoring a backup, the application should create a copy of the current database.

Status:

```text
Not Started
```

---

## Step 17: Package the Application

Prepare the program for normal use outside IntelliJ.

Create application packages for:

* Windows
* macOS

Possible tools:

* Gradle application plugin
* `jpackage`
* Fat JAR packaging

Possible outputs:

```text
RmaTracker.exe
```

```text
RmaTracker.app
```

The packaged application should include everything required to run it.

Status:

```text
Not Started
```

---

# Optional Future Features

These features are not required for the first working version.

---

## Future Step 18: Excel Import

Import existing RMA information from Excel files.

The current Excel organization may contain:

* RMA number as the file name
* County
* Machine type
* Serial number
* Sent information
* Received information

The importer should:

* Read the RMA number from the file name
* Create the RMA
* Add each county
* Add each machine
* Skip duplicate records
* Show an import summary
* Report invalid rows

This feature should be added only after the main database system is working.

Status:

```text
Future Feature
```

---

## Future Step 19: Excel and CSV Export

Export search results or individual RMAs.

Possible exports:

* All active RMAs
* Completed RMAs
* RMAs for one county
* Machines currently out for repair
* Machines received during a date range
* Full RMA detail report

Status:

```text
Future Feature
```

---

## Future Step 20: Reports

Add common reports.

Possible reports:

* Active RMA report
* Completed RMA report
* County repair history
* Machine repair history
* Outstanding machines
* Average repair time
* RMAs by date range
* Machines marked not repairable

Status:

```text
Future Feature
```

---

## Future Step 21: Document Attachments

Allow documents to be attached to an RMA.

Possible attachments:

* Packing slips
* Repair forms
* Shipping labels
* PDF documents
* Scanned documents
* Photos

The database should store the file location instead of placing large files directly inside the SQLite database.

Status:

```text
Future Feature
```

---

## Future Step 22: User Accounts

Add username and password support.

Possible roles:

```text
Administrator
Editor
Viewer
```

Permissions could include:

### Administrator

* Create users
* Edit all RMAs
* Delete RMAs
* Restore backups
* Change settings

### Editor

* Create RMAs
* Edit RMAs
* Update statuses
* Add machines

### Viewer

* Search RMAs
* View RMA details
* View reports

For a local desktop version, accounts can be stored securely in the database using password hashing.

Status:

```text
Future Feature
```

---

## Future Step 23: Server Database

Move from the local SQLite database to a central server.

Possible server databases:

* PostgreSQL
* MySQL
* MariaDB

This would allow multiple computers to use the same RMA data.

The application architecture should keep database code inside repository classes so migration is easier.

Status:

```text
Future Feature
```

---

## Future Step 24: Web Application

Create a browser-based version of the RMA Tracker.

Possible architecture:

```text
Java Spring Boot backend
PostgreSQL database
Web frontend
```

The web application could be opened from:

* Windows computers
* Mac computers
* iPhones
* Android phones
* Tablets

The user would log in through a web browser.

Status:

```text
Future Feature
```

---

## Future Step 25: Mobile-Friendly Access

Create a responsive web interface that works well on phones.

Possible mobile tasks:

* Search for an RMA
* View county and machine information
* Update status
* Add tracking numbers
* Mark equipment as received
* Add notes

A separate mobile application may not be necessary if the website works properly on mobile browsers.

Status:

```text
Future Feature
```

---

# Recommended Development Order

The recommended order is:

```text
Step 1  - Project Setup
Step 2  - Project Structure
Step 3  - Model Classes
Step 4  - Main Window
Step 5  - New/Edit RMA Form
Step 6  - SQLite Setup
Step 7  - Database Tables
Step 8  - Repository Classes
Step 9  - Connect GUI to Database
Step 10 - Search and Filtering
Step 11 - Detailed Status Tracking
Step 12 - Status History
Step 13 - Errors and Confirmations
Step 14 - Testing
Step 15 - GUI Polish
Step 16 - Backup and Restore
Step 17 - Application Packaging
```

Only after the desktop application works reliably should the optional features be added.

---

# First Version Requirements

Version 1.0 should include:

* Desktop application
* SQLite database
* Create an RMA
* Edit an RMA
* Delete an RMA
* Multiple counties per RMA
* Multiple machines per county
* Status tracking
* Sent and received dates
* Tracking numbers
* Notes
* Search by RMA number
* Search by county
* Search by serial number
* Search by status
* Database backup
* Windows and macOS support

Excel integration, user accounts, server access, and mobile access are not required for Version 1.0.

---

# Current Progress

```text
version 1.0
Phase 1 -- Foundation
[x] Step 1: Create the project
[x] Step 2: Create the project structure
[x] Step 3: Create the core model classes
[x] Step 4: Build the main application window
[x] Step 5: Build the New/Edit RMA form
[x] Step 6: Add SQLite Database Integration
    [x] Database Setup
    [x] Create database tables
    [x] Create repository classes
    [x] Connect the GUI to the database
    
Phase 2 User Experience
[x] Step 7:  Add search and filtering
[ ] Step 8:  Improve status tracking (maybe version 3.0)
[x] Step 9:  Add status history
[x] Step 10: Add confirmations and error handling
[x] Step 11: Add testing
[ ] Step 12: Improve the application design (maybe version 3.0)
[x] Step 13: Add backup and restore
[ ] Step 14: Package the application
```

---

# Next Step

The next development task is:

```text
Step 4: Build the Main Application Window
```

This will create the main RMA dashboard, table, buttons, search area, and summary section.

# Future Versions 

```text
1.0 (current)
[X] CRUD for RMAs
[X] Repair Items
[X] Search
[X] SQLite database
[X] Status field (basic)

Version 2.0 – Reports & Export

[x] Step 1: Create the RMA Report Preview Window
    • Open a report for the selected RMA
    • Display RMA information
    • Display all machines in a table
    • Add placeholder Print and PDF buttons

[x] Step 2: Enhance the Report Layout
    • Professional report design
    • Company logo (optional) -> later version
    • Report title and generation date
    • Machine count
    • Status history
    • Better spacing, colors, and page-ready formatting
    • Delete selected status-history entries

[ ] Step 3: Add Printable RMA Reports
    • Print Preview
    • Print dialog
    • Page setup
    • Multi-page support
    • Headers and footers
    • Print the selected RMA

[ ] Step 4: Add Excel Export
    • Export all displayed RMAs
    • Respect search and status filters
    • One machine per row
    • Export as .xlsx

[ ] Step 5: Add PDF Export
    • Export selected RMA
    • Export all displayed RMAs
    • Respect search and status filters
    • Professional PDF formatting
    • Save as .pdf

[ ] Step 6: Add Reports Menu & Toolbar
    • View Selected RMA Report
    • Print Selected RMA
    • Export Selected RMA to PDF
    • Print Current RMA List
    • Print Detailed RMA List
    • Export Current Results to Excel
    • Export Current Results to PDF
    • Double-click support (optional setting)

[ ] Step 7: Add Report & Export Testing
    • Report preview
    • Printing
    • PDF generation
    • Excel export
    • Filtered report testing
    • Large RMA testing

[ ] Step 8: Package Version 2.0
    • Build executable/JAR
    • Create installer
    • Verify reports, printing, and exports

3.0
[ ] Expanded status list
[ ] Color-coded statuses
[ ] Quick status changes
[ ] Automatic date updates
[ ] Status statistics dashboard

4.0
[ ] Web version
[ ] Mobile access
[ ] Email notifications
[ ] Vendor portal/API integration 



```