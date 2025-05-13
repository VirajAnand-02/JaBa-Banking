<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.banking.model.User" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
    <title>User Management - Administrator Portal</title>
</head>
<body class="dashboard-layout">
    <%
        User user = (User)session.getAttribute("user");
        String contextPath = request.getContextPath();
        if (user == null || !"admin".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(contextPath + "/login.html?error=unauthorized_access");
            return;
        }
        String currentPage = "user-management.jsp"; // For active sidebar link
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
                        <li class="<%= "dashboard.jsp".equals(currentPage) ? "active" : "" %>">
                            <a href="<%= contextPath %>/pages/admin/dashboard.jsp">Main Dashboard</a>
                        </li>
                        <li class="<%= "loan-request.jsp".equals(currentPage) ? "active" : "" %>">
                            <a href="<%= contextPath %>/pages/admin/loan-request.jsp">Loan Request</a>
                        </li>
                        <li class="<%= "user-management.jsp".equals(currentPage) ? "active" : "" %>">
                            <a href="<%= contextPath %>/pages/admin/user-management.jsp">User Management</a>
                        </li>
                        <li class="<%= "system-config.jsp".equals(currentPage) ? "active" : "" %>">
                            <a href="<%= contextPath %>/pages/admin/system-config.jsp">System Config</a>
                        </li>
                    </ul>
                </nav>
            </div>

            <div class="main-content">
                <h2>User Management</h2>

                <!-- User Statistics -->
                <div class="dashboard-card">
                    <h3>User Statistics</h3>
                    <div class="stats-container">
                        <div class="stat-item">
                            <span class="stat-label">Total Users</span>
                            <span class="stat-value" id="totalUsersStat">Loading...</span>
                        </div>
                        <div class="stat-item">
                            <span class="stat-label">Employees</span>
                            <span class="stat-value employee" id="employeesStat">Loading...</span>
                        </div>
                        <div class="stat-item">
                            <span class="stat-label">Customers</span>
                            <span class="stat-value customer" id="customersStat">Loading...</span>
                        </div>
                        <div class="stat-item">
                            <span class="stat-label">Admins</span>
                            <span class="stat-value admin" id="adminsStat">Loading...</span>
                        </div>
                    </div>
                </div>

                <!-- User Approval Section -->
                <div class="dashboard-card">
                    <h3>Pending User Approvals</h3>
                    <div class="user-approval-container">
                        <table class="user-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Name</th>
                                    <th>Email</th>
                                    <th>Role</th>
                                    <th>Registration Date</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody id="inactiveUsersTableBody">
                                <tr>
                                    <td colspan="6" class="loading-indicator">Loading inactive users...</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Add User Section -->
                <div class="dashboard-card">
                    <h3>Add New User</h3>
                    <form id="addUserForm" class="admin-form">
                        <div class="form-row">
                            <div class="form-group">
                                <label for="newUserName">Full Name</label>
                                <input type="text" id="newUserName" name="name" required placeholder="Enter full name">
                            </div>
                            <div class="form-group">
                                <label for="newUserEmail">Email</label>
                                <input type="email" id="newUserEmail" name="email" required placeholder="Enter email address">
                            </div>
                        </div>

                        <div class="form-row">
                            <div class="form-group">
                                <label for="newUserPassword">Password</label>
                                <input type="password" id="newUserPassword" name="password" required placeholder="Enter password">
                            </div>
                            <div class="form-group">
                                <label for="newUserRole">Role</label>
                                <select id="newUserRole" name="role" required>
                                    <%-- <option value="">Select Role</option> --%>
                                    <option value="customer">Customer</option>
                                    <option value="employee">Employee</option>
                                    <%-- <option value="admin">Admin</option> --%>
                                </select>
                            </div>
                        </div>

                        <div class="form-actions">
                            <button type="submit" class="action-button">Create User</button>
                            <button type="reset" class="action-button secondary">Reset</button>
                        </div>
                    </form>
                </div>

                <!-- Search Users Section -->
                <div class="dashboard-card">
                    <h3>Search Users</h3>
                    <div class="search-container">
                        <div class="search-form">
                            <div class="form-group">
                                <input type="text" id="searchUserQuery" placeholder="Search by name, email or role">
                            </div>
                            <div class="form-group">
                                <select id="searchUserRole">
                                    <option value="all">All Roles</option>
                                    <option value="customer">Customers</option>
                                    <option value="employee">Employees</option>
                                    <option value="admin">Admins</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <button id="searchUserBtn" class="action-button">Search</button>
                            </div>
                        </div>
                    </div>

                    <div class="user-table-container">
                        <table class="user-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Name</th>
                                    <th>Email</th>
                                    <th>Role</th>
                                    <th>Status</th>
                                    <th>Created Date</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody id="userTableBody">
                                <tr>
                                    <td colspan="7" class="loading-indicator">Loading users...</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <div id="userTablePagination" class="pagination-controls">
                        <!-- Pagination controls will be rendered here -->
                    </div>
                </div>
            </div> <!-- End main-content -->
        </div> <!-- End dashboard-content -->

        <footer>
            <p>© <%= new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) %> JaBa Banking. Administrator Access.</p>
        </footer>
    </div> <!-- End dashboard-wrapper -->

    <!-- Edit User Modal -->
    <div id="editUserModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h3>Edit User</h3>
                <button type="button" class="modal-close-btn" onclick="closeEditUserModal()" aria-label="Close">×</button>
            </div>
            <div class="modal-body">
                <form id="editUserForm">
                    <input type="hidden" id="editUserId" name="userId">
                    
                    <div class="form-group">
                        <label for="editUserName">Full Name</label>
                        <input type="text" id="editUserName" name="name" required>
                    </div>
                    
                    <div class="form-group">
                        <label for="editUserEmail">Email</label>
                        <input type="email" id="editUserEmail" name="email" required readonly>
                        <small>Email cannot be changed</small>
                    </div>
                    
                    <div class="form-group">
                        <label for="editUserRole">Role</label>
                        <select id="editUserRole" name="role" required>
                            <option value="customer">Customer</option>
                            <option value="employee">Employee</option>
                            <option value="admin">Admin</option>
                        </select>
                    </div>
                    
                    <div class="form-group">
                        <label for="editUserStatus">Status</label>
                        <select id="editUserStatus" name="status" required>
                            <option value="active">Active</option>
                            <option value="inactive">Inactive</option>
                            <option value="locked">Locked</option>
                        </select>
                    </div>
                    
                    <div class="form-group reset-password-check">
                        <input type="checkbox" id="resetPassword" name="resetPassword">
                        <label for="resetPassword">Reset Password</label>
                    </div>
                    
                    <div id="newPasswordContainer" class="form-group" style="display: none;">
                        <label for="newPassword">New Password</label>
                        <input type="password" id="newPassword" name="newPassword" minlength="6">
                    </div>
                    
                    <div class="form-group">
                        <button type="submit" class="action-button">Save Changes</button>
                        <button type="button" class="action-button secondary" onclick="closeEditUserModal()">Cancel</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Add the hidden context path for JavaScript use -->
    <input type="hidden" id="contextPath" value="<%=request.getContextPath()%>">
    
    <script src="<%=request.getContextPath()%>/js/darkmode.js"></script>
    <script src="<%=request.getContextPath()%>/js/admin/user-management.js?v=<%=System.currentTimeMillis()%>"></script>
</body>
</html>
