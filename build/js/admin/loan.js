/**
 * Loan management functionality for admin interface
 */
document.addEventListener("DOMContentLoaded", function () {
  // Initialize the page
  loadPendingLoans();

  // Only call loadAllLoans if the table exists
  if (document.getElementById("allLoanRequestsTableBody")) {
    loadAllLoans();
  }

  // Set up event listeners
  const rejectForm = document.getElementById("rejectLoanForm");
  if (rejectForm) {
    rejectForm.addEventListener("submit", function (e) {
      e.preventDefault();
      processSaveRejection();
    });
  }
});

/**
 * Load pending loan requests
 */
async function loadPendingLoans() {
  const tableBody = document.getElementById("loanRequestsTableBody");
  const contextPath = document.getElementById("contextPath").value;

  try {
    // Show loading indicator
    tableBody.innerHTML =
      '<tr><td colspan="6" class="loading-indicator">Loading loan requests...</td></tr>';

    // Fetch pending loans from server
    const response = await fetch(`${contextPath}/api/pending-loans`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest",
      },
      credentials: "same-origin",
    });

    if (!response.ok) {
      throw new Error(`HTTP error! Status: ${response.status}`);
    }

    const pendingLoans = await response.json();

    if (pendingLoans.length === 0) {
      tableBody.innerHTML =
        '<tr><td colspan="6" class="no-requests">No pending loan requests found.</td></tr>';
      return;
    }

    // Clear the table body
    tableBody.innerHTML = "";

    // Create rows using DOM methods instead of template literals
    pendingLoans.forEach((loan) => {
      const row = document.createElement("tr");

      // ID cell
      const idCell = document.createElement("td");
      idCell.textContent = loan.id;
      row.appendChild(idCell);

      // Customer cell
      const customerCell = document.createElement("td");
      customerCell.textContent =
        loan.customerName + " (ID: " + loan.userId + ")";
      row.appendChild(customerCell);

      // Amount cell
      const amountCell = document.createElement("td");
      amountCell.textContent =
        "$" +
        parseFloat(loan.amount).toLocaleString("en-US", {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        });
      row.appendChild(amountCell);

      // Type cell
      const typeCell = document.createElement("td");
      typeCell.textContent = loan.type;
      row.appendChild(typeCell);

      // Date cell
      const dateCell = document.createElement("td");
      dateCell.textContent = formatDate(new Date(loan.date));
      row.appendChild(dateCell);

      // Actions cell
      const actionsCell = document.createElement("td");
      actionsCell.className = "actions-cell";

      // Approve button
      const approveBtn = document.createElement("button");
      approveBtn.className = "action-button success";
      approveBtn.textContent = "Approve";
      approveBtn.onclick = function () {
        approveLoan(loan.id);
      };
      actionsCell.appendChild(approveBtn);

      // Add space between buttons
      actionsCell.appendChild(document.createTextNode(" "));

      // Reject button
      const rejectBtn = document.createElement("button");
      rejectBtn.className = "action-button danger";
      rejectBtn.textContent = "Reject";
      rejectBtn.onclick = function () {
        showRejectModal(loan.id);
      };
      actionsCell.appendChild(rejectBtn);

      row.appendChild(actionsCell);

      tableBody.appendChild(row);
    });
    console.log("done Tabeling shit");
  } catch (error) {
    console.error("Error loading pending loans:", error);
    tableBody.innerHTML =
      '<tr><td colspan="6" class="error-message">Failed to load loan requests. Please try again later.</td></tr>';
  }
}

/**
 * Load all loan requests with status filtering
 */
