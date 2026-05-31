# FinTrack — Personal Finance Tracker

A full-featured personal finance management desktop application built with **Java Swing** and **SQLite**. Developed as a capstone project demonstrating core Object-Oriented Programming concepts including Abstraction, Inheritance, Polymorphism, Encapsulation, Interface, and Singleton design pattern.

---

## Features

| Module | Description |
|---|---|
| **Dashboard** | Monthly stats, 6-month bar chart, budget alerts, savings goals summary, recurring due alerts |
| **Expense / Income Tracking** | Add, view, and delete transactions with category, date, and notes |
| **Budget Manager** | Set monthly category-wise limits with colour-coded progress bars and overspend alerts |
| **Investment Tracker** | Track stocks, mutual funds, crypto, FDs, gold — view P/L and settle investments |
| **Recurring Transactions** | Set up monthly bills/salary templates and apply them in one click |
| **Savings Goals** | Create goals with target amounts, deadlines, and progress tracking |
| **Monthly History** | Browse any of the last 24 months with pie chart and transaction breakdown |
| **Reports** | Category-wise pie chart, full transaction table, CSV export |
| **Multi-user Auth** | Register and login system with per-user data isolation |

---

## OOP Concepts Used

| Concept | Where |
|---|---|
| **Abstraction** | `Transaction` — abstract class with abstract `getType()` and `getDisplayAmount()` |
| **Inheritance** | `Expense` and `Income` both extend `Transaction` |
| **Polymorphism** | `getType()` returns `"EXPENSE"` or `"INCOME"` depending on the object |
| **Encapsulation** | All model classes (`User`, `Budget`, `Investment`, etc.) use private fields with getters/setters |
| **Interface** | `Exportable` interface implemented by `ReportGenerator` for CSV export |
| **Singleton** | `DatabaseHelper` and `SessionManager` — only one instance throughout the app |

---

## Tech Stack

- **Language:** Java 17
- **GUI:** Java Swing (Nimbus Look & Feel)
- **Database:** SQLite via JDBC (`sqlite-jdbc`)
- **Build Tool:** Maven
- **DB File:** `fintrack.db` — auto-created on first run, no setup needed

---

## Project Structure

```
ExpenseTracker/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── Main.java          ← Entire app in one file
```

All classes are inner static classes inside `Main.java`, organized into layers:

```
Main.java
├── Model Layer
│   ├── Transaction (abstract)
│   ├── Expense extends Transaction
│   ├── Income extends Transaction
│   ├── User
│   ├── Category
│   ├── Budget
│   ├── RecurringTransaction
│   ├── SavingsGoal
│   ├── Investment
│   └── Exportable (interface)
├── Database Layer
│   └── DatabaseHelper (Singleton, JDBC + SQLite)
├── Session Layer
│   └── SessionManager (Singleton)
├── Report Layer
│   └── ReportGenerator implements Exportable
└── View Layer (Java Swing screens)
    ├── showLogin / showRegister
    ├── showDashboard
    ├── showAddTransaction
    ├── showBudget
    ├── showInvestments
    ├── showRecurring
    ├── showSavingsGoals
    ├── showHistory
    └── showReports
```

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Run

```bash
# Clone the repository
git clone https://github.com/ShubhamBagiyal/ExpenseTracker.git
cd ExpenseTracker

# Run the app
mvn compile exec:java
```

The SQLite database file `fintrack.db` is created automatically in the project root on first run.

### First time use

1. Click **"Create an account"** on the login screen
2. Register with your name, email, and password
3. Log in and start adding your income and expenses

---

## Database Schema

```sql
users                  → id, name, email, password
categories             → id, name, color, cat_type
transactions           → id, type, amount, description, date, user_id, category_id, note
budgets                → id, user_id, category_id, limit_amount, month
investments            → id, user_id, name, type, invested_amount, current_value, invested_date, note, status
recurring_transactions → id, user_id, type, amount, description, category_id, note, frequency, start_date, last_applied
savings_goals          → id, user_id, name, target_amount, saved_amount, deadline, note, status
```

---

## Income Categories

Income is restricted to three categories to keep reports clean:

- **Pocket Money**
- **Salary**
- **Others**

---

## CSV Export

Go to **Reports → Export as CSV** to download all your transactions as a `.csv` file that opens directly in Excel or Google Sheets.

---

## Course Details

- **Subject:** Object-Oriented Programming with Java
- **Experiment:** 20 — Mini Project (Capstone)
- **CO Mapping:** CO4
- **Objective:** Integrate OOP concepts into a standalone Java application with GUI and database support

---

## Author

**Shubham Singh Bagiyal**
B.Tech Computer Science — UPES Dehradun

[![GitHub](https://img.shields.io/badge/GitHub-ShubhamBagiyal-181717?logo=github)](https://github.com/ShubhamBagiyal)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-shubham--bagiyal-0A66C2?logo=linkedin)](https://linkedin.com/in/shubham-bagiyal/)
