<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8" %> <%@ page import="com.banking.model.User" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css" />
    <!-- Add Chart.js library for transaction graph -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

    <title>Customer Dashboard - JaBa Banking</title>
  </head>
  <body class="dashboard-layout">
    <%
        // --- Security Check ---
        User user = (User)session.getAttribute("user");
        String contextPath = request.getContextPath(); // Get context path once

        if (user == null || !"customer".equals(user.getRole())) {
            response.sendRedirect(contextPath + "/login.html?error=unauthorized_access");
            return; // Stop processing the rest of the page
        }
    %>

    <div class="dashboard-wrapper">
      <header class="dashboard-header">
        <h1>💲 JaBa Banking 💲</h1>
        <div class="user-info">
          Welcome, <strong><%= user.getName() %></strong> |
          <a href="<%=contextPath%>/logout">Logout</a>
        </div>
        <!-- Dark Mode Toggle Button -->
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
      </header>

      <div class="dashboard-content">
        <div class="sidebar">
          <nav>
            <ul>
              <%-- Add 'active' class to the current page's link --%>
              <li class="active"><a href="#">Dashboard</a></li>
              <li><a href="transfer-funds.jsp">Transfer Funds</a></li>
              <li><a href="loan.jsp">Loan</a></li>
              <li><a href="settings.jsp">Settings</a></li>
            </ul>
          </nav>
        </div>

        <div class="main-content">
          <h2>Customer Dashboard</h2>

          <div class="dashboard-card account-summary">
            <h3>Account Summary</h3>
            <div class="account-cards">
              <%-- === Account Card 1 (Example - Replace with dynamic data) === --%>
              <div class="account-card">
                <h4>Savings Account</h4>
                <p class="account-number">**** 1234</p>
                <p class="account-balance">$2,500.00</p>
              </div>
              <%-- === Account Card 2 (Example - Replace with dynamic data) === --%>
              <div class="account-card">
                <h4>Checking Account</h4>
                <p class="account-number">**** 5678</p>
                <p class="account-balance">$1,350.75</p>
              </div>
              <%-- Add more cards dynamically using JSTL/scriptlets if needed --%>
            </div>
          </div>

          <div class="dashboard-card recent-transactions">
            <h3>Recent Transactions</h3>
            <div id="customerTransactionsContainer"></div>
            <div id="customerTransactionsPagination"></div>
          </div>

          <%-- Add more dashboard sections/cards here as needed --%>
        </div>
        <%-- End main-content --%>
      </div>
      <%-- End dashboard-content --%>

      <footer>
        <p>
          © <%= new java.text.SimpleDateFormat("yyyy").format(new
          java.util.Date()) %> JaBa Banking. All rights reserved.
        </p>
      </footer>
    </div>
    <%-- End dashboard-wrapper --%>

    <script src="<%=request.getContextPath()%>/js/darkmode.js"></script>
    <script src="<%=request.getContextPath()%>/js/transaction-list.js"></script>
    <script src="<%=request.getContextPath()%>/js/customer/dashboard.js"></script>
    <script src="<%=request.getContextPath()%>/js/transactionGraph.js"></script>
    <script>
      // Initialize transaction list after page loads
      document.addEventListener('DOMContentLoaded', function() {
        const userId = <%= user.getId() %>; // Get current user ID
        
        // Initialize transaction list with explicit API endpoint
        initTransactionList({
          containerId: 'customerTransactionsContainer',
          paginationId: 'customerTransactionsPagination',
          showUser: false, // User doesn't need to see their own ID
          pageSize: 5,
          apiEndpoint: '<%=request.getContextPath()%>/api/transaction-data',
          useDummyData: true // Allow fallback to mock data if API fails
        });
        
        // Use mock account data as fallback
        fetchAccountData();
      });
      
      // Function to fetch real account data for the current user
      async function fetchAccountData() {
        try {
          console.log("Fetching account data...");
          // Use session authentication - no need to pass userId in URL
          const response = await fetch('<%=request.getContextPath()%>/api/accounts');
          
          console.log("Account API response status:", response.status);
          
          if (!response.ok) {
            console.warn('Account API returned error:', response.status);
            // Instead of throwing, use mock account data
            useMockAccountData();
            return;
          }
          
          const responseData = await response.json();
          console.log("Account data received:", responseData);
          
          if (responseData.accounts && Array.isArray(responseData.accounts) && responseData.accounts.length > 0) {
            console.log(`Found ${responseData.accounts.length} accounts, updating cards...`);
            
            // Fix: Create a new array with plain JavaScript objects
            const accounts = responseData.accounts.map(account => ({
              id: account.id,
              type: String(account.type),
              accountNumber: String(account.accountNumber),
              balance: Number(account.balance)
            }));
            
            console.log("Processed accounts for display:", accounts);
            updateAccountCards(accounts);
          } else {
            console.warn('No account data returned from API or invalid format:', responseData);
            useMockAccountData();
          }
        } catch (error) {
          console.error('Error fetching account data:', error);
          useMockAccountData();
        }
      }
      
      // Update account cards with real data
      function updateAccountCards(accounts) {
        try {
          const accountCardsContainer = document.querySelector('.account-cards');
          if (!accountCardsContainer) {
            console.error("Account cards container not found!");
            return;
          }
          
          console.log("Account container before clearing:", accountCardsContainer.innerHTML);
          accountCardsContainer.innerHTML = '';
          
          accounts.forEach(account => {
            // Simple direct property access
            const id = account.id;
            const type = account.type;
            const number = account.accountNumber;
            const balance = account.balance;
            
            console.log("Creating card for account: type=" + type + ", number=" + number + ", balance=" + balance);
            



            console.log(`================================`);

            console.log(`${number}`);
            console.log(number);

            console.log(`================================`);





            if (!type || !number) {
              console.warn("Invalid account data:", account);
              return; // Skip this account
            }
            
            // Format the balance as currency
            const formattedBalance = new Intl.NumberFormat('en-US', {
              style: 'currency',
              currency: 'USD'
            }).format(balance || 0);
            
            // Create the title with first letter capitalized
            const accountTitle = type.charAt(0).toUpperCase() + type.slice(1) + ' Account';
            
            // Create HTML elements instead of using template literals
            const accountCard = document.createElement('div');
            accountCard.className = 'account-card';
            
            const titleElement = document.createElement('h4');
            titleElement.textContent = accountTitle;
            
            const numberElement = document.createElement('p');
            numberElement.className = 'account-number';
            numberElement.textContent = number;
            
            const balanceElement = document.createElement('p');
            balanceElement.className = 'account-balance';
            balanceElement.textContent = formattedBalance;
            
            // Append elements
            accountCard.appendChild(titleElement);
            accountCard.appendChild(numberElement);
            accountCard.appendChild(balanceElement);
            
            // Add the card to the container
            accountCardsContainer.appendChild(accountCard);
            console.log("Added card for " + accountTitle + " with balance " + formattedBalance);
          });
          
          console.log("Updated account cards:", accountCardsContainer.innerHTML);
        } catch (err) {
          console.error("Error updating account cards:", err);
          useMockAccountData();
        }
      }
      
      // Use mock data for accounts as fallback
      function useMockAccountData() {
        console.log("Using mock account data");
        const mockAccounts = [
          { 
            id: 1, 
            type: "checking", 
            accountNumber: "**** 4321", 
            balance: 1350.75 
          },
          { 
            id: 2, 
            type: "savings", 
            accountNumber: "**** 1234", 
            balance: 2500.00 
          }
        ];
        
        updateAccountCards(mockAccounts);
      }
    </script>
    
    <!-- Add additional debugging to check if script is running -->
    <script>
      console.log("Customer dashboard scripts loaded at", new Date().toLocaleTimeString());
      console.log("User ID from JSP:", <%= user.getId() %>);
      // Check DOM elements
      window.addEventListener('load', function() {
        console.log("Window loaded, checking account cards container...");
        const container = document.querySelector('.account-cards');
        console.log("Account cards container:", container);
        if (container) {
          console.log("Container HTML:", container.innerHTML);
        }
      });
    </script>
  </body>
</html>