async function loadAllLoans(statusFilter = "all") {
  const tableBody = document.getElementById("allLoanRequestsTableBody");

  // Check if element exists before proceeding
  if (!tableBody) {
    console.error(
      "Error: Could not find element with ID 'allLoanRequestsTableBody'"
    );
    return;
  }

  try {
    // Show loading indicator
    tableBody.innerHTML =
      '<tr><td colspan="7" class="loading-indicator">Loading loan requests...</td></tr>';

    // In a real app, fetch loans from server with status filter
    // For now, using mock data
    const contextPath = document.getElementById("contextPath")?.value || "";

    // TODO: Replace with real API call in future
    await mockNetworkDelay(700);
    const allLoans = generateMockAllLoans();

    // Don't proceed if element is no longer available after the delay
    if (!document.getElementById("allLoanRequestsTableBody")) {
      console.error(
        "Element 'allLoanRequestsTableBody' is no longer available after data fetch"
      );
      return;
    }

    // Apply filter if needed
    const filteredLoans =
      statusFilter === "all"
        ? allLoans
        : allLoans.filter((loan) => loan.status === statusFilter);

    if (filteredLoans.length === 0) {
      tableBody.innerHTML =
        '<tr><td colspan="7" class="no-requests">No loan requests found.</td></tr>';
      return;
    }

    // Clear the table body
    tableBody.innerHTML = "";

    // Create rows using DOM methods
    filteredLoans.forEach((loan) => {
      const row = document.createElement("tr");

      // ID cell
      const idCell = document.createElement("td");
      idCell.textContent = loan.id;
      row.appendChild(idCell);

      // Customer cell
      const customerCell = document.createElement("td");
      customerCell.textContent =
        loan.customerName + " (ID: " + loan.userId + ")";
      row.appendChild(customerCell);

      // Amount cell
      const amountCell = document.createElement("td");
      amountCell.textContent =
        "$" +
        loan.amount.toLocaleString("en-US", {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        });
      row.appendChild(amountCell);

      // Type cell
      const typeCell = document.createElement("td");
      typeCell.textContent = loan.type;
      row.appendChild(typeCell);

      // Date cell
      const dateCell = document.createElement("td");
      dateCell.textContent = formatDate(loan.date);
      row.appendChild(dateCell);

      // Status cell
      const statusCell = document.createElement("td");
      statusCell.className = "status-" + loan.status.toLowerCase();
      statusCell.textContent = capitalizeFirstLetter(loan.status);
      row.appendChild(statusCell);

      // Comments cell
      const commentCell = document.createElement("td");
      commentCell.textContent = loan.adminComment || "-";
      row.appendChild(commentCell);

      tableBody.appendChild(row);
    });
  } catch (error) {
    console.error("Error loading all loans:", error);
    // Safety check before trying to set innerHTML
    const errorTableBody = document.getElementById("allLoanRequestsTableBody");
    if (errorTableBody) {
      errorTableBody.innerHTML =
        '<tr><td colspan="7" class="error-message">Failed to load loan requests. Please try again later.</td></tr>';
    }
  }
}

/**
 * Show the reject loan modal
 */
function showRejectModal(loanId) {
  document.getElementById("rejectLoanId").value = loanId;
  document.getElementById("rejectReason").value = "";

  // Show the modal
  const modal = document.getElementById("rejectLoanModal");
  modal.style.display = "flex";
}

/**
 * Close the reject loan modal
 */
function closeRejectModal() {
  const modal = document.getElementById("rejectLoanModal");
  modal.style.display = "none";
}

/**
 * Process the rejection form submission
 */
async function processSaveRejection() {
  const loanId = document.getElementById("rejectLoanId").value;
  const reason = document.getElementById("rejectReason").value;
  const contextPath = document.getElementById("contextPath").value;

  if (!reason || reason.trim() === "") {
    alert("Please provide a reason for rejecting this loan.");
    return;
  }

  try {
    // Create form data for POST request
    const formData = new URLSearchParams();
    formData.append("loanId", loanId);
    formData.append("adminComment", reason);

    // Send rejection request to server
    const response = await fetch(`${contextPath}/api/reject-loan`, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "X-Requested-With": "XMLHttpRequest",
      },
      body: formData.toString(),
      credentials: "same-origin",
    });

    const result = await response.json();

    if (!response.ok) {
      throw new Error(result.message || "Failed to reject loan");
    }

    // Close the modal
    closeRejectModal();

    // Reload data to reflect changes
    loadPendingLoans();

    // Only call loadAllLoans with a status filter if the element exists
    const statusFilter = document.getElementById("statusFilter");
    if (document.getElementById("allLoanRequestsTableBody")) {
      loadAllLoans(statusFilter ? statusFilter.value : "all");
    }

    // Notify user
    alert(`Loan #${loanId} has been rejected successfully.`);
  } catch (error) {
    console.error("Error rejecting loan:", error);
    alert("Failed to reject loan. Please try again.");
  }
}

