/**
 * System Configuration functionality for admin interface
 */
document.addEventListener("DOMContentLoaded", function () {
  // Initialize the page
  loadCurrentRates();
  loadConfigHistory();

  // Set up form submission handler
  document
    .getElementById("interestRateForm")
    .addEventListener("submit", function (e) {
      e.preventDefault();
      showConfirmationModal();
    });

  // Reset form handler - reloads original values
  document
    .getElementById("interestRateForm")
    .addEventListener("reset", function (e) {
      // Give time for the form to reset to default values
      setTimeout(loadCurrentRates, 10);
    });
});

/**
 * Load current system rates
 */
async function loadCurrentRates() {
  const contextPath = document.getElementById("contextPath").value;

  try {
    // Show loading state
    document.getElementById("currentSavingsRate").textContent = "Loading...";
    document.getElementById("currentPersonalLoanRate").textContent =
      "Loading...";
    document.getElementById("currentHomeLoanRate").textContent = "Loading...";
    document.getElementById("lastUpdated").textContent = "Loading...";

    // Fetch current rates from server
    const response = await fetch(`${contextPath}/api/system-config`, {
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

    const config = await response.json();

    // Update the stats display
    document.getElementById("currentSavingsRate").textContent =
      config.basicSavingsRate + "%";
    document.getElementById("currentPersonalLoanRate").textContent =
      config.personalLoanRate + "%";
    document.getElementById("currentHomeLoanRate").textContent =
      config.homeLoanRate + "%";
    document.getElementById("lastUpdated").textContent = formatDate(
      new Date(config.lastUpdated)
    );

    // Populate the form with current values
    const fields = [
      "basicSavingsRate",
      "premiumSavingsRate",
      "personalLoanRate",
      "homeLoanRate",
      "autoLoanRate",
      "businessLoanRate",
      "educationLoanRate",
      "minimumBalance",
      "transactionFee",
    ];

    fields.forEach((field) => {
      const element = document.getElementById(field);
      if (element && config[field] !== undefined) {
        element.value = config[field];
        // Store original value as a data attribute for comparison later
        element.setAttribute("data-original", config[field]);
      }
    });
  } catch (error) {
    console.error("Error loading system configuration:", error);
    document.getElementById("currentSavingsRate").textContent = "Error";
    document.getElementById("currentPersonalLoanRate").textContent = "Error";
    document.getElementById("currentHomeLoanRate").textContent = "Error";
    document.getElementById("lastUpdated").textContent = "Error";

    alert(
      "Failed to load current system configuration. Please refresh the page and try again."
    );
  }
}

/**
 * Load configuration change history
 */
async function loadConfigHistory() {
  const tableBody = document.getElementById("configHistoryTableBody");
  const contextPath = document.getElementById("contextPath").value;

  try {
    // Show loading indicator
    tableBody.innerHTML =
      '<tr><td colspan="5" class="loading-indicator">Loading history...</td></tr>';

    // Fetch history from server
    const response = await fetch(`${contextPath}/api/system-config/history`, {
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

    const history = await response.json();

    if (history.length === 0) {
      tableBody.innerHTML =
        '<tr><td colspan="5" class="no-results">No configuration changes found.</td></tr>';
      return;
    }

    // Generate table rows
    let html = "";
    history.forEach((change) => {
      html += `
            <tr>
                <td>${formatDate(new Date(change.timestamp))}</td>
                <td>${change.adminUsername}</td>
                <td>${formatParameterName(change.parameter)}</td>
                <td>${formatValue(change.parameter, change.oldValue)}</td>
                <td>${formatValue(change.parameter, change.newValue)}</td>
            </tr>
            `;
    });

    tableBody.innerHTML = html;
  } catch (error) {
    console.error("Error loading configuration history:", error);
    tableBody.innerHTML =
      '<tr><td colspan="5" class="error-message">Failed to load configuration history. Please try again.</td></tr>';
  }
}

/**
 * Format a date object to a readable string
 */
function formatDate(date) {
  if (!(date instanceof Date) || isNaN(date)) {
    return "N/A";
  }

  const options = {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  };
  return date.toLocaleDateString(undefined, options);
}

/**
 * Format parameter names for display
 */
function formatParameterName(paramName) {
  // Convert camelCase to Title Case with spaces
  return paramName
    .replace(/([A-Z])/g, " $1")
    .replace(/^./, (str) => str.toUpperCase());
}

/**
 * Format values based on parameter type
 */
function formatValue(parameter, value) {
  if (parameter.includes("Rate")) {
    return value + "%";
  } else if (parameter.includes("Fee") || parameter.includes("Balance")) {
    return "$" + parseFloat(value).toFixed(2);
  }
  return value;
}

/**
 * Show confirmation modal with changes summary
 */
function showConfirmationModal() {
  const form = document.getElementById("interestRateForm");
  const formData = new FormData(form);
  const changesDiv = document.getElementById("changesSummary");

  // Clear previous content
  changesDiv.innerHTML = "";

  // Build changes summary HTML
  let changesFound = false;
  let changesHtml = "<ul>";

  // Check each field for changes
  for (let [key, newValue] of formData.entries()) {
    const element = document.getElementById(key);
    const oldValue =
      element.getAttribute("data-original") || element.defaultValue;

    // Convert to numbers for comparison since inputs are type="number"
    if (parseFloat(oldValue) !== parseFloat(newValue)) {
      changesFound = true;
      changesHtml += `<li><strong>${formatParameterName(key)}:</strong> 
                            ${formatValue(key, oldValue)} → ${formatValue(
        key,
        newValue
      )}</li>`;
    }
  }

  changesHtml += "</ul>";

  // If no changes, show a message
  if (!changesFound) {
    changesDiv.innerHTML = "<p class='no-changes'>No changes detected</p>";
  } else {
    changesDiv.innerHTML = changesHtml;
  }

  // Show the modal
  document.getElementById("confirmationModal").style.display = "flex";
}

/**
 * Close the confirmation modal
 */
function closeConfirmationModal() {
  document.getElementById("confirmationModal").style.display = "none";
}

/**
 * Submit configuration changes to the server
 */
async function submitConfigChanges() {
  const form = document.getElementById("interestRateForm");
  const contextPath = document.getElementById("contextPath").value;

  try {
    // Serialize form data to JSON
    const formData = new FormData(form);
    const data = {};
    formData.forEach((value, key) => (data[key] = parseFloat(value)));

    // Send to server
    const response = await fetch(`${contextPath}/api/system-config`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest",
      },
      body: JSON.stringify(data),
      credentials: "same-origin",
    });

    if (!response.ok) {
      const result = await response.json();
      throw new Error(
        result.message || "Failed to update system configuration"
      );
    }

    // Close modal and show success
    closeConfirmationModal();
    alert("System configuration updated successfully.");

    // Refresh the page to show new values
    loadCurrentRates();
    loadConfigHistory();
  } catch (error) {
    console.error("Error updating system configuration:", error);
    alert(`Failed to update system configuration: ${error.message}`);
  }
}

/**
 * For development/testing only - to be removed in production
 * Creates a mock API response while backend is being developed
 */
function handleDevMockAPI() {
  // Mock the fetch API for development if needed
  const originalFetch = window.fetch;

  window.fetch = function (url, options) {
    // If URL matches our API endpoints, return mock data
    if (url.includes("/api/system-config")) {
      if (!url.includes("/history")) {
        // Mock current config endpoint
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              basicSavingsRate: 2.5,
              premiumSavingsRate: 3.75,
              personalLoanRate: 9.25,
              homeLoanRate: 5.49,
              autoLoanRate: 6.99,
              businessLoanRate: 8.75,
              educationLoanRate: 4.5,
              minimumBalance: 100.0,
              transactionFee: 1.5,
              lastUpdated: new Date().toISOString(),
            }),
        });
      } else {
        // Mock history endpoint
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve([
              {
                timestamp: new Date(Date.now() - 86400000).toISOString(),
                adminUsername: "admin.user",
                parameter: "personalLoanRate",
                oldValue: "8.75",
                newValue: "9.25",
              },
              {
                timestamp: new Date(Date.now() - 172800000).toISOString(),
                adminUsername: "finance.manager",
                parameter: "homeLoanRate",
                oldValue: "5.75",
                newValue: "5.49",
              },
              {
                timestamp: new Date(Date.now() - 259200000).toISOString(),
                adminUsername: "system.admin",
                parameter: "basicSavingsRate",
                oldValue: "2.25",
                newValue: "2.5",
              },
            ]),
        });
      }
    }

    // Otherwise, use the original fetch
    return originalFetch(url, options);
  };

  // Uncomment the line below to enable mock API responses
  // console.log("Development mode: Using mock API responses");
}
