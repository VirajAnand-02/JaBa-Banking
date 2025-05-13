package com.banking.servlet.api;

import com.banking.model.User;
import com.banking.util.DatabaseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import org.json.JSONObject;

/**
 * Servlet to handle flagging suspicious transactions
 */
@WebServlet("/api/employee/flag-transaction")
public class FlagTransactionServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Set response type to JSON
        response.setContentType("application/json");

        // Check if user is logged in and is an employee
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!"employee".equalsIgnoreCase(user.getRole())) {
            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        try {
            // Parse the request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject jsonRequest = new JSONObject(sb.toString());
            int transactionId = jsonRequest.getInt("transactionId");
            String reason = jsonRequest.optString("reason", "Flagged as suspicious");

            // Check if the transaction is already flagged
            if (DatabaseUtil.isTransactionFlagged(transactionId)) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Transaction is already flagged");
                return;
            }

            // Flag the transaction using the existing DatabaseUtil method
            boolean success = DatabaseUtil.flagTransaction(transactionId, user.getId(), reason);

            if (success) {
                JSONObject responseJson = new JSONObject();
                responseJson.put("success", true);
                responseJson.put("message", "Transaction successfully flagged");
                response.getWriter().write(responseJson.toString());
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to flag transaction");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request: " + e.getMessage());
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        JSONObject errorJson = new JSONObject();
        errorJson.put("success", false);
        errorJson.put("message", message);
        response.getWriter().write(errorJson.toString());
    }
}