/**
 * Approve a loan request
 */
async function approveLoan(loanId) {
  if (!confirm(`Are you sure you want to approve loan #${loanId}?`)) {
    return;
  }

  try {
    const contextPath = document.getElementById("contextPath").value;

    // Create form data for POST request
    const formData = new URLSearchParams();
    formData.append("loanId", loanId);

    // Show loading indicator
    const loaderElement = document.createElement("div");
    loaderElement.className = "loan-approval-loader";
    loaderElement.innerHTML = "Processing loan approval...";
    document.body.appendChild(loaderElement);

    // Send approval request to server using the updated URL
    const response = await fetch(`${contextPath}/api/approve-loan-request`, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "X-Requested-With": "XMLHttpRequest",
      },
      body: formData.toString(),
      credentials: "same-origin",
    });

    // Remove loading indicator
    document.body.removeChild(loaderElement);

    const result = await response.json();

    if (!response.ok) {
      throw new Error(result.message || "Failed to approve loan");
    }

    // Reload data to reflect changes
    loadPendingLoans();

    // Only call loadAllLoans with a status filter if the element exists
    const statusFilter = document.getElementById("statusFilter");
    if (document.getElementById("allLoanRequestsTableBody")) {
      loadAllLoans(statusFilter ? statusFilter.value : "all");
    }

    // Notify user with feedback about the credited amount
    alert(
      result.message ||
        `Loan #${loanId} has been approved successfully and funds have been credited to the user's account.`
    );
  } catch (error) {
    console.error("Error approving loan:", error);
    alert(`Failed to approve loan: ${error.message || "Unknown error"}`);
  }
}

/**
 * Filter loans by status
 */
function filterLoansByStatus(status) {
  loadAllLoans(status);
}

/**
 * Generate mock pending loans
 */
function generateMockPendingLoans(count = 5) {
  const loanTypes = ["Personal", "Home", "Auto", "Education", "Business"];
  const loans = [];

  for (let i = 1; i <= count; i++) {
    const loanId = 1000 + i;
    loans.push({
      id: loanId,
      userId: 100 + i,
      customerName: `Customer ${100 + i}`,
      amount: Math.round(1000 + Math.random() * 49000),
      type: loanTypes[Math.floor(Math.random() * loanTypes.length)],
      date: new Date(Date.now() - Math.random() * 10 * 24 * 60 * 60 * 1000),
      status: "pending",
    });
  }

  return loans;
}

/**
 * Generate mock loans with various statuses
 */
function generateMockAllLoans(count = 15) {
  const loanTypes = ["Personal", "Home", "Auto", "Education", "Business"];
  const statuses = ["pending", "approved", "rejected"];
  const loans = [];

  for (let i = 1; i <= count; i++) {
    const loanId = 1000 + i;
    const status = statuses[Math.floor(Math.random() * statuses.length)];

    loans.push({
      id: loanId,
      userId: 100 + i,
      customerName: `Customer ${100 + i}`,
      amount: Math.round(1000 + Math.random() * 49000),
      type: loanTypes[Math.floor(Math.random() * loanTypes.length)],
      date: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000),
      status: status,
      adminComment:
        status === "rejected" ? "Insufficient income or credit history." : null,
    });
  }

  return loans;
}

/**
 * Simulate a network delay
 */
function mockNetworkDelay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Format a date object to a readable string
 */
function formatDate(date) {
  if (!(date instanceof Date)) {
    return date;
  }

  const options = { year: "numeric", month: "short", day: "numeric" };
  return date.toLocaleDateString(undefined, options);
}

/**
 * Capitalize the first letter of a string
 */
function capitalizeFirstLetter(string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
}
