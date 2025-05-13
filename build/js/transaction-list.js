/**
 * Transaction List Functionality
 * Shared between admin, employee, and customer dashboards
 */

/**
 * Initialize the transaction list
 * @param {Object} options Configuration options
 * @param {string} options.containerId ID of the container element for transactions
 * @param {string} options.paginationId ID of the pagination container
 * @param {number} options.userId Optional user ID to filter transactions (admin/employee only)
 * @param {boolean} options.showUser Whether to show user info in the transaction list
 */
function initTransactionList(options) {
  const defaultOptions = {
    containerId: "transactionTableContainer",
    paginationId: "transactionPagination",
    userId: null,
    showUser: false,
    pageSize: 10,
  };

  const config = { ...defaultOptions, ...options };

  // Store config for later use
  window.transactionListConfig = config;

  // Load first page of transactions
  loadTransactions(1);
}

/**
 * Load transactions for the specified page
 * @param {number} page Page number to load
 */
async function loadTransactions(page) {
  const config = window.transactionListConfig;
  if (!config) {
    console.error("Transaction list not initialized");
    return;
  }

  const container = document.getElementById(config.containerId);
  if (!container) {
    console.error(`Container with ID ${config.containerId} not found`);
    return;
  }

  // Show loading state
  container.innerHTML =
    '<div class="loading-indicator">Loading transactions...</div>';

  try {
    // Get transaction data (either from API or mock data)
    const data = await fetchTransactionData(page, config);

    // Check if data contains transactions
    if (!data || !data.transactions) {
      throw new Error("Invalid data format returned");
    }

    // Render transactions
    renderTransactions(data, config);

    // Setup pagination with null check
    if (data.pagination) {
      setupPagination(data.pagination, config);
    } else {
      // Create default pagination if missing
      const defaultPagination = {
        totalItems: data.transactions.length,
        totalPages: 1,
        currentPage: 1,
        pageSize: data.transactions.length,
      };
      setupPagination(defaultPagination, config);
    }
  } catch (error) {
    console.error("Error loading transactions:", error);
    container.innerHTML = `
      <div class="error-message">
        <p>Failed to load transactions. Please try again later.</p>
        <button class="action-button secondary" onclick="loadTransactions(1)">Retry</button>
      </div>
    `;

    // Clear pagination on error
    const paginationContainer = document.getElementById(config.paginationId);
    if (paginationContainer) {
      paginationContainer.innerHTML = "";
    }
  }
}

/**
 * Fetch transaction data from API or generate mock data
 * @param {number} page Page number
 * @param {Object} config Configuration options
 * @returns {Promise<Object>} Transaction data
 */
async function fetchTransactionData(page, config) {
  try {
    // If API endpoint is specified in config, try that first
    if (config.apiEndpoint) {
      try {
        let url = `${config.apiEndpoint}?page=${page}&size=${config.pageSize}`;

        if (config.userId) {
          url += `&userId=${config.userId}`;
        }

        const fetchOptions = config.fetchParams || {};
        const response = await fetch(url, fetchOptions);

        if (response.ok) {
          const data = await response.json();
          if (data && Array.isArray(data.transactions)) {
            console.log("Successfully fetched data from configured endpoint");
            // Ensure the transactions format matches exactly what's expected
            data.transactions = data.transactions.map((tx) => ({
              id: tx.id,
              date: tx.date,
              description: tx.description,
              type: tx.type,
              amount: parseFloat(tx.amount), // Ensure amount is a number
              isDebit: Boolean(tx.isDebit), // Ensure boolean
              fromAccount: tx.fromAccount,
              toAccount: tx.toAccount,
              fromUserId: parseInt(tx.fromUserId),
              toUserId: parseInt(tx.toUserId),
              userName: tx.userName,
            }));
            console.log("Tx Data");
            console.log(data);
            return data;
          } else {
            console.warn("Data format from API is invalid:", data);
          }
        }
      } catch (error) {
        console.warn("Error with configured endpoint:", error);
      }
    }

    // Get context path for default endpoints
    const contextPath =
      window.location.pathname.substring(
        0,
        window.location.pathname.indexOf("/", 1)
      ) || "";

    // Try both endpoints for backward compatibility
    const endpoints = ["/api/transaction-data", "/api/admin-transactions"];
    let lastError = null;

    // Try each endpoint in sequence
    for (const endpoint of endpoints) {
      try {
        // Build URL with parameters
        let url = `${contextPath}${endpoint}?page=${page}&size=${config.pageSize}`;
        if (config.userId) {
          url += `&userId=${config.userId}`;
        }

        // Attempt to fetch from API
        const response = await fetch(url);

        // If response is OK, check the data format
        if (response.ok) {
          const data = await response.json();

          // Check if data has the expected format (transactions and pagination)
          if (data && data.transactions) {
            console.log(`Successfully fetched data from ${endpoint}`);
            return data;
          } else {
            console.warn(
              `API at ${endpoint} returned unexpected data format:`,
              data
            );
          }
        } else {
          throw new Error(
            `API request to ${endpoint} failed: ${response.status}`
          );
        }
      } catch (error) {
        console.warn(`Error with endpoint ${endpoint}:`, error);
        lastError = error;
      }
    }

    // If we get here and useDummyData is explicitly false, don't fall back to mock data
    if (config.useDummyData === false) {
      throw new Error(
        "API requests failed and dummy data generation is disabled"
      );
    }

    // If all API endpoints failed, generate mock data
    console.warn("Falling back to mock transaction data generation");
    return generateMockTransactions(page, config);
  } catch (error) {
    console.error("Error in fetchTransactionData:", error);
    throw error;
  }
}

