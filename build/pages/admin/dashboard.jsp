<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8" %> <%@ page import="com.banking.model.User" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css" />
    <title>Administrator Portal - JaBa Banking</title>
    <!-- Add Chart.js CDN -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
  </head>
  <body class="dashboard-layout">
    <%  // --- Security Check ---
        User user = (User)session.getAttribute("user");
        String contextPath = request.getContextPath();

        // Redirect if not logged in OR if logged-in user is not an admin
        if (user == null || !"admin".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(contextPath + "/login.html?error=unauthorized_access");
            return; // Stop processing the page
        }
    %>

    <div class="dashboard-wrapper">
      <header class="dashboard-header">
        <h1>🛡️ Administrator Portal</h1>
        <div style="display: flex; align-items: center; gap: 1rem">
          <div class="user-info">
            Admin: <strong><%= user.getName() %></strong> |
            <a href="<%=contextPath%>/logout">Logout</a>
          </div>
          <button
            id="darkModeToggle"
            class="dark-mode-toggle"
            aria-label="Toggle dark mode"
          >
            <svg
              class="moon-icon"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
            >
              <path d="M21 12.79A9 9 0 1 1 11.21 3 A7 7 0 0 0 21 12.79z" />
            </svg>
            <svg
              class="sun-icon"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
              fill="none"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="12" cy="12" r="5" />
              <path
                d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"
              />
            </svg>
          </button>
        </div>
      </header>

      <div class="dashboard-content">
        <div class="sidebar">
          <nav>
            <ul>
              <li class="active">
                <a href="#">Main Dashboard</a>
              </li>
              <li>
                <a href="loan-request.jsp">Loan Request</a>
              </li>
              <li>
                <a href="user-management.jsp">User Management</a>
              </li>
              <li>
                <a href="system-config.jsp">System Config</a>
              </li>
            </ul>
          </nav>
        </div>

        <div class="main-content">
          <h2>Admin Dashboard</h2>

          <%-- == Summary Stats Card == --%>
          <div class="dashboard-card">
            <h3>System Overview</h3>
            <div id="statsContainer" class="stats-container">
              <div class="stat-item">
                <span class="stat-label">Total Users</span>
                <span class="stat-value" id="totalUsers">Loading...</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Active Sessions</span>
                <span class="stat-value" id="activeSessions">Loading...</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Pending Loans</span>
                <span class="stat-value" id="pendingLoans">Loading...</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Recent Alerts</span>
                <span class="stat-value" id="recentAlerts">Loading...</span>
              </div>
            </div>
          </div>

          <%-- == Transaction Volume Graph Card == --%>
          <div class="dashboard-card transaction-graph">
            <h3>Transaction Volume - Last 7 Days</h3>
            <div class="chart-container">
              <canvas id="transactionVolumeChart"></canvas>
            </div>
          </div>

          <%-- == Recent Transactions Card == --%>
          <div class="dashboard-card">
            <h3>Recent Transactions</h3>
            <div id="adminTransactionsContainer"></div>
            <div id="adminTransactionsPagination"></div>
          </div>

        </div>
        <%-- End main-content --%>
      </div>
      <%-- End dashboard-content --%>

      <footer>
        <p>
          © <%= new java.text.SimpleDateFormat("yyyy").format(new
          java.util.Date()) %> JaBa Banking. Administrator Access.
        </p>
      </footer>
    </div>
    <%-- End dashboard-wrapper --%> <%-- ============================ --%> <%--
    === Create User Modal HTML === --%> <%-- ============================ --%>

    <script src="<%=request.getContextPath()%>/js/darkmode.js?v=<%=System.currentTimeMillis()%>"></script>
    <script src="<%=request.getContextPath()%>/js/admin/transaction-graph.js?v=<%=System.currentTimeMillis()%>"></script>
    <script src="<%=request.getContextPath()%>/js/transaction-list.js?v=<%=System.currentTimeMillis()%>"></script>
    <script>
      // Initialize transaction list after page loads
      document.addEventListener('DOMContentLoaded', function() {
        initTransactionList({
          containerId: 'adminTransactionsContainer',
          paginationId: 'adminTransactionsPagination',
          showUser: true,
          pageSize: 5,
          apiEndpoint: '<%=request.getContextPath()%>/api/admin-transactions',
          useDummyData: false,
          fetchParams: {
            headers: {
              'Content-Type': 'application/json',
              'X-Admin-Auth': 'true'
            }
          }
        });
      });
      
      // Make contextPath available to other scripts
      const contextPath = '<%=request.getContextPath()%>';
    </script>
    <script src="<%=request.getContextPath()%>/js/admin/dashboard-stats.js?v=<%=System.currentTimeMillis()%>"></script>
  </body>
</html>
