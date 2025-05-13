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
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
// import java.sql.SQLException;

/**
 * Servlet to provide loan data for customers and admins
 */
@WebServlet("/api/loan-data")
public class LoanDataServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Check if user is authenticated
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"You must be logged in to access loan data.\"}");
            return;
        }

        User currentUser = (User) session.getAttribute("user");
        int userId = currentUser.getId();
        String userRole = currentUser.getRole();

        // Get requested user ID parameter
        String requestedUserIdParam = request.getParameter("userId");
        Integer requestedUserId = null;

        if (requestedUserIdParam != null && !requestedUserIdParam.isEmpty()) {
            try {
                requestedUserId = Integer.parseInt(requestedUserIdParam);
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"Invalid user ID format.\"}");
                return;
            }

            // Security check: Only allow if accessing own data or admin/employee role
            boolean isOwnData = requestedUserId == userId;
            boolean hasAdminAccess = "admin".equalsIgnoreCase(userRole) ||
                    "employee".equalsIgnoreCase(userRole);

            if (!isOwnData && !hasAdminAccess) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"error\":\"You do not have permission to view this data.\"}");
                return;
            }
        } else {
            // If no user ID specified, use the current user's ID
            requestedUserId = userId;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT loanid, amount, type, date, status, adminComment FROM loans WHERE userid = ? ORDER BY date DESC";
            StringBuilder jsonBuilder = new StringBuilder("[");
            boolean first = true;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, requestedUserId);
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
                        jsonBuilder.append("\"type\":\"").append(escapeJson(type)).append("\",");

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
            jsonBuilder.append("]");
            out.print(jsonBuilder.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Failed to retrieve loan data: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Escape JSON strings properly
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }

        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
