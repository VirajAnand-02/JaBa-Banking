package com.banking.controller;

import com.banking.model.User;
import com.banking.util.DatabaseUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession; // Import HttpSession
import java.io.IOException;
import java.net.URLEncoder; // Import URLEncoder
import java.nio.charset.StandardCharsets; // Import StandardCharsets

@WebServlet("/register") // Maps requests to /YourAppContext/register
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Get registration parameters from the request
        String name = req.getParameter("name");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");
        String requestedRole = req.getParameter("role");

        String contextPath = req.getContextPath();
        // Define standard redirect paths
        String adminDashboard = contextPath + "/pages/admin/dashboard.jsp"; // Admin page path
        String loginPage = contextPath + "/login.html"; // Public login page
        // Public registration page (if you still have one separate from login)
        // String publicRegisterPage = contextPath + "/register.html";

        // 2. Basic Server-Side Validation
        if (name == null || name.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.isEmpty() || // Don't trim password
            confirmPassword == null || confirmPassword.isEmpty()) {

            // Determine if this likely came from the admin modal based on referrer or a hidden field if needed
            // For now, assume if it fails basic validation and might have a role, it's the modal
            boolean possibleAdminOrigin = (requestedRole != null && !requestedRole.isEmpty());
            handleError(resp,
                        possibleAdminOrigin ? adminDashboard : loginPage, // Redirect appropriately
                        "Missing_required_fields.",
                        "Missing required registration fields.");
            System.out.println("Debug Missing Fields: " + name + " | " + email + " | " +  password + " | " + confirmPassword + " | Role: " + requestedRole);
            return;
        }

        // Trim inputs where appropriate
        name = name.trim();
        email = email.trim();

        if (!password.equals(confirmPassword)) {
            boolean possibleAdminOrigin = (requestedRole != null && !requestedRole.isEmpty());
            handleError(resp,
                        possibleAdminOrigin ? adminDashboard : loginPage,
                        "Passwords_do_not_match.",
                        "Passwords do not match for email: " + email);
            return;
        }

        // Optional: Add more validation (email format, password complexity) here

        // --- 3. Security Check: Determine who is making the request ---
        HttpSession session = req.getSession(false); // Don't create a session if none exists
        User requester = (session != null) ? (User) session.getAttribute("user") : null;
        boolean isAdminRequest = (requester != null && "admin".equalsIgnoreCase(requester.getRole()));

        // --- 4. Determine Role to Assign & Validate ---
        String roleToSave;
        String redirectOnSuccess;
        String redirectOnError; // Base URL for error redirects

        if (isAdminRequest) {
             redirectOnSuccess = adminDashboard + "?success=User_" + URLEncoder.encode(email, StandardCharsets.UTF_8.toString()) + "_created";
             redirectOnError = adminDashboard; // Errors go back to admin dashboard
             // Admin is submitting the form, role *must* be specified from modal
             if (requestedRole != null && !requestedRole.trim().isEmpty()) {
                roleToSave = requestedRole.trim().toLowerCase();
                // Validate admin-creatable roles
                if (!roleToSave.equals("employee") && !roleToSave.equals("admin")) {
                    handleError(resp, redirectOnError, "Invalid_role_specified_by_admin.", "Invalid role specified. Admins can only create Employee or Admin roles.");
                    return;
                }
                System.out.println("RegisterServlet: Admin (" + requester.getEmail() + ") creating user with role: " + roleToSave);
             } else {
                 // Admin used the modal but didn't select a role (client-side validation should prevent this)
                 handleError(resp, redirectOnError, "Role_not_specified_by_admin.", "Admin must specify a role when creating a user.");
                 return;
             }
        } else {
             // Non-admin request (assume self-registration attempt)
             redirectOnSuccess = loginPage + "?success=registered";
             redirectOnError = loginPage; // Errors go back to login page

             // Role should NOT be specified for self-registration
             if (requestedRole != null && !requestedRole.trim().isEmpty()) {
                 System.err.println("RegisterServlet: Security Alert: Non-admin attempted to specify role: " + requestedRole);
                 resp.sendRedirect(loginPage + "?error=invalid_registration_attempt"); // Generic error
                 return;
             }
             roleToSave = "customer"; // Default for self-registration
             System.out.println("RegisterServlet: Self-registration attempt for role: " + roleToSave);
        }


        // 5. Prepare User object
        User newUser = new User(name, email, roleToSave);

        // 6. Attempt registration using DatabaseUtil
        try {
            boolean registrationSuccess = DatabaseUtil.registerUser(newUser, password);

            if (registrationSuccess) {
                System.out.println("RegisterServlet: User registered successfully: " + email + " with role " + roleToSave);
                resp.sendRedirect(redirectOnSuccess);
            } else {
                // Registration failed - likely email already exists
                 String errorKey = "Email_" + URLEncoder.encode(newUser.getEmail(), StandardCharsets.UTF_8.toString()) + "_already_exists";
                 String logMessage = "Registration failed: Email '" + newUser.getEmail() + "' already exists.";
                 handleError(resp, redirectOnError, errorKey, logMessage);
            }
        } catch (Exception e) {
            String errorKey = "Server_error_during_registration";
            String logMessage = "Server error during registration for " + newUser.getEmail() + ": " + e.getMessage();
            System.err.println(logMessage);
            e.printStackTrace(); // Log full stack trace
            handleError(resp, redirectOnError, errorKey, logMessage);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // GET requests to /register likely indicate an error or direct access attempt.
        // Redirect to the main login page.
        System.out.println("RegisterServlet: GET request received, redirecting to login page.");
        resp.sendRedirect(req.getContextPath() + "/login.html");
    }

    /**
     * Helper method to handle error redirection, distinguishing between admin modal errors and other errors.
     * @param resp HttpServletResponse object
     * @param baseUrl The base URL to redirect to (e.g., admin dashboard or login page)
     * @param errorKey The key to use for the error parameter (used specifically for modal errors)
     * @param logMessage Message to log to console
     * @throws IOException
     */
    private void handleError(HttpServletResponse resp, String baseUrl, String errorKey, String logMessage) throws IOException {
         System.err.println("RegisterServlet Error: " + logMessage);
         // Check if the redirect is back to the admin dashboard to use the specific modal error parameter
         if (baseUrl.contains("/admin/dashboard.jsp")) {
             resp.sendRedirect(baseUrl + "?modal_error=" + errorKey);
         } else {
             // Otherwise, use a generic error parameter for the login/public registration page
             resp.sendRedirect(baseUrl + "?error=" + errorKey); // Or a more generic key like 'registration_failed'
         }
    }
}