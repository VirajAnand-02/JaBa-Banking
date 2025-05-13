<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.banking.model.User" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <title>Flagged Transactions - JaBa Banking</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" />
</head>
<body class="dashboard-layout">
    <%
        // --- Security Check ---
        User user = (User)session.getAttribute("user");

        // Redirect if not logged in OR if logged-in user is not an admin
        if (user == null || !"admin".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.html?error=unauthorized_access");
            return; // Stop processing the page
        }
    %>

    <div class="dashboard-wrapper">
      <header class="dashboard-header">
        <h1>🛡️ Administrator Portal</h1>
        <div style="display: flex; align-items: center; gap: 1rem">
          <div class="user-info">
            Admin: <strong><%= user.getName() %></strong> |
            <a href="${pageContext.request.contextPath}/logout">Logout</a>
          </div>
          <button id="darkModeToggle" class="dark-mode-toggle" aria-label="Toggle dark mode">
            <svg class="moon-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 A7 7 0 0 0 21 12.79z" />
            </svg>
            <svg class="sun-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="5" />
              <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
            </svg>
          </button>
        </div>
      </header>

      <div class="dashboard-content">
        <div class="sidebar">
          <nav>
            <ul>
              <li><a href="${pageContext.request.contextPath}/pages/admin/dashboard.jsp">Main Dashboard</a></li>
              <li><a href="${pageContext.request.contextPath}/pages/admin/loan-request.jsp">Loan Request</a></li>
              <li><a href="${pageContext.request.contextPath}/pages/admin/user-management.jsp">User Management</a></li>
              <li class="active"><a href="${pageContext.request.contextPath}/pages/admin/flagged-transactions.jsp">Flagged Transactions</a></li>
              <li><a href="${pageContext.request.contextPath}/pages/admin/system-config.jsp">System Config</a></li>
            </ul>
          </nav>
        </div>

        <div class="main-content">
          <h2>Flagged Transactions</h2>

          <div class="dashboard-card">
            <h3><i class="fas fa-flag"></i> Suspicious Transactions</h3>
            <p>Transactions that were flagged by employees as potentially suspicious.</p>

            <div class="table-responsive" id="flaggedTransactionsContainer">
              <div class="loading-indicator">Loading flagged transactions...</div>
            </div>
          </div>

        </div>
      </div>

      <footer>
        <p>© <%= new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) %> JaBa Banking. Administrator Access.</p>
      </footer>
    </div>

    <script src="${pageContext.request.contextPath}/js/darkmode.js"></script>
    <script>
      // Load flagged transactions
      document.addEventListener('DOMContentLoaded', function() {
        loadFlaggedTransactions();
      });

      async function loadFlaggedTransactions() {
        const container = document.getElementById('flaggedTransactionsContainer');
        
        try {
          const response = await fetch('${pageContext.request.contextPath}/api/admin/flagged-transactions');
          
          if (!response.ok) {
            throw new Error(`Server returned ${response.status}: ${response.statusText}`);
          }
          
          const data = await response.json();
          
          if (data.length === 0) {
            container.innerHTML = '<div class="no-requests">No flagged transactions found.</div>';
            return;
          }
          
          // Create table
          let html = `
            <table class="transaction-table">
              <thead>
                <tr>
                  <th>Date Flagged</th>
                  <th>Transaction ID</th>
                  <th>Transaction Date</th>
                  <th>Amount</th>
                  <th>Description</th>
                  <th>Type</th>
                  <th>Flagged By</th>
                  <th>Reason</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
          `;
          
          data.forEach(item => {
            const flagDate = new Date(item.flagDate).toLocaleString();
            const txDate = new Date(item.timestamp).toLocaleString();
            
            html += `
              <tr>
                <td>${flagDate}</td>
                <td>${item.transactionId}</td>
                <td>${txDate}</td>
                <td>$${parseFloat(item.amount).toFixed(2)}</td>
                <td>${item.description}</td>
                <td>${item.type}</td>
                <td>${item.employeeName}</td>
                <td>${item.reason}</td>
                <td>
                  <span class="status-badge ${item.status}">${item.status}</span>
                </td>
                <td>
                  <button class="action-button small" onclick="updateFlagStatus(${item.id}, 'resolved')">
                    Mark Resolved
                  </button>
                </td>
              </tr>
            `;
          });
          
          html += '</tbody></table>';
          container.innerHTML = html;
          
        } catch (error) {
          console.error('Error loading flagged transactions:', error);
          container.innerHTML = `<div class="error-message">Error loading data: ${error.message}</div>`;
        }
      }
      
      async function updateFlagStatus(flagId, status) {
        try {
          const response = await fetch('${pageContext.request.contextPath}/api/admin/update-flag-status', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({ flagId, status })
          });
          
          if (!response.ok) {
            throw new Error(`Server returned ${response.status}`);
          }
          
          const data = await response.json();
          
          if (data.success) {
            alert('Status updated successfully');
            loadFlaggedTransactions(); // Refresh list
          } else {
            alert(`Failed to update status: ${data.message}`);
          }
        } catch (error) {
          console.error('Error updating flag status:', error);
          alert(`Error: ${error.message}`);
        }
      }
    </script>
  </body>
</html>
