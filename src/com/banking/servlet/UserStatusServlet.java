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
 * Servlet that handles user status updates
 */
@WebServlet("/api/users/*/status")
public class UserStatusServlet extends HttpServlet {

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
        Map<String, Object> statusData = null;
        try {
            statusData = JsonUtil.parseJson(buffer.toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Invalid JSON format");
            return;
        }

        // Validate status
        if (!statusData.containsKey("status")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Status is required");
            return;
        }

        String status = (String) statusData.get("status");
        if (!status.equals("active") && !status.equals("inactive") && !status.equals("locked")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Invalid status. Must be 'active', 'inactive', or 'locked'");
            return;
        }

        // Prevent admins from disabling themselves
        if (userId == currentUser.getId() && (status.equals("inactive") || status.equals("locked"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "You cannot disable your own account");
            return;
        }

        // Update user status
        boolean success = DatabaseUtil.updateUserStatus(userId, status);

        if (success) {
            sendJsonResponse(response, true, "User status updated successfully");
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Failed to update user status");
        }
    }

    /**
     * Extract user ID from path
     */
    private int extractUserIdFromPath(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "User ID is required");
            return -1;
        }

        // Path format should be "/{userId}/status"
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length < 2) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Invalid path format");
            return -1;
        }

        try {
            return Integer.parseInt(pathParts[1]);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Invalid user ID format");
            return -1;
        }
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