/**
 * Generate mock transaction data
 * @param {number} page Page number
 * @param {Object} config Configuration options
 * @returns {Object} Mock transaction data with pagination
 */
function generateMockTransactions(page, config) {
  const pageSize = config.pageSize || 10;
  const totalItems = 58; // Example total
  const totalPages = Math.ceil(totalItems / pageSize);

  const transactions = [];
  const startIdx = (page - 1) * pageSize;
  const endIdx = Math.min(startIdx + pageSize, totalItems);

  // Fix: Use lowercase transaction types to match database schema
  const types = ["deposit", "withdrawal", "transfer"];
  const descriptions = [
    "Grocery Store",
    "Salary Deposit",
    "ATM Withdrawal",
    "Online Transfer",
    "Electric Bill",
    "Restaurant",
    "Online Shopping",
    "Gas Station",
  ];

  // Generate transactions for current page
  for (let i = startIdx; i < endIdx; i++) {
    const isDebit = Math.random() > 0.5;
    const amount = (10 + Math.random() * 990).toFixed(2);
    const date = new Date();
    date.setDate(date.getDate() - Math.floor(Math.random() * 30));

    transactions.push({
      id: i + 1,
      date: formatDate(date),
      description:
        descriptions[Math.floor(Math.random() * descriptions.length)],
      type: types[Math.floor(Math.random() * types.length)],
      amount: amount,
      isDebit: isDebit,
      fromAccount: isDebit ? "Checking ****4321" : "External",
      toAccount: isDebit ? "External" : "Checking ****4321",
      fromUserId: isDebit
        ? config.userId || 1
        : Math.floor(Math.random() * 100) + 1,
      toUserId: isDebit
        ? Math.floor(Math.random() * 100) + 1
        : config.userId || 1,
      userName: config.showUser
        ? `Mock User ${Math.floor(Math.random() * 100) + 1}`
        : null,
    });
  }

  return {
    transactions: transactions,
    pagination: {
      totalItems: totalItems,
      totalPages: totalPages,
      currentPage: page,
      pageSize: pageSize,
    },
  };
}

/**
 * Render transactions to the container
 * @param {Object} data Transaction data from API
 * @param {Object} config Configuration options
 */
