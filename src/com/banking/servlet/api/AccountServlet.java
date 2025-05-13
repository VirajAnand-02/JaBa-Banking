package com.banking.servlet.api;

import com.banking.model.User;
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

@WebServlet("/api/accounts")
public class AccountServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        // Get user from session
        Object userObj = request.getSession().getAttribute("user");
        if (userObj == null || !(userObj instanceof User)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Not authenticated\"}");
            return;
        }

        User user = (User) userObj;
        int userId = user.getId();
        String userRole = user.getRole();

        // Check if a different userId is requested
        String requestedUserIdParam = request.getParameter("userId");
        if (requestedUserIdParam != null) {
            try {
                int requestedUserId = Integer.parseInt(requestedUserIdParam);

                // Allow if it's the same user OR if user is admin/employee
                boolean isSameUser = (requestedUserId == userId);
                boolean isAdminOrEmployee = "admin".equalsIgnoreCase(userRole) ||
                        "employee".equalsIgnoreCase(userRole);

                if (!isSameUser && !isAdminOrEmployee) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\":\"Access denied\"}");
                    return;
                }

                // If authorized, use the requested ID
                userId = requestedUserId;
            } catch (NumberFormatException e) {
                // Invalid userId parameter - ignore and use current user's ID
            }
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT id, account_number, type, balance FROM accounts WHERE user_id = ? ORDER BY type";

            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"accounts\":[");

            boolean first = true;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        if (!first) {
                            jsonBuilder.append(",");
                        }
                        first = false;

                        // Use full account number instead of masking it
                        String accountNumber = rs.getString("account_number");

                        jsonBuilder.append("{")
                                .append("\"id\":").append(rs.getInt("id")).append(",")
                                .append("\"accountNumber\":\"").append(accountNumber).append("\",")
                                .append("\"type\":\"").append(rs.getString("type")).append("\",")
                                .append("\"balance\":").append(rs.getDouble("balance"))
                                .append("}");
                    }
                }
            }

            jsonBuilder.append("]}");
            response.getWriter().write(jsonBuilder.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Database error\"}");
        }
    }
}
