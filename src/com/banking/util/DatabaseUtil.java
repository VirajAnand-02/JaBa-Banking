package com.banking.util;

import com.banking.model.User;

import java.sql.*;
import java.io.File;
import java.util.Random;

public class DatabaseUtil {
    private static final String DB_RELATIVE_PATH = "banking.db";
    private static String DB_URL = null; // Will be set during initialization
    private static boolean initialized = false;
    private static final Object initLock = new Object(); // Lock for thread-safe initialization

    public static void setDatabasePath(String absolutePath) {
        DB_URL = "jdbc:sqlite:" + absolutePath;
        System.out.println("Database path set to: " + DB_URL);
        // Reset initialized flag if path changes after initial use (though typically
        // set once)
        initialized = false;
    }

    private static void ensureDbUrlIsSet() {
        if (DB_URL == null) {
            // Default behavior: use relative path
            // WARNING: In a web app, this might be relative to Tomcat's working dir (often
            // /bin)
            // It's better to set an absolute path using setDatabasePath() from context
            // listener
            File dbFile = new File(DB_RELATIVE_PATH);
            DB_URL = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            System.out.println("Defaulting DB path to: " + DB_URL);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            initializeDatabase(); // This will ensure DB_URL is set
        }
        if (DB_URL == null) {
            throw new SQLException("Database URL has not been initialized.");
        }
        return DriverManager.getConnection(DB_URL);
    }

