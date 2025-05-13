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
import org.json.JSONObject;

/**
 * Servlet to handle fetching and managing flagged transactions
 */
@WebServlet(urlPatterns = {
        "/api/admin/flagged-transactions",
        "/api/admin/update-flag-status"
})
public class FlaggedTransactionsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Set response type to JSON
        response.setContentType("application/json");

        // Check if user is logged in and is an admin
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Not authenticated\"}");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!"admin".equalsIgnoreCase(user.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Access denied\"}");
            return;
        }

        // Get flagged transactions as JSON
        String jsonData = DatabaseUtil.getFlaggedTransactionsAsJson();
        response.getWriter().write(jsonData);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // This handles the update-flag-status endpoint
        if (!request.getServletPath().equals("/api/admin/update-flag-status")) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Set response type to JSON
        response.setContentType("application/json");

        // Check if user is logged in and is an admin
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"Not authenticated\"}");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!"admin".equalsIgnoreCase(user.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"success\":false,\"message\":\"Access denied\"}");
            return;
        }

        try {
            // Parse the request body
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }

            // JSONObject jsonRequest = new JSONObject(sb.toString());
            // int flagId = jsonRequest.getInt("flagId");
            // String status = jsonRequest.getString("status");

            // TODO: Add updateFlagStatus method to DatabaseUtil.java
            // boolean success = DatabaseUtil.updateFlagStatus(flagId, status);
            boolean success = true; // Placeholder

            JSONObject responseJson = new JSONObject();
            responseJson.put("success", success);
            if (success) {
                responseJson.put("message", "Flag status updated successfully");
            } else {
                responseJson.put("message", "Failed to update flag status");
            }
            response.getWriter().write(responseJson.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid request: " + e.getMessage() + "\"}");
        }
    }
}
