package com.banking.servlet;

import com.banking.model.User;
import com.banking.util.DatabaseUtil;
import com.banking.util.JsonUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Servlet that handles individual user operations (get, update)
 */
@WebServlet("/api/users/*")
public class UserDetailsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Security check
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

        // Extract user ID from path
        int userId = extractUserIdFromPath(request, response);
        if (userId == -1)
            return; // Error already sent

        // Get user from database
        User user = DatabaseUtil.getUserById(userId);

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            sendJsonResponse(response, false, "User not found");
            return;
        }

        // Set content type
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Create JSON response
        String json = createUserJson(user);

        // Write response
        try (PrintWriter out = response.getWriter()) {
            out.print(json);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Security check
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

        // Extract user ID from path
        int userId = extractUserIdFromPath(request, response);
        if (userId == -1)
            return; // Error already sent

        // Read request body
        StringBuilder buffer = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        }

        // Parse JSON
        Map<String, Object> userData = null;
        try {
            userData = JsonUtil.parseJson(buffer.toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Invalid JSON format");
            return;
        }

        // Validate data
        if (!validateUpdateData(userData, response)) {
            return;
        }

        // Extract parameters
        String name = (String) userData.get("name");
        String role = (String) userData.get("role");
        String status = (String) userData.get("status");

        // Check if password reset is requested
        String newPassword = null;
        if (userData.containsKey("resetPassword") && (boolean) userData.get("resetPassword")) {
            if (userData.containsKey("newPassword")) {
                newPassword = (String) userData.get("newPassword");
            }
        }

        // Update user
        boolean success = DatabaseUtil.updateUser(userId, name, role, status, newPassword);

        if (success) {
            sendJsonResponse(response, true, "User updated successfully");
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Failed to update user");
        }
    }

    /**
     * Extract user ID from path
     */
    private int extractUserIdFromPath(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();

        // Check if path info exists and is valid format
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "User ID is required");
            return -1;
        }

        // Remove leading slash
        String idStr = pathInfo.substring(1);

        // If it contains another slash, extract just the ID part
        int slashIndex = idStr.indexOf('/');
        if (slashIndex != -1) {
            idStr = idStr.substring(0, slashIndex);
        }

        try {
            return Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Invalid user ID format");
            return -1;
        }
    }

    /**
     * Validate user data for update
     */
    private boolean validateUpdateData(Map<String, Object> userData, HttpServletResponse response) throws IOException {
        if (userData == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "No data provided");
            return false;
        }

        // Check required fields
        if (!userData.containsKey("name") || !userData.containsKey("role") || !userData.containsKey("status")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Missing required fields (name, role, status)");
            return false;
        }

        // Validate role
        String role = (String) userData.get("role");
        if (!role.equals("admin") && !role.equals("employee") && !role.equals("customer")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Invalid role. Must be 'admin', 'employee', or 'customer'");
            return false;
        }

        // Validate status
        String status = (String) userData.get("status");
        if (!status.equals("active") && !status.equals("inactive") && !status.equals("locked")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Invalid status. Must be 'active', 'inactive', or 'locked'");
            return false;
        }

        // Check password if reset is requested
        if (userData.containsKey("resetPassword") && (boolean) userData.get("resetPassword")) {
            if (!userData.containsKey("newPassword") || ((String) userData.get("newPassword")).length() < 6) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJsonResponse(response, false, "New password must be at least 6 characters");
                return false;
            }
        }

        return true;
    }

    /**
     * Create JSON response for a user
     */
    private String createUserJson(User user) {
        StringBuilder json = new StringBuilder();
        json.append("{")
                .append("\"id\":").append(user.getId()).append(",")
                .append("\"name\":\"").append(escapeJson(user.getName())).append("\",")
                .append("\"email\":\"").append(escapeJson(user.getEmail())).append("\",")
                .append("\"role\":\"").append(user.getRole()).append("\",")
                .append("\"status\":\"").append(user.getStatus()).append("\",")
                .append("\"createdAt\":\"").append(user.getCreatedAt()).append("\"")
                .append("}");
        return json.toString();
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

    /**
     * Send JSON response helper
     */
    private void sendJsonResponse(HttpServletResponse response, boolean success, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json = String.format("{\"success\":%s,\"message\":\"%s\"}", success, message);

        try (PrintWriter out = response.getWriter()) {
            out.print(json);
        }
    }
}
