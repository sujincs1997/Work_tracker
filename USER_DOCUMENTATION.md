# WorkTracker - Desktop Work Tracking Application
## User Documentation & Guide

Welcome to **WorkTracker**, a premium offline-first desktop application designed to replace spreadsheet-based workflows for logging, organizing, and analyzing daily professional activity.

---

## Table of Contents
1. [Overview](#1-overview)
2. [Quickstart Guide](#2-quickstart-guide)
3. [Core Modules](#3-core-modules)
   - [Dashboard](#dashboard)
   - [Task Management](#task-management)
   - [Shift & Time Tracking](#shift--time-tracking)
   - [Import Wizard](#import-wizard)
   - [Analytics & Reports](#analytics--reports)
4. [Settings & Configurations](#4-settings--configurations)
5. [Advanced Features](#5-advanced-features)

---

## 1. Overview
WorkTracker is built as a self-contained, offline-first application. All data is saved locally on your computer in a SQLite database.
- **Offline First**: No internet required.
- **Fast Startup**: Zero web browser or local server runtime overhead.
- **Automatic Backups**: Protects database logs on exit.

---

## 2. Quickstart Guide
1. **Launch the Application**: Run the executable launcher.
2. **Start a Shift**: In the left sidebar or Shift tracker panel, select your Shift (e.g. *General*) and click **Login**.
3. **Create a Task**:
   - Navigate to **Task Management**.
   - Click **➕ New Task**.
   - Fill in Task Name, Project, Client, Category, and Priority.
   - Click **Save Task**.
4. **Log Time**:
   - Go to **Dashboard**.
   - Under **Task Timer Console**, select the task you just created.
   - Click **▶ Start**.
   - When finished or taking a break, click **⏹ Stop** and enter completion progress and session notes.

---

## 3. Core Modules

### Dashboard
Provides a unified view of your current day, week, and month:
- **Metrics**: Total work hours logged, shift coverage, pending/completed task tallies, and average productivity rates.
- **Timer Console**: Select a task, assign an optional sub-process, and trigger timer states. Includes work time, break time, and auto-idle tracking.
- **Chronological Logs**: A live feed of today's activities.

### Task Management
A robust grid showing all recorded work items:
- **Filters**: Search by ID, client, project, dates, status, or category.
- **Context Menus**: Right-click on any task to edit details, delete, start a timer, or log revision additions.
- **Sub-Processes**: Break down complex tasks into sub-tasks (e.g., *Analysis, Design, QA*). Track separate timelines and completion rollups.
- **Revisions**: Log additional work requests without losing original metrics. Each revision logs a date, reasoning, and time spent.

### Shift & Time Tracking
Manage your daily attendance shifts:
- Support for **Morning, General, Evening, and Night** shifts.
- Tracks check-in (login) and check-out (logout).
- Automatically calculates worked hours by deducting custom break times.

### Import Wizard
Import existing task databases from Microsoft Excel (`.xlsx`, `.xls`) or CSV files:
1. **Upload**: Select the data source.
2. **Column Mapping**: Match spreadsheet headers to application database fields. Contains fuzzy logic auto-matching.
3. **Preview & Deduplicate**: Review the first 5 rows and choose whether to skip or overwrite tasks with matching IDs.

### Analytics & Reports
Visualize and export records:
- **Interactive Charts**: Allocation of time by project and category, completion trends, and daily productivity ratios.
- **Exporting**: Generate Excel files, CSV files, and professionally styled landscape PDF summary sheets.

---

## 4. Settings & Configurations
- **Database Location**: Move the active SQLite `.db` file to a custom directory or load a shared database.
- **Backup Folder**: Choose where automated backups are stored.
- **Auto Backup**: Automatically copy the active database on exit, keeping a rotating pool of the 10 most recent states.
- **Appearance**: Toggle between Light and Dark visual modes.
- **Shift Timings**: Define custom hours for Morning, General, Evening, and Night shifts.

---

## 5. Advanced Features
- **Progress Rollup**: Task completion percentage is automatically computed as the mathematical average of its sub-process progress states.
- **User Idle Detection**: If the application detects no mouse or keyboard inputs for 5 minutes while a timer is active, the timer automatically shifts seconds from *Worked Time* into *Idle Time* to guarantee productivity analytics integrity.
- **Revision Safeguard**: WorkTracker never overwrites original task baselines. When additional iterations are required, logging a *Revision* increments revision logs, recording reasons and separate durations.
