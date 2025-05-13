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

/**
 * Servlet to handle loan rejection requests
 */
@WebServlet("/api/reject-loan")
public class LoanRejectionServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Set response type
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Check for authenticated admin session
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"Authentication required\"}");
            return;
        }

        User currentUser = (User) session.getAttribute("user");
        if (!"admin".equalsIgnoreCase(currentUser.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"success\":false,\"message\":\"Admin privileges required\"}");
            return;
        }

        // Get loan ID and rejection reason from request
        String loanIdParam = request.getParameter("loanId");
        String adminComment = request.getParameter("adminComment");

        if (loanIdParam == null || loanIdParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Loan ID is required\"}");
            return;
        }

        if (adminComment == null || adminComment.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Rejection reason is required\"}");
            return;
        }

        int loanId;
        try {
            loanId = Integer.parseInt(loanIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid loan ID format\"}");
            return;
        }

        // Reject the loan in database
        boolean success = DatabaseUtil.rejectLoan(loanId, currentUser.getId(), adminComment);

        // Send response
        PrintWriter out = response.getWriter();
        if (success) {
            out.write("{\"success\":true,\"message\":\"Loan #" + loanId + " has been rejected successfully\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(
                    "{\"success\":false,\"message\":\"Failed to reject loan. The loan may not exist or has already been processed.\"}");
        }
    }
}