    private static void initializeDatabase() {
        // Double-checked locking for thread safety
        if (!initialized) {
            synchronized (initLock) {
                if (!initialized) {
                    try {
                        ensureDbUrlIsSet(); // Make sure DB_URL has a value

                        // Ensure SQLite JDBC driver is loaded
                        Class.forName("org.sqlite.JDBC");

                        // Check if database file exists using the determined URL/path
                        String dbFilePath = DB_URL.substring("jdbc:sqlite:".length());
                        File dbFile = new File(dbFilePath);
                        boolean dbExists = dbFile.exists() && dbFile.length() > 0;

                        // Ensure parent directory exists if DB doesn't
                        if (!dbExists && dbFile.getParentFile() != null) {
                            dbFile.getParentFile().mkdirs();
                        }

                        // Get connection (creates DB if it doesn't exist)
                        try (Connection conn = DriverManager.getConnection(DB_URL)) {

                            System.out.println("Init Database, DB URL: " + DB_URL);
                            createTables(conn);
                            initialized = true;
                            System.out.println("Database initialization complete for: " + DB_URL);
                        }
                    } catch (Exception e) {
                        System.err.println("FATAL: Database initialization failed for URL: "
                                + (DB_URL != null ? DB_URL : "Not Set") + " - " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Create Users table
            System.out.println("Creating users table...");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "name TEXT NOT NULL, " +
                            "email TEXT UNIQUE NOT NULL, " +
                            "password_hash TEXT NOT NULL, " +
                            "salt TEXT NOT NULL, " +
                            "role TEXT NOT NULL CHECK(role IN ('admin', 'employee', 'customer')), " +
                            "status TEXT DEFAULT 'active' CHECK(status IN ('active', 'inactive', 'locked')), " +
                            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                            ")");

            // Create Accounts table
            System.out.println("Creating accounts table...");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS accounts (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "user_id INTEGER NOT NULL, " +
                            "account_number TEXT UNIQUE NOT NULL, " +
                            "type TEXT NOT NULL CHECK(type IN ('checking', 'savings')), " +
                            "balance DECIMAL(15,2) DEFAULT 0.00, " +
                            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                            "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                            ")");

            // Create Transactions table
            System.out.println("Creating transactions table...");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS transactions (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "from_account_id INTEGER, " +
                            "to_account_id INTEGER, " +
                            "type TEXT NOT NULL CHECK(type IN ('deposit', 'withdrawal', 'transfer')), " +
                            "amount DECIMAL(15,2) NOT NULL, " +
                            "description TEXT, " +
                            "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                            "FOREIGN KEY (from_account_id) REFERENCES accounts(id) ON DELETE SET NULL, " +
                            "FOREIGN KEY (to_account_id) REFERENCES accounts(id) ON DELETE SET NULL" +
                            ")");

            // Create Loans table
            System.out.println("Creating loans table...");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS loans (" +
                            "loanid INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "userid INTEGER NOT NULL, " +
                            "amount DECIMAL(15,2) NOT NULL, " +
                            "type TEXT NOT NULL, " +
                            "date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                            "status TEXT DEFAULT 'pending' CHECK(status IN ('pending', 'approved', 'rejected')), " +
                            "adminComment TEXT, " +
                            "FOREIGN KEY (userid) REFERENCES users(id) ON DELETE CASCADE" +
                            ")");

            // Create Flagged Transactions table
            System.out.println("Creating flagged_transactions table...");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS flagged_transactions (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "transaction_id INTEGER NOT NULL, " +
                            "employee_id INTEGER NOT NULL, " +
                            "flag_reason TEXT, " +
                            "flag_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                            "status TEXT DEFAULT 'pending' CHECK(status IN ('pending', 'reviewing', 'resolved')), " +
                            "FOREIGN KEY (transaction_id) REFERENCES transactions(id), " +
                            "FOREIGN KEY (employee_id) REFERENCES users(id)" +
                            ")");

            // Check if admin exists before inserting
            boolean adminExists = false;
            String checkAdminSql = "SELECT 1 FROM users WHERE email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkAdminSql)) {
                checkStmt.setString(1, "admin@jababanking.com");
                try (ResultSet rs = checkStmt.executeQuery()) {
                    adminExists = rs.next();
                }
            }

            if (!adminExists) {
                System.out.println("Creating default admin user...");
                String adminSql = "INSERT INTO users (name, email, password_hash, salt, role, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(adminSql)) {
                    String salt = PasswordUtil.generateSalt();
                    String passwordHash = PasswordUtil.hashPassword("admin123", salt);

                    pstmt.setString(1, "Admin User");
                    pstmt.setString(2, "admin@jababanking.com");
                    pstmt.setString(3, passwordHash);
                    pstmt.setString(4, salt);
                    pstmt.setString(5, "admin");
                    pstmt.setString(6, "active");
                    pstmt.executeUpdate();
                    System.out.println("Default admin user created.");
                }
            } else {
                System.out.println("Admin user already exists. Skipping creation.");
            }

            System.out.println("Database tables checked/created successfully.");
        } catch (SQLException e) {
            System.err.println("Error during table creation: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Registers a new user in the database.
     * Assumes the User object contains name, email, and role. Status defaults to
     * 'active'.
     * The role ('admin', 'employee', 'customer') should be validated before calling
     * this method (e.g., in the Servlet).
     * If the role is 'customer', this will also create checking and savings
     * accounts.
     * 
     * @param user     User object containing details
     * @param password Plain text password for the new user
     * @return true if registration is successful, false otherwise (e.g., email
     *         already exists)
     */
    public static boolean registerUser(User user, String password) {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Insert user
            String sql = "INSERT INTO users (name, email, password_hash, salt, role, status) VALUES (?, ?, ?, ?, ?, ?)";
            int userId = -1;

            try (PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                String salt = PasswordUtil.generateSalt();
                String passwordHash = PasswordUtil.hashPassword(password, salt);

                pstmt.setString(1, user.getName());
                pstmt.setString(2, user.getEmail());
                pstmt.setString(3, passwordHash);
                pstmt.setString(4, salt);
                pstmt.setString(5, user.getRole());
                pstmt.setString(6, user.getRole().equals("customer") ? "inactive" : "active");

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("Creating user failed, no rows affected.");
                }

                // Get the generated user ID
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        userId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }

            // If the user is a customer, create accounts
            if ("customer".equalsIgnoreCase(user.getRole()) && userId > 0) {
                // Create checking account
                createAccount(conn, userId, "checking", 100.00); // Initial balance of $100

                // Create savings account
                createAccount(conn, userId, "savings", 500.00); // Initial balance of $500
            }

            conn.commit(); // Commit transaction
            return true;

        } catch (SQLException e) {
            // Roll back transaction on error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

            if (e.getMessage() != null && e.getMessage().contains("CHECK constraint failed")
                    && e.getMessage().contains("role")) {
                System.err.println("Registration failed: Invalid role specified: '" + user.getRole() +
                        "'. Must be 'admin', 'employee', or 'customer'.");
            } else if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed: users.email")) {
                System.err.println("Registration failed: Email '" + user.getEmail() + "' already exists.");
            } else {
                System.err.println(
                        "Database error during user registration for " + user.getEmail() + ": " + e.getMessage());
                e.printStackTrace();
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Creates a new account for a user
     * 
     * @param conn           Database connection (from transaction)
     * @param userId         User ID to create account for
     * @param type           Account type ("checking" or "savings")
     * @param initialBalance Initial account balance
     * @return true if successful, false otherwise
     * @throws SQLException on database error
     */
    private static boolean createAccount(Connection conn, int userId, String type, double initialBalance)
            throws SQLException {
        String sql = "INSERT INTO accounts (user_id, account_number, type, balance) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String accountNumber = generateAccountNumber(userId, type);

            pstmt.setInt(1, userId);
            pstmt.setString(2, accountNumber);
            pstmt.setString(3, type);
            pstmt.setDouble(4, initialBalance);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Generates a unique account number
     * 
     * @param userId User ID
     * @param type   Account type
     * @return Generated account number
     */
    private static String generateAccountNumber(int userId, String type) {
        // Generate a unique account number based on user ID, type and timestamp
        // Format: XX-YYYYYYY-Z where:
        // XX is account type code (10=checking, 20=savings)
        // YYYYYYY is user ID padded with leading zeros
        // Z is a random digit for additional uniqueness

        String typeCode = "checking".equalsIgnoreCase(type) ? "10" : "20";
        String userPart = String.format("%07d", userId);
        int randomDigit = new Random().nextInt(10);

        return typeCode + "-" + userPart + "-" + randomDigit;
    }

    /**
     * Flag a transaction as suspicious
     * 
     * @param transactionId The ID of the transaction to flag
     * @param employeeId    The ID of the employee flagging the transaction
     * @param reason        The reason for flagging
     * @return true if the operation was successful, false otherwise
     */
    public static boolean flagTransaction(int transactionId, int employeeId, String reason) {
        String sql = "INSERT INTO flagged_transactions (transaction_id, employee_id, flag_reason) " +
                "VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, transactionId);
            pstmt.setInt(2, employeeId);
            pstmt.setString(3, reason);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error flagging transaction ID " + transactionId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if a transaction has already been flagged
     * 
     * @param transactionId The ID of the transaction to check
     * @return true if the transaction is already flagged, false otherwise
     */
    public static boolean isTransactionFlagged(int transactionId) {
        String sql = "SELECT 1 FROM flagged_transactions WHERE transaction_id = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, transactionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Returns true if the transaction is flagged
            }

        } catch (SQLException e) {
            System.err
                    .println("Error checking flag status for transaction ID " + transactionId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all flagged transactions as JSON
     * 
     * @return JSON string containing flagged transaction data
     */
    public static String getFlaggedTransactionsAsJson() {
        StringBuilder jsonBuilder = new StringBuilder("[");

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT ft.id, ft.transaction_id, ft.employee_id, ft.flag_reason, ft.flag_date, ft.status, " +
                                "t.amount, t.description, t.type, t.timestamp, " +
                                "e.name as employee_name " +
                                "FROM flagged_transactions ft " +
                                "JOIN transactions t ON ft.transaction_id = t.id " +
                                "JOIN users e ON ft.employee_id = e.id " +
                                "ORDER BY ft.flag_date DESC")) {

            try (ResultSet rs = pstmt.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) {
                        jsonBuilder.append(",");
                    }
                    first = false;

                    jsonBuilder.append("{")
                            .append("\"id\":").append(rs.getInt("id")).append(",")
                            .append("\"transactionId\":").append(rs.getInt("transaction_id")).append(",")
                            .append("\"employeeId\":").append(rs.getInt("employee_id")).append(",")
                            .append("\"employeeName\":\"").append(escapeJson(rs.getString("employee_name")))
                            .append("\",")
                            .append("\"reason\":\"").append(escapeJson(rs.getString("flag_reason"))).append("\",")
                            .append("\"flagDate\":\"").append(rs.getString("flag_date")).append("\",")
                            .append("\"status\":\"").append(rs.getString("status")).append("\",")
                            .append("\"amount\":").append(rs.getDouble("amount")).append(",")
                            .append("\"description\":\"").append(escapeJson(rs.getString("description"))).append("\",")
                            .append("\"type\":\"").append(rs.getString("type")).append("\",")
                            .append("\"timestamp\":\"").append(rs.getString("timestamp")).append("\"")
                            .append("}");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting flagged transactions: " + e.getMessage());
            e.printStackTrace();
            return "[]"; // Return empty array on error
        }

        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    // === User Management Methods ===

    /**
     * Authenticates a user based on email, password, and expected role.
     * Handles 'admin', 'employee', and 'customer' roles.
     * 
     * @param email    User's email
     * @param password User's plain text password
     * @param role     Expected role ('admin', 'employee', or 'customer')
     * @return true if authentication is successful, false otherwise
     */
    public static boolean authenticate(String email, String password, String role) {
        String sql = "SELECT password_hash, salt, role, status FROM users WHERE email = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("salt");
                    String storedRole = rs.getString("role");
                    String status = rs.getString("status");

                    if (!"active".equalsIgnoreCase(status)) {
                        System.err.println(
                                "Authentication failed for " + email + ": Account not active (status: " + status + ")");
                        return false;
                    }
                    if (!role.equalsIgnoreCase(storedRole)) {
                        System.err.println("Authentication failed for " + email + ": Role mismatch (expected: " + role
                                + ", found: " + storedRole + ")");
                        return false;
                    }

                    boolean passwordMatch = PasswordUtil.verifyPassword(password, storedHash, storedSalt);
                    if (!passwordMatch) {
                        System.err.println("Authentication failed for " + email + ": Invalid password");
                    }
                    return passwordMatch;
                } else {
                    System.err.println("Authentication failed: User not found with email " + email);
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error during authentication for " + email + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if a user's account is pending approval (inactive status)
     * 
     * @param email User's email
     * @param role  Expected role
     * @return true if the user exists, has the specified role, and has inactive
     *         status
     */
    public static boolean isAccountPendingApproval(String email, String role) {
        String sql = "SELECT role, status FROM users WHERE email = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedRole = rs.getString("role");
                    String status = rs.getString("status");

                    return "inactive".equalsIgnoreCase(status) && role.equalsIgnoreCase(storedRole);
                }
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Database error checking account status for " + email + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves user details by email. Excludes sensitive information like password
     * hash/salt.
     * 
     * @param email User's email
     * @return User object if found, null otherwise
     */
    public static User getUserByEmail(String email) {
        String sql = "SELECT id, name, email, role, status, created_at FROM users WHERE email = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    user.setStatus(rs.getString("status"));
                    user.setCreatedAt(rs.getTimestamp("created_at"));
                    return user;
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error retrieving user by email " + email + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Changes the password for a user if the old password is correct.
     * 
     * @param userId      The ID of the user changing password
     * @param oldPassword The current plain text password
     * @param newPassword The new plain text password
     * @return true if the password was successfully changed, false otherwise (user
     *         not found, old password incorrect, db error)
     */
    public static boolean changePassword(int userId, String oldPassword, String newPassword) {
        String selectSql = "SELECT password_hash, salt FROM users WHERE id = ?";
        String updateSql = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";

        try (Connection conn = getConnection()) {
            String storedHash = null;
            String storedSalt = null;
            try (PreparedStatement selectPstmt = conn.prepareStatement(selectSql)) {
                selectPstmt.setInt(1, userId);
                try (ResultSet rs = selectPstmt.executeQuery()) {
                    if (rs.next()) {
                        storedHash = rs.getString("password_hash");
                        storedSalt = rs.getString("salt");
                    } else {
                        System.err.println("Change password failed: User with ID " + userId + " not found.");
                        return false;
                    }
                }
            }

            if (!PasswordUtil.verifyPassword(oldPassword, storedHash, storedSalt)) {
                System.err.println("Change password failed for user ID " + userId + ": Incorrect old password.");
                return false;
            }

            String newSalt = PasswordUtil.generateSalt();
            String newHash = PasswordUtil.hashPassword(newPassword, newSalt);

            try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                updatePstmt.setString(1, newHash);
                updatePstmt.setString(2, newSalt);
                updatePstmt.setInt(3, userId);

                int affectedRows = updatePstmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("Password changed successfully for user ID " + userId);
                    return true;
                } else {
                    System.err.println(
                            "Change password failed for user ID " + userId + ": User not found during update phase.");
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error changing password for user ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // === User Management Methods for Admin Portal ===

    /**
     * Gets user statistics counts by role
     * 
     * @return JSON string with counts
     */
    public static String getUserStatistics() {
        StringBuilder jsonBuilder = new StringBuilder();

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            int totalUsers = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next()) {
                    totalUsers = rs.getInt(1);
                }
            }

            int adminCount = getUserCountByRole(conn, "admin");
            int employeeCount = getUserCountByRole(conn, "employee");
            int customerCount = getUserCountByRole(conn, "customer");

            jsonBuilder.append("{")
                    .append("\"totalUsers\":").append(totalUsers).append(",")
                    .append("\"adminCount\":").append(adminCount).append(",")
                    .append("\"employeeCount\":").append(employeeCount).append(",")
                    .append("\"customerCount\":").append(customerCount)
                    .append("}");

        } catch (SQLException e) {
            System.err.println("Error getting user statistics: " + e.getMessage());
            e.printStackTrace();

            return "{\"totalUsers\":0,\"adminCount\":0,\"employeeCount\":0,\"customerCount\":0}";
        }

        return jsonBuilder.toString();
    }

    private static int getUserCountByRole(Connection conn, String role) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, role);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Search for users with optional filtering
     * 
     * @param searchTerm Search query for name or email (optional)
     * @param role       Role filter (optional, 'all' for no filter)
     * @param page       Page number (1-based)
     * @param pageSize   Items per page
     * @return JSON string with users and pagination info
     */
    public static String searchUsers(String searchTerm, String role, int page, int pageSize) {
        StringBuilder jsonBuilder = new StringBuilder();
        final int DEFAULT_PAGE_SIZE = 10;

        if (page < 1)
            page = 1;
        if (pageSize < 1)
            pageSize = DEFAULT_PAGE_SIZE;

        try (Connection conn = getConnection()) {
            StringBuilder sqlBuilder = new StringBuilder(
                    "SELECT id, name, email, role, status, created_at FROM users WHERE 1=1");

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                sqlBuilder.append(" AND (LOWER(name) LIKE ? OR LOWER(email) LIKE ?)");
            }

            if (role != null && !role.trim().isEmpty() && !"all".equalsIgnoreCase(role)) {
                sqlBuilder.append(" AND role = ?");
            }

            sqlBuilder.append(" ORDER BY id DESC LIMIT ? OFFSET ?");

            try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
                int paramIndex = 1;

                if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                    String searchPattern = "%" + searchTerm.toLowerCase() + "%";
                    pstmt.setString(paramIndex++, searchPattern);
                    pstmt.setString(paramIndex++, searchPattern);
                }

                if (role != null && !role.trim().isEmpty() && !"all".equalsIgnoreCase(role)) {
                    pstmt.setString(paramIndex++, role);
                }

                pstmt.setInt(paramIndex++, pageSize);
                pstmt.setInt(paramIndex++, (page - 1) * pageSize);

                StringBuilder usersJson = new StringBuilder("[");
                boolean first = true;
                int count = 0;

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        if (!first)
                            usersJson.append(",");
                        first = false;

                        usersJson.append("{")
                                .append("\"id\":").append(rs.getInt("id")).append(",")
                                .append("\"name\":\"").append(escapeJson(rs.getString("name"))).append("\",")
                                .append("\"email\":\"").append(escapeJson(rs.getString("email"))).append("\",")
                                .append("\"role\":\"").append(rs.getString("role")).append("\",")
                                .append("\"status\":\"").append(rs.getString("status")).append("\",")
                                .append("\"createdAt\":\"").append(rs.getString("created_at")).append("\"")
                                .append("}");

                        count++;
                    }
                }
                usersJson.append("]");

                String countSql = "SELECT COUNT(*) FROM users WHERE 1=1";
                if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                    countSql += " AND (LOWER(name) LIKE ? OR LOWER(email) LIKE ?)";
                }
                if (role != null && !role.trim().isEmpty() && !"all".equalsIgnoreCase(role)) {
                    countSql += " AND role = ?";
                }

                int totalCount = 0;
                try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                    int countParamIndex = 1;

                    if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                        String searchPattern = "%" + searchTerm.toLowerCase() + "%";
                        countStmt.setString(countParamIndex++, searchPattern);
                        countStmt.setString(countParamIndex++, searchPattern);
                    }

                    if (role != null && !role.trim().isEmpty() && !"all".equalsIgnoreCase(role)) {
                        countStmt.setString(countParamIndex++, role);
                    }

                    try (ResultSet countRs = countStmt.executeQuery()) {
                        if (countRs.next()) {
                            totalCount = countRs.getInt(1);
                        }
                    }
                }

                int totalPages = (int) Math.ceil((double) totalCount / pageSize);

                jsonBuilder.append("{")
                        .append("\"users\":").append(usersJson).append(",")
                        .append("\"pagination\":{")
                        .append("\"currentPage\":").append(page).append(",")
                        .append("\"totalPages\":").append(totalPages).append(",")
                        .append("\"pageSize\":").append(pageSize).append(",")
                        .append("\"totalItems\":").append(totalCount).append(",")
                        .append("\"count\":").append(count)
                        .append("}")
                        .append("}");
            }
        } catch (SQLException e) {
            System.err.println("Error searching users: " + e.getMessage());
            e.printStackTrace();
            return "{\"users\":[],\"pagination\":{\"currentPage\":1,\"totalPages\":0,\"pageSize\":" + pageSize
                    + ",\"totalItems\":0,\"count\":0}}";
        }

        return jsonBuilder.toString();
    }

    /**
     * Get user details by ID
     * 
     * @param userId The user ID to retrieve
     * @return User object if found, null otherwise
     */
    public static User getUserById(int userId) {
        String sql = "SELECT id, name, email, role, status, created_at FROM users WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    user.setStatus(rs.getString("status"));
                    user.setCreatedAt(rs.getTimestamp("created_at"));
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving user by ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Update user details
     * 
     * @param userId      User ID to update
     * @param name        New name
     * @param role        New role
     * @param status      New status
     * @param newPassword New password (null if not changing)
     * @return true if successful, false otherwise
     */
    public static boolean updateUser(int userId, String name, String role, String status, String newPassword) {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            String updateSql = "UPDATE users SET name = ?, role = ?, status = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, name);
                pstmt.setString(2, role);
                pstmt.setString(3, status);
                pstmt.setInt(4, userId);

                int rowsAffected = pstmt.executeUpdate();

                if (newPassword != null && !newPassword.trim().isEmpty()) {
                    String passwordSql = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";
                    try (PreparedStatement passwordStmt = conn.prepareStatement(passwordSql)) {
                        String salt = PasswordUtil.generateSalt();
                        String passwordHash = PasswordUtil.hashPassword(newPassword, salt);

                        passwordStmt.setString(1, passwordHash);
                        passwordStmt.setString(2, salt);
                        passwordStmt.setInt(3, userId);

                        passwordStmt.executeUpdate();
                    }
                }

                conn.commit();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Update a user's status
     * 
     * @param userId User ID to update
     * @param status New status ('active', 'inactive', or 'locked')
     * @return true if successful, false otherwise
     */
    public static boolean updateUserStatus(int userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, userId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating status for user ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get a list of inactive users as JSON array
     * 
     * @return JSON array of inactive user data
     */
    public static String getInactiveUsersAsJson() {
        StringBuilder jsonBuilder = new StringBuilder("[");

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT id, name, email, role, created_at FROM users " +
                                "WHERE status = 'inactive' " +
                                "ORDER BY created_at DESC");
                ResultSet rs = pstmt.executeQuery()) {

            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                first = false;

                jsonBuilder.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"name\":\"").append(escapeJson(rs.getString("name"))).append("\",")
                        .append("\"email\":\"").append(escapeJson(rs.getString("email"))).append("\",")
                        .append("\"role\":\"").append(rs.getString("role")).append("\",")
                        .append("\"createdAt\":\"").append(rs.getString("created_at")).append("\"")
                        .append("}");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching inactive users: " + e.getMessage());
            e.printStackTrace();
            return "[]";
        }

        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    /**
     * Approves a user by changing their status from 'inactive' to 'active'
     * 
     * @param userId  The ID of the user to approve
     * @param adminId The ID of the admin performing the approval
     * @return true if successful, false otherwise
     */
    public static boolean approveUser(int userId, int adminId) {
        String sql = "UPDATE users SET status = 'active' WHERE id = ? AND status = 'inactive'";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("User #" + userId + " successfully approved by admin #" + adminId);
                return true;
            } else {
                System.out.println("User #" + userId + " not found or not in inactive status");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error approving user #" + userId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // === Admin Dashboard Statistics Methods ===

    /**
     * Gets the count of all users in the system
     * 
     * @return Total number of users
     */
    public static int getTotalUsersCount() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("Error counting users: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Gets the count of active sessions (approximation based on active users)
     * In a real system, this would query a sessions table
     * 
     * @return Number of active sessions
     */
    public static int getActiveSessionsCount() {
        String sql = "SELECT COUNT(*) FROM users WHERE status = 'active'";
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("Error counting active sessions: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Gets the count of pending loan requests
     * 
     * @return Number of pending loans
     */
    public static int getPendingLoansCount() {
        String sql = "SELECT COUNT(*) FROM loans WHERE status = 'pending'";
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("Error counting pending loans: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Gets the count of approved loan requests
     * 
     * @return Number of approved loans
     */
    public static int getApprovedLoansCount() {
        String sql = "SELECT COUNT(*) FROM loans WHERE status = 'approved'";
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("Error counting approved loans: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Gets the count of rejected loan requests
     * 
     * @return Number of rejected loans
     */
    public static int getRejectedLoansCount() {
        String sql = "SELECT COUNT(*) FROM loans WHERE status = 'rejected'";
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("Error counting rejected loans: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Gets the total count of all loan requests
     * 
     * @return Total number of loans in the system
     */
    public static int getTotalLoansCount() {
        String sql = "SELECT COUNT(*) FROM loans";
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("Error counting total loans: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Gets the count of recent system alerts (e.g., failed login attempts, unusual
     * transactions)
     * For demonstration - in production, you would have an actual alerts table
     * 
     * @return Number of recent alerts
     */
    public static int getRecentAlertsCount() {
        String sql = "SELECT COUNT(*) FROM transactions WHERE timestamp > datetime('now', '-24 hours')";
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("Error counting recent alerts: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Fetches pending loan requests with customer information
     * 
     * @return List of pending loan requests as a ResultSet
     */
    public static ResultSet getPendingLoanRequests() throws SQLException {
        String sql = "SELECT l.loanid, l.userid, u.name as customerName, l.amount, " +
                "l.type, l.date, l.status " +
                "FROM loans l " +
                "JOIN users u ON l.userid = u.id " +
                "WHERE l.status = 'pending' " +
                "ORDER BY l.date DESC";

        Connection conn = getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        return pstmt.executeQuery();
    }

    /**
     * Get a list of pending loans as JSON array
     * 
     * @return JSON array of pending loan data
     */
    public static String getPendingLoansAsJson() {
        StringBuilder jsonBuilder = new StringBuilder("[");

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT l.loanid, l.userid, u.name as customerName, l.amount, " +
                                "l.type, l.date, l.status " +
                                "FROM loans l " +
                                "JOIN users u ON l.userid = u.id " +
                                "WHERE l.status = 'pending' " +
                                "ORDER BY l.date DESC");
                ResultSet rs = pstmt.executeQuery()) {

            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                first = false;

                jsonBuilder.append("{")
                        .append("\"id\":").append(rs.getInt("loanid")).append(",")
                        .append("\"userId\":").append(rs.getInt("userid")).append(",")
                        .append("\"customerName\":\"").append(escapeJson(rs.getString("customerName"))).append("\",")
                        .append("\"amount\":").append(rs.getDouble("amount")).append(",")
                        .append("\"type\":\"").append(escapeJson(rs.getString("type"))).append("\",")
                        .append("\"date\":\"").append(rs.getString("date")).append("\",")
                        .append("\"status\":\"").append(rs.getString("status")).append("\"")
                        .append("}");
            }

        } catch (SQLException e) {
            System.err.println("Error fetching pending loans: " + e.getMessage());
            e.printStackTrace();
            return "[]";
        }

        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    /**
     * Escape JSON strings properly
     */
    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }

        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Approves a loan request by setting its status to 'approved'
     * 
     * @param loanId  The ID of the loan to approve
     * @param adminId The ID of the admin who approved the loan
     * @return true if successful, false otherwise
     */
    public static boolean approveLoan(int loanId, int adminId) {
        String sql = "UPDATE loans SET status = 'approved', adminComment = 'Approved by admin ID: ' || ? WHERE loanid = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, adminId);
            pstmt.setInt(2, loanId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Loan #" + loanId + " successfully approved by admin #" + adminId);
                return true;
            } else {
                System.out.println("Loan #" + loanId + " not found or already processed");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error approving loan #" + loanId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Rejects a loan request by setting its status to 'rejected' with admin comment
     * 
     * @param loanId  The ID of the loan to reject
     * @param adminId The ID of the admin who rejected the loan
     * @param comment The reason for rejection
     * @return true if successful, false otherwise
     */
    public static boolean rejectLoan(int loanId, int adminId, String comment) {
        String sql = "UPDATE loans SET status = 'rejected', adminComment = ? WHERE loanid = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String adminComment = comment;
            if (comment == null || comment.trim().isEmpty()) {
                adminComment = "Rejected by admin ID: " + adminId;
            } else {
                adminComment = escapeJson(comment) + " (Rejected by admin ID: " + adminId + ")";
            }

            pstmt.setString(1, adminComment);
            pstmt.setInt(2, loanId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Loan #" + loanId + " successfully rejected by admin #" + adminId);
                return true;
            } else {
                System.out.println("Loan #" + loanId + " not found or already processed");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error rejecting loan #" + loanId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets a loan by ID
     * 
     * @param loanId The ID of the loan to retrieve
     * @return JSON string with loan data or empty JSON object if not found
     */
    public static String getLoanById(int loanId) {
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT l.loanid, l.userid, u.name as customerName, l.amount, " +
                                "l.type, l.date, l.status, l.adminComment " +
                                "FROM loans l " +
                                "JOIN users u ON l.userid = u.id " +
                                "WHERE l.loanid = ?")) {

            pstmt.setInt(1, loanId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    StringBuilder json = new StringBuilder();
                    json.append("{")
                            .append("\"id\":").append(rs.getInt("loanid")).append(",")
                            .append("\"userId\":").append(rs.getInt("userid")).append(",")
                            .append("\"customerName\":\"").append(escapeJson(rs.getString("customerName")))
                            .append("\",")
                            .append("\"amount\":").append(rs.getDouble("amount")).append(",")
                            .append("\"type\":\"").append(escapeJson(rs.getString("type"))).append("\",")
                            .append("\"date\":\"").append(rs.getString("date")).append("\",")
                            .append("\"status\":\"").append(rs.getString("status")).append("\"");

                    String comment = rs.getString("adminComment");
                    if (comment != null) {
                        json.append(",\"adminComment\":\"").append(escapeJson(comment)).append("\"");
                    }

                    json.append("}");
                    return json.toString();
                } else {
                    return "{}";
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving loan #" + loanId + ": " + e.getMessage());
            e.printStackTrace();
            return "{}";
        }
    }

    /**
     * Gets all loans for a specific user as JSON
     *
     * @param userId The ID of the user whose loans to retrieve
     * @return JSON string containing user's loan data
     */
    public static String getUserLoansAsJson(int userId) {
        StringBuilder jsonBuilder = new StringBuilder("[");

        try (Connection conn = getConnection()) {
            String sql = "SELECT l.loanid, l.amount, l.type, l.date, l.status, l.adminComment " +
                    "FROM loans l " +
                    "WHERE l.userid = ? " +
                    "ORDER BY l.date DESC";

            boolean first = true;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        if (!first) {
                            jsonBuilder.append(",");
                        }
                        first = false;

                        jsonBuilder.append("{");
                        jsonBuilder.append("\"id\":").append(rs.getInt("loanid")).append(",");
                        jsonBuilder.append("\"amount\":").append(rs.getDouble("amount")).append(",");

                        String type = rs.getString("type");
                        jsonBuilder.append("\"type\":\"").append(type != null ? escapeJson(type) : "").append("\",");

                        String date = rs.getString("date");
                        jsonBuilder.append("\"date\":\"").append(date != null ? date : "").append("\",");

                        String status = rs.getString("status");
                        jsonBuilder.append("\"status\":\"").append(status != null ? status : "pending").append("\"");

                        String comment = rs.getString("adminComment");
                        if (comment != null) {
                            jsonBuilder.append(",\"adminComment\":\"").append(escapeJson(comment)).append("\"");
                        }

                        jsonBuilder.append("}");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting loans for user ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return "[]"; // Return empty array on error
        }

        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    /**
     * Gets loan details by ID
     *
     * @param loanId The ID of the loan to retrieve
     * @return JSON string with loan data or empty JSON object if not found
     */
    public static String getLoanDetailsAsJson(int loanId) {
        try (Connection conn = getConnection()) {
            String sql = "SELECT l.loanid, l.userid, l.amount, l.type, l.date, l.status, l.adminComment, " +
                    "u.name as customerName " +
                    "FROM loans l " +
                    "JOIN users u ON l.userid = u.id " +
                    "WHERE l.loanid = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, loanId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        StringBuilder json = new StringBuilder();
                        json.append("{")
                                .append("\"id\":").append(rs.getInt("loanid")).append(",")
                                .append("\"userId\":").append(rs.getInt("userid")).append(",")
                                .append("\"customerName\":\"").append(escapeJson(rs.getString("customerName")))
                                .append("\",")
                                .append("\"amount\":").append(rs.getDouble("amount")).append(",")
                                .append("\"type\":\"").append(escapeJson(rs.getString("type"))).append("\",")
                                .append("\"date\":\"").append(rs.getString("date")).append("\",")
                                .append("\"status\":\"").append(rs.getString("status")).append("\"");

                        String comment = rs.getString("adminComment");
                        if (comment != null) {
                            json.append(",\"adminComment\":\"").append(escapeJson(comment)).append("\"");
                        }

                        json.append("}");
                        return json.toString();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving loan details for ID " + loanId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return "{}";
    }

    public static void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null)
                rs.close();
        } catch (SQLException e) {
            System.err.println("Error closing ResultSet: " + e.getMessage());
        }
        try {
            if (stmt != null)
                stmt.close();
        } catch (SQLException e) {
            System.err.println("Error closing Statement: " + e.getMessage());
        }
        try {
            if (conn != null)
                conn.close();
        } catch (SQLException e) {
            System.err.println("Error closing Connection: " + e.getMessage());
        }
    }
}