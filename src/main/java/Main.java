import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

// ─────────────────────────────────────────────
//  FinTrack — Single File Expense Tracker
//  All OOP concepts demonstrated via inner classes
// ─────────────────────────────────────────────

public class Main extends Application {

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

        public Transaction(double amount, String description, LocalDate date, int userId, int categoryId, String note) {
            if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");
            this.amount = amount; this.description = description; this.date = date;
            this.userId = userId; this.categoryId = categoryId; this.note = note;
        }

        // Abstract methods — Polymorphism
        public abstract String getType();
        public abstract String getDisplayAmount();

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public double getAmount() { return amount; }
        public String getDescription() { return description; }
        public LocalDate getDate() { return date; }
        public int getUserId() { return userId; }
        public int getCategoryId() { return categoryId; }
        public String getNote() { return note != null ? note : ""; }
        public void setDescription(String d) { this.description = d; }
        public void setDate(LocalDate d) { this.date = d; }
        public void setNote(String n) { this.note = n; }
    }

    // Inheritance — Expense extends Transaction
    static class Expense extends Transaction {
        public Expense(double amount, String description, LocalDate date, int userId, int categoryId, String note) {
            super(amount, description, date, userId, categoryId, note);
        }
        @Override public String getType() { return "EXPENSE"; }
        @Override public String getDisplayAmount() { return "- ₹" + String.format("%.2f", getAmount()); }
    }

    // Inheritance — Income extends Transaction
    static class Income extends Transaction {
        public Income(double amount, String description, LocalDate date, int userId, int categoryId, String note) {
            super(amount, description, date, userId, categoryId, note);
        }
        @Override public String getType() { return "INCOME"; }
        @Override public String getDisplayAmount() { return "+ ₹" + String.format("%.2f", getAmount()); }
    }

    // Encapsulation
    static class User {
        private int id;
        private String name, email, password;
        public User(String name, String email, String password) {
            this.name = name; this.email = email; this.password = password;
        }
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPassword() { return password; }
    }

    static class Category {
        private int id;
        private String name, color;
        public Category(String name, String color) { this.name = name; this.color = color; }
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public String getColor() { return color; }
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
        public boolean isExceeded() { return spentAmount > limitAmount; }
        public boolean isWarning() { return getUsagePercent() >= 80 && !isExceeded(); }
        public int getId() { return id; } public void setId(int id) { this.id = id; }
        public int getUserId() { return userId; }
        public int getCategoryId() { return categoryId; }
        public String getCategoryName() { return categoryName; } public void setCategoryName(String n) { this.categoryName = n; }
        public double getLimitAmount() { return limitAmount; }
        public double getSpentAmount() { return spentAmount; } public void setSpentAmount(double s) { this.spentAmount = s; }
        public String getMonth() { return month; }
    }

    // Interface — Abstraction
    interface Exportable {
        void exportToCSV(String filePath) throws Exception;
    }

    // ══════════════════════════════════════════
    //  DATABASE LAYER (JDBC + SQLite)
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
            } catch (SQLException e) { System.err.println("DB Error: " + e.getMessage()); }
        }

        public static DatabaseHelper getInstance() {
            if (instance == null) instance = new DatabaseHelper();
            return instance;
        }

        private void createTables() throws SQLException {
            Statement s = conn.createStatement();
            s.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT UNIQUE, password TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, color TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, amount REAL, description TEXT, date TEXT, user_id INTEGER, category_id INTEGER, note TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS budgets (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, category_id INTEGER, limit_amount REAL, month TEXT)");
            s.close();
        }

        private void insertDefaultCategories() throws SQLException {
            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM categories");
            if (rs.getInt(1) > 0) return;
            String sql = "INSERT INTO categories (name, color) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            String[][] cats = {{"Food","#E24B4A"},{"Transport","#1D9E75"},{"Shopping","#EF9F27"},
                               {"Bills","#378ADD"},{"Health","#D4537E"},{"Entertainment","#7F77DD"},
                               {"Salary","#3B6D11"},{"Other","#888780"}};
            for (String[] c : cats) { ps.setString(1, c[0]); ps.setString(2, c[1]); ps.executeUpdate(); }
            ps.close();
        }

        public boolean registerUser(User u) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO users (name, email, password) VALUES (?,?,?)")) {
                ps.setString(1, u.getName()); ps.setString(2, u.getEmail()); ps.setString(3, u.getPassword());
                ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public User loginUser(String email, String password) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email=? AND password=?")) {
                ps.setString(1, email); ps.setString(2, password);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    User u = new User(rs.getString("name"), rs.getString("email"), rs.getString("password"));
                    u.setId(rs.getInt("id")); return u;
                }
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return null;
        }

        public boolean addTransaction(Transaction t) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO transactions (type,amount,description,date,user_id,category_id,note) VALUES (?,?,?,?,?,?,?)")) {
                ps.setString(1, t.getType()); ps.setDouble(2, t.getAmount());
                ps.setString(3, t.getDescription()); ps.setString(4, t.getDate().toString());
                ps.setInt(5, t.getUserId()); ps.setInt(6, t.getCategoryId()); ps.setString(7, t.getNote());
                ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public boolean deleteTransaction(int id) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE id=?")) {
                ps.setInt(1, id); ps.executeUpdate(); return true;
            } catch (SQLException e) { return false; }
        }

        public List<Transaction> getTransactionsByUser(int userId) {
            List<Transaction> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM transactions WHERE user_id=? ORDER BY date DESC")) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapTx(rs));
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return list;
        }

        public double getTotalByTypeAndMonth(int userId, String type, String month) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT SUM(amount) FROM transactions WHERE user_id=? AND type=? AND strftime('%Y-%m',date)=?")) {
                ps.setInt(1, userId); ps.setString(2, type); ps.setString(3, month);
                ResultSet rs = ps.executeQuery(); return rs.getDouble(1);
            } catch (SQLException e) { return 0; }
        }

        public double getSpentByCategory(int userId, int catId, String month) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT SUM(amount) FROM transactions WHERE user_id=? AND category_id=? AND type='EXPENSE' AND strftime('%Y-%m',date)=?")) {
                ps.setInt(1, userId); ps.setInt(2, catId); ps.setString(3, month);
                ResultSet rs = ps.executeQuery(); return rs.getDouble(1);
            } catch (SQLException e) { return 0; }
        }

        public List<Category> getAllCategories() {
            List<Category> list = new ArrayList<>();
            try (Statement s = conn.createStatement()) {
                ResultSet rs = s.executeQuery("SELECT * FROM categories");
                while (rs.next()) {
                    Category c = new Category(rs.getString("name"), rs.getString("color"));
                    c.setId(rs.getInt("id")); list.add(c);
                }
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return list;
        }

        public String getCategoryName(int id) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM categories WHERE id=?")) {
                ps.setInt(1, id); ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getString("name");
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return "Other";
        }

        public boolean saveBudget(Budget b) {
            try {
                PreparedStatement check = conn.prepareStatement("SELECT id FROM budgets WHERE user_id=? AND category_id=? AND month=?");
                check.setInt(1, b.getUserId()); check.setInt(2, b.getCategoryId()); check.setString(3, b.getMonth());
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    PreparedStatement upd = conn.prepareStatement("UPDATE budgets SET limit_amount=? WHERE id=?");
                    upd.setDouble(1, b.getLimitAmount()); upd.setInt(2, rs.getInt("id")); upd.executeUpdate();
                } else {
                    PreparedStatement ins = conn.prepareStatement("INSERT INTO budgets (user_id,category_id,limit_amount,month) VALUES (?,?,?,?)");
                    ins.setInt(1, b.getUserId()); ins.setInt(2, b.getCategoryId());
                    ins.setDouble(3, b.getLimitAmount()); ins.setString(4, b.getMonth()); ins.executeUpdate();
                }
                return true;
            } catch (SQLException e) { return false; }
        }

        public List<Budget> getBudgetsByMonth(int userId, String month) {
            List<Budget> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT b.*, c.name as cat_name FROM budgets b JOIN categories c ON b.category_id=c.id WHERE b.user_id=? AND b.month=?")) {
                ps.setInt(1, userId); ps.setString(2, month);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Budget bg = new Budget(userId, rs.getInt("category_id"), rs.getDouble("limit_amount"), month);
                    bg.setId(rs.getInt("id")); bg.setCategoryName(rs.getString("cat_name"));
                    bg.setSpentAmount(getSpentByCategory(userId, rs.getInt("category_id"), month));
                    list.add(bg);
                }
            } catch (SQLException e) { System.err.println(e.getMessage()); }
            return list;
        }

        private Transaction mapTx(ResultSet rs) throws SQLException {
            String type = rs.getString("type");
            double amt = rs.getDouble("amount");
            String desc = rs.getString("description");
            LocalDate date = LocalDate.parse(rs.getString("date"));
            int uid = rs.getInt("user_id"), cid = rs.getInt("category_id");
            String note = rs.getString("note");
            Transaction t = "INCOME".equals(type)
                ? new Income(amt, desc, date, uid, cid, note)
                : new Expense(amt, desc, date, uid, cid, note);
            t.setId(rs.getInt("id"));
            return t;
        }
    }

    // ══════════════════════════════════════════
    //  SESSION MANAGER (Singleton)
    // ══════════════════════════════════════════

    static class SessionManager {
        private static SessionManager instance;
        private User currentUser;
        private SessionManager() {}
        public static SessionManager getInstance() {
            if (instance == null) instance = new SessionManager();
            return instance;
        }
        public void login(User u) { currentUser = u; }
        public void logout() { currentUser = null; }
        public User getCurrentUser() { return currentUser; }
        public int getCurrentUserId() { return currentUser != null ? currentUser.getId() : -1; }
    }

    // ══════════════════════════════════════════
    //  REPORT GENERATOR (implements Exportable)
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
                    w.write(String.join(",",
                        String.valueOf(t.getId()), t.getType(), csv(t.getDescription()),
                        String.format("%.2f", t.getAmount()), csv(db.getCategoryName(t.getCategoryId())),
                        t.getDate().toString(), csv(t.getNote())));
                    w.newLine();
                }
            } catch (IOException e) { throw new Exception("Export failed: " + e.getMessage()); }
        }

        private String csv(String v) {
            if (v == null) return "";
            if (v.contains(",") || v.contains("\"")) v = "\"" + v.replace("\"", "\"\"") + "\"";
            return v;
        }
    }

    // ══════════════════════════════════════════
    //  HELPER STYLES
    // ══════════════════════════════════════════

    static String inputStyle() {
        return "-fx-background-color:#f9f9f9;-fx-border-color:#ddd;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8 12;-fx-font-size:13;";
    }
    static String primaryBtn(String color) {
        return "-fx-background-color:" + color + ";-fx-text-fill:white;-fx-font-size:14;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:10 0;-fx-cursor:hand;";
    }
    static String navStyle() { return "-fx-background-color:transparent;-fx-text-fill:#555;-fx-font-size:13;-fx-background-radius:8;-fx-cursor:hand;"; }
    static String activeNavStyle() { return "-fx-background-color:#E1F5EE;-fx-text-fill:#0F6E56;-fx-font-size:13;-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;"; }

    static HBox topBar(String title) {
        HBox bar = new HBox();
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#1D9E75;");
        Label lbl = new Label(title);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lbl.setTextFill(Color.WHITE);
        bar.getChildren().add(lbl);
        return bar;
    }

    static VBox sideNav(Stage stage, String active) {
        VBox nav = new VBox(4);
        nav.setPadding(new Insets(16, 8, 16, 8));
        nav.setStyle("-fx-background-color:#ffffff;-fx-border-color:#dddddd;-fx-border-width:0 1 0 0;");
        nav.setPrefWidth(160);
        String[] labels = {"Dashboard", "Add Expense", "Add Income", "Budget", "Reports"};
        for (String label : labels) {
            Button btn = new Button(label);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPadding(new Insets(10, 14, 10, 14));
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setStyle(label.equals(active) ? activeNavStyle() : navStyle());
            btn.setOnAction(e -> navigate(stage, label));
            nav.getChildren().add(btn);
        }
        return nav;
    }

    static void navigate(Stage stage, String label) {
        switch (label) {
            case "Dashboard"   -> showDashboard(stage);
            case "Add Expense" -> showAddTransaction(stage, "EXPENSE");
            case "Add Income"  -> showAddTransaction(stage, "INCOME");
            case "Budget"      -> showBudget(stage);
            case "Reports"     -> showReports(stage);
        }
    }

    static String currentMonth() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    // ══════════════════════════════════════════
    //  SCREEN: LOGIN
    // ══════════════════════════════════════════

    static void showLogin(Stage stage) {
        stage.setTitle("FinTrack — Login");
        stage.setWidth(420); stage.setHeight(520); stage.setResizable(false);

        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color:#f5f5f5;");

        Label title = new Label("FinTrack");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#1D9E75"));

        Label sub = new Label("Personal Finance Tracker");
        sub.setFont(Font.font("Arial", 13)); sub.setTextFill(Color.GRAY);

        VBox card = new VBox(12);
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color:#ffffff;-fx-background-radius:12;-fx-border-color:#dddddd;-fx-border-radius:12;");

        Label formTitle = new Label("Sign In");
        formTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        formTitle.setStyle("-fx-text-fill:#1a1a1a;");
        formTitle.setStyle("-fx-text-fill:#1a1a1a;");

        TextField emailF = new TextField(); emailF.setPromptText("Email"); emailF.setStyle(inputStyle());
        PasswordField passF = new PasswordField(); passF.setPromptText("Password"); passF.setStyle(inputStyle());

        Button loginBtn = new Button("Sign In");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle(primaryBtn("#1D9E75"));

        Label errLbl = new Label(); errLbl.setTextFill(Color.RED); errLbl.setFont(Font.font("Arial", 12));

        Button regBtn = new Button("Create New Account");
        regBtn.setMaxWidth(Double.MAX_VALUE);
        regBtn.setStyle("-fx-background-color:#ffffff;-fx-text-fill:#1D9E75;-fx-font-size:13;-fx-border-color:#1D9E75;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:8 0;-fx-cursor:hand;");

        card.getChildren().addAll(formTitle, emailF, passF, loginBtn, errLbl, new Separator(), regBtn);

        loginBtn.setOnAction(e -> {
            User u = DatabaseHelper.getInstance().loginUser(emailF.getText().trim(), passF.getText());
            if (u != null) { SessionManager.getInstance().login(u); showDashboard(stage); }
            else errLbl.setText("Invalid email or password.");
        });

        regBtn.setOnAction(e -> showRegister(stage));
        root.getChildren().addAll(title, sub, card);
        stage.setScene(new Scene(root));
        stage.show();
    }

    static void showRegister(Stage stage) {
        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color:#f5f5f5;");

        Label title = new Label("FinTrack");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#1D9E75"));

        VBox card = new VBox(12);
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color:#ffffff;-fx-background-radius:12;-fx-border-color:#dddddd;-fx-border-radius:12;");

        Label formTitle = new Label("Create Account");
        formTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        formTitle.setStyle("-fx-text-fill:#1a1a1a;");
        formTitle.setStyle("-fx-text-fill:#1a1a1a;");

        TextField nameF = new TextField(); nameF.setPromptText("Full Name"); nameF.setStyle(inputStyle());
        TextField emailF = new TextField(); emailF.setPromptText("Email"); emailF.setStyle(inputStyle());
        PasswordField passF = new PasswordField(); passF.setPromptText("Password (min 4 chars)"); passF.setStyle(inputStyle());

        Button regBtn = new Button("Register");
        regBtn.setMaxWidth(Double.MAX_VALUE);
        regBtn.setStyle(primaryBtn("#1D9E75"));

        Label msg = new Label(); msg.setFont(Font.font("Arial", 12));

        Button backBtn = new Button("Back to Login");
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setStyle("-fx-background-color:#ffffff;-fx-text-fill:#1D9E75;-fx-border-color:#1D9E75;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:8 0;-fx-cursor:hand;");

        card.getChildren().addAll(formTitle, nameF, emailF, passF, regBtn, msg, new Separator(), backBtn);

        regBtn.setOnAction(e -> {
            String name = nameF.getText().trim(), email = emailF.getText().trim(), pass = passF.getText();
            if (name.isEmpty() || email.isEmpty() || pass.length() < 4) {
                msg.setTextFill(Color.RED); msg.setText("Fill all fields. Password min 4 chars."); return;
            }
            User u = new User(name, email, pass);
            if (DatabaseHelper.getInstance().registerUser(u)) {
                msg.setTextFill(Color.web("#1D9E75")); msg.setText("Account created! Please login.");
            } else {
                msg.setTextFill(Color.RED); msg.setText("Email already exists.");
            }
        });

        backBtn.setOnAction(e -> showLogin(stage));
        root.getChildren().addAll(title, card);
        stage.setScene(new Scene(root));
    }

    // ══════════════════════════════════════════
    //  SCREEN: DASHBOARD
    // ══════════════════════════════════════════

    static void showDashboard(Stage stage) {
        stage.setTitle("FinTrack — Dashboard");
        if (stage.getWidth() < 800) { stage.setWidth(900); stage.setHeight(680); }
        stage.setResizable(true);

        DatabaseHelper db = DatabaseHelper.getInstance();
        SessionManager session = SessionManager.getInstance();
        int uid = session.getCurrentUserId();
        String month = currentMonth();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#f5f5f5;");

        // Top bar with logout
        HBox bar = new HBox();
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#1D9E75;");
        Label appName = new Label("FinTrack");
        appName.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        appName.setTextFill(Color.WHITE);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label userLbl = new Label("Hi, " + session.getCurrentUser().getName());
        userLbl.setFont(Font.font("Arial", 13)); userLbl.setTextFill(Color.web("#c8f5e8"));
        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:white;-fx-border-color:rgba(255,255,255,0.5);-fx-border-radius:6;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:4 12;");
        logoutBtn.setOnAction(e -> { session.logout(); showLogin(stage); });
        bar.getChildren().addAll(appName, spacer, userLbl, new Label("  "), logoutBtn);
        root.setTop(bar);
        root.setLeft(sideNav(stage, "Dashboard"));

        // Content
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Stat cards
        double expense = db.getTotalByTypeAndMonth(uid, "EXPENSE", month);
        double income  = db.getTotalByTypeAndMonth(uid, "INCOME",  month);
        double balance = income - expense;
        HBox stats = new HBox(12);
        stats.getChildren().addAll(
            statCard("This Month Spent", "₹" + String.format("%.0f", expense), "#A32D2D"),
            statCard("Income",           "₹" + String.format("%.0f", income),  "#0F6E56"),
            statCard("Balance",          "₹" + String.format("%.0f", balance), balance >= 0 ? "#0F6E56" : "#A32D2D")
        );
        content.getChildren().add(stats);

        // Bar chart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount (₹)");
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Last 6 Months Spending");
        barChart.setLegendVisible(false);
        barChart.setPrefHeight(260);
        barChart.setBarGap(6);
        barChart.setCategoryGap(30);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Expenses");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yy");
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            String mk = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            double amt = db.getTotalByTypeAndMonth(uid, "EXPENSE", mk);
            series.getData().add(new XYChart.Data<>(ym.format(fmt), amt == 0 ? 0 : amt));
        }
        barChart.getData().add(series);
        // Apply green color to bars after data is added
        javafx.application.Platform.runLater(() ->
            barChart.lookupAll(".bar").forEach(node ->
                node.setStyle("-fx-bar-fill:#1D9E75; -fx-background-radius:4;")
            )
        );
        VBox chartBox = new VBox(barChart);
        chartBox.setPadding(new Insets(8));
        chartBox.setStyle("-fx-background-color:#ffffff;-fx-background-radius:10;-fx-border-color:#dddddd;-fx-border-radius:10;-fx-border-width:1;");
        content.getChildren().add(chartBox);

        // Budget alerts
        VBox budgetBox = new VBox(10);
        budgetBox.setPadding(new Insets(16));
        budgetBox.setStyle("-fx-background-color:#ffffff;-fx-background-radius:10;-fx-border-color:#dddddd;-fx-border-radius:10;-fx-border-width:1;");
        Label bTitle = new Label("Budget Alerts — " + month);
        bTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        bTitle.setStyle("-fx-text-fill:#1a1a1a;");
        budgetBox.getChildren().add(bTitle);
        List<Budget> budgets = db.getBudgetsByMonth(uid, month);
        if (budgets.isEmpty()) {
            Label noB = new Label("No budgets set. Go to Budget tab to add limits.");
            noB.setStyle("-fx-text-fill:#666666;-fx-font-size:13;");
            budgetBox.getChildren().add(noB);
        } else {
            for (Budget b : budgets) {
                VBox bRow = new VBox(5);
                bRow.setPadding(new Insets(6, 0, 6, 0));
                bRow.setStyle("-fx-border-color:transparent transparent #eeeeee transparent;-fx-border-width:0 0 1 0;");

                // Top row: name + percentage
                HBox topRow = new HBox();
                topRow.setAlignment(Pos.CENTER_LEFT);
                Label nm = new Label(b.getCategoryName());
                nm.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#333333;");
                Region sp3 = new Region(); HBox.setHgrow(sp3, Priority.ALWAYS);
                String bc = b.isExceeded() ? "#E24B4A" : b.isWarning() ? "#EF9F27" : "#1D9E75";
                Label pctLbl = new Label(String.format("%.0f%%", b.getUsagePercent()));
                pctLbl.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:" + bc + ";");
                topRow.getChildren().addAll(nm, sp3, pctLbl);

                // Progress bar
                ProgressBar pb = new ProgressBar(Math.min(b.getUsagePercent() / 100, 1.0));
                pb.setMaxWidth(Double.MAX_VALUE); pb.setPrefHeight(10);
                pb.setStyle("-fx-accent:" + bc + ";");

                // Bottom row: spent / limit
                Label detail = new Label(String.format("Spent ₹%.0f  of  ₹%.0f limit  —  ₹%.0f remaining",
                    b.getSpentAmount(), b.getLimitAmount(),
                    Math.max(0, b.getLimitAmount() - b.getSpentAmount())));
                detail.setStyle("-fx-font-size:11;-fx-text-fill:#888888;");

                bRow.getChildren().addAll(topRow, pb, detail);
                budgetBox.getChildren().add(bRow);
            }
        }
        content.getChildren().add(budgetBox);

        // Recent transactions table
        content.getChildren().add(buildTxTable(stage, db, uid, true));

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        root.setCenter(sp);
        stage.setScene(new Scene(root));
    }

    @SuppressWarnings("unchecked")
    static VBox buildTxTable(Stage stage, DatabaseHelper db, int uid, boolean limited) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(16));
        section.setStyle("-fx-background-color:#ffffff;-fx-background-radius:10;-fx-border-color:#dddddd;-fx-border-radius:10;");

        Label title = new Label(limited ? "Recent Transactions" : "All Transactions");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        title.setStyle("-fx-text-fill:#1a1a1a;");
        title.setStyle("-fx-text-fill:#1a1a1a;");

        TableView<Transaction> table = new TableView<>();
        table.setPrefHeight(limited ? 200 : 300);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
        typeCol.setPrefWidth(80);

        TableColumn<Transaction, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(180);

        TableColumn<Transaction, String> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDisplayAmount()));
        amtCol.setPrefWidth(120);

        TableColumn<Transaction, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(db.getCategoryName(d.getValue().getCategoryId())));
        catCol.setPrefWidth(100);

        TableColumn<Transaction, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate().toString()));
        dateCol.setPrefWidth(100);

        table.getColumns().addAll(typeCol, descCol, amtCol, catCol, dateCol);

        List<Transaction> all = db.getTransactionsByUser(uid);
        List<Transaction> shown = limited ? all.subList(0, Math.min(8, all.size())) : all;
        table.setItems(FXCollections.observableArrayList(shown));

        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) setStyle("");
                else if ("INCOME".equals(item.getType())) setStyle("-fx-background-color:#f0faf5;");
                else setStyle("-fx-background-color:#fff8f8;");
            }
        });

        // Delete button column
        TableColumn<Transaction, Void> delCol = new TableColumn<>("Action");
        delCol.setPrefWidth(80);
        delCol.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("Delete");
            { btn.setStyle("-fx-background-color:#FCEBEB;-fx-text-fill:#A32D2D;-fx-font-size:11;-fx-background-radius:6;-fx-cursor:hand;"); }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                btn.setOnAction(e -> {
                    Transaction t = getTableView().getItems().get(getIndex());
                    db.deleteTransaction(t.getId());
                    showDashboard(stage);
                });
                setGraphic(btn);
            }
        });
        table.getColumns().add(delCol);

        section.getChildren().addAll(title, table);
        return section;
    }

    static VBox statCard(String label, String value, String color) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:#ffffff;-fx-background-radius:10;-fx-border-color:#dddddd;-fx-border-radius:10;-fx-border-width:1;");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        lbl.setStyle("-fx-text-fill:#888888;");
        Label val = new Label(value);
        val.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        val.setStyle("-fx-text-fill:" + color + ";");
        card.getChildren().addAll(lbl, val);
        return card;
    }

    // ══════════════════════════════════════════
    //  SCREEN: ADD TRANSACTION
    // ══════════════════════════════════════════

    static void showAddTransaction(Stage stage, String type) {
        stage.setTitle("FinTrack — Add " + capitalize(type));
        DatabaseHelper db = DatabaseHelper.getInstance();
        SessionManager session = SessionManager.getInstance();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#f5f5f5;");
        root.setTop(topBar("FinTrack"));
        root.setLeft(sideNav(stage, "Add " + capitalize(type)));

        VBox form = new VBox(14); form.setPadding(new Insets(28)); form.setMaxWidth(500);
        String hcolor = "EXPENSE".equals(type) ? "#A32D2D" : "#0F6E56";
        Label heading = new Label("Add " + capitalize(type));
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 20)); heading.setTextFill(Color.web(hcolor));

        VBox card = new VBox(12); card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color:#ffffff;-fx-background-radius:12;-fx-border-color:#dddddd;-fx-border-radius:12;");

        TextField amtF = new TextField(); amtF.setPromptText("Amount (₹)"); amtF.setStyle(inputStyle());
        TextField descF = new TextField(); descF.setPromptText("Description"); descF.setStyle(inputStyle());
        ComboBox<Category> catBox = new ComboBox<>();
        catBox.getItems().addAll(db.getAllCategories());
        catBox.setMaxWidth(Double.MAX_VALUE);
        if (!catBox.getItems().isEmpty()) catBox.getSelectionModel().selectFirst();
        DatePicker datePicker = new DatePicker(LocalDate.now()); datePicker.setMaxWidth(Double.MAX_VALUE);
        TextField noteF = new TextField(); noteF.setPromptText("Note (optional)"); noteF.setStyle(inputStyle());

        String btnColor = "EXPENSE".equals(type) ? "#E24B4A" : "#1D9E75";
        Button saveBtn = new Button("Save " + capitalize(type));
        saveBtn.setMaxWidth(Double.MAX_VALUE); saveBtn.setStyle(primaryBtn(btnColor));

        Label msg = new Label(); msg.setFont(Font.font("Arial", 12));

        saveBtn.setOnAction(e -> {
            try {
                double amount = Double.parseDouble(amtF.getText().trim());
                String desc = descF.getText().trim();
                Category cat = catBox.getValue();
                LocalDate date = datePicker.getValue();
                if (desc.isEmpty() || cat == null) {
                    msg.setTextFill(Color.RED); msg.setText("Fill all fields."); return;
                }
                Transaction t = "INCOME".equals(type)
                    ? new Income(amount, desc, date, session.getCurrentUserId(), cat.getId(), noteF.getText())
                    : new Expense(amount, desc, date, session.getCurrentUserId(), cat.getId(), noteF.getText());
                if (db.addTransaction(t)) {
                    msg.setTextFill(Color.web("#1D9E75")); msg.setText("Saved successfully!");
                    amtF.clear(); descF.clear(); noteF.clear(); datePicker.setValue(LocalDate.now());
                } else { msg.setTextFill(Color.RED); msg.setText("Failed to save."); }
            } catch (NumberFormatException ex) { msg.setTextFill(Color.RED); msg.setText("Enter a valid amount."); }
        });

        card.getChildren().addAll(
            fLabel("Amount (₹)"), amtF, fLabel("Description"), descF,
            fLabel("Category"), catBox, fLabel("Date"), datePicker,
            fLabel("Note (optional)"), noteF, saveBtn, msg
        );
        form.getChildren().addAll(heading, card);
        ScrollPane sp = new ScrollPane(form); sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        root.setCenter(sp);
        stage.setScene(new Scene(root));
    }

    // ══════════════════════════════════════════
    //  SCREEN: BUDGET
    // ══════════════════════════════════════════

    static void showBudget(Stage stage) {
        stage.setTitle("FinTrack — Budget");
        DatabaseHelper db = DatabaseHelper.getInstance();
        int uid = SessionManager.getInstance().getCurrentUserId();
        String month = currentMonth();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#f5f5f5;");
        root.setTop(topBar("FinTrack"));
        root.setLeft(sideNav(stage, "Budget"));

        VBox content = new VBox(20); content.setPadding(new Insets(28));
        Label heading = new Label("Budget Manager — " + month);
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        heading.setStyle("-fx-text-fill:#1a1a1a;");

        // Set budget card
        VBox setCard = new VBox(12); setCard.setPadding(new Insets(20));
        setCard.setStyle("-fx-background-color:#ffffff;-fx-background-radius:12;-fx-border-color:#dddddd;-fx-border-radius:12;");
        Label setTitle = new Label("Set Monthly Budget Limit");
        setTitle.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        setTitle.setStyle("-fx-text-fill:#1a1a1a;");
        ComboBox<Category> catBox = new ComboBox<>();
        catBox.getItems().addAll(db.getAllCategories());
        catBox.setPromptText("Select category"); catBox.setMaxWidth(Double.MAX_VALUE);
        TextField limitF = new TextField(); limitF.setPromptText("Limit amount (₹)"); limitF.setStyle(inputStyle());
        Button setBtn = new Button("Set Budget");
        setBtn.setStyle("-fx-background-color:#1D9E75;-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 20;-fx-cursor:hand;");
        Label msg = new Label(); msg.setFont(Font.font("Arial", 12));
        setBtn.setOnAction(e -> {
            try {
                Category cat = catBox.getValue();
                double limit = Double.parseDouble(limitF.getText().trim());
                if (cat == null) { msg.setTextFill(Color.RED); msg.setText("Select a category."); return; }
                Budget b = new Budget(uid, cat.getId(), limit, month);
                if (db.saveBudget(b)) {
                    msg.setTextFill(Color.web("#1D9E75")); msg.setText("Budget saved for " + cat.getName() + "!");
                    limitF.clear(); showBudget(stage);
                } else { msg.setTextFill(Color.RED); msg.setText("Failed."); }
            } catch (NumberFormatException ex) { msg.setTextFill(Color.RED); msg.setText("Enter a valid number."); }
        });
        setCard.getChildren().addAll(setTitle, fLabel("Category"), catBox, fLabel("Monthly Limit (₹)"), limitF, setBtn, msg);

        // Current budgets
        VBox curCard = new VBox(12); curCard.setPadding(new Insets(20));
        curCard.setStyle("-fx-background-color:#ffffff;-fx-background-radius:12;-fx-border-color:#dddddd;-fx-border-radius:12;");
        Label curTitle = new Label("Current Budgets"); curTitle.setFont(Font.font("Arial", FontWeight.BOLD, 15)); curTitle.setStyle("-fx-text-fill:#1a1a1a;");
        curCard.getChildren().add(curTitle);
        List<Budget> budgets = db.getBudgetsByMonth(uid, month);
        if (budgets.isEmpty()) {
            curCard.getChildren().add(new Label("No budgets set for this month."));
        } else {
            for (Budget b : budgets) {
                VBox row = new VBox(4);
                row.setPadding(new Insets(8, 0, 8, 0));
                row.setStyle("-fx-border-color:transparent transparent #eee transparent;-fx-border-width:0 0 1 0;");
                HBox top = new HBox(); top.setAlignment(Pos.CENTER_LEFT);
                Label nm = new Label(b.getCategoryName()); nm.setFont(Font.font("Arial", FontWeight.BOLD, 13)); nm.setStyle("-fx-text-fill:#1a1a1a;");
                Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
                String sc = b.isExceeded() ? "#E24B4A" : b.isWarning() ? "#EF9F27" : "#1D9E75";
                Label pct = new Label(String.format("%.0f%%", b.getUsagePercent()));
                pct.setFont(Font.font("Arial", FontWeight.BOLD, 13)); pct.setTextFill(Color.web(sc));
                top.getChildren().addAll(nm, sp2, pct);
                ProgressBar pb = new ProgressBar(Math.min(b.getUsagePercent() / 100, 1.0));
                pb.setMaxWidth(Double.MAX_VALUE); pb.setPrefHeight(10); pb.setStyle("-fx-accent:" + sc + ";");
                Label detail = new Label(String.format("Spent: ₹%.0f  /  Limit: ₹%.0f  /  Remaining: ₹%.0f",
                    b.getSpentAmount(), b.getLimitAmount(), Math.max(0, b.getLimitAmount() - b.getSpentAmount())));
                detail.setFont(Font.font("Arial", 11)); detail.setTextFill(Color.GRAY);
                row.getChildren().addAll(top, pb, detail);
                curCard.getChildren().add(row);
            }
        }
        content.getChildren().addAll(heading, setCard, curCard);
        ScrollPane sp = new ScrollPane(content); sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        root.setCenter(sp);
        stage.setScene(new Scene(root));
    }

    // ══════════════════════════════════════════
    //  SCREEN: REPORTS
    // ══════════════════════════════════════════

    static void showReports(Stage stage) {
        stage.setTitle("FinTrack — Reports");
        DatabaseHelper db = DatabaseHelper.getInstance();
        int uid = SessionManager.getInstance().getCurrentUserId();
        String month = currentMonth();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#f5f5f5;");
        root.setTop(topBar("FinTrack"));
        root.setLeft(sideNav(stage, "Reports"));

        VBox content = new VBox(20); content.setPadding(new Insets(28));
        Label heading = new Label("Reports — " + month);
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        heading.setStyle("-fx-text-fill:#1a1a1a;");

        // Summary cards
        double expense = db.getTotalByTypeAndMonth(uid, "EXPENSE", month);
        double income  = db.getTotalByTypeAndMonth(uid, "INCOME",  month);
        HBox summary = new HBox(12);
        summary.getChildren().addAll(
            statCard("Total Spent",   "₹" + String.format("%.2f", expense), "#A32D2D"),
            statCard("Total Income",  "₹" + String.format("%.2f", income),  "#0F6E56"),
            statCard("Net Balance",   "₹" + String.format("%.2f", income - expense), income >= expense ? "#0F6E56" : "#A32D2D"),
            statCard("Transactions",  String.valueOf(db.getTransactionsByUser(uid).size()), "#185FA5")
        );

        // Pie chart
        PieChart pie = new PieChart();
        pie.setTitle("Spending by Category — " + month);
        pie.setPrefHeight(280);
        List<Category> cats = db.getAllCategories();
        boolean hasData = false;
        for (Category c : cats) {
            double spent = db.getSpentByCategory(uid, c.getId(), month);
            if (spent > 0) { pie.getData().add(new PieChart.Data(c.getName() + " ₹" + String.format("%.0f", spent), spent)); hasData = true; }
        }
        if (!hasData) pie.getData().add(new PieChart.Data("No expenses", 1));
        VBox pieBox = new VBox(pie);
        pieBox.setStyle("-fx-background-color:#ffffff;-fx-background-radius:10;-fx-border-color:#dddddd;-fx-border-radius:10;");

        // Full transaction table
        VBox tableSection = buildTxTable(stage, db, uid, false);

        // Export section
        VBox exportCard = new VBox(12); exportCard.setPadding(new Insets(20));
        exportCard.setStyle("-fx-background-color:#ffffff;-fx-background-radius:10;-fx-border-color:#dddddd;-fx-border-radius:10;");
        Label expTitle = new Label("Export Data"); expTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14)); expTitle.setStyle("-fx-text-fill:#1a1a1a;");
        Label expDesc = new Label("Export all transactions to a CSV file (opens in Excel / Google Sheets).");
        expDesc.setFont(Font.font("Arial", 12)); expDesc.setTextFill(Color.GRAY); expDesc.setWrapText(true);
        Button expBtn = new Button("Export as CSV");
        expBtn.setStyle("-fx-background-color:#185FA5;-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:10 24;-fx-cursor:hand;");
        Label expMsg = new Label(); expMsg.setFont(Font.font("Arial", 12));
        expBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Save CSV Report");
            fc.setInitialFileName("fintrack_report.csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            java.io.File file = fc.showSaveDialog(stage);
            if (file != null) {
                try {
                    new ReportGenerator(db.getTransactionsByUser(uid)).exportToCSV(file.getAbsolutePath());
                    expMsg.setTextFill(Color.web("#1D9E75")); expMsg.setText("Exported: " + file.getName());
                } catch (Exception ex) { expMsg.setTextFill(Color.RED); expMsg.setText("Failed: " + ex.getMessage()); }
            }
        });
        exportCard.getChildren().addAll(expTitle, expDesc, expBtn, expMsg);

        content.getChildren().addAll(heading, summary, pieBox, tableSection, exportCard);
        ScrollPane sp = new ScrollPane(content); sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        root.setCenter(sp);
        stage.setScene(new Scene(root));
    }

    // ══════════════════════════════════════════
    //  UTILS
    // ══════════════════════════════════════════

    static Label fLabel(String text) {
        Label l = new Label(text); l.setFont(Font.font("Arial", 12)); l.setStyle("-fx-text-fill:#666666;"); return l;
    }

    static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    // ══════════════════════════════════════════
    //  ENTRY POINT
    // ══════════════════════════════════════════

    @Override
    public void start(Stage primaryStage) {
        showLogin(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
