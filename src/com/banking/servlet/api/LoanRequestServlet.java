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
import java.sql.SQLException;

@WebServlet("/api/loan-request")
public class LoanRequestServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Verify user is logged in and is a customer
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"You must be logged in to apply for a loan.\"}");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!"customer".equals(user.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Only customers can apply for loans.\"}");
            return;
        }

        // Get form parameters
        String loanType = request.getParameter("loanType");
        String loanAmountStr = request.getParameter("loanAmount");
        String loanPurpose = request.getParameter("loanPurpose");

        // Validate input
        if (loanType == null || loanAmountStr == null || loanPurpose == null ||
                loanType.trim().isEmpty() || loanAmountStr.trim().isEmpty() || loanPurpose.trim().isEmpty()) {
            setErrorAndRedirect(request, response, "All fields are required.");
            return;
        }

        double loanAmount;
        try {
            loanAmount = Double.parseDouble(loanAmountStr);
            if (loanAmount < 1000) {
                setErrorAndRedirect(request, response, "Loan amount must be at least $1,000.");
                return;
            }
        } catch (NumberFormatException e) {
            setErrorAndRedirect(request, response, "Invalid amount format.");
            return;
        }

        // Validate loan type
        if (!isValidLoanType(loanType)) {
            setErrorAndRedirect(request, response, "Invalid loan type selected.");
            return;
        }

        // Create the loan request
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "INSERT INTO loans (userid, amount, type, status, adminComment) VALUES (?, ?, ?, 'pending', NULL)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, user.getId());
                stmt.setDouble(2, loanAmount);
                String fullType = loanType + " - " + loanPurpose;
                stmt.setString(3, fullType);

                int result = stmt.executeUpdate();

                if (result > 0) {
                    request.setAttribute("loanResult",
                            "Your loan application has been submitted successfully and is pending review.");
                } else {
                    request.setAttribute("loanError", "Failed to submit your loan application. Please try again.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("loanError", "Database error: " + e.getMessage());
        }

        // Redirect back to the loan page
        request.getRequestDispatcher("/pages/customer/loan.jsp").forward(request, response);
    }

    private boolean isValidLoanType(String loanType) {
        return "personal".equals(loanType) || "auto".equals(loanType) || "mortgage".equals(loanType);
    }

    private void setErrorAndRedirect(HttpServletRequest request, HttpServletResponse response, String errorMessage)
            throws ServletException, IOException {
        request.setAttribute("loanError", errorMessage);
        request.getRequestDispatcher("/pages/customer/loan.jsp").forward(request, response);
    }
}
