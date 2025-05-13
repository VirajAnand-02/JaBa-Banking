package com.banking.controller; // Changed package to match your other controllers

import jakarta.servlet.ServletException; // Use jakarta namespace for Tomcat 10+
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/logout") // Maps requests to /YourAppContext/logout
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        System.out.println("LogoutServlet: Received GET request.");

        // 1. Get the current session, *don't* create one if it doesn't exist
        HttpSession session = req.getSession(false);

        if (session != null) {
            System.out.println("LogoutServlet: Session found (ID: " + session.getId() + "). Invalidating session.");
            // 2. Invalidate the session - removes all bound objects (like 'user')
            session.invalidate();
        } else {
            System.out.println("LogoutServlet: No active session found.");
        }

        // 3. Redirect the user to the login page
        String loginPage = req.getContextPath() + "/login.html";
        System.out.println("LogoutServlet: Redirecting to " + loginPage);
        resp.sendRedirect(loginPage);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // It's good practice to handle POST as well, in case a form submits here.
        // Simply delegate to doGet.
        System.out.println("LogoutServlet: Received POST request. Delegating to doGet.");
        doGet(req, resp);
    }
}