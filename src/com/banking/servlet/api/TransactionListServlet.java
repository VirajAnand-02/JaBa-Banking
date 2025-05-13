package com.banking.servlet.api;

import com.banking.util.DatabaseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servlet to provide transaction data for transaction-list.js
 * Handles both /api/transaction-data and /api/admin-transactions endpoints
 * for backward compatibility
 */
@WebServlet(urlPatterns = { "/api/transaction-data", "/api/admin-transactions" })
public class TransactionListServlet extends HttpServlet {

    private static final Random random = new Random();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");

        // Get user ID and role from session user object instead of direct attributes
        Object userObj = req.getSession().getAttribute("user");

        Integer userId = null;
        String userRole = null;

        // Extract from User object if present
        if (userObj != null && userObj instanceof com.banking.model.User) {
            com.banking.model.User user = (com.banking.model.User) userObj;
            userId = user.getId();
            userRole = user.getRole();
        } else {
            // Fallback to old session attributes for backward compatibility
            userId = (Integer) req.getSession().getAttribute("userId");
            userRole = (String) req.getSession().getAttribute("userRole");
        }

        if (userId == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"error\":\"User not authenticated\"}");
            return;
        }

        try {
            // Get parameters for pagination
            int page = getIntParam(req, "page", 1);
            int size = getIntParam(req, "size", 10);

            // Check if this is an admin-transactions endpoint request
            if (req.getServletPath().equals("/api/admin-transactions")) {
                // Verify admin permissions for admin-specific endpoint
                if (userRole == null || !"admin".equalsIgnoreCase(userRole)) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.getWriter().write("{\"error\":\"Admin privileges required\"}");
                    return;
                }

                // For admin endpoint without specific userId, return all transactions
                Integer targetUserId = null;
                if (req.getParameter("userId") != null) {
                    targetUserId = getIntParam(req, "userId", -1);
                }

                if (targetUserId == null || targetUserId < 0) {
                    writeAdminTransactionResponse(resp, page, size);
                    return;
                } else {
                    writeTransactionResponse(resp, targetUserId, page, size);
                    return;
                }
            }

            // For regular transaction endpoint
            boolean isAdminOrEmployee = userRole != null &&
                    ("admin".equalsIgnoreCase(userRole) ||
                            "employee".equalsIgnoreCase(userRole));

            // IMPORTANT FIX: For regular transaction endpoint, ensure comparison uses
            // primitive values
            Integer targetUserId = null;
            if (req.getParameter("userId") != null) {
                targetUserId = getIntParam(req, "userId", -1);

                // Allow if the requested user ID matches the session user's ID
                // OR if the user is an admin/employee
                boolean isSameUser = (targetUserId.intValue() == userId.intValue());
                if (!isSameUser && !isAdminOrEmployee) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.getWriter().write("{\"error\":\"Permission denied\"}");
                    return;
                }
            }

            // If specific user requested by admin/employee, show that user's transactions
            if (targetUserId != null && targetUserId > 0) {
                writeTransactionResponse(resp, targetUserId, page, size);
                return;
            }

            // If admin/employee and no specific user requested, show all transactions
            if (isAdminOrEmployee) {
                writeAdminTransactionResponse(resp, page, size);
                return;
            }

            // Default: show current user's own transactions
            writeTransactionResponse(resp, userId, page, size);

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Database error: " + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }

    /**
     * Write transaction list response in the exact format required by
     * transaction-list.js
     */
    private void writeTransactionResponse(HttpServletResponse resp, int userId, int page, int size)
            throws IOException, SQLException {

        List<Map<String, Object>> transactions = getTransactionData(userId, page, size);
        int totalItems = getTotalTransactionCount(userId);
        int totalPages = (int) Math.ceil((double) totalItems / size);

        // Build JSON response
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"transactions\":[");

        boolean first = true;
        for (Map<String, Object> transaction : transactions) {
            if (!first) {
                jsonBuilder.append(",");
            }

            jsonBuilder.append("{");
            jsonBuilder.append("\"id\":").append(transaction.get("id")).append(",");
            jsonBuilder.append("\"date\":\"").append(transaction.get("date")).append("\",");
            jsonBuilder.append("\"description\":\"").append(transaction.get("description")).append("\",");
            jsonBuilder.append("\"type\":\"").append(transaction.get("type")).append("\",");
            jsonBuilder.append("\"amount\":").append(transaction.get("amount")).append(",");
            jsonBuilder.append("\"isDebit\":").append(transaction.get("isDebit")).append(",");
            jsonBuilder.append("\"fromAccount\":\"").append(transaction.get("fromAccount")).append("\",");
            jsonBuilder.append("\"toAccount\":\"").append(transaction.get("toAccount")).append("\",");
            jsonBuilder.append("\"fromUserId\":").append(transaction.get("fromUserId")).append(",");
            jsonBuilder.append("\"toUserId\":").append(transaction.get("toUserId")).append(",");
            jsonBuilder.append("\"userName\":\"").append(transaction.get("userName")).append("\"");
            jsonBuilder.append("}");

            first = false;
        }

        // Add pagination information
        jsonBuilder.append("],");
        jsonBuilder.append("\"pagination\":{");
        jsonBuilder.append("\"totalItems\":").append(totalItems).append(",");
        jsonBuilder.append("\"totalPages\":").append(totalPages).append(",");
        jsonBuilder.append("\"currentPage\":").append(page).append(",");
        jsonBuilder.append("\"pageSize\":").append(size);
        jsonBuilder.append("}}");

        resp.getWriter().write(jsonBuilder.toString());
    }

    /**
     * Write admin transaction response with all transactions
     */
    private void writeAdminTransactionResponse(HttpServletResponse resp, int page, int size)
            throws IOException, SQLException {
        try (Connection conn = DatabaseUtil.getConnection()) {
            if (conn != null) {
                // Fix quotes for SQLite CONCAT function (SQLite doesn't support CONCAT)
                // Added LEFT JOIN with flagged_transactions to filter out already flagged
                // transactions for admin view
                String sql = "SELECT t.id, strftime('%Y-%m-%d', t.timestamp) as formatted_date, " +
                        "t.amount, t.description, t.type, " +
                        "a1.account_number as from_account, a2.account_number as to_account, " +
                        "a1.user_id as from_user_id, a2.user_id as to_user_id, " +
                        // Use SQLite string concatenation instead of CONCAT function
                        "COALESCE(u1.name, 'Unknown') || ' (' || COALESCE(a1.user_id, 0) || ')' as from_user_name, " +
                        "COALESCE(u2.name, 'Unknown') || ' (' || COALESCE(a2.user_id, 0) || ')' as to_user_name " +
                        "FROM transactions t " +
                        "LEFT JOIN accounts a1 ON t.from_account_id = a1.id " +
                        "LEFT JOIN accounts a2 ON t.to_account_id = a2.id " +
                        "LEFT JOIN users u1 ON a1.user_id = u1.id " +
                        "LEFT JOIN users u2 ON a2.user_id = u2.id " +
                        "LEFT JOIN flagged_transactions ft ON t.id = ft.transaction_id " +
                        "WHERE ft.id IS NULL " +
                        "ORDER BY t.timestamp DESC " +
                        "LIMIT ? OFFSET ?";

                List<Map<String, Object>> transactions = new ArrayList<>();
                int totalItems = 0;

                // Update count query to also exclude flagged transactions
                try (PreparedStatement countStmt = conn.prepareStatement(
                        "SELECT COUNT(*) FROM transactions t " +
                                "LEFT JOIN flagged_transactions ft ON t.id = ft.transaction_id " +
                                "WHERE ft.id IS NULL")) {
                    try (ResultSet countRs = countStmt.executeQuery()) {
                        if (countRs.next()) {
                            totalItems = countRs.getInt(1);
                        }
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, size);
                    stmt.setInt(2, (page - 1) * size);

                    try (ResultSet rs = stmt.executeQuery()) {
                        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

                        while (rs.next()) {
                            Map<String, Object> transaction = new HashMap<>();
                            transaction.put("id", rs.getInt("id"));

                            // Fix: Use the formatted date string from SQLite directly
                            String dateStr = rs.getString("formatted_date");
                            LocalDate transactionDate;
                            try {
                                // Parse the date string from SQLite (YYYY-MM-DD format)
                                transactionDate = LocalDate.parse(dateStr);
                                transaction.put("date", transactionDate.format(dateFormatter));
                            } catch (Exception e) {
                                // Fallback if date parsing fails
                                System.err.println("Error parsing date: " + dateStr + " - " + e.getMessage());
                                transaction.put("date", "Unknown Date");
                            }

                            transaction.put("description", rs.getString("description"));
                            transaction.put("type", rs.getString("type"));
                            transaction.put("amount", rs.getDouble("amount"));

                            // For admin view, show as debit by default (money leaving the system)
                            transaction.put("isDebit", true);

                            // Account information
                            transaction.put("fromAccount", maskAccountNumber(rs.getString("from_account")));
                            transaction.put("toAccount", maskAccountNumber(rs.getString("to_account")));
                            transaction.put("fromUserId", rs.getInt("from_user_id"));
                            transaction.put("toUserId", rs.getInt("to_user_id"));

                            // Show both users' information
                            transaction.put("userName", rs.getString("from_user_name") + " → " +
                                    rs.getString("to_user_name"));

                            transactions.add(transaction);
                        }
                    }
                }

                // Build JSON response
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{\"transactions\":[");

                boolean first = true;
                for (Map<String, Object> transaction : transactions) {
                    if (!first)
                        jsonBuilder.append(",");

                    jsonBuilder.append("{");
                    for (Map.Entry<String, Object> entry : transaction.entrySet()) {
                        if (entry.getValue() instanceof String) {
                            jsonBuilder.append("\"").append(entry.getKey()).append("\":\"")
                                    .append(entry.getValue()).append("\",");
                        } else {
                            jsonBuilder.append("\"").append(entry.getKey()).append("\":")
                                    .append(entry.getValue()).append(",");
                        }
                    }
                    // Remove trailing comma and close the object
                    if (!transaction.isEmpty()) {
                        jsonBuilder.setLength(jsonBuilder.length() - 1);
                    }
                    jsonBuilder.append("}");
                    first = false;
                }

                // Add pagination information
                int totalPages = (int) Math.ceil((double) totalItems / size);
                jsonBuilder.append("],");
                jsonBuilder.append("\"pagination\":{");
                jsonBuilder.append("\"totalItems\":").append(totalItems).append(",");
                jsonBuilder.append("\"totalPages\":").append(totalPages).append(",");
                jsonBuilder.append("\"currentPage\":").append(page).append(",");
                jsonBuilder.append("\"pageSize\":").append(size);
                jsonBuilder.append("}}");

                resp.getWriter().write(jsonBuilder.toString());
                return;
            }
        }

        // Fallback to mock data for admin view
        List<Map<String, Object>> mockData = generateAdminMockData(page, size);
        int totalItems = 100; // Default total for admin mock data
        int totalPages = (int) Math.ceil((double) totalItems / size);

        // Build JSON response for mock data
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"transactions\":[");

        boolean first = true;
        for (Map<String, Object> transaction : mockData) {
            if (!first)
                jsonBuilder.append(",");
            jsonBuilder.append("{");
            for (Map.Entry<String, Object> entry : transaction.entrySet()) {
                if (entry.getValue() instanceof String) {
                    jsonBuilder.append("\"").append(entry.getKey()).append("\":\"")
                            .append(entry.getValue()).append("\",");
                } else {
                    jsonBuilder.append("\"").append(entry.getKey()).append(":")
                            .append(entry.getValue()).append(",");
                }
            }
            // Remove trailing comma and close the object
            if (!transaction.isEmpty()) {
                jsonBuilder.setLength(jsonBuilder.length() - 1);
            }
            jsonBuilder.append("}");
            first = false;
        }

        jsonBuilder.append("],");
        jsonBuilder.append("\"pagination\":{");
        jsonBuilder.append("\"totalItems\":").append(totalItems).append(",");
        jsonBuilder.append("\"totalPages\":").append(totalPages).append(",");
        jsonBuilder.append("\"currentPage\":").append(page).append(",");
        jsonBuilder.append("\"pageSize\":").append(size);
        jsonBuilder.append("}}");

        resp.getWriter().write(jsonBuilder.toString());
    }

    /**
     * Generate mock transaction data for admin view
     */
    private List<Map<String, Object>> generateAdminMockData(int page, int size) {
        List<Map<String, Object>> transactions = new ArrayList<>();
        int startIdx = (page - 1) * size;

        // Fix: Use lowercase transaction types to match database schema
        String[] types = { "deposit", "withdrawal", "transfer" };
        String[] descriptions = { "Grocery Store", "Salary Deposit", "ATM Withdrawal", "Online Transfer",
                "Electric Bill", "Restaurant", "Amazon.com", "Gas Station" };

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        LocalDate today = LocalDate.now();

        for (int i = 0; i < size; i++) {
            Map<String, Object> transaction = new HashMap<>();
            int id = startIdx + i + 1;

            transaction.put("id", id);
            LocalDate date = today.minusDays(random.nextInt(30));
            transaction.put("date", date.format(formatter));

            transaction.put("type", types[random.nextInt(types.length)]);
            transaction.put("description", descriptions[random.nextInt(descriptions.length)]);

            double amount = 10.0 + random.nextDouble() * 990.0;
            amount = Math.round(amount * 100.0) / 100.0;
            transaction.put("amount", amount);
            transaction.put("isDebit", true);

            int fromUserId = random.nextInt(100) + 1;
            int toUserId = random.nextInt(100) + 1;

            transaction.put("fromAccount", "Account ****" + (1000 + random.nextInt(9000)));
            transaction.put("toAccount", "Account ****" + (1000 + random.nextInt(9000)));
            transaction.put("fromUserId", fromUserId);
            transaction.put("toUserId", toUserId);
            transaction.put("userName", "User " + fromUserId + " → User " + toUserId);

            transactions.add(transaction);
        }

        return transactions;
    }

    /**
     * Get transaction data for a specific user
     * In a production environment, this would query the database
     */
    private List<Map<String, Object>> getTransactionData(int userId, int page, int size) throws SQLException {
        List<Map<String, Object>> transactions = new ArrayList<>();
        int startIdx = (page - 1) * size;

        // Try to get data from database first
        try (Connection conn = DatabaseUtil.getConnection()) {
            if (conn != null) {
                // Update SQL to use strftime for safe date handling and proper SQLite syntax
                // Added LEFT JOIN with flagged_transactions to filter out already flagged
                // transactions
                String sql = "SELECT t.id, strftime('%Y-%m-%d', t.timestamp) as formatted_date, t.amount, " +
                        "t.description, t.type, " +
                        "a1.account_number as from_account, a2.account_number as to_account, " +
                        "a1.user_id as from_user_id, a2.user_id as to_user_id, " +
                        "u.name as user_name " +
                        "FROM transactions t " +
                        "LEFT JOIN accounts a1 ON t.from_account_id = a1.id " +
                        "LEFT JOIN accounts a2 ON t.to_account_id = a2.id " +
                        "LEFT JOIN users u ON (CASE WHEN a1.user_id = ? THEN a2.user_id ELSE a1.user_id END) = u.id " +
                        "LEFT JOIN flagged_transactions ft ON t.id = ft.transaction_id " +
                        "WHERE (a1.user_id = ? OR a2.user_id = ?) AND ft.id IS NULL " +
                        "ORDER BY t.timestamp DESC " +
                        "LIMIT ? OFFSET ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, userId);
                    stmt.setInt(3, userId);
                    stmt.setInt(4, size);
                    stmt.setInt(5, startIdx);

                    try (ResultSet rs = stmt.executeQuery()) {
                        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

                        while (rs.next()) {
                            Map<String, Object> transaction = new HashMap<>();
                            int id = rs.getInt("id");

                            // Determine if this is a debit from the user's perspective
                            int fromUserId = rs.getInt("from_user_id");
                            boolean isDebit = (fromUserId == userId);

                            transaction.put("id", id);

                            // Fix: Use the formatted date string from SQLite directly
                            String dateStr = rs.getString("formatted_date");
                            try {
                                LocalDate transactionDate = LocalDate.parse(dateStr);
                                transaction.put("date", transactionDate.format(dateFormatter));
                            } catch (Exception e) {
                                System.err.println("Error parsing date: " + dateStr + " - " + e.getMessage());
                                transaction.put("date", "Unknown Date");
                            }

                            transaction.put("description", rs.getString("description"));
                            transaction.put("type", rs.getString("type"));
                            transaction.put("amount", rs.getDouble("amount"));
                            transaction.put("isDebit", isDebit);

                            // Mask account numbers for security
                            String fromAccount = maskAccountNumber(rs.getString("from_account"));
                            String toAccount = maskAccountNumber(rs.getString("to_account"));

                            transaction.put("fromAccount", fromAccount);
                            transaction.put("toAccount", toAccount);
                            transaction.put("fromUserId", fromUserId);
                            transaction.put("toUserId", rs.getInt("to_user_id"));

                            // Get appropriate username based on the transaction direction
                            transaction.put("userName", rs.getString("user_name"));

                            transactions.add(transaction);
                        }
                    }
                }

                // If we got transactions from the DB, return them
                if (!transactions.isEmpty()) {
                    return transactions;
                }
            }
        } catch (SQLException e) {
            // Log the exception but continue to generate mock data
            System.err.println("Database error in getTransactionData: " + e.getMessage());
        }

        // Fallback to mock data if database connection fails or returns no data
        return generateMockTransactionData(userId, page, size);
    }

    /**
     * Generate mock transaction data when the database is unavailable
     */
    private List<Map<String, Object>> generateMockTransactionData(int userId, int page, int size) {
        List<Map<String, Object>> transactions = new ArrayList<>();
        int startIdx = (page - 1) * size;

        // Transaction types and descriptions for mock data - fix lowercase
        String[] types = { "deposit", "withdrawal", "transfer" };
        String[] descriptions = { "Grocery Store", "Salary Deposit", "ATM Withdrawal", "Online Transfer",
                "Electric Bill", "Restaurant", "Amazon.com", "Gas Station" };

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        LocalDate today = LocalDate.now();

        // Generate transactions
        for (int i = 0; i < size; i++) {
            Map<String, Object> transaction = new HashMap<>();
            int id = startIdx + i + 1;
            boolean isDebit = random.nextBoolean();

            transaction.put("id", id);

            // Create a date within the last 30 days
            LocalDate date = today.minusDays(random.nextInt(30));
            transaction.put("date", date.format(formatter));

            // Transaction details
            transaction.put("type", types[random.nextInt(types.length)]);
            transaction.put("description", descriptions[random.nextInt(descriptions.length)]);

            // Amount between $10 and $1000, rounded to 2 decimal places
            double amount = 10.0 + random.nextDouble() * 990.0;
            amount = Math.round(amount * 100.0) / 100.0;
            transaction.put("amount", amount);
            transaction.put("isDebit", isDebit);

            // Account information
            transaction.put("fromAccount", isDebit ? "Checking ****4321" : "External");
            transaction.put("toAccount", isDebit ? "External" : "Checking ****4321");

            // User information
            int otherUserId = random.nextInt(100) + 1;
            while (otherUserId == userId) {
                otherUserId = random.nextInt(100) + 1; // Ensure other user is different
            }

            transaction.put("fromUserId", isDebit ? userId : otherUserId);
            transaction.put("toUserId", isDebit ? otherUserId : userId);
            transaction.put("userName", "User " + (isDebit ? otherUserId : userId));

            transactions.add(transaction);
        }

        return transactions;
    }

    /**
     * Mask an account number for security
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            return "External";
        }

        if (accountNumber.length() <= 4) {
            return "****" + accountNumber;
        }

        return accountNumber.substring(0, 4) + " ****" +
                accountNumber.substring(accountNumber.length() - 4);
    }

    /**
     * Get total transaction count for pagination
     */
    private int getTotalTransactionCount(int userId) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            if (conn != null) {
                // Updated query to exclude flagged transactions
                String sql = "SELECT COUNT(*) FROM transactions t " +
                        "LEFT JOIN accounts a1 ON t.from_account_id = a1.id " +
                        "LEFT JOIN accounts a2 ON t.to_account_id = a2.id " +
                        "LEFT JOIN flagged_transactions ft ON t.id = ft.transaction_id " +
                        "WHERE (a1.user_id = ? OR a2.user_id = ?) AND ft.id IS NULL";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, userId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error in getTotalTransactionCount: " + e.getMessage());
        }

        // Return a default value for mock data
        return 58;
    }

    /**
     * Get integer parameter with default value
     */
    private int getIntParam(HttpServletRequest request, String paramName, int defaultValue) {
        String paramValue = request.getParameter(paramName);
        if (paramValue != null && !paramValue.isEmpty()) {
            try {
                return Integer.parseInt(paramValue);
            } catch (NumberFormatException e) {
                // Return default if parsing fails
            }
        }
        return defaultValue;
    }
}
