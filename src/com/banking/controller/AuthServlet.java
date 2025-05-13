package com.banking.controller;

import com.banking.model.User; // Keep the User record/class import
import com.banking.util.DatabaseUtil; // Import your utility class

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
// No longer need PrintWriter if only redirecting
// import java.io.PrintWriter;

// The @WebServlet annotation relies on Servlet 3.0+ features.
// Ensure your web.xml is either absent or configured correctly
// (e.g., version="3.0" or higher and metadata-complete="false" or omitted).
// Also ensure Tomcat is configured to scan for annotations.
@WebServlet("/auth")
public class AuthServlet extends HttpServlet {

    // No AuthService instance needed as DatabaseUtil methods are static

    // init() method is not required anymore

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String email = req.getParameter("email");
        String password = req.getParameter("password");
        // Assuming role is passed for routing/authorization check during login
        // Note: DatabaseUtil.authenticate now also checks the role internally.
        String role = req.getParameter("role");

        // Basic validation for required parameters
        if (email == null || email.trim().isEmpty() ||
                password == null || password.isEmpty() || // Password shouldn't be trimmed
                role == null || role.trim().isEmpty()) {

            System.err.println("AuthServlet: Missing required parameters (email, password, role).");
            resp.sendRedirect(req.getContextPath() + "/login.html?error=missing_params");
            return; // Stop further processing
        }

        // Trim email and role for consistency
        email = email.trim();
        role = role.trim().toLowerCase(); // Standardize role to lowercase

        try {
            // First check if the account is pending approval
            if (DatabaseUtil.isAccountPendingApproval(email, role)) {
                System.err.println("AuthServlet: Account pending approval for email: " + email);
                resp.sendRedirect(req.getContextPath() + "/login.html?error=account_pending_approval");
                return;
            }

            // Use DatabaseUtil directly for authentication
            // It checks email, password, role match, and active status
            if (DatabaseUtil.authenticate(email, password, role)) {

                // Authentication successful, get user details (excluding sensitive info)
                User user = DatabaseUtil.getUserByEmail(email);

                if (user == null) {
                    // Should not happen if authenticate passed, but good practice to check
                    System.err.println(
                            "AuthServlet: Authentication succeeded but failed to retrieve user details for " + email);
                    resp.sendRedirect(req.getContextPath() + "/login.html?error=internal_error");
                    return;
                }

                // Create or get the session
                HttpSession session = req.getSession(true); // true = create if not exists

                // Store user information in the session
                // Use record-style accessors: user.id(), user.role(), user.name()
                session.setAttribute("user", user); // Store the whole User object (if needed downstream)
                session.setAttribute("userId", user.getId());
                session.setAttribute("userRole", user.getRole());
                session.setAttribute("userName", user.getName());

                System.out.println("AuthServlet: User '" + user.getEmail() + "' logged in successfully as '"
                        + user.getRole() + "'. Redirecting...");

                // Redirect based on the role confirmed by authentication
                switch (user.getRole()) { // Use the role from the retrieved User object for safety
                    case "customer":
                        resp.sendRedirect(req.getContextPath() + "/pages/customer/dashboard.jsp");
                        break;
                    case "employee": // Add if you have an employee role
                        resp.sendRedirect(req.getContextPath() + "/pages/employee/dashboard.jsp");
                        break;
                    case "admin":
                        resp.sendRedirect(req.getContextPath() + "/pages/admin/dashboard.jsp");
                        break;
                    default:
                        // Should not happen if roles are constrained in DB/Auth, but handle defensively
                        System.err.println("AuthServlet: Invalid role '" + user.getRole()
                                + "' encountered after successful login for " + email);
                        resp.sendRedirect(req.getContextPath() + "/login.html?error=invalid_role");
                }
            } else {
                // Authentication failed (user not found, wrong password, wrong role, inactive
                // status)
                System.out.println("AuthServlet: Authentication failed for email: " + email + ", role: " + role);
                resp.sendRedirect(req.getContextPath() + "/login.html?error=invalid_credentials");
            }
        } catch (Exception e) {
            // Catch potential SQLExceptions or other runtime errors from DatabaseUtil
            System.err.println("AuthServlet: Server error during authentication for " + email + ": " + e.getMessage());
            e.printStackTrace(); // Log the full stack trace to server logs
            resp.sendRedirect(req.getContextPath() + "/login.html?error=server_error");
        }
    }

    // Optional: Implement doGet if you want to handle direct access to /auth URL
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Typically, login actions are POST. GET might redirect to login page.
        resp.sendRedirect(req.getContextPath() + "/login.html?error=direct_access_not_allowed");
    }
}