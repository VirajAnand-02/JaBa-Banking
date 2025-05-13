<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8" %>
<%@ page import="com.banking.model.User" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css" />
    <title>Chat Support - Employee Portal - JaBa Banking</title>
    <link
      rel="stylesheet"
      href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css"
    />
  </head>
  <body class="dashboard-layout">
    <%
        // --- Security Check ---
        User user = (User)session.getAttribute("user");
        String contextPath = request.getContextPath();

        // Redirect if not logged in OR if logged-in user is not an employee
        if (user == null || !"employee".equalsIgnoreCase(user.getRole())) {
            response.sendRedirect(contextPath + "/login.html?error=unauthorized_access");
            return; // Stop processing the page
        }
    %>

    <div class="dashboard-wrapper">
      <header class="dashboard-header">
        <h1>🏦 Bank Employee Portal</h1>
        <div style="display: flex; align-items: center; gap: 1rem">
          <div class="user-info">
            Employee: <strong><%= user.getName() %></strong> |
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
              <li>
                <a href="<%=contextPath%>/pages/employee/dashboard.jsp"><i class="fas fa-tachometer-alt"></i>Dashboard</a>
              </li>
              <li class="active">
                <a href="#"><i class="fas fa-comments"></i>Chat Support</a>
              </li>
            </ul>
          </nav>
        </div>

        <div class="main-content">
          <h2>Chat Support</h2>

          <%-- == Chat Support Section == --%>
          <div id="chat-support-section">
            <div class="chat-support-container">
              <%-- Panel 1: Ticket List - Now also a dashboard-card --%>
              <div class="dashboard-card ticket-list-panel">
                <h4><i class="fas fa-ticket-alt"></i> Open Tickets</h4>
                <%-- NOTE: This list is currently static. Needs dynamic population in a real app. --%>
                <ul class="ticket-list">
                  <li onclick="selectTicket('T1001', 'Alice Wonderland')">
                    <div class="ticket-info">
                      <span class="ticket-id">#T1001</span>
                      <span class="customer-name">Alice Wonderland</span>
                    </div>
                    <span class="ticket-preview">Issue with login...</span>
                  </li>
                  <li onclick="selectTicket('T1002', 'Bob The Builder')">
                    <div class="ticket-info">
                      <span class="ticket-id">#T1002</span>
                      <span class="customer-name">Bob The Builder</span>
                    </div>
                    <span class="ticket-preview">Question about fees...</span>
                  </li>
                  <li class="active-ticket" onclick="selectTicket('T1003', 'Charlie Brown')">
                    <div class="ticket-info">
                      <span class="ticket-id">#T1003</span>
                      <span class="customer-name">Charlie Brown</span>
                    </div>
                    <span class="ticket-preview">Transaction query...</span>
                  </li>
                  <li onclick="selectTicket('T1004', 'Diana Prince')">
                    <div class="ticket-info">
                      <span class="ticket-id">#T1004</span>
                      <span class="customer-name">Diana Prince</span>
                    </div>
                    <span class="ticket-preview">How to reset password?</span>
                  </li>
                </ul>
              </div>
              <%-- Panel 2: Chat Window - Now also a dashboard-card --%>
              <div class="dashboard-card chat-window-panel">
                <div class="chat-header">
                  <h4>Chat with <span id="chattingWith">Charlie Brown</span> (<span id="chatTicketId">#T1003</span>)</h4>
                </div>
                <%-- NOTE: Message area is currently static. Needs dynamic population/updating via JS. --%>
                <div class="message-area" id="messageArea">
                  <div class="message received">
                    <p>Hi, I have a question about a transaction on my account.</p>
                    <span class="timestamp">10:30 AM</span>
                  </div>
                  <div class="message sent">
                    <p>Hello Charlie, I can help with that. Which transaction are you referring to?</p>
                    <span class="timestamp">10:31 AM</span>
                  </div>
                  <div class="message received">
                    <p>It was on July 27th, for about $25. I don't recognize it.</p>
                    <span class="timestamp">10:32 AM</span>
                  </div>
                </div>
                <div class="chat-input-area">
                  <textarea id="chatMessageInput" placeholder="Type your message..."></textarea>
                  <button class="action-button primary" onclick="sendMessage()">
                    <i class="fas fa-paper-plane"></i> Send
                  </button>
                </div>
              </div>
              <%-- Panel 3: Chat Options - Now also a dashboard-card --%>
              <div class="dashboard-card chat-options-panel">
                <h4><i class="fas fa-info-circle"></i> Ticket Details</h4>
                <%-- NOTE: Details area is currently static. Needs dynamic population via JS. --%>
                <div id="ticketDetailsContent">
                  <p><strong>Customer:</strong> <span id="detailCustomerName">Charlie Brown</span></p>
                  <p><strong>Ticket ID:</strong> <span id="detailTicketId">#T1003</span></p>
                  <p><strong>Status:</strong> <span id="detailTicketStatus">Open</span></p>
                  <p><strong>Opened:</strong> July 28, 2024, 10:28 AM</p>
                  <p><strong>Account Type:</strong> Premium Savings</p>
                </div>
                <hr>
                <h4><i class="fas fa-cogs"></i> Actions</h4>
                <%-- NOTE: Buttons need corresponding JS/backend functionality. --%>
                <button class="action-button secondary full-width"><i class="fas fa-user-circle"></i> View Customer Profile</button>
                <button class="action-button secondary full-width"><i class="fas fa-history"></i> View Chat History</button>
                <button class="action-button secondary full-width"><i class="fas fa-sticky-note"></i> Add Internal Note</button>
                <button class="action-button warning full-width"><i class="fas fa-tag"></i> Add Tag/Category</button>
                <button class="action-button danger full-width"><i class="fas fa-times-circle"></i> Close Ticket</button>
              </div>
            </div>
          </div>

        </div>
        <%-- End main-content --%>
      </div>
      <%-- End dashboard-content --%>

      <footer>
        <p>
          © <%= new java.text.SimpleDateFormat("yyyy").format(new
          java.util.Date()) %> JaBa Banking. Employee Access.
        </p>
      </footer>
    </div>
    <%-- End dashboard-wrapper --%>

    <script src="<%=request.getContextPath()%>/js/darkmode.js"></script>
    <%-- The dashboard.js contains the chat functions for now. Consider splitting if it grows. --%>
    <script src="<%=request.getContextPath()%>/js/employee/dashboard.js"></script> 
  </body>
</html>
