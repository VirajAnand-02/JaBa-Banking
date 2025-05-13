<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.banking.model.User" %>
<%@ page import="com.banking.util.DatabaseUtil" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
    <title>Loan Request - JaBa Banking</title>
    <style>
        .loan-status-pending { color: #f39c12; }
        .loan-status-approved { color: #27ae60; }
        .loan-status-rejected { color: #e74c3c; }
        
        .loan-type-selector {
            display: flex;
            margin-bottom: 20px;
            flex-wrap: wrap;
            gap: 10px;
        }
        
        .loan-type-option {
            border: 1px solid #ddd;
            padding: 15px;
            border-radius: 5px;
            cursor: pointer;
            flex: 1;
            min-width: 150px;
            text-align: center;
            transition: all 0.3s;
        }
        
        .loan-type-option:hover {
            background-color: #f5f5f5;
        }
        
        .loan-type-option.selected {
            border-color: #3498db;
            background-color: rgba(52, 152, 219, 0.1);
        }
        
        .loan-type-option h4 {
            margin-top: 0;
        }
    </style>
</head>
<body class="dashboard-layout">
    <%
        // --- Security Check ---
        User user = (User)session.getAttribute("user");
        String contextPath = request.getContextPath();

        if (user == null || !"customer".equals(user.getRole())) {
            response.sendRedirect(contextPath + "/login.html?error=unauthorized_access");
            return;
        }
        
        // Get existing loan requests for this user - Convert to JSON manually
        String loanRequestsJson = "[]"; // Default empty array
        StringBuilder jsonBuilder = new StringBuilder("[");
        
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT loanid, amount, type, date, status, adminComment FROM loans WHERE userid = ? ORDER BY date DESC";
            boolean first = true;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, user.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        if (!first) {
                            jsonBuilder.append(",");
                        }
                        first = false;
                        
                        jsonBuilder.append("{");
                        jsonBuilder.append("\"id\":").append(rs.getInt("loanid")).append(",");
                        jsonBuilder.append("\"amount\":").append(rs.getDouble("amount")).append(",");
                        
                        String type = rs.getString("type");
                        jsonBuilder.append("\"type\":\"").append(type != null ? type.replace("\"", "\\\"") : "").append("\",");
                        
                        String date = rs.getString("date");
                        jsonBuilder.append("\"date\":\"").append(date != null ? date : "").append("\",");
                        
                        String status = rs.getString("status");
                        jsonBuilder.append("\"status\":\"").append(status != null ? status : "pending").append("\",");
                        
                        String comment = rs.getString("adminComment");
                        if (comment != null) {
                            jsonBuilder.append("\"adminComment\":\"").append(comment.replace("\"", "\\\"")).append("\"");
                        } else {
                            jsonBuilder.append("\"adminComment\":null");
                        }
                        
                        jsonBuilder.append("}");
                    }
                }
            }
            jsonBuilder.append("]");
            loanRequestsJson = jsonBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            // Create a visible error message on the page instead of failing silently
            request.setAttribute("dbError", "Error loading loan data: " + e.getMessage());
        }
        
        // Get loan application result if available
        String loanResult = (String)request.getAttribute("loanResult");
        String loanError = (String)request.getAttribute("loanError");
    %>

    <div class="dashboard-wrapper">
        <header class="dashboard-header">
            <h1>💲 JaBa Banking 💲</h1>
            <div class="user-info">
                Welcome, <strong><%= user.getName() %></strong> |
                <a href="<%=contextPath%>/logout">Logout</a>
            </div>
            <!-- Dark Mode Toggle Button -->
            <button id="darkModeToggle" class="dark-mode-toggle" aria-label="Toggle dark mode">
                <svg class="moon-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                    <path d="M21 12.79A9 9 0 1 1 11.21 3 A7 7 0 0 0 21 12.79z"/>
                </svg>
                <svg class="sun-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" 
                    stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" 
                    stroke-linejoin="round">
                    <circle cx="12" cy="12" r="5"/>
                    <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
                </svg>
            </button>
        </header>

        <div class="dashboard-content">
            <div class="sidebar">
                <nav>
                    <ul>
                        <li><a href="<%=contextPath%>/pages/customer/dashboard.jsp">Dashboard</a></li>
                        <li><a href="<%=contextPath%>/pages/customer/transfer-funds.jsp">Transfer Funds</a></li>
                        <li class="active"><a href="<%=contextPath%>/pages/customer/loan.jsp">Loan</a></li>
                        <li><a href="<%=contextPath%>/pages/customer/settings.jsp">Settings</a></li>
                    </ul>
                </nav>
            </div>

            <div class="main-content">
                <h2>Loan Application</h2>
                
                <!-- Display any JSP errors if they occurred -->
                <% if(request.getAttribute("dbError") != null) { %>
                    <div class="notification error">
                        Database Error: <%= request.getAttribute("dbError") %>
                    </div>
                <% } %>
                
                <!-- Notifications container - will be populated by JavaScript -->
                <div id="notificationsContainer"></div>

                <div class="dashboard-card">
                    <h3>Apply for a Loan</h3>
                    <form id="loanForm" action="<%=contextPath%>/api/loan-request" method="post" class="form-container">
                        <div class="form-group">
                            <label>Select Loan Type:</label>
                            <div class="loan-type-selector">
                                <div class="loan-type-option" data-type="personal">
                                    <h4>Personal Loan</h4>
                                    <p>For personal expenses, debt consolidation</p>
                                    <p><strong>Rate:</strong> 8-15%</p>
                                </div>
                                <div class="loan-type-option" data-type="auto">
                                    <h4>Auto Loan</h4>
                                    <p>For purchasing a new or used vehicle</p>
                                    <p><strong>Rate:</strong> 5-9%</p>
                                </div>
                                <div class="loan-type-option" data-type="mortgage">
                                    <h4>Mortgage</h4>
                                    <p>For home purchase or refinancing</p>
                                    <p><strong>Rate:</strong> 3-6%</p>
                                </div>
                            </div>
                            <input type="hidden" id="loanType" name="loanType" required>
                        </div>

                        <div class="form-group">
                            <label for="loanAmount">Loan Amount ($):</label>
                            <input type="number" id="loanAmount" name="loanAmount" min="1000" step="100" required
                                   placeholder="Enter loan amount (min $1,000)">
                        </div>

                        <div class="form-group">
                            <label for="loanPurpose">Purpose of Loan:</label>
                            <textarea id="loanPurpose" name="loanPurpose" rows="3" 
                                   placeholder="Briefly describe why you need this loan" required></textarea>
                        </div>

                        <div class="form-actions">
                            <button type="submit" class="action-button primary">Submit Loan Application</button>
                            <button type="reset" class="action-button secondary">Reset</button>
                        </div>
                    </form>
                </div>
                
                <div class="dashboard-card">
                    <h3>Your Loan Applications</h3>
                    <div id="loanRequestsContainer">
                        <!-- Loading indicator will be replaced by actual content -->
                        <div class="loading-indicator">Loading your loan applications...</div>
                    </div>
                </div>
            </div>
        </div>

        <footer>
            <p>© <%= new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) %> JaBa Banking. All rights reserved.</p>
        </footer>
    </div>

    <script src="<%=request.getContextPath()%>/js/darkmode.js"></script>
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            // Get notification values from server-side
            const loanResult = <%= loanResult != null ? "'" + loanResult + "'" : "null" %>;
            const loanError = <%= loanError != null ? "'" + loanError + "'" : "null" %>;
            
            // Display notifications if present
            const notificationsContainer = document.getElementById('notificationsContainer');
            
            if (loanResult) {
                const successNotification = document.createElement('div');
                successNotification.className = 'notification success';
                successNotification.textContent = loanResult;
                notificationsContainer.appendChild(successNotification);
            }
            
            if (loanError) {
                const errorNotification = document.createElement('div');
                errorNotification.className = 'notification error';
                errorNotification.textContent = loanError;
                notificationsContainer.appendChild(errorNotification);
            }
            
            // Handle loan type selection
            const loanTypeOptions = document.querySelectorAll('.loan-type-option');
            const loanTypeInput = document.getElementById('loanType');
            
            loanTypeOptions.forEach(option => {
                option.addEventListener('click', function() {
                    // Remove selected class from all options
                    loanTypeOptions.forEach(opt => opt.classList.remove('selected'));
                    
                    // Add selected class to clicked option
                    this.classList.add('selected');
                    
                    // Update hidden input with selected type
                    loanTypeInput.value = this.dataset.type;
                });
            });
            
            // Load user's loan applications from API instead of embedded JSON
            loadUserLoans();
            
            // Function to load loan requests from API
            async function loadUserLoans() {
                const container = document.getElementById('loanRequestsContainer');
                if (!container) return;
                
                try {
                    // Show loading state
                    container.innerHTML = '<div class="loading-indicator">Loading your loan applications...</div>';
                    
                    // Fetch loan data from the API with debugging
                    console.log("Fetching loan data from API...");
                    const response = await fetch('<%=contextPath%>/api/loan-data');
                    
                    if (!response.ok) {
                        throw new Error(`HTTP error ${response.status}`);
                    }
                    
                    const responseText = await response.text();
                    console.log("Raw API response:", responseText);
                    
                    // Parse JSON safely
                    let loanRequests;
                    try {
                        loanRequests = JSON.parse(responseText);
                        console.log("Parsed loan requests:", loanRequests);
                    } catch (jsonError) {
                        console.error("JSON parse error:", jsonError);
                        throw new Error("Invalid JSON response from server");
                    }
                    
                    // Check if we have a valid array
                    if (!Array.isArray(loanRequests)) {
                        console.error("Expected array, got:", typeof loanRequests, loanRequests);
                        throw new Error("Invalid data structure - expected array");
                    }
                    
                    if (loanRequests.length === 0) {
                        container.innerHTML = '<p>You don\'t have any loan applications yet.</p>';
                        return;
                    }
                    
                    // Create table using DOM methods instead of template literals
                    const table = document.createElement('table');
                    table.className = 'data-table';
                    
                    // Create table header
                    const thead = document.createElement('thead');
                    const headerRow = document.createElement('tr');
                    
                    const headers = ['ID', 'Date', 'Loan Type', 'Amount', 'Status', 'Comments'];
                    headers.forEach(headerText => {
                        const th = document.createElement('th');
                        th.textContent = headerText;
                        headerRow.appendChild(th);
                    });
                    
                    thead.appendChild(headerRow);
                    table.appendChild(thead);
                    
                    // Create table body
                    const tbody = document.createElement('tbody');
                    
                    loanRequests.forEach(loan => {
                        try {
                            // Format amount as currency
                            const formattedAmount = new Intl.NumberFormat('en-US', {
                                style: 'currency',
                                currency: 'USD'
                            }).format(loan.amount);
                            
                            // Parse and format the date safely
                            let formattedDate = 'Unknown Date';
                            try {
                                if (loan.date) {
                                    // Handle different date formats
                                    const dateObj = new Date(loan.date);
                                    if (!isNaN(dateObj.getTime())) {
                                        formattedDate = dateObj.toLocaleDateString('en-US', {
                                            year: 'numeric', 
                                            month: 'short', 
                                            day: 'numeric'
                                        });
                                    }
                                }
                            } catch (dateError) {
                                console.error("Error formatting date:", dateError, loan.date);
                            }
                            
                            // Extract main loan type from the potentially longer description
                            let loanType = loan.type || 'Unknown';
                            // If type contains a hyphen, take just the first part (e.g., "personal - Home renovation" -> "personal")
                            if (loanType.includes('-')) {
                                loanType = loanType.split('-')[0].trim();
                            }
                            // Capitalize first letter
                            loanType = loanType.charAt(0).toUpperCase() + loanType.slice(1);
                            
                            // Get status with appropriate CSS class
                            const status = loan.status || 'pending';
                            const statusClass = `loan-status-${status.toLowerCase()}`;
                            
                            // Create table row using DOM methods
                            const row = document.createElement('tr');
                            
                            // ID cell
                            const idCell = document.createElement('td');
                            idCell.textContent = '#' + loan.id;
                            row.appendChild(idCell);
                            
                            // Date cell
                            const dateCell = document.createElement('td');
                            dateCell.textContent = formattedDate;
                            row.appendChild(dateCell);
                            
                            // Loan Type cell
                            const typeCell = document.createElement('td');
                            typeCell.textContent = loanType;
                            row.appendChild(typeCell);
                            
                            // Amount cell
                            const amountCell = document.createElement('td');
                            amountCell.textContent = formattedAmount;
                            row.appendChild(amountCell);
                            
                            // Status cell
                            const statusCell = document.createElement('td');
                            const statusSpan = document.createElement('span');
                            statusSpan.className = statusClass;
                            statusSpan.textContent = status.toUpperCase();
                            statusCell.appendChild(statusSpan);
                            row.appendChild(statusCell);
                            
                            // Comments cell
                            const commentsCell = document.createElement('td');
                            commentsCell.textContent = loan.adminComment || '-';
                            row.appendChild(commentsCell);
                            
                            tbody.appendChild(row);
                        } catch (rowError) {
                            console.error("Error creating loan row:", rowError, loan);
                            const errorRow = document.createElement('tr');
                            const errorCell = document.createElement('td');
                            errorCell.colSpan = 6;
                            errorCell.className = 'error-row';
                            errorCell.textContent = 'Error displaying this loan';
                            errorRow.appendChild(errorCell);
                            tbody.appendChild(errorRow);
                        }
                    });
                    
                    table.appendChild(tbody);
                    container.innerHTML = '';
                    container.appendChild(table);
                    
                } catch (error) {
                    console.error("Error loading loan requests:", error);
                    
                    // Fall back to embedded data if API call fails
                    console.log("Falling back to embedded loan data");
                    
                    // Use the JSON string built in the JSP for fallback display
                    let loanRequests = <%= loanRequestsJson %>;
                    
                    if (!loanRequests || !Array.isArray(loanRequests) || loanRequests.length === 0) {
                        container.innerHTML = '<p>You don\'t have any loan applications yet.</p>';
                        return;
                    }
                    
                    // Create table using DOM methods
                    const table = document.createElement('table');
                    table.className = 'data-table';
                    
                    // Create table header
                    const thead = document.createElement('thead');
                    const headerRow = document.createElement('tr');
                    
                    const headers = ['ID', 'Date', 'Loan Type', 'Amount', 'Status', 'Comments'];
                    headers.forEach(headerText => {
                        const th = document.createElement('th');
                        th.textContent = headerText;
                        headerRow.appendChild(th);
                    });
                    
                    thead.appendChild(headerRow);
                    table.appendChild(thead);
                    
                    // Create table body
                    const tbody = document.createElement('tbody');
                    
                    loanRequests.forEach(loan => {
                        try {
                            // Format amount as currency
                            const formattedAmount = new Intl.NumberFormat('en-US', {
                                style: 'currency',
                                currency: 'USD'
                            }).format(loan.amount);
                            
                            // Parse and format the date
                            let formattedDate = 'Unknown Date';
                            try {
                                if (loan.date) {
                                    const dateObj = new Date(loan.date);
                                    if (!isNaN(dateObj.getTime())) {
                                        formattedDate = dateObj.toLocaleDateString('en-US', {
                                            year: 'numeric', 
                                            month: 'short', 
                                            day: 'numeric'
                                        });
                                    }
                                }
                            } catch (dateError) {
                                console.error("Error formatting date:", dateError, loan.date);
                            }
                            
                            // Extract main loan type
                            let loanType = loan.type || 'Unknown';
                            if (loanType.includes('-')) {
                                loanType = loanType.split('-')[0].trim();
                            }
                            loanType = loanType.charAt(0).toUpperCase() + loanType.slice(1);
                            
                            // Get status with appropriate CSS class
                            const status = loan.status || 'pending';
                            const statusClass = `loan-status-${status.toLowerCase()}`;
                            
                            // Create table row using DOM methods
                            const row = document.createElement('tr');
                            
                            // ID cell
                            const idCell = document.createElement('td');
                            idCell.textContent = '#' + loan.id;
                            row.appendChild(idCell);
                            
                            // Date cell
                            const dateCell = document.createElement('td');
                            dateCell.textContent = formattedDate;
                            row.appendChild(dateCell);
                            
                            // Loan Type cell
                            const typeCell = document.createElement('td');
                            typeCell.textContent = loanType;
                            row.appendChild(typeCell);
                            
                            // Amount cell
                            const amountCell = document.createElement('td');
                            amountCell.textContent = formattedAmount;
                            row.appendChild(amountCell);
                            
                            // Status cell
                            const statusCell = document.createElement('td');
                            const statusSpan = document.createElement('span');
                            statusSpan.className = statusClass;
                            statusSpan.textContent = status.toUpperCase();
                            statusCell.appendChild(statusSpan);
                            row.appendChild(statusCell);
                            
                            // Comments cell
                            const commentsCell = document.createElement('td');
                            commentsCell.textContent = loan.adminComment || '-';
                            row.appendChild(commentsCell);
                            
                            tbody.appendChild(row);
                        } catch (rowError) {
                            console.error("Error creating loan row:", rowError, loan);
                            const errorRow = document.createElement('tr');
                            const errorCell = document.createElement('td');
                            errorCell.colSpan = 6;
                            errorCell.className = 'error-row';
                            errorCell.textContent = 'Error displaying this loan';
                            errorRow.appendChild(errorCell);
                            tbody.appendChild(errorRow);
                        }
                    });
                    
                    table.appendChild(tbody);
                    container.innerHTML = '';
                    container.appendChild(table);
                }
            }
            
            // Form validation
            const loanForm = document.getElementById('loanForm');
            if (loanForm) {
                loanForm.addEventListener('submit', function(event) {
                    if (!loanTypeInput.value) {
                        event.preventDefault();
                        alert('Please select a loan type');
                    }
                });
            }
        });
    </script>
</body>
</html>
