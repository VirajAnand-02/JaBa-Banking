package com.banking.servlet;

import com.banking.model.Transaction;
import com.banking.model.User;
import com.banking.util.DatabaseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/employee/dashboard")
public class EmployeeDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);

        // Security check
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login.html?error=session_expired");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!"employee".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.html?error=unauthorized_access");
            return;
        }

        // Get pagination parameters
        int pageSize = getIntParameter(request, "size", 5);
        int currentPage = getIntParameter(request, "page", 1);

        try {
            // Fetch transactions from database
            List<Transaction> transactions = fetchTransactions(currentPage, pageSize);
            int totalItems = countTotalTransactions();
            int totalPages = (totalItems + pageSize - 1) / pageSize; // Ceiling division

            // Set attributes for the JSP
            request.setAttribute("transactions", transactions);
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("pageSize", pageSize);

            // Forward to the dashboard page
            request.getRequestDispatcher("/pages/employee/dashboard.jsp").forward(request, response);

        } catch (SQLException e) {
            // Log the error and show an error page
            e.printStackTrace();
            request.setAttribute("errorMessage", "Database error: " + e.getMessage());
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        }
    }

    /**
     * Fetch paginated transactions from database
     */
    private List<Transaction> fetchTransactions(int page, int pageSize) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");

        int offset = (page - 1) * pageSize;

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT t.id, t.timestamp, t.amount, t.description, t.type, " +
                    "a1.account_number as from_account, a2.account_number as to_account, " +
                    "a1.user_id as from_user_id, a2.user_id as to_user_id, " +
                    "u.name as user_name " +
                    "FROM transactions t " +
                    "LEFT JOIN accounts a1 ON t.from_account_id = a1.id " +
                    "LEFT JOIN accounts a2 ON t.to_account_id = a2.id " +
                    "LEFT JOIN users u ON (CASE WHEN a1.user_id = a2.user_id THEN a1.user_id ELSE a1.user_id END) = u.id "
                    +
                    "ORDER BY t.timestamp DESC " +
                    "LIMIT ? OFFSET ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, pageSize);
                stmt.setInt(2, offset);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Transaction transaction = new Transaction();
                        transaction.setId(rs.getInt("id"));
                        transaction.setDate(dateFormat.format(rs.getTimestamp("timestamp")));
                        transaction.setAmount(rs.getDouble("amount"));
                        transaction.setDescription(rs.getString("description"));
                        transaction.setType(rs.getString("type"));

                        // Determine if this is a debit transaction
                        int fromUserId = rs.getInt("from_user_id");
                        int toUserId = rs.getInt("to_user_id");
                        transaction.setIsDebit(fromUserId == toUserId); // Simplified logic for example

                        // Mask account numbers
                        String fromAccount = maskAccountNumber(rs.getString("from_account"));
                        String toAccount = maskAccountNumber(rs.getString("to_account"));
                        transaction.setFromAccount(fromAccount);
                        transaction.setToAccount(toAccount);

                        transaction.setFromUserId(fromUserId);
                        transaction.setToUserId(toUserId);
                        transaction.setUserName(rs.getString("user_name"));

                        transactions.add(transaction);
                    }
                }
            }
        }

        return transactions;
    }

    /**
     * Count total number of transactions for pagination
     */
    private int countTotalTransactions() throws SQLException {
        int count = 0;

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT COUNT(*) FROM transactions";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                }
            }
        }

        return count;
    }

    /**
     * Mask account number for security
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            return "External";
        }

        if (accountNumber.length() <= 4) {
            return "****" + accountNumber;
        }

        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    /**
     * Get integer parameter with default value
     */
    private int getIntParameter(HttpServletRequest request, String paramName, int defaultValue) {
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
