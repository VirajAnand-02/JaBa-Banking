package com.banking.servlet;

import com.banking.util.DatabaseUtil;
import com.banking.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet that provides dashboard statistics for the admin panel
 */
@WebServlet("/api/admin-stats")
public class AdminStatsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Security check: ensure user is logged in and is an admin
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You must be logged in to access this resource");
            return;
        }

        User currentUser = (User) session.getAttribute("user");
        if (!"admin".equalsIgnoreCase(currentUser.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        // Fetch statistics from database
        int totalUsers = DatabaseUtil.getTotalUsersCount();
        int activeSessions = DatabaseUtil.getActiveSessionsCount();
        int pendingLoans = DatabaseUtil.getPendingLoansCount();
        int recentAlerts = DatabaseUtil.getRecentAlertsCount();

        // Return as JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (PrintWriter out = response.getWriter()) {
            out.print("{");
            out.print("\"totalUsers\":" + totalUsers + ",");
            out.print("\"activeSessions\":" + activeSessions + ",");
            out.print("\"pendingLoans\":" + pendingLoans + ",");
            out.print("\"recentAlerts\":" + recentAlerts);
            out.print("}");
        }
    }
}
