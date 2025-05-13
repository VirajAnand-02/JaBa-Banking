package com.banking.servlet;

import com.banking.model.User;
import com.banking.util.DatabaseUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Servlet that handles loan approval requests
 */
@WebServlet("/api/approve-loan-request")
public class ApproveLoanServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Security check: ensure user is logged in and is an admin
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\":false,\"message\":\"You must be logged in to perform this action.\"}");
            return;
        }

        User currentUser = (User) session.getAttribute("user");
        if (!"admin".equalsIgnoreCase(currentUser.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print("{\"success\":false,\"message\":\"Admin access required to approve loans.\"}");
            return;
        }

        try {
            // Get loan ID from request
            String loanIdParam = request.getParameter("loanId");
            if (loanIdParam == null || loanIdParam.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"Loan ID is required.\"}");
                return;
            }

            int loanId = Integer.parseInt(loanIdParam);
            int adminId = currentUser.getId();

            // Get loan details to find userId and amount
            Connection conn = null;
            try {
                conn = DatabaseUtil.getConnection();
                conn.setAutoCommit(false); // Start transaction

                // First, get the loan details
                int userId = 0;
                double loanAmount = 0;
                String loanType = "";

                String getLoanSql = "SELECT userid, amount, type FROM loans WHERE loanid = ? AND status = 'pending'";
                try (PreparedStatement loanStmt = conn.prepareStatement(getLoanSql)) {
                    loanStmt.setInt(1, loanId);
                    try (ResultSet rs = loanStmt.executeQuery()) {
                        if (rs.next()) {
                            userId = rs.getInt("userid");
                            loanAmount = rs.getDouble("amount");
                            loanType = rs.getString("type");
                        } else {
                            conn.rollback();
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            out.print("{\"success\":false,\"message\":\"Loan not found or already processed.\"}");
                            return;
                        }
                    }
                }

                // Find the user's checking account
                int checkingAccountId = 0;
                String getAccountSql = "SELECT id FROM accounts WHERE user_id = ? AND type = 'checking' LIMIT 1";
                try (PreparedStatement accountStmt = conn.prepareStatement(getAccountSql)) {
                    accountStmt.setInt(1, userId);
                    try (ResultSet rs = accountStmt.executeQuery()) {
                        if (rs.next()) {
                            checkingAccountId = rs.getInt("id");
                        } else {
                            conn.rollback();
                            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            out.print("{\"success\":false,\"message\":\"User does not have a checking account.\"}");
                            return;
                        }
                    }
                }

                // Update loan status
                String updateLoanSql = "UPDATE loans SET status = 'approved', adminComment = 'Approved by admin ID: ' || ? WHERE loanid = ?";
                try (PreparedStatement updateLoanStmt = conn.prepareStatement(updateLoanSql)) {
                    updateLoanStmt.setInt(1, adminId);
                    updateLoanStmt.setInt(2, loanId);
                    updateLoanStmt.executeUpdate();
                }

                // Credit the account with the loan amount
                String creditAccountSql = "UPDATE accounts SET balance = balance + ? WHERE id = ?";
                try (PreparedStatement creditStmt = conn.prepareStatement(creditAccountSql)) {
                    creditStmt.setDouble(1, loanAmount);
                    creditStmt.setInt(2, checkingAccountId);
                    creditStmt.executeUpdate();
                }

                // Create a transaction record
                String createTransactionSql = "INSERT INTO transactions (from_account_id, to_account_id, type, amount, description) VALUES (NULL, ?, 'deposit', ?, ?)";
                try (PreparedStatement transactionStmt = conn.prepareStatement(createTransactionSql)) {
                    transactionStmt.setInt(1, checkingAccountId);
                    transactionStmt.setDouble(2, loanAmount);
                    transactionStmt.setString(3, "Loan disbursement - " + loanType);
                    transactionStmt.executeUpdate();
                }

                // Commit all changes
                conn.commit();
                out.print("{\"success\":true,\"message\":\"Loan #" + loanId +
                        " has been approved successfully. $" + String.format("%.2f", loanAmount) +
                        " has been credited to the user's account.\"}");

            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                throw e;
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
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Invalid loan ID format.\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Server error: " + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }
}
