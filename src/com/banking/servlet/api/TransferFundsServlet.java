package com.banking.servlet.api;

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

@WebServlet("/api/transfer-funds")
public class TransferFundsServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Verify user is logged in and is a customer
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"You must be logged in to perform this action.\"}");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!"customer".equals(user.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Only customers can initiate transfers.\"}");
            return;
        }

        // Get form parameters
        String fromAccountId = request.getParameter("fromAccount");
        String toAccountNumber = request.getParameter("toAccountNumber");
        String amountStr = request.getParameter("amount");
        String description = request.getParameter("description");

        // Validate input
        if (fromAccountId == null || toAccountNumber == null || amountStr == null ||
                fromAccountId.trim().isEmpty() || toAccountNumber.trim().isEmpty() || amountStr.trim().isEmpty()) {
            setErrorAndRedirect(request, response, "All fields are required.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                setErrorAndRedirect(request, response, "Amount must be greater than zero.");
                return;
            }
        } catch (NumberFormatException e) {
            setErrorAndRedirect(request, response, "Invalid amount format.");
            return;
        }

        // Process the transfer
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Verify the source account belongs to the current user
            int sourceAccountId = Integer.parseInt(fromAccountId);
            double sourceBalance = getAccountBalanceAndVerifyOwner(conn, sourceAccountId, user.getId());

            if (sourceBalance < 0) {
                throw new SQLException("Source account not found or does not belong to the current user.");
            }

            // 2. Check if user has sufficient funds
            if (sourceBalance < amount) {
                setErrorAndRedirect(request, response, "Insufficient funds to complete this transfer.");
                return;
            }

            // 3. Find the destination account
            int destinationAccountId = getAccountIdByNumber(conn, toAccountNumber);
            if (destinationAccountId < 0) {
                setErrorAndRedirect(request, response, "Destination account not found.");
                return;
            }

            // 4. Prevent transfer to the same account
            if (sourceAccountId == destinationAccountId) {
                setErrorAndRedirect(request, response, "Cannot transfer to the same account.");
                return;
            }

            // 5. Update source account balance (subtract amount)
            updateAccountBalance(conn, sourceAccountId, -amount); // Negative for deduction

            // 6. Update destination account balance (add amount)
            updateAccountBalance(conn, destinationAccountId, amount); // Positive for addition

            // 7. Create transaction record
            createTransactionRecord(conn, sourceAccountId, destinationAccountId, amount,
                    description != null && !description.trim().isEmpty() ? description : "Fund transfer");

            // Commit the transaction
            conn.commit();

            // Set success message and redirect
            request.setAttribute("transferResult", "Transfer completed successfully!");
            request.getRequestDispatcher("/pages/customer/transfer-funds.jsp").forward(request, response);

        } catch (SQLException e) {
            // Roll back the transaction on error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

            e.printStackTrace();
            setErrorAndRedirect(request, response, "Database error: " + e.getMessage());
            return;
        } finally {
            // Restore auto-commit mode and close connection
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
     * Set error message and redirect back to transfer form
     */
    private void setErrorAndRedirect(HttpServletRequest request, HttpServletResponse response, String errorMessage)
            throws ServletException, IOException {
        request.setAttribute("transferError", errorMessage);
        request.getRequestDispatcher("/pages/customer/transfer-funds.jsp").forward(request, response);
    }

    /**
     * Get account balance and verify ownership
     * 
     * @return account balance if found and owned by user, -1 otherwise
     */
    private double getAccountBalanceAndVerifyOwner(Connection conn, int accountId, int userId) throws SQLException {
        String sql = "SELECT balance FROM accounts WHERE id = ? AND user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, accountId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        }
        return -1;
    }

    /**
     * Get account ID by account number
     * 
     * @return account ID if found, -1 otherwise
     */
    private int getAccountIdByNumber(Connection conn, String accountNumber) throws SQLException {
        String sql = "SELECT id FROM accounts WHERE account_number = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }

    /**
     * Update account balance
     * 
     * @param amount Positive for deposit, negative for withdrawal
     */
    private void updateAccountBalance(Connection conn, int accountId, double amount) throws SQLException {
        String sql = "UPDATE accounts SET balance = balance + ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setInt(2, accountId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Failed to update account balance. Account may not exist.");
            }
        }
    }

    /**
     * Create transaction record
     */
    private void createTransactionRecord(Connection conn, int fromAccountId, int toAccountId, double amount,
            String description)
            throws SQLException {
        String sql = "INSERT INTO transactions (from_account_id, to_account_id, amount, description, type) VALUES (?, ?, ?, ?, 'transfer')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fromAccountId);
            stmt.setInt(2, toAccountId);
            stmt.setDouble(3, amount);
            stmt.setString(4, description);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Failed to create transaction record.");
            }
        }
    }
}