function renderTransactions(data, config) {
  const container = document.getElementById(config.containerId);
  const transactions = data.transactions;

  // Store transaction data globally for access by other scripts
  window._transactionData = transactions;

  if (!transactions || transactions.length === 0) {
    container.innerHTML =
      '<div class="no-requests">No transactions found.</div>';
    return;
  }

  // Create table
  const table = document.createElement("table");
  table.className = "transaction-table";

  // Create table header
  let headerHtml = "<thead><tr>";
  // Add ID column for debugging/reference
  headerHtml += "<th>ID</th>";
  headerHtml += "<th>Date</th>";

  if (config.showUser) {
    headerHtml += "<th>User</th>";
  }

  headerHtml += "<th>Description</th><th>Type</th><th>Amount</th>";
  headerHtml += "<th>Account</th>";
  headerHtml += "</tr></thead>";

  // Create table body
  let bodyHtml = "<tbody>";

  transactions.forEach((transaction) => {
    try {
      // Add extra validation to handle potential JSON parsing issues
      const transactionType = transaction.type || "other";
      const amount = parseFloat(transaction.amount) || 0;

      // Determine styling and prefix based on transaction type (case insensitive)
      let amountClass, amountPrefix;

      switch (transactionType.toLowerCase()) {
        case "withdrawal":
          amountClass = "withdrawal";
          amountPrefix = "-";
          break;
        case "deposit":
          amountClass = "deposit";
          amountPrefix = "+";
          break;
        case "transfer":
          amountClass = "transfer";
          amountPrefix = "";
          break;
        default:
          // Fallback to debit/credit logic for unknown types
          amountClass = transaction.isDebit ? "debit" : "credit";
          amountPrefix = transaction.isDebit ? "-" : "+";
      }

      // Convert first letter to uppercase for display only
      const displayType =
        transactionType.charAt(0).toUpperCase() + transactionType.slice(1);

      const formattedAmount = amountPrefix + "$" + amount.toFixed(2);

      bodyHtml += "<tr>";
      bodyHtml += `<td>${transaction.id || "N/A"}</td>`;
      bodyHtml += `<td>${transaction.date || "N/A"}</td>`;

      if (config.showUser) {
        const userName =
          transaction.userName ||
          (transaction.isDebit
            ? `User #${transaction.fromUserId || "?"}`
            : `User #${transaction.toUserId || "?"}`);
        bodyHtml += `<td>${userName}</td>`;
      }

      bodyHtml += `<td>${transaction.description || "Unknown"}</td>`;
      bodyHtml += `<td>${displayType}</td>`;
      bodyHtml += `<td class="${amountClass}">${formattedAmount}</td>`;

      // Account info - show the relevant account based on debit/credit
      const accountInfo = transaction.isDebit
        ? transaction.fromAccount || "Unknown Account"
        : transaction.toAccount || "Unknown Account";
      bodyHtml += `<td>${accountInfo}</td>`;

      bodyHtml += "</tr>";
    } catch (err) {
      console.error("Error rendering transaction:", err, transaction);
      bodyHtml += `<tr><td colspan="${
        config.showUser ? 7 : 6
      }" class="error-row">Error displaying transaction</td></tr>`;
    }
  });

  bodyHtml += "</tbody>";

  // Set table HTML
  table.innerHTML = headerHtml + bodyHtml;

  // Add CSS for transaction types if not already present
  if (!document.getElementById("transaction-type-styles")) {
    const styleElement = document.createElement("style");
    styleElement.id = "transaction-type-styles";
    styleElement.textContent = `
      .transaction-table .withdrawal { color: #e74c3c; }
      .transaction-table .deposit { color: #27ae60; }
      .transaction-table .transfer { color: #7f8c8d; }
      .transaction-table .debit { color: #e74c3c; }
      .transaction-table .credit { color: #27ae60; }
    `;
    document.head.appendChild(styleElement);
  }

  // Clear and append to container
  container.innerHTML = "";
  container.appendChild(table);
}

/**
 * Setup pagination controls
 * @param {Object} pagination Pagination data from API
 * @param {Object} config Configuration options
 */
function setupPagination(pagination, config) {
  const container = document.getElementById(config.paginationId);
  if (!container) return;

  // Ensure pagination object exists and has required properties
  if (!pagination) {
    console.error("Pagination object is missing");
    container.innerHTML = "";
    return;
  }

  const totalPages = pagination.totalPages || 1;
  const currentPage = pagination.currentPage || 1;

  if (totalPages <= 1) {
    container.innerHTML = "";
    return;
  }

  let html = '<div class="pagination-controls">';

  // Previous button
  html += `<button class="action-button secondary" ${
    currentPage === 1 ? "disabled" : ""
  } onclick="loadTransactions(${currentPage - 1})">Previous</button>`;

  // Page info
  html += `<span class="pagination-info">Page ${currentPage} of ${totalPages}</span>`;

  // Next button
  html += `<button class="action-button secondary" ${
    currentPage === totalPages ? "disabled" : ""
  } onclick="loadTransactions(${currentPage + 1})">Next</button>`;

  html += "</div>";

  container.innerHTML = html;
}

/**
 * Format a date object as a string
 * @param {Date} date Date to format
 * @returns {string} Formatted date string
 */
function formatDate(date) {
  const options = { year: "numeric", month: "short", day: "numeric" };
  return date.toLocaleDateString(undefined, options);
}
