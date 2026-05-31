import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

// ─────────────────────────────────────────────
//  FinTrack — Single File Expense Tracker
//  OOP concepts: Abstraction, Encapsulation,
//  Inheritance, Polymorphism, Singleton, Interface
// ─────────────────────────────────────────────

public class Main {

    // ══════════════════════════════════════════
    //  MODEL LAYER
    // ══════════════════════════════════════════

    // Abstract class — Abstraction + Encapsulation
    static abstract class Transaction {
        private int id;
        private double amount;
        private String description;
        private LocalDate date;
        private int userId, categoryId;
        private String note;

        public Transaction(double amount, String description, LocalDate date,
                           int userId, int categoryId, String note) {
            if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");
            this.amount = amount; this.description = description; this.date = date;
            this.userId = userId; this.categoryId = categoryId; this.note = note;
        }

        public abstract String getType();
        public abstract String getDisplayAmount();

        public int getId()              { return id; }
        public void setId(int id)       { this.id = id; }
        public double getAmount()       { return amount; }
        public String getDescription()  { return description; }
        public LocalDate getDate()      { return date; }
        public int getUserId()          { return userId; }
        public int getCategoryId()      { return categoryId; }
        public String getNote()         { return note != null ? note : ""; }
        public void setDescription(String d) { this.description = d; }
        public void setDate(LocalDate d)     { this.date = d; }
        public void setNote(String n)        { this.note = n; }
    }

    // Inheritance
    static class Expense extends Transaction {
        public Expense(double amount, String description, LocalDate date,
                       int userId, int categoryId, String note) {
            super(amount, description, date, userId, categoryId, note);
        }
        @Override public String getType()          { return "EXPENSE"; }
        @Override public String getDisplayAmount() { return "- \u20b9" + String.format("%.2f", getAmount()); }
    }

    static class Income extends Transaction {
        public Income(double amount, String description, LocalDate date,
                      int userId, int categoryId, String note) {
            super(amount, description, date, userId, categoryId, note);
        }
        @Override public String getType()          { return "INCOME"; }
        @Override public String getDisplayAmount() { return "+ \u20b9" + String.format("%.2f", getAmount()); }
    }

    // Encapsulation
    static class User {
        private int id;
        private String name, email, password;
        public User(String name, String email, String password) {
            this.name = name; this.email = email; this.password = password;
        }
        public int getId()          { return id; }
        public void setId(int id)   { this.id = id; }
        public String getName()     { return name; }
        public String getEmail()    { return email; }
        public String getPassword() { return password; }
    }

    static class Category {
        private int id;
        private String name, color;
        public Category(String name, String color) { this.name = name; this.color = color; }
        public int getId()         { return id; }
        public void setId(int id)  { this.id = id; }
        public String getName()    { return name; }
        public String getColor()   { return color; }
        @Override public String toString() { return name; }
    }

    static class Budget {
        private int id, userId, categoryId;
        private String categoryName, month;
        private double limitAmount, spentAmount;
        public Budget(int userId, int categoryId, double limitAmount, String month) {
            this.userId = userId; this.categoryId = categoryId;
            this.limitAmount = limitAmount; this.month = month;
        }
        public double getUsagePercent() { return limitAmount == 0 ? 0 : (spentAmount / limitAmount) * 100; }
        public boolean isExceeded()     { return spentAmount > limitAmount; }
        public boolean isWarning()      { return getUsagePercent() >= 80 && !isExceeded(); }
        public int getId()                     { return id; }
        public void setId(int id)              { this.id = id; }
        public int getUserId()                 { return userId; }
        public int getCategoryId()             { return categoryId; }
        public String getCategoryName()        { return categoryName; }
        public void setCategoryName(String n)  { this.categoryName = n; }
        public double getLimitAmount()         { return limitAmount; }
        public double getSpentAmount()         { return spentAmount; }
        public void setSpentAmount(double s)   { this.spentAmount = s; }
        public String getMonth()               { return month; }
    }

    // ── NEW: Recurring Transaction model ────────────────────────────────────
    static class RecurringTransaction {
        private int id, userId, categoryId;
        private String type, description, note, frequency;
        private double amount;
        private LocalDate startDate, lastApplied;

        public RecurringTransaction(int userId, String type, double amount, String description,
                                    int categoryId, String note, String frequency, LocalDate startDate) {
            this.userId = userId; this.type = type; this.amount = amount;
            this.description = description; this.categoryId = categoryId;
            this.note = note; this.frequency = frequency; this.startDate = startDate;
        }
        /** True if not yet applied this calendar month */
        public boolean isDueThisMonth() {
            String thisMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            if (lastApplied == null) return true;
            return !lastApplied.format(DateTimeFormatter.ofPattern("yyyy-MM")).equals(thisMonth);
        }
        public int getId()                       { return id; }
        public void setId(int id)                { this.id = id; }
        public int getUserId()                   { return userId; }
        public String getType()                  { return type; }
        public double getAmount()                { return amount; }
        public String getDescription()           { return description; }
        public int getCategoryId()               { return categoryId; }
        public String getNote()                  { return note != null ? note : ""; }
        public String getFrequency()             { return frequency; }
        public LocalDate getStartDate()          { return startDate; }
        public LocalDate getLastApplied()        { return lastApplied; }
        public void setLastApplied(LocalDate d)  { this.lastApplied = d; }
    }

    // ── NEW: Savings Goal model ──────────────────────────────────────────────
    static class SavingsGoal {
        private int id, userId;
        private String name, note, status;
        private double targetAmount, savedAmount;
        private LocalDate deadline;

        public SavingsGoal(int userId, String name, double targetAmount,
                           double savedAmount, LocalDate deadline, String note) {
            this.userId = userId; this.name = name; this.targetAmount = targetAmount;
            this.savedAmount = savedAmount; this.deadline = deadline;
            this.note = note; this.status = "ACTIVE";
        }
        public double getProgressPercent() {
            return targetAmount == 0 ? 0 : Math.min((savedAmount / targetAmount) * 100, 100);
        }
        public double getRemaining()  { return Math.max(0, targetAmount - savedAmount); }
        public boolean isAchieved()   { return savedAmount >= targetAmount; }

        public int getId()                    { return id; }
        public void setId(int id)             { this.id = id; }
        public int getUserId()                { return userId; }
        public String getName()               { return name; }
        public double getTargetAmount()       { return targetAmount; }
        public double getSavedAmount()        { return savedAmount; }
        public void setSavedAmount(double s)  { this.savedAmount = s; }
        public LocalDate getDeadline()        { return deadline; }
        public String getNote()               { return note != null ? note : ""; }
        public String getStatus()             { return status; }
        public void setStatus(String s)       { this.status = s; }
    }

    // Investment model
    static class Investment {
        private int id, userId;
        private String name, type, status;
        private double investedAmount, currentValue;
        private LocalDate investedDate;
        private String note;

        public Investment(int userId, String name, String type, double investedAmount,
                          double currentValue, LocalDate investedDate, String note) {
            this.userId = userId; this.name = name; this.type = type;
            this.investedAmount = investedAmount; this.currentValue = currentValue;
            this.investedDate = investedDate; this.note = note; this.status = "ACTIVE";
        }
        public double getProfitLoss()    { return currentValue - investedAmount; }
        public double getReturnPercent() { return investedAmount == 0 ? 0 : (getProfitLoss() / investedAmount) * 100; }
        public boolean isProfit()        { return getProfitLoss() >= 0; }
        public int getId()                       { return id; }
        public void setId(int id)                { this.id = id; }
        public int getUserId()                   { return userId; }
        public String getName()                  { return name; }
        public String getType()                  { return type; }
        public double getInvestedAmount()        { return investedAmount; }
        public double getCurrentValue()          { return currentValue; }
        public void setCurrentValue(double v)    { this.currentValue = v; }
        public LocalDate getInvestedDate()       { return investedDate; }
        public String getNote()                  { return note != null ? note : ""; }
        public String getStatus()                { return status; }
        public void setStatus(String s)          { this.status = s; }
    }

    // Interface — Abstraction
    interface Exportable {
        void exportToCSV(String filePath) throws Exception;
    }

    // ══════════════════════════════════════════
    //  DATABASE LAYER  (JDBC + SQLite)
    // ══════════════════════════════════════════

    // Singleton pattern
    static class DatabaseHelper {
        private static DatabaseHelper instance;
        private Connection conn;

        private DatabaseHelper() {
            try {
                conn = DriverManager.getConnection("jdbc:sqlite:fintrack.db");
                createTables();
                insertDefaultCategories();
                migrateIncomeCategories();
            } catch (SQLException e) { System.err.println("DB Error: " + e.getMessage()); }
        }

        public static DatabaseHelper getInstance() {
            if (instance == null) instance = new DatabaseHelper();
            return instance;
        }

        private void createTables() throws SQLException {
            Statement s = conn.createStatement();
            s.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT UNIQUE, password TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, color TEXT, cat_type TEXT DEFAULT 'ALL')");
            s.execute("CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, amount REAL, description TEXT, date TEXT, user_id INTEGER, category_id INTEGER, note TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS budgets (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, category_id INTEGER, limit_amount REAL, month TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS investments (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, name TEXT, type TEXT, invested_amount REAL, current_value REAL, invested_date TEXT, note TEXT, status TEXT DEFAULT 'ACTIVE')");
            s.execute("CREATE TABLE IF NOT EXISTS recurring_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, type TEXT, amount REAL, description TEXT, category_id INTEGER, note TEXT, frequency TEXT, start_date TEXT, last_applied TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS savings_goals (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, name TEXT, target_amount REAL, saved_amount REAL DEFAULT 0, deadline TEXT, note TEXT, status TEXT DEFAULT 'ACTIVE')");
            s.close();
        }

        // ── Categories: expense & the 3 income ones ──────────────────────────
        private void insertDefaultCategories() throws SQLException {
            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM categories");
            if (rs.getInt(1) > 0) return;
            PreparedStatement ps = conn.prepareStatement("INSERT INTO categories (name, color, cat_type) VALUES (?, ?, ?)");
            String[][] exp = {{"Food","#E24B4A","EXPENSE"},{"Transport","#1D9E75","EXPENSE"},
                {"Shopping","#EF9F27","EXPENSE"},{"Bills","#378ADD","EXPENSE"},
                {"Health","#D4537E","EXPENSE"},{"Entertainment","#7F77DD","EXPENSE"},
                {"Other","#888780","EXPENSE"}};
            String[][] inc = {{"Pocket Money","#3B6D11","INCOME"},{"Salary","#0F6E56","INCOME"},{"Others","#185FA5","INCOME"}};
            for (String[] c : exp) { ps.setString(1,c[0]); ps.setString(2,c[1]); ps.setString(3,c[2]); ps.executeUpdate(); }
            for (String[] c : inc) { ps.setString(1,c[0]); ps.setString(2,c[1]); ps.setString(3,c[2]); ps.executeUpdate(); }
            ps.close();
        }

