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
    <title>Transfer Funds - JaBa Banking</title>
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
        
        // Fetch user accounts for dropdown
        List<Map<String, Object>> userAccounts = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT id, account_number, type, balance FROM accounts WHERE user_id = ? ORDER BY type";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, user.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> account = new HashMap<>();
                        account.put("id", rs.getInt("id"));
                        account.put("accountNumber", rs.getString("account_number"));
                        account.put("type", rs.getString("type"));
                        account.put("balance", rs.getDouble("balance"));
                        userAccounts.add(account);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Get transfer result if available
        String transferResult = (String)request.getAttribute("transferResult");
        String transferError = (String)request.getAttribute("transferError");
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
                        <li class="active"><a href="<%=contextPath%>/pages/customer/transfer-funds.jsp">Transfer Funds</a></li>
                        <li><a href="loan.jsp">Loan</a></li>
                        <li><a href="<%=contextPath%>/pages/customer/settings.jsp">Settings</a></li>
                    </ul>
                </nav>
            </div>

            <div class="main-content">
                <h2>Transfer Funds</h2>
                
                <!-- Notifications container - will be populated by JavaScript -->
                <div id="notificationsContainer"></div>

                <div class="dashboard-card">
                    <form id="transferForm" action="<%=contextPath%>/api/transfer-funds" method="post" class="form-container">
                        <div class="form-group">
                            <label for="fromAccount">From Account:</label>
                            <select id="fromAccount" name="fromAccount" required>
                                <option value="">Select Your Account</option>
                                <% for(Map<String, Object> account : userAccounts) { %>
                                    <option value="<%= account.get("id") %>" 
                                            data-balance="<%= account.get("balance") %>"
                                            data-account-number="<%= account.get("accountNumber") %>">
                                        <%= account.get("type").toString().substring(0, 1).toUpperCase() + 
                                           account.get("type").toString().substring(1) %> - 
                                        <%= account.get("accountNumber") %> 
                                        (Balance: $<%= String.format("%.2f", account.get("balance")) %>)
                                    </option>
                                <% } %>
                            </select>
                        </div>

                        <div class="form-group">
                            <label for="toAccountNumber">To Account Number:</label>
                            <input type="text" id="toAccountNumber" name="toAccountNumber" required
                                   placeholder="Enter recipient's account number">
                        </div>

                        <div class="form-group">
                            <label for="amount">Amount ($):</label>
                            <input type="number" id="amount" name="amount" min="0.01" step="0.01" required
                                   placeholder="Enter amount to transfer">
                            <p id="balanceWarning" class="error-message" style="display: none;">
                                Insufficient funds for this transfer!
                            </p>
                        </div>

                        <div class="form-group">
                            <label for="description">Description (optional):</label>
                            <input type="text" id="description" name="description" 
                                   placeholder="Enter a description for this transfer">
                        </div>

                        <div class="form-actions">
                            <button type="submit" class="action-button primary">Transfer Funds</button>
                            <button type="reset" class="action-button secondary">Reset</button>
                        </div>
                    </form>
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
            const transferResult = <%= transferResult != null ? "'" + transferResult + "'" : "null" %>;
            const transferError = <%= transferError != null ? "'" + transferError + "'" : "null" %>;
            
            // Display notifications if present
            const notificationsContainer = document.getElementById('notificationsContainer');
            
            if (transferResult) {
                const successNotification = document.createElement('div');
                successNotification.className = 'notification success';
                successNotification.textContent = transferResult;
                notificationsContainer.appendChild(successNotification);
            }
            
            if (transferError) {
                const errorNotification = document.createElement('div');
                errorNotification.className = 'notification error';
                errorNotification.textContent = transferError;
                notificationsContainer.appendChild(errorNotification);
            }
            
            // Initialize form validation and other functionality
            const form = document.getElementById('transferForm');
            const fromAccountSelect = document.getElementById('fromAccount');
            const amountInput = document.getElementById('amount');
            const balanceWarning = document.getElementById('balanceWarning');
            
            // Load recent transfers
            loadRecentTransfers();
            
            // Check available balance when amount is entered
            function validateAmount() {
                const selectedOption = fromAccountSelect.options[fromAccountSelect.selectedIndex];
                if (selectedOption.value) {
                    const balance = parseFloat(selectedOption.dataset.balance);
                    const amount = parseFloat(amountInput.value);
                    
                    if (amount > balance) {
                        balanceWarning.style.display = 'block';
                        return false;
                    } else {
                        balanceWarning.style.display = 'none';
                        return true;
                    }
                }
                return true;
            }
            
            amountInput.addEventListener('input', validateAmount);
            fromAccountSelect.addEventListener('change', validateAmount);
            
            form.addEventListener('submit', (event) => {
                if (!validateAmount()) {
                    event.preventDefault();
                    alert('Cannot transfer more than your available balance.');
                }
            });
            
            // Function to load recent transfers
            async function loadRecentTransfers() {
                const container = document.getElementById('recentTransfersContainer');
                try {
                    const response = await fetch('<%=contextPath%>/api/transaction-data?type=transfer&userId=<%= user.getId() %>&pageSize=5');
                    if (response.ok) {
                        const data = await response.json();
                        
                        if (data.transactions && data.transactions.length > 0) {
                            let html = '<table class="data-table"><thead><tr>' +
                                       '<th>Date</th><th>Description</th><th>From</th><th>To</th><th>Amount</th>' +
                                       '</tr></thead><tbody>';
                                       
                            data.transactions.forEach(tx => {
                                const formattedAmount = new Intl.NumberFormat('en-US', {
                                    style: 'currency',
                                    currency: 'USD'
                                }).format(tx.amount);
                                
                                html += `<tr>
                                    <td>${tx.date}</td>
                                    <td>${tx.description || 'Transfer'}</td>
                                    <td>${tx.fromAccount}</td>
                                    <td>${tx.toAccount}</td>
                                    <td class="${tx.isDebit ? 'debit' : 'credit'}">${formattedAmount}</td>
                                </tr>`;
                            });
                            
                            html += '</tbody></table>';
                            container.innerHTML = html;
                        } else {
                            container.innerHTML = '<p>No recent transfers found.</p>';
                        }
                    } else {
                        throw new Error('Failed to load recent transfers');
                    }
                } catch (error) {
                    console.error('Error loading recent transfers:', error);
                    container.innerHTML = '<p>Could not load recent transfers. Please try again later.</p>';
                }
            }
        });
    </script>
</body>
</html>
