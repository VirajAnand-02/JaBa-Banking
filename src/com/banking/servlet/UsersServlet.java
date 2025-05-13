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
 * Servlet that handles user listing, searching and creation
 */
@WebServlet("/api/users")
public class UsersServlet extends HttpServlet {

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

        // Get parameters
        String searchQuery = request.getParameter("search");
        String roleFilter = request.getParameter("role");

        int page = 1;
        int pageSize = 10;

        try {
            String pageParam = request.getParameter("page");
            if (pageParam != null && !pageParam.trim().isEmpty()) {
                page = Integer.parseInt(pageParam);
            }

            String pageSizeParam = request.getParameter("pageSize");
            if (pageSizeParam != null && !pageSizeParam.trim().isEmpty()) {
                pageSize = Integer.parseInt(pageSizeParam);
            }
        } catch (NumberFormatException e) {
            // Use defaults if parsing fails
        }

        // Set content type
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Search users
        String usersJson = DatabaseUtil.searchUsers(searchQuery, roleFilter, page, pageSize);

        // Write response
        try (PrintWriter out = response.getWriter()) {
            out.print(usersJson);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
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

        // Validate required fields
        if (!validateUserData(userData, response)) {
            return;
        }

        // Create user object
        User newUser = new User();
        newUser.setName((String) userData.get("name"));
        newUser.setEmail((String) userData.get("email"));
        newUser.setRole((String) userData.get("role"));

        // Get password
        String password = (String) userData.get("password");

        // Register user
        boolean success = DatabaseUtil.registerUser(newUser, password);

        if (success) {
            response.setStatus(HttpServletResponse.SC_CREATED);
            sendJsonResponse(response, true, "User created successfully");
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Failed to create user. Email might already exist.");
        }
    }

    /**
     * Validate user data for creation
     */
    private boolean validateUserData(Map<String, Object> userData, HttpServletResponse response) throws IOException {
        if (userData == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "No data provided");
            return false;
        }

        // Check required fields
        if (!userData.containsKey("name") || !userData.containsKey("email") ||
                !userData.containsKey("password") || !userData.containsKey("role")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Missing required fields (name, email, password, role)");
            return false;
        }

        // Validate email format
        String email = (String) userData.get("email");
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Invalid email format");
            return false;
        }

        // Validate password length
        String password = (String) userData.get("password");
        if (password.length() < 6) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Password must be at least 6 characters");
            return false;
        }

        // Validate role
        String role = (String) userData.get("role");
        if (!role.equals("admin") && !role.equals("employee") && !role.equals("customer")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            sendJsonResponse(response, false, "Invalid role. Must be 'admin', 'employee', or 'customer'");
            return false;
        }

        return true;
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
