package com.banking.controller;

import com.banking.model.User;
import com.banking.util.DatabaseUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/change-password") // Matches form action
public class ChangePasswordServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        String contextPath = req.getContextPath();
        String settingsPage = contextPath + "/pages/customer/settings.jsp"; // Redirect back here

        // --- Security Check: Ensure user is logged in ---
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(contextPath + "/login.html?error=session_expired");
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getId(); // Get user ID from session

        // --- Get Parameters ---
        String oldPassword = req.getParameter("oldPassword");
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");

        // --- Server-Side Validation ---
        if (oldPassword == null || oldPassword.isEmpty() ||
            newPassword == null || newPassword.isEmpty() ||
            confirmPassword == null || confirmPassword.isEmpty()) {
            resp.sendRedirect(settingsPage + "?error=missing_params");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            resp.sendRedirect(settingsPage + "?error=password_mismatch");
            return;
        }

        // Optional: Add server-side password complexity rules here
        if (newPassword.length() < 6) {
             resp.sendRedirect(settingsPage + "?error=password_too_short"); // Example error
             return;
        }


        // --- Attempt Password Change ---
        try {
            boolean success = DatabaseUtil.changePassword(userId, oldPassword, newPassword);

            if (success) {
                System.out.println("Password changed successfully for user ID: " + userId);
                resp.sendRedirect(settingsPage + "?success=password_changed");
            } else {
                // changePassword logs specific reason (user not found / incorrect old pwd)
                // Provide a slightly more generic error back to user unless you check logs
                System.err.println("Password change failed for user ID: " + userId + " (Check DatabaseUtil logs for reason)");
                resp.sendRedirect(settingsPage + "?error=incorrect_old_password"); // Most likely reason if method returns false
            }
        } catch (Exception e) {
            System.err.println("Server error during password change for user ID: " + userId + ": " + e.getMessage());
            e.printStackTrace();
            resp.sendRedirect(settingsPage + "?error=server_error");
        }
    }

     @Override
     protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
         // Redirect GET requests back to settings page or dashboard
         resp.sendRedirect(req.getContextPath() + "/pages/customer/settings.jsp");
     }
}