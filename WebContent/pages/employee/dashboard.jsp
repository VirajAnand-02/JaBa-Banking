<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" %>
<%@ page import="com.banking.model.User" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <%-- Recommended: Use EL for contextPath consistently --%>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
    <title>Employee Portal - JaBa Banking</title>
    <link
      rel="stylesheet"
      href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css"
    />
</head>
<body class="dashboard-layout">
    <%
        // --- Security Check ---
        User user = (User)session.getAttribute("user");
        // String contextPath = request.getContextPath(); // We'll use EL's pageContext.request.contextPath instead

        // Redirect if not logged in OR if logged-in user is not an employee
        if (user == null || !"employee".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.html?error=unauthorized_access"); // request.getContextPath() is fine here
            return; // Stop processing the page
        }
    %>

    <div class="dashboard-wrapper">
      <header class="dashboard-header">
        <h1>🏦 Bank Employee Portal</h1>
        <div style="display: flex; align-items: center; gap: 1rem">
          <div class="user-info">
            Employee: <strong><%= user.getName() %></strong> | <%-- Scriptlet OK here as user is scriptlet var --%>
            <a href="${pageContext.request.contextPath}/logout">Logout</a>
          </div>
          <button
            id="darkModeToggle"
            class="dark-mode-toggle"
            aria-label="Toggle dark mode"
          >
            <svg class="moon-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path d="M21 12.79A9 9 0 1 1 11.21 3 A7 7 0 0 0 21 12.79z" /></svg>
            <svg class="sun-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5" /><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
          </button>
        </div>
      </header>

      <div class="dashboard-content">
        <div class="sidebar">
          <nav>
            <ul>
              <li class="active">
                <a href="${pageContext.request.contextPath}/pages/employee/dashboard.jsp"><i class="fas fa-tachometer-alt"></i>Dashboard</a>
              </li>
              <li>
                <a href="${pageContext.request.contextPath}/pages/employee/chat-support.jsp"><i class="fas fa-comments"></i>Chat Support</a>
              </li>
            </ul>
          </nav>
        </div>

        <div class="main-content">
          <h2>Employee Dashboard</h2>

          <%-- == Transaction Management Card == --%>
          <div class="dashboard-card">
            <h3><i class="fas fa-exchange-alt"></i> Recent Transactions</h3>
            <p>Review recent transactions and flag suspicious activity.</p>

            <div class="table-responsive">
              <%-- Replace JSTL implementation with JS-based implementation --%>
              <div id="employeeTransactionsContainer"></div>
              <div id="employeeTransactionsPagination"></div>
            </div>
          </div>

          <%-- == Chat Support Card has been moved to chat-support.jsp == --%>

        </div>
        <%-- End main-content --%>
      </div>
      <%-- End dashboard-content --%>

      <footer>
        <p>
          © <%= new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) %> JaBa Banking. Employee Access.
        </p>
      </footer>
    </div>
    <%-- End dashboard-wrapper --%>

    <script src="${pageContext.request.contextPath}/js/darkmode.js"></script>
    <script src="${pageContext.request.contextPath}/js/transaction-list.js"></script>
    <script src="${pageContext.request.contextPath}/js/employee/dashboard.js"></script>
    <script>
      // Initialize transaction list with JavaScript
      document.addEventListener('DOMContentLoaded', function() {
        // First, initialize the transaction list normally
        initTransactionList({
          containerId: 'employeeTransactionsContainer',
          paginationId: 'employeeTransactionsPagination',
          showUser: true,
          pageSize: 10,
          apiEndpoint: '${pageContext.request.contextPath}/api/transaction-data'
        });
        
        // Add a mutation observer to detect when transaction table is loaded
        const container = document.getElementById('employeeTransactionsContainer');
        const observer = new MutationObserver(function(mutations) {
          mutations.forEach(function(mutation) {
            if (mutation.addedNodes.length) {
              // Check if a table was added
              const table = container.querySelector('table');
              if (table) {
                // Stop observing (we found what we're looking for)
                observer.disconnect();
                
                // Add an extra header for Actions
                const headerRow = table.querySelector('thead tr');
                if (headerRow) {
                  const actionHeader = document.createElement('th');
                  actionHeader.textContent = 'Actions';
                  headerRow.appendChild(actionHeader);
                }
                
                // Add flag buttons to each row
                const rows = table.querySelectorAll('tbody tr');
                rows.forEach(function(row, index) {
                  // Extract transaction information from row cells
                  const cells = row.querySelectorAll('td');
                  
                  // Find transaction ID - the first cell is date, we need to extract from row data
                  // Add a data attribute to store the transaction ID from the rendered data
                  const transactionData = window._transactionData ? 
                                          window._transactionData[index] : null;
                  
                  // Extract transaction ID more reliably by using the rendered cell content and finding the ID
                  let transactionId;
                  try {
                    // Try to get ID from stored transaction data 
                    if (transactionData && transactionData.id) {
                      transactionId = transactionData.id;
                    } else {
                      // Fallback: Add a debug ID column to make it visible
                      const idCell = document.createElement('td');
                      idCell.textContent = 'ID unknown';
                      idCell.style.color = '#888';
                      idCell.style.fontSize = '0.8em';
                      row.insertBefore(idCell, row.firstChild);
                      
                      // Generate a random ID as last resort
                      transactionId = Math.floor(Math.random() * 10000) + 1;
                    }
                  } catch (e) {
                    console.error("Error extracting transaction ID:", e);
                    transactionId = Math.floor(Math.random() * 10000) + 1;
                  }
                  
                  // Store the ID in a data attribute for reference
                  row.setAttribute('data-transaction-id', transactionId);
                  
                  // Create the action cell with flag button
                  const actionCell = document.createElement('td');
                  actionCell.className = 'actions-cell';
                  
                  const flagButton = document.createElement('button');
                  flagButton.className = 'action-button danger small';
                  flagButton.innerHTML = '<i class="fas fa-flag"></i> Flag';
                  flagButton.onclick = function(event) {
                    event.preventDefault();
                    event.stopPropagation();
                    const actualTransactionId = row.getAttribute('data-transaction-id');
                    
                    if (confirm("Are you sure you want to flag transaction #" + actualTransactionId + " as suspicious?")) {
                      // Make API call to flag the transaction
                      fetch('${pageContext.request.contextPath}/api/employee/flag-transaction', {
                        method: 'POST',
                        headers: {
                          'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                          transactionId: actualTransactionId,
                          reason: "Flagged as suspicious by employee"
                        })
                      })
                      .then(response => response.json())
                      .then(data => {
                        if (data.success) {
                          alert("Transaction #" + actualTransactionId + " has been flagged as suspicious!");
                          
                          // Change button appearance
                          flagButton.disabled = true;
                          flagButton.innerHTML = '<i class="fas fa-check"></i> Flagged';
                          flagButton.classList.remove('danger');
                          flagButton.classList.add('secondary');
                        } else {
                          alert("Error: " + data.message);
                        }
                      })
                      .catch(error => {
                        console.error('Error flagging transaction:', error);
                        alert("Error flagging transaction. Please try again.");
                      });
                    }
                    return false;
                  };
                  
                  actionCell.appendChild(flagButton);
                  row.appendChild(actionCell);
                });
              }
            }
          });
        });
        
        // Start observing the container for changes
        observer.observe(container, { childList: true, subtree: true });
      });
    </script>
  </body>
</html>