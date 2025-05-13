<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.banking.model.User" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
    <title>Loan Requests - Administrator Portal</title>
</head>
<body class="dashboard-layout">
    <%
        User user = (User)session.getAttribute("user");
        String contextPath = request.getContextPath();
        if (user == null || !"admin".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(contextPath + "/login.html?error=unauthorized_access");
            return;
        }
        String currentPage = "loan-request.jsp"; // For active sidebar link
    %>

    <div class="dashboard-wrapper">
        <header class="dashboard-header">
            <h1>🛡️ Administrator Portal</h1>
             <div style="display: flex; align-items: center; gap: 1rem;">
                <div class="user-info">Admin: <strong><%= user.getName() %></strong> | <a href="<%=contextPath%>/logout">Logout</a></div>
                <button id="darkModeToggle" class="dark-mode-toggle" aria-label="Toggle dark mode">
                    <svg class="moon-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path d="M21 12.79A9 9 0 1 1 11.21 3 A7 7 0 0 0 21 12.79z" /></svg>
                    <svg class="sun-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5" /><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
                </button>
            </div>
        </header>

        <div class="dashboard-content">
            <div class="sidebar">
                <nav>
                    <ul>
                        <%-- Adjust sidebar links based on actual Admin pages --%>
                        <li class="<%= "dashboard.jsp".equals(currentPage) ? "active" : "" %>"><a href="<%= contextPath %>/pages/admin/dashboard.jsp">Main Dashboard</a></li>
                        <li class="<%= "loan-request.jsp".equals(currentPage) ? "active" : "" %>"><a href="<%= contextPath %>/pages/admin/loan-request.jsp">Loan Request</a></li>
                        <li class="<%= "user-management.jsp".equals(currentPage) ? "active" : "" %>"><a href="<%= contextPath %>/pages/admin/user-management.jsp">User Management</a></li>
                        <li class="<%= "system-config.jsp".equals(currentPage) ? "active" : "" %>"><a href="<%= contextPath %>/pages/admin/system-config.jsp">System Config</a></li>
                    </ul>
                </nav>
            </div>

            <div class="main-content">
                <h2>Loan Request Management</h2>

                <div class="dashboard-card">
                    <h3>Loan Request Statistics</h3>
                    <div class="stats-container">
                        <div class="stat-item">
                            <span class="stat-label">Total Requests</span>
                            <span class="stat-value" id="totalRequestsStat">Loading...</span>
                        </div>
                        <div class="stat-item">
                            <span class="stat-label">Pending</span>
                            <span class="stat-value pending" id="pendingRequestsStat">Loading...</span>
                        </div>
                        <div class="stat-item">
                            <span class="stat-label">Approved</span>
                            <span class="stat-value approved" id="approvedRequestsStat">Loading...</span>
                        </div>
                        <div class="stat-item">
                            <span class="stat-label">Rejected</span>
                            <span class="stat-value rejected" id="rejectedRequestsStat">Loading...</span>
                        </div>
                    </div>
                </div>

                <div class="dashboard-card">
                    <h3>Pending Loan Requests</h3>
                    <div class="loan-table-container">
                        <table class="loan-requests-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Customer</th>
                                    <th>Amount</th>
                                    <th>Type</th>
                                    <th>Date</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody id="loanRequestsTableBody">
                                <tr>
                                    <td colspan="6" class="loading-indicator">Loading loan requests...</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <div id="paginationContainer" class="pagination-controls">
                        <%-- Pagination controls will be rendered here --%>
                    </div>
                </div>

                
            </div> <%-- End main-content --%>
        </div> <%-- End dashboard-content --%>

        <footer>
            <p>© <%= new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) %> JaBa Banking. Administrator Access.</p>
        </footer>
    </div> <%-- End dashboard-wrapper --%>

    <!-- Rejection Modal -->
    <div id="rejectLoanModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h3>Reject Loan Request</h3>
                <button type="button" class="modal-close-btn" onclick="closeRejectModal()" aria-label="Close">×</button>
            </div>
            <div class="modal-body">
                <form id="rejectLoanForm">
                    <input type="hidden" id="rejectLoanId" name="loanId">
                    <div class="form-group">
                        <label for="rejectReason">Rejection Reason:</label>
                        <textarea id="rejectReason" name="adminComment" rows="4" required placeholder="Please provide a reason for rejecting this loan request."></textarea>
                    </div>
                    <div class="form-group">
                        <button type="submit" class="action-button danger">Confirm Rejection</button>
                        <button type="button" class="action-button secondary" onclick="closeRejectModal()">Cancel</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Add the hidden context path for JavaScript use -->
    <input type="hidden" id="contextPath" value="<%=request.getContextPath()%>">
    
    <script src="<%=request.getContextPath()%>/js/darkmode.js"></script>
    <script src="<%=request.getContextPath()%>/js/admin/loan.js"></script>
    <script src="<%=request.getContextPath()%>/js/admin/loan-stats.js?v=<%=System.currentTimeMillis()%>"></script>
</body>
</html>