        /**
         * Migration for existing DBs:
         * 1. Adds cat_type column if missing.
         * 2. Ensures Pocket Money / Salary / Others income categories exist.
         * 3. Re-maps all INCOME transactions whose category is not one of those
         *    three to the "Others" income category.
         */
        private void migrateIncomeCategories() throws SQLException {
            try { conn.createStatement().execute("ALTER TABLE categories ADD COLUMN cat_type TEXT DEFAULT 'ALL'"); }
            catch (SQLException ignored) {} // column already exists

            String[] incNames  = {"Pocket Money","Salary","Others"};
            String[] incColors = {"#3B6D11","#0F6E56","#185FA5"};
            for (int i = 0; i < incNames.length; i++) {
                ResultSet chk = conn.prepareStatement("SELECT id FROM categories WHERE name='"+incNames[i]+"'").executeQuery();
                if (!chk.next()) {
                    PreparedStatement ins = conn.prepareStatement("INSERT INTO categories (name, color, cat_type) VALUES (?,?,?)");
                    ins.setString(1,incNames[i]); ins.setString(2,incColors[i]); ins.setString(3,"INCOME"); ins.executeUpdate();
                } else {
                    conn.prepareStatement("UPDATE categories SET cat_type='INCOME' WHERE name='"+incNames[i]+"'").executeUpdate();
                }
            }

            // Collect valid income category IDs
            ResultSet vrs = conn.prepareStatement("SELECT id FROM categories WHERE cat_type='INCOME'").executeQuery();
            List<Integer> validIds = new ArrayList<>();
            while (vrs.next()) validIds.add(vrs.getInt("id"));
            if (validIds.isEmpty()) return;

            // "Others" ID
            ResultSet ors = conn.prepareStatement("SELECT id FROM categories WHERE name='Others' AND cat_type='INCOME'").executeQuery();
            if (!ors.next()) return;
            int othersId = ors.getInt("id");

            // Remap
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < validIds.size(); i++) { sb.append(validIds.get(i)); if(i<validIds.size()-1) sb.append(","); }
            conn.createStatement().execute(
                "UPDATE transactions SET category_id="+othersId+" WHERE type='INCOME' AND category_id NOT IN ("+sb+")");
        }

        public boolean registerUser(User u) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO users (name, email, password) VALUES (?,?,?)")) {
                ps.setString(1,u.getName()); ps.setString(2,u.getEmail()); ps.setString(3,u.getPassword());
                ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public User loginUser(String email, String password) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email=? AND password=?")) {
                ps.setString(1,email); ps.setString(2,password);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    User u = new User(rs.getString("name"),rs.getString("email"),rs.getString("password"));
                    u.setId(rs.getInt("id")); return u;
                }
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return null;
        }

        public boolean addTransaction(Transaction t) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO transactions (type,amount,description,date,user_id,category_id,note) VALUES (?,?,?,?,?,?,?)")) {
                ps.setString(1,t.getType()); ps.setDouble(2,t.getAmount());
                ps.setString(3,t.getDescription()); ps.setString(4,t.getDate().toString());
                ps.setInt(5,t.getUserId()); ps.setInt(6,t.getCategoryId()); ps.setString(7,t.getNote());
                ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public boolean deleteTransaction(int id) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE id=?")) {
                ps.setInt(1,id); ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public List<Transaction> getTransactionsByUser(int userId) {
            List<Transaction> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM transactions WHERE user_id=? ORDER BY date DESC")) {
                ps.setInt(1,userId); ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapTx(rs));
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return list;
        }

        public double getTotalByTypeAndMonth(int userId, String type, String month) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT SUM(amount) FROM transactions WHERE user_id=? AND type=? AND strftime('%Y-%m',date)=?")) {
                ps.setInt(1,userId); ps.setString(2,type); ps.setString(3,month);
                ResultSet rs = ps.executeQuery(); return rs.getDouble(1);
            } catch (SQLException e) { return 0; }
        }

        public double getSpentByCategory(int userId, int catId, String month) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT SUM(amount) FROM transactions WHERE user_id=? AND category_id=? AND type='EXPENSE' AND strftime('%Y-%m',date)=?")) {
                ps.setInt(1,userId); ps.setInt(2,catId); ps.setString(3,month);
                ResultSet rs = ps.executeQuery(); return rs.getDouble(1);
            } catch (SQLException e) { return 0; }
        }

        public List<Category> getExpenseCategories() {
            List<Category> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM categories WHERE cat_type='EXPENSE' OR cat_type='ALL' ORDER BY id")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) { Category c = new Category(rs.getString("name"),rs.getString("color")); c.setId(rs.getInt("id")); list.add(c); }
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return list;
        }

        /** Returns only Pocket Money, Salary, Others */
        public List<Category> getIncomeCategories() {
            List<Category> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM categories WHERE cat_type='INCOME' ORDER BY id")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) { Category c = new Category(rs.getString("name"),rs.getString("color")); c.setId(rs.getInt("id")); list.add(c); }
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return list;
        }

        public List<Category> getAllCategories() {
            List<Category> list = new ArrayList<>();
            try (Statement s = conn.createStatement()) {
                ResultSet rs = s.executeQuery("SELECT * FROM categories ORDER BY id");
                while (rs.next()) { Category c = new Category(rs.getString("name"),rs.getString("color")); c.setId(rs.getInt("id")); list.add(c); }
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return list;
        }

        public String getCategoryName(int id) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM categories WHERE id=?")) {
                ps.setInt(1,id); ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getString("name");
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return "Other";
        }

        public boolean saveBudget(Budget b) {
            try {
                PreparedStatement check = conn.prepareStatement("SELECT id FROM budgets WHERE user_id=? AND category_id=? AND month=?");
                check.setInt(1,b.getUserId()); check.setInt(2,b.getCategoryId()); check.setString(3,b.getMonth());
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    PreparedStatement upd = conn.prepareStatement("UPDATE budgets SET limit_amount=? WHERE id=?");
                    upd.setDouble(1,b.getLimitAmount()); upd.setInt(2,rs.getInt("id")); upd.executeUpdate();
                } else {
                    PreparedStatement ins = conn.prepareStatement("INSERT INTO budgets (user_id,category_id,limit_amount,month) VALUES (?,?,?,?)");
                    ins.setInt(1,b.getUserId()); ins.setInt(2,b.getCategoryId()); ins.setDouble(3,b.getLimitAmount()); ins.setString(4,b.getMonth()); ins.executeUpdate();
                }
                return true;
            } catch (SQLException e) { return false; }
        }

        public List<Budget> getBudgetsByMonth(int userId, String month) {
            List<Budget> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT b.*, c.name as cat_name FROM budgets b JOIN categories c ON b.category_id=c.id WHERE b.user_id=? AND b.month=?")) {
                ps.setInt(1,userId); ps.setString(2,month); ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Budget bg = new Budget(userId,rs.getInt("category_id"),rs.getDouble("limit_amount"),month);
                    bg.setId(rs.getInt("id")); bg.setCategoryName(rs.getString("cat_name"));
                    bg.setSpentAmount(getSpentByCategory(userId,rs.getInt("category_id"),month)); list.add(bg);
                }
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return list;
        }

        // ── Investment CRUD ──────────────────────────────────────────────────
        public boolean addInvestment(Investment inv) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO investments (user_id,name,type,invested_amount,current_value,invested_date,note,status) VALUES (?,?,?,?,?,?,?,?)")) {
                ps.setInt(1,inv.getUserId()); ps.setString(2,inv.getName()); ps.setString(3,inv.getType());
                ps.setDouble(4,inv.getInvestedAmount()); ps.setDouble(5,inv.getCurrentValue());
                ps.setString(6,inv.getInvestedDate().toString()); ps.setString(7,inv.getNote()); ps.setString(8,inv.getStatus());
                ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public List<Investment> getInvestmentsByUser(int userId) {
            List<Investment> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM investments WHERE user_id=? ORDER BY invested_date DESC")) {
                ps.setInt(1,userId); ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Investment inv = new Investment(userId,rs.getString("name"),rs.getString("type"),
                        rs.getDouble("invested_amount"),rs.getDouble("current_value"),
                        LocalDate.parse(rs.getString("invested_date")),rs.getString("note"));
                    inv.setId(rs.getInt("id")); inv.setStatus(rs.getString("status")); list.add(inv);
                }
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return list;
        }

        public boolean updateInvestmentValue(int id, double newValue) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE investments SET current_value=? WHERE id=?")) {
                ps.setDouble(1,newValue); ps.setInt(2,id); ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public boolean settleInvestment(int id, String status) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE investments SET status=? WHERE id=?")) {
                ps.setString(1,status); ps.setInt(2,id); ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public boolean deleteInvestment(int id) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM investments WHERE id=?")) {
                ps.setInt(1,id); ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public List<Transaction> getTransactionsByUserAndMonth(int userId, String month) {
            List<Transaction> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM transactions WHERE user_id=? AND strftime('%Y-%m',date)=? ORDER BY date DESC")) {
                ps.setInt(1,userId); ps.setString(2,month); ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapTx(rs));
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return list;
        }

        // ── Recurring Transactions CRUD ──────────────────────────────────────
        public boolean addRecurring(RecurringTransaction r) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO recurring_transactions (user_id,type,amount,description,category_id,note,frequency,start_date,last_applied) VALUES (?,?,?,?,?,?,?,?,?)")) {
                ps.setInt(1,r.getUserId()); ps.setString(2,r.getType()); ps.setDouble(3,r.getAmount());
                ps.setString(4,r.getDescription()); ps.setInt(5,r.getCategoryId()); ps.setString(6,r.getNote());
                ps.setString(7,r.getFrequency()); ps.setString(8,r.getStartDate().toString()); ps.setString(9,null);
                ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public List<RecurringTransaction> getRecurringByUser(int userId) {
            List<RecurringTransaction> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM recurring_transactions WHERE user_id=? ORDER BY id DESC")) {
                ps.setInt(1,userId); ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    RecurringTransaction r = new RecurringTransaction(userId,rs.getString("type"),rs.getDouble("amount"),
                        rs.getString("description"),rs.getInt("category_id"),rs.getString("note"),
                        rs.getString("frequency"),LocalDate.parse(rs.getString("start_date")));
                    r.setId(rs.getInt("id"));
                    String la = rs.getString("last_applied");
                    if (la != null) r.setLastApplied(LocalDate.parse(la));
                    list.add(r);
                }
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return list;
        }

        public boolean applyRecurring(RecurringTransaction r) {
            Transaction t = "INCOME".equals(r.getType())
                ? new Income(r.getAmount(),r.getDescription(),LocalDate.now(),r.getUserId(),r.getCategoryId(),"Recurring: "+r.getNote())
                : new Expense(r.getAmount(),r.getDescription(),LocalDate.now(),r.getUserId(),r.getCategoryId(),"Recurring: "+r.getNote());
            addTransaction(t);
            try (PreparedStatement ps = conn.prepareStatement("UPDATE recurring_transactions SET last_applied=? WHERE id=?")) {
                ps.setString(1,LocalDate.now().toString()); ps.setInt(2,r.getId()); ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public boolean deleteRecurring(int id) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM recurring_transactions WHERE id=?")) {
                ps.setInt(1,id); ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        // ── Savings Goals CRUD ───────────────────────────────────────────────
        public boolean addSavingsGoal(SavingsGoal g) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO savings_goals (user_id,name,target_amount,saved_amount,deadline,note,status) VALUES (?,?,?,?,?,?,?)")) {
                ps.setInt(1,g.getUserId()); ps.setString(2,g.getName());
                ps.setDouble(3,g.getTargetAmount()); ps.setDouble(4,g.getSavedAmount());
                ps.setString(5,g.getDeadline()!=null?g.getDeadline().toString():null);
                ps.setString(6,g.getNote()); ps.setString(7,g.getStatus());
                ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public List<SavingsGoal> getSavingsGoalsByUser(int userId) {
            List<SavingsGoal> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM savings_goals WHERE user_id=? ORDER BY id DESC")) {
                ps.setInt(1,userId); ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String dl = rs.getString("deadline");
                    SavingsGoal g = new SavingsGoal(userId,rs.getString("name"),rs.getDouble("target_amount"),
                        rs.getDouble("saved_amount"),dl!=null?LocalDate.parse(dl):null,rs.getString("note"));
                    g.setId(rs.getInt("id")); g.setStatus(rs.getString("status")); list.add(g);
                }
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return list;
        }

        public boolean addToSavingsGoal(int goalId, double amount) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE savings_goals SET saved_amount = saved_amount + ? WHERE id=?")) {
                ps.setDouble(1,amount); ps.setInt(2,goalId); ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public boolean markGoalAchieved(int goalId) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE savings_goals SET status='ACHIEVED' WHERE id=?")) {
                ps.setInt(1,goalId); ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public boolean deleteSavingsGoal(int id) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM savings_goals WHERE id=?")) {
                ps.setInt(1,id); ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        private Transaction mapTx(ResultSet rs) throws SQLException {
            String type = rs.getString("type");
            Transaction t = "INCOME".equals(type)
                ? new Income(rs.getDouble("amount"),rs.getString("description"),LocalDate.parse(rs.getString("date")),rs.getInt("user_id"),rs.getInt("category_id"),rs.getString("note"))
                : new Expense(rs.getDouble("amount"),rs.getString("description"),LocalDate.parse(rs.getString("date")),rs.getInt("user_id"),rs.getInt("category_id"),rs.getString("note"));
            t.setId(rs.getInt("id")); return t;
        }
    }

    // ══════════════════════════════════════════
    //  SESSION MANAGER (Singleton)
    // ══════════════════════════════════════════

    static class SessionManager {
        private static SessionManager instance;
        private User currentUser;
        private SessionManager() {}
        public static SessionManager getInstance() { if(instance==null) instance=new SessionManager(); return instance; }
        public void login(User u)     { currentUser = u; }
        public void logout()          { currentUser = null; }
        public User getCurrentUser()  { return currentUser; }
        public int getCurrentUserId() { return currentUser!=null?currentUser.getId():-1; }
    }

    // ══════════════════════════════════════════
    //  REPORT GENERATOR  (implements Exportable)
    // ══════════════════════════════════════════

    static class ReportGenerator implements Exportable {
        private final List<Transaction> transactions;
        public ReportGenerator(List<Transaction> transactions) { this.transactions = transactions; }

        @Override
        public void exportToCSV(String filePath) throws Exception {
            try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath))) {
                w.write("ID,Type,Description,Amount,Category,Date,Note"); w.newLine();
                DatabaseHelper db = DatabaseHelper.getInstance();
                for (Transaction t : transactions) {
                    w.write(String.join(",", String.valueOf(t.getId()), t.getType(), csv(t.getDescription()),
                        String.format("%.2f",t.getAmount()), csv(db.getCategoryName(t.getCategoryId())),
                        t.getDate().toString(), csv(t.getNote())));
                    w.newLine();
                }
            } catch (IOException e) { throw new Exception("Export failed: "+e.getMessage()); }
        }

        private String csv(String v) {
            if (v==null) return "";
            if (v.contains(",") || v.contains("\"")) v = "\""+v.replace("\"","\"\"")+"\"";
            return v;
        }
    }

    // ══════════════════════════════════════════
    //  DESIGN TOKENS  — modern dark-sidebar palette
    // ══════════════════════════════════════════

    // Sidebar / topbar
    static final Color NAV_BG        = new Color(0x1A,0x1F,0x2E);   // deep navy
    static final Color NAV_ACTIVE_BG = new Color(0x25,0x2D,0x44);   // slightly lighter navy
    static final Color NAV_ACCENT    = new Color(0x4A,0xDE,0x80);   // vivid green accent
    static final Color NAV_TEXT      = new Color(0xB0,0xBB,0xCC);   // muted blue-grey
    static final Color NAV_TEXT_ACT  = Color.WHITE;

    // Content area
    static final Color BG            = new Color(0xF0,0xF2,0xF7);   // soft blue-grey page
    static final Color WHITE         = Color.WHITE;
    static final Color CARD_BG       = new Color(0xFF,0xFF,0xFF);
    static final Color BORDER_COL    = new Color(0xE4,0xE8,0xF0);
    static final Color DARK_TEXT     = new Color(0x12,0x17,0x28);
    static final Color GRAY_TEXT     = new Color(0x7A,0x85,0x9A);
    static final Color SUBTEXT       = new Color(0x9A,0xA5,0xB8);

    // Semantic colours
    static final Color GREEN         = new Color(0x10,0xB9,0x81);   // emerald
    static final Color GREEN_DARK    = new Color(0x05,0x7A,0x55);
    static final Color GREEN_LIGHT   = new Color(0xD1,0xFA,0xE5);
    static final Color RED_COL       = new Color(0xEF,0x44,0x44);
    static final Color RED_LIGHT     = new Color(0xFE,0xE2,0xE2);
    static final Color AMBER         = new Color(0xF5,0x9E,0x0B);
    static final Color AMBER_LIGHT   = new Color(0xFE,0xF3,0xC7);
    static final Color BLUE          = new Color(0x38,0x8B,0xFF);
    static final Color BLUE_LIGHT    = new Color(0xDB,0xEA,0xFF);
    static final Color PURPLE        = new Color(0x7C,0x3A,0xED);
    static final Color PURPLE_LIGHT  = new Color(0xED,0xE9,0xFE);
    static final Color ORANGE        = new Color(0xF9,0x73,0x16);
    static final Color ORANGE_LIGHT  = new Color(0xFF,0xED,0xD5);

    // Table row tints
    static final Color INC_ROW       = new Color(0xF0,0xFD,0xF4);
    static final Color EXP_ROW       = new Color(0xFF,0xF1,0xF2);

    static Color hex(String h) { return Color.decode(h); }

    // ── Rounded card panel ───────────────────────────────────────────────────
    static JPanel card(int pad) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                g2.setColor(BORDER_COL);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(pad,pad,pad,pad));
        return p;
    }

    // Coloured accent card (left border stripe)
    static JPanel accentCard(int pad, Color accent) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                g2.setColor(BORDER_COL);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
                g2.setColor(accent);
                g2.fillRoundRect(0,0,5,getHeight(),4,4);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(pad, pad+10, pad, pad));
        return p;
    }

    static JLabel fLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI",Font.PLAIN,11));
        l.setForeground(GRAY_TEXT);
        return l;
    }

    // ── Styled text field with focus glow ────────────────────────────────────
    static JTextField styledField(String prompt) {
        JTextField f = new JTextField() {
            boolean focused = false;
            { addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { focused=true;  repaint(); }
                public void focusLost (FocusEvent e) { focused=false; repaint(); }
            }); }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(focused ? GREEN : BORDER_COL);
                g2.setStroke(new BasicStroke(focused ? 2f : 1f));
                g2.drawRoundRect(1,1,getWidth()-3,getHeight()-3,10,10);
                g2.dispose();
            }
        };
        f.setFont(new Font("Segoe UI",Font.PLAIN,13));
        f.setBackground(CARD_BG);
        f.setForeground(DARK_TEXT);
        f.setCaretColor(GREEN);
        f.setOpaque(true);
        f.setBorder(new EmptyBorder(9,13,9,13));
        return f;
    }

    static JPasswordField styledPass(String prompt) {
        JPasswordField f = new JPasswordField() {
            boolean focused = false;
            { addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { focused=true;  repaint(); }
                public void focusLost (FocusEvent e) { focused=false; repaint(); }
            }); }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(focused ? GREEN : BORDER_COL);
                g2.setStroke(new BasicStroke(focused ? 2f : 1f));
                g2.drawRoundRect(1,1,getWidth()-3,getHeight()-3,10,10);
                g2.dispose();
            }
        };
        f.setFont(new Font("Segoe UI",Font.PLAIN,13));
        f.setBackground(CARD_BG);
        f.setForeground(DARK_TEXT);
        f.setCaretColor(GREEN);
        f.setOpaque(true);
        f.setBorder(new EmptyBorder(9,13,9,13));
        return f;
    }

    // ── Pill-shaped primary button ────────────────────────────────────────────
    static JButton primaryBtn(String text, Color bg) {
        JButton b = new JButton(text) {
            Color hover = bg.brighter();
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { setBackground(hover); }
                public void mouseExited (MouseEvent e) { setBackground(bg); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setBackground(bg); b.setForeground(WHITE);
        b.setFont(new Font("Segoe UI",Font.BOLD,13));
        b.setFocusPainted(false); b.setBorderPainted(false); b.setContentAreaFilled(false); b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(11,22,11,22));
        return b;
    }

    // ── Ghost / outline button ────────────────────────────────────────────────
    static JButton outlineBtn(String text, Color fg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(fg.getRed(),fg.getGreen(),fg.getBlue(),20));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(fg);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1,1,getWidth()-3,getHeight()-3,12,12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(fg); b.setFont(new Font("Segoe UI",Font.PLAIN,13));
        b.setFocusPainted(false); b.setBorderPainted(false); b.setContentAreaFilled(false); b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(9,18,9,18));
        return b;
    }

    // ── Compact action chip button ────────────────────────────────────────────
    static JButton smallBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setBackground(bg); b.setForeground(fg);
        b.setFont(new Font("Segoe UI",Font.BOLD,11));
        b.setBorderPainted(false); b.setFocusPainted(false); b.setContentAreaFilled(false); b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(4,10,4,10));
        return b;
    }

    // ── Stat card with coloured top stripe ───────────────────────────────────
    static JPanel statCard(String label, String value, String colorHex) {
        Color accent = hex(colorHex);
        Color lightBg = new Color(
            Math.min(255, accent.getRed()+180),
            Math.min(255, accent.getGreen()+180),
            Math.min(255, accent.getBlue()+180));
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                g2.setColor(accent);
                g2.fillRoundRect(0,0,getWidth(),8,8,8);
                g2.fillRect(0,4,getWidth(),4);
                g2.setColor(BORDER_COL);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,14,14);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(20,16,16,16));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI",Font.PLAIN,11));
        lbl.setForeground(GRAY_TEXT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI",Font.BOLD,24));
        val.setForeground(accent);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl); p.add(Box.createVerticalStrut(6)); p.add(val);
        return p;
    }

    // ── Top header bar ────────────────────────────────────────────────────────
    static JPanel topBar(JFrame frame, String title, boolean withLogout) {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0,0,NAV_BG,getWidth(),0,new Color(0x1E,0x29,0x47));
                g2.setPaint(gp); g2.fillRect(0,0,getWidth(),getHeight()); g2.dispose();
            }
        };
        bar.setOpaque(false); bar.setBorder(new EmptyBorder(0,0,0,0));

        // Left: logo dot + title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0)); left.setOpaque(false); left.setBorder(new EmptyBorder(12,20,12,0));
        JLabel dot = new JLabel("●"); dot.setFont(new Font("Segoe UI",Font.BOLD,18)); dot.setForeground(NAV_ACCENT);
        JLabel lbl = new JLabel("  FinTrack"); lbl.setFont(new Font("Segoe UI",Font.BOLD,17)); lbl.setForeground(WHITE);
        left.add(dot); left.add(lbl);
        bar.add(left, BorderLayout.WEST);

        if (withLogout) {
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,12)); right.setOpaque(false); right.setBorder(new EmptyBorder(0,0,0,16));
            User u = SessionManager.getInstance().getCurrentUser();
            // Avatar circle + name
            JLabel avatar = new JLabel(String.valueOf(u.getName().charAt(0)).toUpperCase()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(NAV_ACCENT); g2.fillOval(0,0,getWidth(),getHeight()); g2.dispose();
                    super.paintComponent(g);
                }
            };
            avatar.setFont(new Font("Segoe UI",Font.BOLD,13)); avatar.setForeground(NAV_BG);
            avatar.setHorizontalAlignment(SwingConstants.CENTER); avatar.setVerticalAlignment(SwingConstants.CENTER);
            avatar.setPreferredSize(new Dimension(30,30));
            JLabel userLbl = new JLabel(u.getName());
            userLbl.setFont(new Font("Segoe UI",Font.PLAIN,13)); userLbl.setForeground(new Color(0xD0,0xD8,0xE8));
            JButton logoutBtn = new JButton("Sign Out");
            logoutBtn.setFont(new Font("Segoe UI",Font.PLAIN,12)); logoutBtn.setForeground(new Color(0xFC,0xCA,0xCA));
            logoutBtn.setBackground(new Color(0xFF,0x00,0x00,40)); logoutBtn.setOpaque(true);
            logoutBtn.setBorder(new CompoundBorder(new LineBorder(new Color(0xFF,0x80,0x80,80),1,true), new EmptyBorder(4,12,4,12)));
            logoutBtn.setFocusPainted(false); logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            logoutBtn.addActionListener(e -> { SessionManager.getInstance().logout(); showLogin(frame); });
            right.add(avatar); right.add(userLbl); right.add(logoutBtn);
            bar.add(right, BorderLayout.EAST);
        }
        return bar;
    }

    // ── Dark sidebar nav ──────────────────────────────────────────────────────
    static JPanel sideNav(JFrame frame, String active) {
        JPanel nav = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(NAV_BG); g.fillRect(0,0,getWidth(),getHeight());
            }
        };
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setOpaque(false);
        nav.setPreferredSize(new Dimension(185,0));
        nav.setBorder(new EmptyBorder(12,10,12,10));

        // Nav items with emoji icons
        String[][] items = {
            {"⬛ Dashboard",  "Dashboard"},
            {"➖ Add Expense", "Add Expense"},
            {"➕ Add Income",  "Add Income"},
            {"📈 Investments", "Investments"},
            {"🔁 Recurring",   "Recurring"},
            {"🎯 Savings Goals","Savings Goals"},
            {"💰 Budget",      "Budget"},
            {"📅 History",     "History"},
            {"📊 Reports",     "Reports"},
        };
        // icon replacements using text symbols
        String[][] navItems = {
            {"\uD83D\uDCC8  Dashboard",   "Dashboard"},
            {"\u2796  Add Expense",        "Add Expense"},
            {"\u2795  Add Income",         "Add Income"},
            {"\uD83D\uDCC9  Investments",  "Investments"},
            {"\uD83D\uDD01  Recurring",    "Recurring"},
            {"\uD83C\uDFAF  Savings Goals","Savings Goals"},
            {"\uD83D\uDCB0  Budget",       "Budget"},
            {"\uD83D\uDCC5  History",      "History"},
            {"\uD83D\uDCCA  Reports",      "Reports"},
        };

        for (String[] item : navItems) {
            String displayLabel = item[0];
            String navKey       = item[1];
            boolean isActive    = navKey.equals(active);

            JButton btn = new JButton(displayLabel) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    if (isActive) {
                        g2.setColor(NAV_ACTIVE_BG); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                        g2.setColor(NAV_ACCENT); g2.setStroke(new BasicStroke(3f));
                        g2.drawLine(0,6,0,getHeight()-6);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setFont(new Font("Segoe UI", isActive ? Font.BOLD : Font.PLAIN, 12));
            btn.setForeground(isActive ? NAV_TEXT_ACT : NAV_TEXT);
            btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setContentAreaFilled(false); btn.setOpaque(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(new EmptyBorder(10,14,10,14));
            btn.addActionListener(e -> navigate(frame, navKey));
            nav.add(btn); nav.add(Box.createVerticalStrut(2));
        }
        nav.add(Box.createVerticalGlue());
        return nav;
    }

    static void navigate(JFrame frame, String label) {
        switch (label) {
            case "Dashboard"     -> showDashboard(frame);
            case "Add Expense"   -> showAddTransaction(frame, "EXPENSE");
            case "Add Income"    -> showAddTransaction(frame, "INCOME");
            case "Investments"   -> showInvestments(frame);
            case "Recurring"     -> showRecurring(frame);
            case "Savings Goals" -> showSavingsGoals(frame);
            case "Budget"        -> showBudget(frame);
            case "History"       -> showHistory(frame);
            case "Reports"       -> showReports(frame);
        }
    }

    static String currentMonth() { return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")); }
    static String capitalize(String s) { if(s==null||s.isEmpty()) return s; return s.charAt(0)+s.substring(1).toLowerCase(); }

    static JScrollPane scrollWrap(JComponent content) {
        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null); sp.getViewport().setBackground(BG);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    static void setContent(JFrame frame, JComponent content) {
        frame.getContentPane().removeAll(); frame.getContentPane().add(content); frame.revalidate(); frame.repaint();
    }

    static JLabel sectionHeading(String text) {
        JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI",Font.BOLD,20)); l.setForeground(DARK_TEXT); l.setAlignmentX(Component.LEFT_ALIGNMENT); return l;
    }
    static JLabel cardTitle(String text) {
        JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI",Font.BOLD,14)); l.setForeground(DARK_TEXT); l.setAlignmentX(Component.LEFT_ALIGNMENT); return l;
    }
    static void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI",Font.PLAIN,12)); table.setRowHeight(32);
        table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0,0));
        table.setSelectionBackground(new Color(0xE8,0xF5,0xED)); table.setSelectionForeground(DARK_TEXT);
        JTableHeader h = table.getTableHeader();
        h.setFont(new Font("Segoe UI",Font.BOLD,11)); h.setBackground(new Color(0xF8,0xF9,0xFC));
        h.setForeground(GRAY_TEXT); h.setBorder(BorderFactory.createMatteBorder(0,0,1,0,BORDER_COL));
        h.setPreferredSize(new Dimension(h.getPreferredSize().width,36));
    }

    // ══════════════════════════════════════════
    //  SCREEN: LOGIN
    // ══════════════════════════════════════════

    static void showLogin(JFrame frame) {
        frame.setTitle("FinTrack — Login"); frame.setSize(900,580); frame.setResizable(false);

        // Two-panel layout: left brand panel + right form
        JPanel root = new JPanel(new GridLayout(1,2)); root.setBackground(NAV_BG);

        // ── Left brand panel ──────────────────────────────────────────────────
        JPanel brand = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0,0,NAV_BG,getWidth(),getHeight(),new Color(0x0F,0x2A,0x1E));
                g2.setPaint(gp); g2.fillRect(0,0,getWidth(),getHeight());
                // Decorative circles
                g2.setColor(new Color(0x4A,0xDE,0x80,30)); g2.fillOval(-60,-60,240,240);
                g2.setColor(new Color(0x4A,0xDE,0x80,18)); g2.fillOval(getWidth()-120,getHeight()-120,200,200);
                g2.dispose();
            }
        };
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(60,50,60,50));

        JLabel dot = new JLabel("●"); dot.setFont(new Font("Segoe UI",Font.BOLD,32)); dot.setForeground(NAV_ACCENT); dot.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel brandTitle = new JLabel("FinTrack"); brandTitle.setFont(new Font("Segoe UI",Font.BOLD,38)); brandTitle.setForeground(WHITE); brandTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel brandSub = new JLabel("<html><div style='width:200px;color:#9ab'>Your personal finance companion.<br>Track, budget & grow your wealth.</div></html>");
        brandSub.setFont(new Font("Segoe UI",Font.PLAIN,14)); brandSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Feature bullets
        String[] features = {"Track income & expenses","Smart budget alerts","Investment portfolio","Savings goals tracker"};
        brand.add(dot); brand.add(Box.createVerticalStrut(8)); brand.add(brandTitle); brand.add(Box.createVerticalStrut(16)); brand.add(brandSub); brand.add(Box.createVerticalStrut(40));
        for (String f : features) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel check = new JLabel("✓"); check.setFont(new Font("Segoe UI",Font.BOLD,13)); check.setForeground(NAV_ACCENT);
            JLabel feat = new JLabel(f); feat.setFont(new Font("Segoe UI",Font.PLAIN,13)); feat.setForeground(new Color(0xC0,0xCC,0xD8));
            row.add(check); row.add(feat); brand.add(row); brand.add(Box.createVerticalStrut(10));
        }

        // ── Right form panel ──────────────────────────────────────────────────
        JPanel formSide = new JPanel(new GridBagLayout()); formSide.setBackground(new Color(0xF8,0xF9,0xFC));
        JPanel inner = new JPanel(); inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false); inner.setMaximumSize(new Dimension(360,9999));

        JLabel formTitle = new JLabel("Welcome back"); formTitle.setFont(new Font("Segoe UI",Font.BOLD,26)); formTitle.setForeground(DARK_TEXT); formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel formSub = new JLabel("Sign in to your account"); formSub.setFont(new Font("Segoe UI",Font.PLAIN,13)); formSub.setForeground(GRAY_TEXT); formSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField emailF = styledField("Email"); emailF.setAlignmentX(Component.LEFT_ALIGNMENT); emailF.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
        JPasswordField passF = styledPass("Password"); passF.setAlignmentX(Component.LEFT_ALIGNMENT); passF.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
        JButton loginBtn = primaryBtn("Sign In →", GREEN); loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT); loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        JLabel errLbl = new JLabel(" "); errLbl.setFont(new Font("Segoe UI",Font.PLAIN,12)); errLbl.setForeground(RED_COL); errLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel divRow = new JPanel(new FlowLayout(FlowLayout.CENTER)); divRow.setOpaque(false); divRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel orLbl = new JLabel("— New here? —"); orLbl.setFont(new Font("Segoe UI",Font.PLAIN,12)); orLbl.setForeground(SUBTEXT); divRow.add(orLbl);

        JButton regBtn = outlineBtn("Create an account", GREEN); regBtn.setAlignmentX(Component.LEFT_ALIGNMENT); regBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));

        inner.add(formTitle); inner.add(Box.createVerticalStrut(4)); inner.add(formSub); inner.add(Box.createVerticalStrut(28));
        inner.add(fLabel("Email address")); inner.add(Box.createVerticalStrut(5)); inner.add(emailF); inner.add(Box.createVerticalStrut(14));
        inner.add(fLabel("Password")); inner.add(Box.createVerticalStrut(5)); inner.add(passF); inner.add(Box.createVerticalStrut(20));
        inner.add(loginBtn); inner.add(Box.createVerticalStrut(8)); inner.add(errLbl); inner.add(Box.createVerticalStrut(20));
        inner.add(divRow); inner.add(Box.createVerticalStrut(12)); inner.add(regBtn);

        loginBtn.addActionListener(e -> {
            User u = DatabaseHelper.getInstance().loginUser(emailF.getText().trim(), new String(passF.getPassword()));
            if (u!=null) { SessionManager.getInstance().login(u); showDashboard(frame); } else errLbl.setText("Invalid email or password.");
        });
        regBtn.addActionListener(e -> showRegister(frame));
        formSide.add(inner);

        root.add(brand); root.add(formSide);
        setContent(frame, root); frame.setLocationRelativeTo(null); frame.setVisible(true);
    }

    // ══════════════════════════════════════════
    //  SCREEN: REGISTER
    // ══════════════════════════════════════════

    static void showRegister(JFrame frame) {
        JPanel root = new JPanel(new GridBagLayout()); root.setBackground(new Color(0xF8,0xF9,0xFC));
        JPanel inner = new JPanel(); inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS)); inner.setOpaque(false); inner.setMaximumSize(new Dimension(380,9999));

        JLabel formTitle = new JLabel("Create your account"); formTitle.setFont(new Font("Segoe UI",Font.BOLD,26)); formTitle.setForeground(DARK_TEXT); formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel formSub = new JLabel("Free forever. No credit card needed."); formSub.setFont(new Font("Segoe UI",Font.PLAIN,13)); formSub.setForeground(GRAY_TEXT); formSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField nameF = styledField("Full Name"); nameF.setMaximumSize(new Dimension(Integer.MAX_VALUE,42)); nameF.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField emailF = styledField("Email"); emailF.setMaximumSize(new Dimension(Integer.MAX_VALUE,42)); emailF.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPasswordField passF = styledPass("Password (min 4 chars)"); passF.setMaximumSize(new Dimension(Integer.MAX_VALUE,42)); passF.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton regBtn = primaryBtn("Create Account →", GREEN); regBtn.setAlignmentX(Component.LEFT_ALIGNMENT); regBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        JLabel msg = new JLabel(" "); msg.setFont(new Font("Segoe UI",Font.PLAIN,12)); msg.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton backBtn = outlineBtn("← Back to Login", GREEN); backBtn.setAlignmentX(Component.LEFT_ALIGNMENT); backBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));

        inner.add(formTitle); inner.add(Box.createVerticalStrut(4)); inner.add(formSub); inner.add(Box.createVerticalStrut(28));
        inner.add(fLabel("Full Name")); inner.add(Box.createVerticalStrut(5)); inner.add(nameF); inner.add(Box.createVerticalStrut(14));
        inner.add(fLabel("Email address")); inner.add(Box.createVerticalStrut(5)); inner.add(emailF); inner.add(Box.createVerticalStrut(14));
        inner.add(fLabel("Password")); inner.add(Box.createVerticalStrut(5)); inner.add(passF); inner.add(Box.createVerticalStrut(20));
        inner.add(regBtn); inner.add(Box.createVerticalStrut(8)); inner.add(msg); inner.add(Box.createVerticalStrut(20)); inner.add(backBtn);

        regBtn.addActionListener(e -> {
            String name=nameF.getText().trim(), email=emailF.getText().trim(), pass=new String(passF.getPassword());
            if (name.isEmpty()||email.isEmpty()||pass.length()<4) { msg.setForeground(RED_COL); msg.setText("Fill all fields. Password min 4 chars."); return; }
            if (DatabaseHelper.getInstance().registerUser(new User(name,email,pass))) { msg.setForeground(GREEN); msg.setText("✓ Account created! Please login."); }
            else { msg.setForeground(RED_COL); msg.setText("Email already exists."); }
        });
        backBtn.addActionListener(e -> showLogin(frame));
        root.add(inner); setContent(frame, root);
    }

    // ══════════════════════════════════════════
    //  SCREEN: DASHBOARD
    // ══════════════════════════════════════════

    static void showDashboard(JFrame frame) {
        frame.setTitle("FinTrack — Dashboard"); frame.setSize(980,720); frame.setResizable(true);
        DatabaseHelper db = DatabaseHelper.getInstance();
        int uid = SessionManager.getInstance().getCurrentUserId();
        String month = currentMonth();

        JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG);
        root.add(topBar(frame,"FinTrack",true), BorderLayout.NORTH);
        root.add(sideNav(frame,"Dashboard"), BorderLayout.WEST);

        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG); content.setBorder(new EmptyBorder(24,24,24,24));

        // Stat cards
        double expense = db.getTotalByTypeAndMonth(uid,"EXPENSE",month);
        double income  = db.getTotalByTypeAndMonth(uid,"INCOME",month);
        double balance = income - expense;
        List<Investment> allInv = db.getInvestmentsByUser(uid);
        double totalCurrent  = allInv.stream().filter(i->"ACTIVE".equals(i.getStatus())).mapToDouble(Investment::getCurrentValue).sum();
        double totalInvested = allInv.stream().filter(i->"ACTIVE".equals(i.getStatus())).mapToDouble(Investment::getInvestedAmount).sum();
        double invPL = totalCurrent - totalInvested;

        JPanel stats = new JPanel(new GridLayout(1,4,12,0)); stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE,90)); stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.add(statCard("This Month Spent", "\u20b9"+String.format("%.0f",expense), "#A32D2D"));
        stats.add(statCard("Income",           "\u20b9"+String.format("%.0f",income),  "#0F6E56"));
        stats.add(statCard("Balance",          "\u20b9"+String.format("%.0f",balance), balance>=0?"#0F6E56":"#A32D2D"));
        stats.add(statCard("Portfolio P/L",    (invPL>=0?"+":"")+"\u20b9"+String.format("%.0f",invPL), invPL>=0?"#0F6E56":"#A32D2D"));
        content.add(stats); content.add(Box.createVerticalStrut(16));

        // Bar chart
        JPanel chartBox = new JPanel(new BorderLayout()); chartBox.setBackground(CARD_BG);
        chartBox.setBorder(new CompoundBorder(new LineBorder(BORDER_COL,1,true), new EmptyBorder(8,8,8,8)));
        chartBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,280)); chartBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartBox.add(new BarChartPanel(db,uid)); content.add(chartBox); content.add(Box.createVerticalStrut(16));

        // ── Recurring due alert ───────────────────────────────────────────────
        List<RecurringTransaction> due = db.getRecurringByUser(uid).stream().filter(RecurringTransaction::isDueThisMonth).toList();
        if (!due.isEmpty()) {
            JPanel recBox = new JPanel(); recBox.setLayout(new BoxLayout(recBox, BoxLayout.Y_AXIS));
            recBox.setBackground(new Color(0xFF,0xF8,0xEC));
            recBox.setBorder(new CompoundBorder(new LineBorder(new Color(0xEF,0x9F,0x27),1,true), new EmptyBorder(14,16,14,16)));
            recBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel recTitle = new JLabel("\u23F0  "+due.size()+" recurring transaction(s) due this month");
            recTitle.setFont(new Font("Segoe UI",Font.BOLD,13)); recTitle.setForeground(new Color(0x7A,0x4A,0x00)); recTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            recBox.add(recTitle); recBox.add(Box.createVerticalStrut(8));
            for (RecurringTransaction r : due) {
                JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT);
                String sign = "INCOME".equals(r.getType()) ? "+" : "-";
                JLabel lbl = new JLabel(r.getDescription()+"  |  "+sign+"\u20b9"+String.format("%.0f",r.getAmount())+"  ("+r.getFrequency()+")");
                lbl.setFont(new Font("Segoe UI",Font.PLAIN,12)); lbl.setForeground(new Color(0x55,0x55,0x55));
                JButton applyBtn = smallBtn("Apply Now", new Color(0xEF,0x9F,0x27), WHITE);
                applyBtn.addActionListener(e -> { db.applyRecurring(r); showDashboard(frame); });
                row.add(lbl, BorderLayout.WEST); row.add(applyBtn, BorderLayout.EAST);
                recBox.add(row); recBox.add(Box.createVerticalStrut(4));
            }
            content.add(recBox); content.add(Box.createVerticalStrut(16));
        }

        // Savings Goals summary
        List<SavingsGoal> activeGoals = db.getSavingsGoalsByUser(uid).stream().filter(g->"ACTIVE".equals(g.getStatus())).toList();
        if (!activeGoals.isEmpty()) {
            JPanel goalBox = new JPanel(); goalBox.setLayout(new BoxLayout(goalBox, BoxLayout.Y_AXIS));
            goalBox.setBackground(CARD_BG);
            goalBox.setBorder(new CompoundBorder(new LineBorder(BORDER_COL,1,true), new EmptyBorder(14,16,14,16)));
            goalBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel gt = new JLabel("Savings Goals"); gt.setFont(new Font("Segoe UI",Font.BOLD,14)); gt.setForeground(DARK_TEXT); gt.setAlignmentX(Component.LEFT_ALIGNMENT);
            goalBox.add(gt); goalBox.add(Box.createVerticalStrut(8));
            for (SavingsGoal g : activeGoals.subList(0, Math.min(3,activeGoals.size()))) {
                goalBox.add(buildGoalRow(g)); goalBox.add(Box.createVerticalStrut(8));
            }
            if (activeGoals.size() > 3) {
                JLabel more = new JLabel("+ "+(activeGoals.size()-3)+" more — see Savings Goals tab");
                more.setFont(new Font("Segoe UI",Font.PLAIN,11)); more.setForeground(GRAY_TEXT); more.setAlignmentX(Component.LEFT_ALIGNMENT);
                goalBox.add(more);
            }
            content.add(goalBox); content.add(Box.createVerticalStrut(16));
        }

        // Budget alerts
        JPanel budgetBox = new JPanel(); budgetBox.setLayout(new BoxLayout(budgetBox, BoxLayout.Y_AXIS));
        budgetBox.setBackground(CARD_BG);
        budgetBox.setBorder(new CompoundBorder(new LineBorder(BORDER_COL,1,true), new EmptyBorder(16,16,16,16)));
        budgetBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel bTitle = new JLabel("Budget Alerts — "+month);
        bTitle.setFont(new Font("Segoe UI",Font.BOLD,14)); bTitle.setForeground(DARK_TEXT); bTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        budgetBox.add(bTitle); budgetBox.add(Box.createVerticalStrut(8));
        List<Budget> budgets = db.getBudgetsByMonth(uid,month);
        if (budgets.isEmpty()) {
            JLabel noB = new JLabel("No budgets set. Go to Budget tab to add limits."); noB.setForeground(new Color(0x66,0x66,0x66));
            noB.setFont(new Font("Segoe UI",Font.PLAIN,13)); noB.setAlignmentX(Component.LEFT_ALIGNMENT); budgetBox.add(noB);
        } else { for (Budget b : budgets) { budgetBox.add(buildBudgetRow(b)); budgetBox.add(Box.createVerticalStrut(6)); } }
        content.add(budgetBox); content.add(Box.createVerticalStrut(16));
        content.add(buildTxTable(frame,db,uid,true));
        root.add(scrollWrap(content), BorderLayout.CENTER);
        setContent(frame, root); frame.setLocationRelativeTo(null);
    }

    // ── Bar chart ────────────────────────────────────────────────────────────
    static class BarChartPanel extends JPanel {
        private final double[] values = new double[6];
        private final String[] labels = new String[6];
        BarChartPanel(DatabaseHelper db, int uid) {
            setBackground(CARD_BG); setPreferredSize(new Dimension(0,260));
            YearMonth now = YearMonth.now();
            for (int i=5; i>=0; i--) {
                YearMonth ym = now.minusMonths(i);
                values[5-i] = db.getTotalByTypeAndMonth(uid,"EXPENSE",ym.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                labels[5-i] = ym.format(DateTimeFormatter.ofPattern("MMM yy"));
            }
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight(), padL=60, padR=24, padT=44, padB=44;
            int chartW=w-padL-padR, chartH=h-padT-padB;

            // Title
            g2.setFont(new Font("Segoe UI",Font.BOLD,13)); g2.setColor(DARK_TEXT);
            g2.drawString("Monthly Expenses — Last 6 Months", padL, 26);

            double maxVal=0; for (double v:values) if(v>maxVal) maxVal=v; if(maxVal==0) maxVal=1;

            // Horizontal grid lines
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0));
            for (int i=1; i<=4; i++) {
                int gy = padT + chartH - (int)(chartH * i / 4.0);
                g2.setColor(new Color(0xE8,0xEC,0xF2)); g2.drawLine(padL, gy, padL+chartW, gy);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,9)); g2.setColor(SUBTEXT);
                String lv = "\u20b9"+(int)(maxVal*i/4); g2.drawString(lv, 4, gy+4);
            }
            g2.setStroke(new BasicStroke(1f));

            int barW = chartW / values.length; int gap = barW/5;
            for (int i=0; i<values.length; i++) {
                int barH = Math.max(4,(int)(values[i]/maxVal*chartH));
                int x=padL+i*barW+gap, y=padT+chartH-barH, bw=barW-gap*2;

                // Drop shadow
                g2.setColor(new Color(0,0,0,18)); g2.fillRoundRect(x+2,y+3,bw,barH,10,10);

                // Gradient bar
                GradientPaint gp = new GradientPaint(x,y,new Color(0x10,0xB9,0x81),x,y+barH,new Color(0x05,0x7A,0x55));
                g2.setPaint(gp); g2.fillRoundRect(x,y,bw,barH,10,10);

                // Amount label on bar
                if (values[i]>0) {
                    String amt="\u20b9"+(int)values[i];
                    g2.setFont(new Font("Segoe UI",Font.BOLD,9)); FontMetrics fm=g2.getFontMetrics();
                    g2.setColor(WHITE); g2.drawString(amt, x+bw/2-fm.stringWidth(amt)/2, y+14);
                }

                // X-axis label
                g2.setFont(new Font("Segoe UI",Font.PLAIN,10)); g2.setColor(GRAY_TEXT);
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(labels[i], x+bw/2-fm.stringWidth(labels[i])/2, padT+chartH+16);
            }
        }
    }

    // ── Budget row ───────────────────────────────────────────────────────────
    static JPanel buildBudgetRow(Budget b) {
        JPanel row = new JPanel(); row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS)); row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT);
        Color barColor = b.isExceeded() ? RED_COL : b.isWarning() ? AMBER : GREEN;
        JPanel topRow = new JPanel(new BorderLayout()); topRow.setOpaque(false); topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel nm = new JLabel(b.getCategoryName()); nm.setFont(new Font("Segoe UI",Font.BOLD,13)); nm.setForeground(DARK_TEXT);
        JLabel pct = new JLabel(String.format("%.0f%%",b.getUsagePercent())); pct.setFont(new Font("Segoe UI",Font.BOLD,13)); pct.setForeground(barColor);
        topRow.add(nm, BorderLayout.WEST); topRow.add(pct, BorderLayout.EAST);
        JProgressBar pb = new JProgressBar(0,100); pb.setValue((int)Math.min(b.getUsagePercent(),100));
        pb.setForeground(barColor); pb.setBackground(new Color(0xED,0xF0,0xF7)); pb.setBorderPainted(false);
        pb.setMaximumSize(new Dimension(Integer.MAX_VALUE,8)); pb.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel det = new JLabel(String.format("Spent \u20b9%.0f  of  \u20b9%.0f limit  —  \u20b9%.0f remaining",
            b.getSpentAmount(),b.getLimitAmount(),Math.max(0,b.getLimitAmount()-b.getSpentAmount())));
        det.setFont(new Font("Segoe UI",Font.PLAIN,11)); det.setForeground(SUBTEXT); det.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(topRow); row.add(Box.createVerticalStrut(6)); row.add(pb); row.add(Box.createVerticalStrut(4)); row.add(det); return row;
    }

    // ── Savings Goal progress row ────────────────────────────────────────────
    static JPanel buildGoalRow(SavingsGoal g) {
        JPanel row = new JPanel(); row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS)); row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT);
        double pct = g.getProgressPercent();
        Color barColor = g.isAchieved() ? GREEN : pct>=75 ? GREEN_DARK : PURPLE;
        JPanel topRow = new JPanel(new BorderLayout()); topRow.setOpaque(false); topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel nm = new JLabel(g.getName()+(g.isAchieved()?"  \u2713":"")); nm.setFont(new Font("Segoe UI",Font.BOLD,13)); nm.setForeground(g.isAchieved()?GREEN:DARK_TEXT);
        JLabel pctLbl = new JLabel(String.format("%.0f%%",pct)); pctLbl.setFont(new Font("Segoe UI",Font.BOLD,13)); pctLbl.setForeground(barColor);
        topRow.add(nm, BorderLayout.WEST); topRow.add(pctLbl, BorderLayout.EAST);
        JProgressBar pb = new JProgressBar(0,100); pb.setValue((int)pct); pb.setForeground(barColor);
        pb.setBackground(new Color(0xED,0xF0,0xF7)); pb.setBorderPainted(false);
        pb.setMaximumSize(new Dimension(Integer.MAX_VALUE,8)); pb.setAlignmentX(Component.LEFT_ALIGNMENT);
        String dlStr = g.getDeadline()!=null ? "  ·  Deadline: "+g.getDeadline() : "";
        JLabel det = new JLabel(String.format("Saved \u20b9%.0f  of  \u20b9%.0f  —  \u20b9%.0f to go%s",g.getSavedAmount(),g.getTargetAmount(),g.getRemaining(),dlStr));
        det.setFont(new Font("Segoe UI",Font.PLAIN,11)); det.setForeground(SUBTEXT); det.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(topRow); row.add(Box.createVerticalStrut(6)); row.add(pb); row.add(Box.createVerticalStrut(4)); row.add(det); return row;
    }

    // ── Transaction table ────────────────────────────────────────────────────
    static JPanel buildTxTable(JFrame frame, DatabaseHelper db, int uid, boolean limited) {
        JPanel section = new JPanel(); section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false); section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel wrapper = card(16); wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS)); wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel title = cardTitle(limited ? "Recent Transactions" : "All Transactions");
        wrapper.add(title); wrapper.add(Box.createVerticalStrut(12));

        List<Transaction> all = db.getTransactionsByUser(uid);
        List<Transaction> shown = limited ? all.subList(0, Math.min(8,all.size())) : all;
        String[] cols = {"Type","Description","Amount","Category","Date","Action"};
        Object[][] data = new Object[shown.size()][6];
        for (int i=0; i<shown.size(); i++) {
            Transaction t=shown.get(i);
            data[i][0]=t.getType(); data[i][1]=t.getDescription(); data[i][2]=t.getDisplayAmount();
            data[i][3]=db.getCategoryName(t.getCategoryId()); data[i][4]=t.getDate().toString(); data[i][5]=t.getId();
        }
        DefaultTableModel model = new DefaultTableModel(data, cols) { @Override public boolean isCellEditable(int r, int c) { return c==5; } };
        JTable table = new JTable(model) {
            @Override public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground("INCOME".equals(getModel().getValueAt(row,0)) ? INC_ROW : EXP_ROW);
                    if (col==0) {
                        boolean inc = "INCOME".equals(getModel().getValueAt(row,0));
                        ((JLabel)c).setForeground(inc ? GREEN_DARK : RED_COL);
                        ((JLabel)c).setFont(new Font("Segoe UI",Font.BOLD,11));
                    }
                }
                return c;
            }
        };
        styleTable(table);
        table.getColumn("Action").setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) { return smallBtn("Delete",RED_LIGHT,RED_COL); }
        });
        table.getColumn("Action").setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            JButton btn=smallBtn("Delete",RED_LIGHT,RED_COL); int editRow;
            { btn.addActionListener(e->{ fireEditingStopped(); db.deleteTransaction((int)model.getValueAt(editRow,5)); showDashboard(frame); }); }
            @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) { editRow=r; return btn; }
            @Override public Object getCellEditorValue() { return null; }
        });
        JScrollPane sp = new JScrollPane(table); sp.setPreferredSize(new Dimension(0, limited?210:320));
        sp.setAlignmentX(Component.LEFT_ALIGNMENT); sp.setBorder(BorderFactory.createMatteBorder(1,0,0,0,BORDER_COL));
        wrapper.add(sp); section.add(wrapper); return section;
    }

    // ══════════════════════════════════════════
    //  SCREEN: ADD TRANSACTION
    //  Income → only Pocket Money / Salary / Others
    // ══════════════════════════════════════════

    static void showAddTransaction(JFrame frame, String type) {
        frame.setTitle("FinTrack — Add "+capitalize(type));
        DatabaseHelper db = DatabaseHelper.getInstance();
        SessionManager session = SessionManager.getInstance();

        JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG);
        root.add(topBar(frame,"FinTrack",true), BorderLayout.NORTH);
        root.add(sideNav(frame,"Add "+capitalize(type)), BorderLayout.WEST);

        JPanel formOuter = new JPanel(); formOuter.setLayout(new BoxLayout(formOuter, BoxLayout.Y_AXIS));
        formOuter.setBackground(BG); formOuter.setBorder(new EmptyBorder(28,28,28,28));

        Color hcolor = "EXPENSE".equals(type) ? RED_COL : new Color(0x0F,0x6E,0x56);
        JLabel heading = new JLabel("Add "+capitalize(type)); heading.setFont(new Font("Segoe UI",Font.BOLD,20)); heading.setForeground(hcolor); heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel cardP = card(24); cardP.setLayout(new BoxLayout(cardP, BoxLayout.Y_AXIS));
        cardP.setAlignmentX(Component.LEFT_ALIGNMENT); cardP.setMaximumSize(new Dimension(500,9999));

        JTextField amtF = styledField("Amount (\u20b9)"); amtF.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); amtF.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField descF = styledField("Description"); descF.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); descF.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Income gets only 3 income categories; Expense gets expense categories
        List<Category> cats = "INCOME".equals(type) ? db.getIncomeCategories() : db.getExpenseCategories();
        JComboBox<Category> catBox = new JComboBox<>(cats.toArray(new Category[0]));
        catBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); catBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        SpinnerDateModel dateModel = new SpinnerDateModel();
        JSpinner dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner,"yyyy-MM-dd"));
        dateSpinner.setValue(new java.util.Date());
        dateSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); dateSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField noteF = styledField("Note (optional)"); noteF.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); noteF.setAlignmentX(Component.LEFT_ALIGNMENT);

        Color btnColor = "EXPENSE".equals(type) ? new Color(0xE2,0x4B,0x4A) : GREEN;
        JButton saveBtn = primaryBtn("Save "+capitalize(type), btnColor);
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT); saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
        JLabel msg = new JLabel(" "); msg.setFont(new Font("Segoe UI",Font.PLAIN,12)); msg.setAlignmentX(Component.LEFT_ALIGNMENT);

        saveBtn.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(amtF.getText().trim());
                String desc = descF.getText().trim();
                Category cat = (Category) catBox.getSelectedItem();
                LocalDate date = ((java.util.Date)dateSpinner.getValue()).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                if (desc.isEmpty()||cat==null) { msg.setForeground(RED_COL); msg.setText("Fill all fields."); return; }
                Transaction t = "INCOME".equals(type)
                    ? new Income(amount,desc,date,session.getCurrentUserId(),cat.getId(),noteF.getText())
                    : new Expense(amount,desc,date,session.getCurrentUserId(),cat.getId(),noteF.getText());
                if (db.addTransaction(t)) { msg.setForeground(GREEN); msg.setText("Saved successfully!"); amtF.setText(""); descF.setText(""); noteF.setText(""); dateSpinner.setValue(new java.util.Date()); }
                else { msg.setForeground(RED_COL); msg.setText("Failed to save."); }
            } catch (NumberFormatException ex) { msg.setForeground(RED_COL); msg.setText("Enter a valid amount."); }
        });

        if ("INCOME".equals(type)) {
            JLabel hint = new JLabel("Income Category: Pocket Money | Salary | Others");
            hint.setFont(new Font("Segoe UI",Font.ITALIC,11)); hint.setForeground(new Color(0x0F,0x6E,0x56)); hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            cardP.add(hint); cardP.add(Box.createVerticalStrut(8));
        }

        cardP.add(fLabel("Amount (\u20b9)")); cardP.add(Box.createVerticalStrut(4)); cardP.add(amtF); cardP.add(Box.createVerticalStrut(8));
        cardP.add(fLabel("Description")); cardP.add(Box.createVerticalStrut(4)); cardP.add(descF); cardP.add(Box.createVerticalStrut(8));
        cardP.add(fLabel("Category")); cardP.add(Box.createVerticalStrut(4)); cardP.add(catBox); cardP.add(Box.createVerticalStrut(8));
        cardP.add(fLabel("Date")); cardP.add(Box.createVerticalStrut(4)); cardP.add(dateSpinner); cardP.add(Box.createVerticalStrut(8));
        cardP.add(fLabel("Note (optional)")); cardP.add(Box.createVerticalStrut(4)); cardP.add(noteF); cardP.add(Box.createVerticalStrut(10));
        cardP.add(saveBtn); cardP.add(Box.createVerticalStrut(6)); cardP.add(msg);
        formOuter.add(heading); formOuter.add(Box.createVerticalStrut(12)); formOuter.add(cardP);
        root.add(scrollWrap(formOuter), BorderLayout.CENTER);
        setContent(frame, root);
    }

    // ══════════════════════════════════════════
    //  SCREEN: RECURRING TRANSACTIONS  (NEW)
    // ══════════════════════════════════════════

    static void showRecurring(JFrame frame) {
        frame.setTitle("FinTrack — Recurring Transactions");
        DatabaseHelper db = DatabaseHelper.getInstance();
        int uid = SessionManager.getInstance().getCurrentUserId();

        JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG);
        root.add(topBar(frame,"FinTrack",true), BorderLayout.NORTH);
        root.add(sideNav(frame,"Recurring"), BorderLayout.WEST);

        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG); content.setBorder(new EmptyBorder(28,28,28,28));

        JLabel heading = new JLabel("Recurring Transactions"); heading.setFont(new Font("Segoe UI",Font.BOLD,20)); heading.setForeground(DARK_TEXT); heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel("Set up monthly bills, salary, subscriptions etc. Apply them this month with one click.");
        sub.setFont(new Font("Segoe UI",Font.PLAIN,12)); sub.setForeground(GRAY_TEXT); sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(heading); content.add(Box.createVerticalStrut(4)); content.add(sub); content.add(Box.createVerticalStrut(20));

        // ── Add new recurring form ────────────────────────────────────────────
        JPanel addCard = card(20); addCard.setLayout(new BoxLayout(addCard, BoxLayout.Y_AXIS));
        addCard.setAlignmentX(Component.LEFT_ALIGNMENT); addCard.setMaximumSize(new Dimension(560,9999));
        JLabel addTitle = new JLabel("Add New Recurring Template"); addTitle.setFont(new Font("Segoe UI",Font.BOLD,14)); addTitle.setForeground(DARK_TEXT); addTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComboBox<String> typeBox = new JComboBox<>(new String[]{"EXPENSE","INCOME"});
        typeBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); typeBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField amtF = styledField("Amount (\u20b9)"); amtF.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); amtF.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField descF = styledField("Description (e.g. Netflix Subscription)"); descF.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); descF.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Category box updates dynamically based on type
        JComboBox<Category> catBox = new JComboBox<>(db.getExpenseCategories().toArray(new Category[0]));
        catBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); catBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        typeBox.addActionListener(ev -> {
            catBox.removeAllItems();
            List<Category> newCats = "INCOME".equals(typeBox.getSelectedItem()) ? db.getIncomeCategories() : db.getExpenseCategories();
            newCats.forEach(catBox::addItem);
        });

        JComboBox<String> freqBox = new JComboBox<>(new String[]{"Monthly","Weekly","Bi-weekly"});
        freqBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); freqBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField noteF = styledField("Note (optional)"); noteF.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); noteF.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addBtn = primaryBtn("Add Recurring Template", ORANGE);
        addBtn.setAlignmentX(Component.LEFT_ALIGNMENT); addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        JLabel addMsg = new JLabel(" "); addMsg.setFont(new Font("Segoe UI",Font.PLAIN,12)); addMsg.setAlignmentX(Component.LEFT_ALIGNMENT);

        addBtn.addActionListener(e -> {
            try {
                String type = (String) typeBox.getSelectedItem();
                double amount = Double.parseDouble(amtF.getText().trim());
                String desc = descF.getText().trim();
                Category cat = (Category) catBox.getSelectedItem();
                String freq = (String) freqBox.getSelectedItem();
                if (desc.isEmpty()||cat==null) { addMsg.setForeground(RED_COL); addMsg.setText("Fill all fields."); return; }
                RecurringTransaction r = new RecurringTransaction(uid,type,amount,desc,cat.getId(),noteF.getText().trim(),freq,LocalDate.now());
                if (db.addRecurring(r)) { addMsg.setForeground(GREEN); addMsg.setText("Template saved!"); amtF.setText(""); descF.setText(""); noteF.setText(""); showRecurring(frame); }
                else { addMsg.setForeground(RED_COL); addMsg.setText("Failed."); }
            } catch (NumberFormatException ex) { addMsg.setForeground(RED_COL); addMsg.setText("Enter a valid amount."); }
        });

        addCard.add(addTitle); addCard.add(Box.createVerticalStrut(12));
        addCard.add(fLabel("Type")); addCard.add(Box.createVerticalStrut(4)); addCard.add(typeBox); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Amount (\u20b9)")); addCard.add(Box.createVerticalStrut(4)); addCard.add(amtF); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Description")); addCard.add(Box.createVerticalStrut(4)); addCard.add(descF); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Category")); addCard.add(Box.createVerticalStrut(4)); addCard.add(catBox); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Frequency")); addCard.add(Box.createVerticalStrut(4)); addCard.add(freqBox); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Note (optional)")); addCard.add(Box.createVerticalStrut(4)); addCard.add(noteF); addCard.add(Box.createVerticalStrut(10));
        addCard.add(addBtn); addCard.add(Box.createVerticalStrut(6)); addCard.add(addMsg);
        content.add(addCard); content.add(Box.createVerticalStrut(20));

        // ── Saved templates table ─────────────────────────────────────────────
        JPanel listCard = card(16); listCard.setLayout(new BoxLayout(listCard, BoxLayout.Y_AXIS)); listCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel listTitle = new JLabel("Saved Templates"); listTitle.setFont(new Font("Segoe UI",Font.BOLD,14)); listTitle.setForeground(DARK_TEXT); listTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        listCard.add(listTitle); listCard.add(Box.createVerticalStrut(10));

        List<RecurringTransaction> recs = db.getRecurringByUser(uid);
        if (recs.isEmpty()) {
            JLabel none = new JLabel("No templates yet. Add one above."); none.setForeground(GRAY_TEXT); none.setFont(new Font("Segoe UI",Font.PLAIN,12)); none.setAlignmentX(Component.LEFT_ALIGNMENT); listCard.add(none);
        } else {
            String[] cols = {"Type","Description","Amount","Category","Frequency","Status","Apply","Delete"};
            Object[][] data = new Object[recs.size()][8];
            for (int i=0; i<recs.size(); i++) {
                RecurringTransaction r=recs.get(i);
                data[i][0]=r.getType(); data[i][1]=r.getDescription(); data[i][2]="\u20b9"+String.format("%.2f",r.getAmount());
                data[i][3]=db.getCategoryName(r.getCategoryId()); data[i][4]=r.getFrequency();
                data[i][5]=r.isDueThisMonth()?"DUE \u26a0":"Applied \u2713"; data[i][6]=r.getId(); data[i][7]=r.getId();
            }
            DefaultTableModel model = new DefaultTableModel(data, cols) { @Override public boolean isCellEditable(int r, int c) { return c>=6; } };
            JTable table = new JTable(model) {
                @Override public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                    Component c = super.prepareRenderer(renderer, row, col);
                    if (!isRowSelected(row)) { String status=(String)getModel().getValueAt(row,5); c.setBackground(status.startsWith("DUE")?new Color(0xFF,0xF8,0xEC):WHITE); }
                    return c;
                }
            };
            table.setFont(new Font("Segoe UI",Font.PLAIN,12)); table.setRowHeight(28);
            table.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12)); table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0,0));
            // Apply button
            table.getColumn("Apply").setCellRenderer(new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) { return smallBtn("Apply",new Color(0xFF,0xF3,0xE0),ORANGE); }
            });
            table.getColumn("Apply").setCellEditor(new DefaultCellEditor(new JCheckBox()) {
                JButton btn=smallBtn("Apply",new Color(0xFF,0xF3,0xE0),ORANGE); int editRow;
                { btn.addActionListener(e->{ fireEditingStopped(); int id=(int)model.getValueAt(editRow,6);
                    recs.stream().filter(x->x.getId()==id).findFirst().ifPresent(r->{ db.applyRecurring(r); showRecurring(frame); }); }); }
                @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) { editRow=r; return btn; }
                @Override public Object getCellEditorValue() { return null; }
            });
            // Delete button
            table.getColumn("Delete").setCellRenderer(new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) { return smallBtn("Delete",RED_LIGHT,RED_COL); }
            });
            table.getColumn("Delete").setCellEditor(new DefaultCellEditor(new JCheckBox()) {
                JButton btn=smallBtn("Delete",RED_LIGHT,RED_COL); int editRow;
                { btn.addActionListener(e->{ fireEditingStopped(); db.deleteRecurring((int)model.getValueAt(editRow,7)); showRecurring(frame); }); }
                @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) { editRow=r; return btn; }
                @Override public Object getCellEditorValue() { return null; }
            });
            JScrollPane sp = new JScrollPane(table); sp.setPreferredSize(new Dimension(0, Math.min(recs.size()*30+30,260))); sp.setAlignmentX(Component.LEFT_ALIGNMENT);
            listCard.add(sp);
        }
        content.add(listCard);
        root.add(scrollWrap(content), BorderLayout.CENTER);
        setContent(frame, root);
    }

    // ══════════════════════════════════════════
    //  SCREEN: SAVINGS GOALS  (NEW)
    // ══════════════════════════════════════════

    static void showSavingsGoals(JFrame frame) {
        frame.setTitle("FinTrack — Savings Goals");
        DatabaseHelper db = DatabaseHelper.getInstance();
        int uid = SessionManager.getInstance().getCurrentUserId();

        JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG);
        root.add(topBar(frame,"FinTrack",true), BorderLayout.NORTH);
        root.add(sideNav(frame,"Savings Goals"), BorderLayout.WEST);

        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG); content.setBorder(new EmptyBorder(28,28,28,28));

        JLabel heading = new JLabel("Savings Goals"); heading.setFont(new Font("Segoe UI",Font.BOLD,20)); heading.setForeground(DARK_TEXT); heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel("Track what you're saving toward. Add funds manually and watch your progress grow.");
        sub.setFont(new Font("Segoe UI",Font.PLAIN,12)); sub.setForeground(GRAY_TEXT); sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(heading); content.add(Box.createVerticalStrut(4)); content.add(sub); content.add(Box.createVerticalStrut(20));

        // ── Create goal form ──────────────────────────────────────────────────
        JPanel addCard = card(20); addCard.setLayout(new BoxLayout(addCard, BoxLayout.Y_AXIS));
        addCard.setAlignmentX(Component.LEFT_ALIGNMENT); addCard.setMaximumSize(new Dimension(520,9999));
        JLabel addTitle = new JLabel("Create New Savings Goal"); addTitle.setFont(new Font("Segoe UI",Font.BOLD,14)); addTitle.setForeground(DARK_TEXT); addTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField nameF = styledField("Goal name (e.g. Laptop, Vacation, Emergency Fund)");
        nameF.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); nameF.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField targetF = styledField("Target amount (\u20b9)");
        targetF.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); targetF.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField initialF = styledField("Already saved (\u20b9) — enter 0 if starting fresh");
        initialF.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); initialF.setAlignmentX(Component.LEFT_ALIGNMENT);

        SpinnerDateModel dlModel = new SpinnerDateModel();
        JSpinner deadlineSpin = new JSpinner(dlModel);
        deadlineSpin.setEditor(new JSpinner.DateEditor(deadlineSpin,"yyyy-MM-dd"));
        deadlineSpin.setValue(new java.util.Date());
        deadlineSpin.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); deadlineSpin.setAlignmentX(Component.LEFT_ALIGNMENT);
        JCheckBox noDeadline = new JCheckBox("No deadline"); noDeadline.setOpaque(false); noDeadline.setFont(new Font("Segoe UI",Font.PLAIN,12)); noDeadline.setAlignmentX(Component.LEFT_ALIGNMENT);
        noDeadline.addActionListener(e -> deadlineSpin.setEnabled(!noDeadline.isSelected()));

        JTextField noteF = styledField("Note (optional)"); noteF.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); noteF.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addBtn = primaryBtn("Create Goal", PURPLE);
        addBtn.setAlignmentX(Component.LEFT_ALIGNMENT); addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        JLabel addMsg = new JLabel(" "); addMsg.setFont(new Font("Segoe UI",Font.PLAIN,12)); addMsg.setAlignmentX(Component.LEFT_ALIGNMENT);

        addBtn.addActionListener(e -> {
            try {
                String name = nameF.getText().trim();
                double target = Double.parseDouble(targetF.getText().trim());
                double initial = initialF.getText().trim().isEmpty() ? 0 : Double.parseDouble(initialF.getText().trim());
                if (name.isEmpty()) { addMsg.setForeground(RED_COL); addMsg.setText("Enter a goal name."); return; }
                LocalDate deadline = noDeadline.isSelected() ? null
                    : ((java.util.Date)deadlineSpin.getValue()).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                SavingsGoal g = new SavingsGoal(uid,name,target,initial,deadline,noteF.getText().trim());
                if (db.addSavingsGoal(g)) { addMsg.setForeground(GREEN); addMsg.setText("Goal created!"); nameF.setText(""); targetF.setText(""); initialF.setText(""); noteF.setText(""); showSavingsGoals(frame); }
                else { addMsg.setForeground(RED_COL); addMsg.setText("Failed."); }
            } catch (NumberFormatException ex) { addMsg.setForeground(RED_COL); addMsg.setText("Enter valid amounts."); }
        });

        addCard.add(addTitle); addCard.add(Box.createVerticalStrut(12));
        addCard.add(fLabel("Goal Name *")); addCard.add(Box.createVerticalStrut(4)); addCard.add(nameF); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Target Amount (\u20b9) *")); addCard.add(Box.createVerticalStrut(4)); addCard.add(targetF); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Already Saved (\u20b9)")); addCard.add(Box.createVerticalStrut(4)); addCard.add(initialF); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Deadline")); addCard.add(Box.createVerticalStrut(4)); addCard.add(deadlineSpin); addCard.add(Box.createVerticalStrut(4)); addCard.add(noDeadline); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Note (optional)")); addCard.add(Box.createVerticalStrut(4)); addCard.add(noteF); addCard.add(Box.createVerticalStrut(10));
        addCard.add(addBtn); addCard.add(Box.createVerticalStrut(6)); addCard.add(addMsg);
        content.add(addCard); content.add(Box.createVerticalStrut(20));

        // ── Goal cards ────────────────────────────────────────────────────────
        List<SavingsGoal> goals = db.getSavingsGoalsByUser(uid);
        if (!goals.isEmpty()) {
            JPanel listCard = card(16); listCard.setLayout(new BoxLayout(listCard, BoxLayout.Y_AXIS)); listCard.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel listTitle = new JLabel("Your Goals"); listTitle.setFont(new Font("Segoe UI",Font.BOLD,14)); listTitle.setForeground(DARK_TEXT); listTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            listCard.add(listTitle); listCard.add(Box.createVerticalStrut(12));

            for (SavingsGoal g : goals) {
                JPanel goalCard = new JPanel(); goalCard.setLayout(new BoxLayout(goalCard, BoxLayout.Y_AXIS)); goalCard.setOpaque(false);
                goalCard.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(g.isAchieved()?new Color(0xB2,0xE0,0xD0):BORDER_COL, 1, true),
                    new EmptyBorder(12,14,12,14)));
                goalCard.setAlignmentX(Component.LEFT_ALIGNMENT); goalCard.setMaximumSize(new Dimension(Integer.MAX_VALUE,9999));
                goalCard.add(buildGoalRow(g));

                JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); btnRow.setOpaque(false); btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                if (!g.isAchieved()) {
                    JButton addFundsBtn = smallBtn("+ Add Funds", new Color(0xE8,0xF5,0xEE), new Color(0x0F,0x6E,0x56));
                    addFundsBtn.addActionListener(e -> {
                        String val = JOptionPane.showInputDialog(frame, "Add how much to \""+g.getName()+"\"?", "0");
                        if (val!=null) { try { double amt=Double.parseDouble(val.trim()); if(amt>0) { db.addToSavingsGoal(g.getId(),amt);
                            if(g.getSavedAmount()+amt>=g.getTargetAmount()) db.markGoalAchieved(g.getId()); showSavingsGoals(frame); } } catch (NumberFormatException ignored) {} }
                    });
                    JButton markBtn = smallBtn("Mark Achieved", new Color(0xE1,0xF5,0xEE), GREEN);
                    markBtn.addActionListener(e -> { db.markGoalAchieved(g.getId()); showSavingsGoals(frame); });
                    btnRow.add(addFundsBtn); btnRow.add(markBtn);
                } else {
                    JLabel doneTag = new JLabel(" \u2713 ACHIEVED "); doneTag.setFont(new Font("Segoe UI",Font.BOLD,11));
                    doneTag.setForeground(WHITE); doneTag.setBackground(GREEN); doneTag.setOpaque(true); doneTag.setBorder(new EmptyBorder(3,8,3,8));
                    btnRow.add(doneTag);
                }
                JButton delBtn = smallBtn("Delete", RED_LIGHT, RED_COL);
                delBtn.addActionListener(e -> { if(JOptionPane.showConfirmDialog(frame,"Delete \""+g.getName()+"\"?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) { db.deleteSavingsGoal(g.getId()); showSavingsGoals(frame); } });
                btnRow.add(delBtn);
                goalCard.add(btnRow);
                listCard.add(goalCard); listCard.add(Box.createVerticalStrut(10));
            }
            content.add(listCard);
        }
        root.add(scrollWrap(content), BorderLayout.CENTER);
        setContent(frame, root);
    }

    // ══════════════════════════════════════════
    //  SCREEN: BUDGET
    // ══════════════════════════════════════════

    static void showBudget(JFrame frame) {
        frame.setTitle("FinTrack — Budget");
        DatabaseHelper db = DatabaseHelper.getInstance();
        int uid = SessionManager.getInstance().getCurrentUserId();
        String month = currentMonth();

        JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG);
        root.add(topBar(frame,"FinTrack",true), BorderLayout.NORTH);
        root.add(sideNav(frame,"Budget"), BorderLayout.WEST);

        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG); content.setBorder(new EmptyBorder(28,28,28,28));

        JLabel heading = new JLabel("Budget Manager — "+month); heading.setFont(new Font("Segoe UI",Font.BOLD,20)); heading.setForeground(DARK_TEXT); heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(heading); content.add(Box.createVerticalStrut(16));

        JPanel setCard = card(20); setCard.setLayout(new BoxLayout(setCard, BoxLayout.Y_AXIS)); setCard.setAlignmentX(Component.LEFT_ALIGNMENT); setCard.setMaximumSize(new Dimension(500,9999));
        JLabel setTitle = new JLabel("Set Monthly Budget Limit"); setTitle.setFont(new Font("Segoe UI",Font.BOLD,15)); setTitle.setForeground(DARK_TEXT); setTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComboBox<Category> catBox = new JComboBox<>(db.getExpenseCategories().toArray(new Category[0])); catBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); catBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField limitF = styledField("Limit amount (\u20b9)"); limitF.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); limitF.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton setBtn = primaryBtn("Set Budget", GREEN); setBtn.setAlignmentX(Component.LEFT_ALIGNMENT); setBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        JLabel msg = new JLabel(" "); msg.setFont(new Font("Segoe UI",Font.PLAIN,12)); msg.setAlignmentX(Component.LEFT_ALIGNMENT);
        setBtn.addActionListener(e -> {
            try {
                Category cat = (Category) catBox.getSelectedItem(); double limit = Double.parseDouble(limitF.getText().trim());
                if (cat==null) { msg.setForeground(RED_COL); msg.setText("Select a category."); return; }
                Budget b = new Budget(uid, cat.getId(), limit, month);
                if (db.saveBudget(b)) { msg.setForeground(GREEN); msg.setText("Budget saved for "+cat.getName()+"!"); limitF.setText(""); showBudget(frame); }
                else { msg.setForeground(RED_COL); msg.setText("Failed."); }
            } catch (NumberFormatException ex) { msg.setForeground(RED_COL); msg.setText("Enter a valid number."); }
        });
        setCard.add(setTitle); setCard.add(Box.createVerticalStrut(10));
        setCard.add(fLabel("Category")); setCard.add(Box.createVerticalStrut(4)); setCard.add(catBox); setCard.add(Box.createVerticalStrut(8));
        setCard.add(fLabel("Monthly Limit (\u20b9)")); setCard.add(Box.createVerticalStrut(4)); setCard.add(limitF); setCard.add(Box.createVerticalStrut(10));
        setCard.add(setBtn); setCard.add(Box.createVerticalStrut(6)); setCard.add(msg);
        content.add(setCard); content.add(Box.createVerticalStrut(20));

        JPanel curCard = card(20); curCard.setLayout(new BoxLayout(curCard, BoxLayout.Y_AXIS)); curCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel curTitle = new JLabel("Current Budgets"); curTitle.setFont(new Font("Segoe UI",Font.BOLD,15)); curTitle.setForeground(DARK_TEXT); curTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        curCard.add(curTitle); curCard.add(Box.createVerticalStrut(8));
        List<Budget> budgets = db.getBudgetsByMonth(uid,month);
        if (budgets.isEmpty()) { JLabel none = new JLabel("No budgets set for this month."); none.setAlignmentX(Component.LEFT_ALIGNMENT); curCard.add(none); }
        else { for (Budget b : budgets) { curCard.add(buildBudgetRow(b)); curCard.add(Box.createVerticalStrut(8)); } }
        content.add(curCard);
        root.add(scrollWrap(content), BorderLayout.CENTER);
        setContent(frame, root);
    }

    // ══════════════════════════════════════════
    //  SCREEN: REPORTS
    // ══════════════════════════════════════════

    static void showReports(JFrame frame) {
        frame.setTitle("FinTrack — Reports");
        DatabaseHelper db = DatabaseHelper.getInstance();
        int uid = SessionManager.getInstance().getCurrentUserId();
        String month = currentMonth();

        JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG);
        root.add(topBar(frame,"FinTrack",true), BorderLayout.NORTH);
        root.add(sideNav(frame,"Reports"), BorderLayout.WEST);

        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG); content.setBorder(new EmptyBorder(28,28,28,28));

        JLabel heading = new JLabel("Reports — "+month); heading.setFont(new Font("Segoe UI",Font.BOLD,20)); heading.setForeground(DARK_TEXT); heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(heading); content.add(Box.createVerticalStrut(16));

        double expense = db.getTotalByTypeAndMonth(uid,"EXPENSE",month);
        double income  = db.getTotalByTypeAndMonth(uid,"INCOME",month);
        JPanel summary = new JPanel(new GridLayout(1,4,12,0)); summary.setOpaque(false);
        summary.setMaximumSize(new Dimension(Integer.MAX_VALUE,90)); summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        summary.add(statCard("Total Spent",  "\u20b9"+String.format("%.2f",expense), "#A32D2D"));
        summary.add(statCard("Total Income", "\u20b9"+String.format("%.2f",income),  "#0F6E56"));
        summary.add(statCard("Net Balance",  "\u20b9"+String.format("%.2f",income-expense), income>=expense?"#0F6E56":"#A32D2D"));
        summary.add(statCard("Transactions", String.valueOf(db.getTransactionsByUser(uid).size()), "#185FA5"));
        content.add(summary); content.add(Box.createVerticalStrut(16));

        JPanel pieBox = new JPanel(new BorderLayout()); pieBox.setBackground(CARD_BG);
        pieBox.setBorder(new CompoundBorder(new LineBorder(BORDER_COL,1,true), new EmptyBorder(8,8,8,8)));
        pieBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,280)); pieBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        pieBox.add(new PieChartPanel(db,uid,month));
        content.add(pieBox); content.add(Box.createVerticalStrut(16));
        content.add(buildTxTable(frame,db,uid,false)); content.add(Box.createVerticalStrut(16));

        JPanel exportCard = card(20); exportCard.setLayout(new BoxLayout(exportCard, BoxLayout.Y_AXIS)); exportCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel expTitle = new JLabel("Export Data"); expTitle.setFont(new Font("Segoe UI",Font.BOLD,14)); expTitle.setForeground(DARK_TEXT); expTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel expDesc = new JLabel("Export all transactions to CSV (opens in Excel / Google Sheets)."); expDesc.setFont(new Font("Segoe UI",Font.PLAIN,12)); expDesc.setForeground(GRAY_TEXT); expDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton expBtn = primaryBtn("Export as CSV", new Color(0x18,0x5F,0xA5)); expBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel expMsg = new JLabel(" "); expMsg.setFont(new Font("Segoe UI",Font.PLAIN,12)); expMsg.setAlignmentX(Component.LEFT_ALIGNMENT);
        expBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Save CSV Report"); fc.setSelectedFile(new java.io.File("fintrack_report.csv"));
            if (fc.showSaveDialog(frame)==JFileChooser.APPROVE_OPTION) {
                try { new ReportGenerator(db.getTransactionsByUser(uid)).exportToCSV(fc.getSelectedFile().getAbsolutePath()); expMsg.setForeground(GREEN); expMsg.setText("Exported: "+fc.getSelectedFile().getName()); }
                catch (Exception ex) { expMsg.setForeground(RED_COL); expMsg.setText("Failed: "+ex.getMessage()); }
            }
        });
        exportCard.add(expTitle); exportCard.add(Box.createVerticalStrut(6)); exportCard.add(expDesc); exportCard.add(Box.createVerticalStrut(10));
        exportCard.add(expBtn); exportCard.add(Box.createVerticalStrut(6)); exportCard.add(expMsg);
        content.add(exportCard);
        root.add(scrollWrap(content), BorderLayout.CENTER);
        setContent(frame, root);
    }

    // ── Pie chart ────────────────────────────────────────────────────────────
    static class PieChartPanel extends JPanel {
        private final List<double[]> slices = new ArrayList<>();
        private final List<String> sliceLabels = new ArrayList<>();
        private final String title;
        PieChartPanel(DatabaseHelper db, int uid, String month) {
            setBackground(CARD_BG); setPreferredSize(new Dimension(0,270));
            this.title = "Spending by Category — "+month;
            for (Category c : db.getExpenseCategories()) {
                double spent = db.getSpentByCategory(uid,c.getId(),month);
                if (spent>0) { slices.add(new double[]{spent}); sliceLabels.add(c.getName()+" \u20b9"+String.format("%.0f",spent)); }
            }
            if (slices.isEmpty()) { slices.add(new double[]{1}); sliceLabels.add("No expenses yet"); }
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            g2.setFont(new Font("Segoe UI",Font.BOLD,13)); g2.setColor(DARK_TEXT); g2.drawString(title, 20, 28);

            int size=Math.min(w/2-50, h-70);
            int cx=w/4+10, cy=h/2+15, x=cx-size/2, y=cy-size/2;
            double total=slices.stream().mapToDouble(s->s[0]).sum();
            Color[] palette={
                new Color(0x10,0xB9,0x81), new Color(0x38,0x8B,0xFF), new Color(0xF5,0x9E,0x0B),
                new Color(0xEF,0x44,0x44), new Color(0x7C,0x3A,0xED), new Color(0xF9,0x73,0x16),
                new Color(0x06,0xB6,0xD4), new Color(0x6B,0x7B,0x99)
            };

            double start=0;
            for (int i=0; i<slices.size(); i++) {
                double angle=slices.get(i)[0]/total*360;
                g2.setColor(palette[i%palette.length]); g2.fillArc(x,y,size,size,(int)start,(int)angle);
                // Slight white gap between slices
                g2.setColor(CARD_BG); g2.setStroke(new BasicStroke(2f)); g2.drawArc(x,y,size,size,(int)start,(int)angle);
                start+=angle;
            }
            // Donut hole
            g2.setColor(CARD_BG); g2.fillOval(x+size/4, y+size/4, size/2, size/2);

            // Legend
            int lx=w/2+20, ly=50; g2.setFont(new Font("Segoe UI",Font.PLAIN,11));
            for (int i=0; i<sliceLabels.size(); i++) {
                g2.setColor(palette[i%palette.length]); g2.fillRoundRect(lx,ly+i*22,12,12,4,4);
                g2.setColor(DARK_TEXT); g2.drawString(sliceLabels.get(i), lx+18, ly+i*22+11);
            }
        }
    }

    // ══════════════════════════════════════════
    //  SCREEN: INVESTMENTS
    // ══════════════════════════════════════════

    static void showInvestments(JFrame frame) {
        frame.setTitle("FinTrack — Investments");
        DatabaseHelper db = DatabaseHelper.getInstance();
        int uid = SessionManager.getInstance().getCurrentUserId();

        JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG);
        root.add(topBar(frame,"FinTrack",true), BorderLayout.NORTH);
        root.add(sideNav(frame,"Investments"), BorderLayout.WEST);

        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG); content.setBorder(new EmptyBorder(28,28,28,28));

        JLabel heading = new JLabel("Investment Tracker"); heading.setFont(new Font("Segoe UI",Font.BOLD,20)); heading.setForeground(DARK_TEXT); heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(heading); content.add(Box.createVerticalStrut(16));

        JPanel addCard = card(20); addCard.setLayout(new BoxLayout(addCard, BoxLayout.Y_AXIS)); addCard.setAlignmentX(Component.LEFT_ALIGNMENT); addCard.setMaximumSize(new Dimension(500,9999));
        JLabel addTitle = new JLabel("Add New Investment"); addTitle.setFont(new Font("Segoe UI",Font.BOLD,15)); addTitle.setForeground(DARK_TEXT); addTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField nameF = styledField("Investment name"); nameF.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); nameF.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Stocks","Mutual Fund","Crypto","Fixed Deposit","Gold","Real Estate","Other"});
        typeBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); typeBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField investedF = styledField("Amount Invested (\u20b9)"); investedF.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); investedF.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField currentF = styledField("Current Value (\u20b9)"); currentF.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); currentF.setAlignmentX(Component.LEFT_ALIGNMENT);
        SpinnerDateModel dm2 = new SpinnerDateModel(); JSpinner dateSpin = new JSpinner(dm2);
        dateSpin.setEditor(new JSpinner.DateEditor(dateSpin,"yyyy-MM-dd")); dateSpin.setValue(new java.util.Date());
        dateSpin.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); dateSpin.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField noteFI = styledField("Note (optional)"); noteFI.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); noteFI.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton addBtn = primaryBtn("Add Investment", PURPLE); addBtn.setAlignmentX(Component.LEFT_ALIGNMENT); addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        JLabel addMsg = new JLabel(" "); addMsg.setFont(new Font("Segoe UI",Font.PLAIN,12)); addMsg.setAlignmentX(Component.LEFT_ALIGNMENT);

        addBtn.addActionListener(e -> {
            try {
                String name=(String)nameF.getText().trim(); String type=(String)typeBox.getSelectedItem();
                double inv=Double.parseDouble(investedF.getText().trim()), cur=Double.parseDouble(currentF.getText().trim());
                LocalDate dt=((java.util.Date)dateSpin.getValue()).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                if (name.isEmpty()) { addMsg.setForeground(RED_COL); addMsg.setText("Enter investment name."); return; }
                Investment investment = new Investment(uid,name,type,inv,cur,dt,noteFI.getText().trim());
                if (db.addInvestment(investment)) { addMsg.setForeground(GREEN); addMsg.setText("Added!"); nameF.setText(""); investedF.setText(""); currentF.setText(""); noteFI.setText(""); dateSpin.setValue(new java.util.Date()); typeBox.setSelectedIndex(0); showInvestments(frame); }
                else { addMsg.setForeground(RED_COL); addMsg.setText("Failed."); }
            } catch (NumberFormatException ex) { addMsg.setForeground(RED_COL); addMsg.setText("Enter valid amounts."); }
        });
        addCard.add(addTitle); addCard.add(Box.createVerticalStrut(10));
        addCard.add(fLabel("Investment Name *")); addCard.add(Box.createVerticalStrut(4)); addCard.add(nameF); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Type *")); addCard.add(Box.createVerticalStrut(4)); addCard.add(typeBox); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Amount Invested (\u20b9) *")); addCard.add(Box.createVerticalStrut(4)); addCard.add(investedF); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Current Value (\u20b9) *")); addCard.add(Box.createVerticalStrut(4)); addCard.add(currentF); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Date")); addCard.add(Box.createVerticalStrut(4)); addCard.add(dateSpin); addCard.add(Box.createVerticalStrut(8));
        addCard.add(fLabel("Note (optional)")); addCard.add(Box.createVerticalStrut(4)); addCard.add(noteFI); addCard.add(Box.createVerticalStrut(10));
        addCard.add(addBtn); addCard.add(Box.createVerticalStrut(6)); addCard.add(addMsg);
        content.add(addCard); content.add(Box.createVerticalStrut(16));

        List<Investment> investments = db.getInvestmentsByUser(uid);
        List<Investment> active = investments.stream().filter(i->"ACTIVE".equals(i.getStatus())).toList();
        double totalInvested=active.stream().mapToDouble(Investment::getInvestedAmount).sum();
        double totalCurrent=active.stream().mapToDouble(Investment::getCurrentValue).sum();
        double totalPL=totalCurrent-totalInvested;
        JPanel summaryRow = new JPanel(new GridLayout(1,4,12,0)); summaryRow.setOpaque(false); summaryRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,90)); summaryRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        summaryRow.add(statCard("Total Invested","\u20b9"+String.format("%.0f",totalInvested),"#185FA5"));
        summaryRow.add(statCard("Current Value","\u20b9"+String.format("%.0f",totalCurrent),"#7F77DD"));
        summaryRow.add(statCard("Total P/L",(totalPL>=0?"+":"")+"\u20b9"+String.format("%.0f",totalPL),totalPL>=0?"#0F6E56":"#A32D2D"));
        summaryRow.add(statCard("Active Holdings",String.valueOf(active.size()),"#888888"));
        content.add(summaryRow); content.add(Box.createVerticalStrut(16));

        JPanel tableCard = card(16); tableCard.setLayout(new BoxLayout(tableCard, BoxLayout.Y_AXIS)); tableCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tblTitle = new JLabel("Your Holdings"); tblTitle.setFont(new Font("Segoe UI",Font.BOLD,14)); tblTitle.setForeground(DARK_TEXT); tblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel infoLbl = new JLabel("Tip: \"Settle\" realises P/L and posts it automatically as Income or Expense."); infoLbl.setFont(new Font("Segoe UI",Font.PLAIN,11)); infoLbl.setForeground(GRAY_TEXT); infoLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        String[] invCols = {"Name","Type","Invested","Current","P/L","Status","Date","Update","Settle","Delete"};
        Object[][] invData = new Object[investments.size()][10];
        for (int i=0; i<investments.size(); i++) {
            Investment inv=investments.get(i);
            String pl=(inv.isProfit()?"+":"")+"\u20b9"+String.format("%.2f",inv.getProfitLoss())+"("+String.format("%.1f",inv.getReturnPercent())+"%)";
            invData[i]=new Object[]{inv.getName(),inv.getType(),String.format("%.2f",inv.getInvestedAmount()),String.format("%.2f",inv.getCurrentValue()),pl,inv.getStatus(),inv.getInvestedDate().toString(),inv.getId(),inv.getId(),inv.getId()};
        }
        DefaultTableModel invModel = new DefaultTableModel(invData, invCols) { @Override public boolean isCellEditable(int r, int c) { return c>=7; } };
        JTable invTable = new JTable(invModel) {
            @Override public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c=super.prepareRenderer(renderer, row, col);
                if (!isRowSelected(row)) { String st=(String)getModel().getValueAt(row,5); c.setBackground("PROFIT".equals(st)?INC_ROW:"LOSS".equals(st)?EXP_ROW:WHITE); } return c;
            }
        };
        invTable.setFont(new Font("Segoe UI",Font.PLAIN,12)); invTable.setRowHeight(28); invTable.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12)); invTable.setShowGrid(false); invTable.setIntercellSpacing(new Dimension(0,0));

        // Update
        invTable.getColumn("Update").setCellRenderer(new DefaultTableCellRenderer() { @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) { return smallBtn("Update",new Color(0xE8,0xF4,0xFD),new Color(0x18,0x5F,0xA5)); } });
        invTable.getColumn("Update").setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            JButton btn=smallBtn("Update",new Color(0xE8,0xF4,0xFD),new Color(0x18,0x5F,0xA5)); int editRow;
            { btn.addActionListener(e->{ fireEditingStopped(); int id=(int)invModel.getValueAt(editRow,7);
                investments.stream().filter(i->i.getId()==id).findFirst().ifPresent(inv2->{
                    String val=JOptionPane.showInputDialog(frame,"New current value for "+inv2.getName()+":",String.format("%.2f",inv2.getCurrentValue()));
                    if(val!=null){try{db.updateInvestmentValue(id,Double.parseDouble(val.trim()));showInvestments(frame);}catch(NumberFormatException ignored){}}
                }); }); }
            @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) { editRow=r; return btn; }
            @Override public Object getCellEditorValue() { return null; }
        });
        // Settle
        invTable.getColumn("Settle").setCellRenderer(new DefaultTableCellRenderer() { @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) { return smallBtn("Settle",new Color(0xFF,0xF3,0xE0),ORANGE); } });
        invTable.getColumn("Settle").setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            JButton btn=smallBtn("Settle",new Color(0xFF,0xF3,0xE0),ORANGE); int editRow;
            { btn.addActionListener(e->{ fireEditingStopped(); int id=(int)invModel.getValueAt(editRow,8);
                investments.stream().filter(i->i.getId()==id).findFirst().ifPresent(inv2->{
                    double pl=inv2.getProfitLoss();
                    List<Category> incCats=db.getIncomeCategories();
                    int catId=incCats.isEmpty()?1:incCats.get(incCats.size()-1).getId(); // Others
                    if(pl>=0) db.addTransaction(new Income(pl,"Investment profit: "+inv2.getName(),LocalDate.now(),uid,catId,"Settled investment"));
                    else db.addTransaction(new Expense(-pl,"Investment loss: "+inv2.getName(),LocalDate.now(),uid,catId,"Settled investment"));
                    db.settleInvestment(id,pl>=0?"PROFIT":"LOSS"); showInvestments(frame);
                }); }); }
            @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) { editRow=r; return btn; }
            @Override public Object getCellEditorValue() { return null; }
        });
        // Delete
        invTable.getColumn("Delete").setCellRenderer(new DefaultTableCellRenderer() { @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) { return smallBtn("Delete",RED_LIGHT,RED_COL); } });
        invTable.getColumn("Delete").setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            JButton btn=smallBtn("Delete",RED_LIGHT,RED_COL); int editRow;
            { btn.addActionListener(e->{ fireEditingStopped(); db.deleteInvestment((int)invModel.getValueAt(editRow,9)); showInvestments(frame); }); }
            @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) { editRow=r; return btn; }
            @Override public Object getCellEditorValue() { return null; }
        });
        JScrollPane invSp = new JScrollPane(invTable); invSp.setPreferredSize(new Dimension(0,300)); invSp.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableCard.add(tblTitle); tableCard.add(Box.createVerticalStrut(4)); tableCard.add(infoLbl); tableCard.add(Box.createVerticalStrut(8)); tableCard.add(invSp);
        content.add(tableCard);
        root.add(scrollWrap(content), BorderLayout.CENTER);
        setContent(frame, root);
    }

    // ══════════════════════════════════════════
    //  SCREEN: MONTHLY HISTORY
    // ══════════════════════════════════════════

    static void showHistory(JFrame frame) {
        frame.setTitle("FinTrack — Monthly History");
        DatabaseHelper db = DatabaseHelper.getInstance();
        int uid = SessionManager.getInstance().getCurrentUserId();

        JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG);
        root.add(topBar(frame,"FinTrack",true), BorderLayout.NORTH);
        root.add(sideNav(frame,"History"), BorderLayout.WEST);

        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG); content.setBorder(new EmptyBorder(28,28,28,28));

        JLabel heading = new JLabel("Monthly History"); heading.setFont(new Font("Segoe UI",Font.BOLD,20)); heading.setForeground(DARK_TEXT); heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(heading); content.add(Box.createVerticalStrut(16));

        DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("MMMM yyyy");
        DateTimeFormatter keyFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        List<String> monthKeys=new ArrayList<>(), monthLabels=new ArrayList<>();
        YearMonth cur = YearMonth.now();
        for (int i=1; i<=24; i++) { YearMonth ym=cur.minusMonths(i); monthKeys.add(ym.format(keyFmt)); monthLabels.add(ym.format(displayFmt)); }

        JPanel pickerRow = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); pickerRow.setOpaque(false); pickerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel pickLabel = new JLabel("Select Month:"); pickLabel.setFont(new Font("Segoe UI",Font.BOLD,13)); pickLabel.setForeground(new Color(0x55,0x55,0x55));
        JComboBox<String> monthBox = new JComboBox<>(monthLabels.toArray(new String[0])); monthBox.setPreferredSize(new Dimension(220,32));
        pickerRow.add(pickLabel); pickerRow.add(monthBox); content.add(pickerRow); content.add(Box.createVerticalStrut(16));

        JPanel detailHolder = new JPanel(); detailHolder.setLayout(new BoxLayout(detailHolder, BoxLayout.Y_AXIS)); detailHolder.setOpaque(false); detailHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(detailHolder);

        Runnable loadMonth = () -> {
            detailHolder.removeAll(); int selIdx=monthBox.getSelectedIndex(); if(selIdx<0) return;
            String selectedKey=monthKeys.get(selIdx), selectedLabel=monthLabels.get(selIdx);
            double exp=db.getTotalByTypeAndMonth(uid,"EXPENSE",selectedKey);
            double inc=db.getTotalByTypeAndMonth(uid,"INCOME",selectedKey);
            double bal=inc-exp;
            JPanel statsRow = new JPanel(new GridLayout(1,3,12,0)); statsRow.setOpaque(false);
            statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,90)); statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            statsRow.add(statCard("Total Spent","\u20b9"+String.format("%.2f",exp),"#A32D2D"));
            statsRow.add(statCard("Total Income","\u20b9"+String.format("%.2f",inc),"#0F6E56"));
            statsRow.add(statCard("Net Balance","\u20b9"+String.format("%.2f",bal),bal>=0?"#0F6E56":"#A32D2D"));
            detailHolder.add(statsRow); detailHolder.add(Box.createVerticalStrut(16));

            JPanel pBox = new JPanel(new BorderLayout()); pBox.setBackground(CARD_BG);
            pBox.setBorder(new CompoundBorder(new LineBorder(BORDER_COL,1,true), new EmptyBorder(8,8,8,8)));
            pBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,280)); pBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            pBox.add(new PieChartPanel(db,uid,selectedKey)); detailHolder.add(pBox); detailHolder.add(Box.createVerticalStrut(16));

            List<Transaction> txs=db.getTransactionsByUserAndMonth(uid,selectedKey);
            JPanel txCard=card(16); txCard.setLayout(new BoxLayout(txCard, BoxLayout.Y_AXIS)); txCard.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel txTitle=new JLabel("Transactions — "+selectedLabel); txTitle.setFont(new Font("Segoe UI",Font.BOLD,14)); txTitle.setForeground(DARK_TEXT); txTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            txCard.add(txTitle); txCard.add(Box.createVerticalStrut(8));
            if (txs.isEmpty()) { JLabel none=new JLabel("No transactions for this month."); none.setAlignmentX(Component.LEFT_ALIGNMENT); txCard.add(none); }
            else {
                String[] cols={"Type","Description","Amount","Category","Date","Note"};
                Object[][] data=new Object[txs.size()][6];
                for (int i=0; i<txs.size(); i++) { Transaction t=txs.get(i); data[i][0]=t.getType(); data[i][1]=t.getDescription(); data[i][2]=t.getDisplayAmount(); data[i][3]=db.getCategoryName(t.getCategoryId()); data[i][4]=t.getDate().toString(); data[i][5]=t.getNote(); }
                DefaultTableModel m = new DefaultTableModel(data,cols) { @Override public boolean isCellEditable(int r, int c) { return false; } };
                JTable table = new JTable(m) {
                    @Override public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                        Component c=super.prepareRenderer(renderer, row, col);
                        if (!isRowSelected(row)) c.setBackground("INCOME".equals(getModel().getValueAt(row,0))?INC_ROW:EXP_ROW); return c;
                    }
                };
                table.setFont(new Font("Segoe UI",Font.PLAIN,12)); table.setRowHeight(26); table.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12)); table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0,0));
                JScrollPane sp=new JScrollPane(table); sp.setPreferredSize(new Dimension(0,280)); sp.setAlignmentX(Component.LEFT_ALIGNMENT); txCard.add(sp);
            }
            detailHolder.add(txCard); detailHolder.add(Box.createVerticalStrut(16));

            List<Budget> bList=db.getBudgetsByMonth(uid,selectedKey);
            if (!bList.isEmpty()) {
                JPanel budgetCard=card(16); budgetCard.setLayout(new BoxLayout(budgetCard, BoxLayout.Y_AXIS)); budgetCard.setAlignmentX(Component.LEFT_ALIGNMENT);
                JLabel bTitleL=new JLabel("Budget Performance — "+selectedLabel); bTitleL.setFont(new Font("Segoe UI",Font.BOLD,14)); bTitleL.setForeground(DARK_TEXT); bTitleL.setAlignmentX(Component.LEFT_ALIGNMENT);
                budgetCard.add(bTitleL); budgetCard.add(Box.createVerticalStrut(8));
                for (Budget b:bList) { budgetCard.add(buildBudgetRow(b)); budgetCard.add(Box.createVerticalStrut(6)); }
                detailHolder.add(budgetCard);
            }
            detailHolder.revalidate(); detailHolder.repaint();
        };

        monthBox.addActionListener(e -> loadMonth.run());
        loadMonth.run();
        root.add(scrollWrap(content), BorderLayout.CENTER);
        setContent(frame, root);
    }

    // ══════════════════════════════════════════
    //  ENTRY POINT
    // ══════════════════════════════════════════

    public static void main(String[] args) {
        // Enable font anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext","true");
        // Use Nimbus for better widget rendering
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) { UIManager.setLookAndFeel(info.getClassName()); break; }
            }
            // Nimbus theme overrides
            UIManager.put("control",          new Color(0xF0,0xF2,0xF7));
            UIManager.put("info",             new Color(0xF0,0xF2,0xF7));
            UIManager.put("nimbusBase",       new Color(0x10,0xB9,0x81));
            UIManager.put("nimbusBlueGrey",   new Color(0x8A,0x96,0xAA));
            UIManager.put("nimbusLightBackground", Color.WHITE);
            UIManager.put("text",             new Color(0x12,0x17,0x28));
            UIManager.put("nimbusFocus",      new Color(0x10,0xB9,0x81));
            UIManager.put("nimbusSelectionBackground", new Color(0x10,0xB9,0x81));
            UIManager.put("nimbusSelectedText", Color.WHITE);
            UIManager.put("ScrollBar.thumb",  new Color(0xC8,0xD0,0xDE));
            UIManager.put("ScrollBar.thumbHighlight", new Color(0xA0,0xAC,0xBE));
        } catch (Exception ignored) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e2) { }
        }
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("FinTrack");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900,580);
            frame.setLocationRelativeTo(null);
            showLogin(frame);
        });
    }
}