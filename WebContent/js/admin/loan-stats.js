/**
 * Loan Statistics Module
 * Fetches real-time loan statistics from the server
 */
document.addEventListener("DOMContentLoaded", function () {
  // Only proceed if at least one of the stat elements exists
  if (
    document.getElementById("totalRequestsStat") ||
    document.getElementById("pendingRequestsStat") ||
    document.getElementById("approvedRequestsStat") ||
    document.getElementById("rejectedRequestsStat")
  ) {
    loadLoanStats();

    // Refresh loan stats every 30 seconds
    setInterval(loadLoanStats, 30000);
  } else {
    console.log(
      "No loan statistics elements found in the page, skipping stats loading"
    );
  }
});

/**
 * Fetches loan statistics from the server
 */
function loadLoanStats() {
  const contextPathElement = document.getElementById("contextPath");
  if (!contextPathElement) {
    console.error("Context path element not found");
    return;
  }

  const contextPath = contextPathElement.value;
  console.log(
    "Fetching loan statistics from: " + contextPath + "/api/loan-statistics"
  );

  fetch(contextPath + "/api/loan-statistics", {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      "X-Requested-With": "XMLHttpRequest",
    },
    credentials: "same-origin",
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error(
          "Loan statistics API returned status: " + response.status
        );
      }
      return response.json();
    })
    .then((data) => {
      console.log("Loan statistics loaded successfully:", data);
      // Update loan stats in the UI - only if elements exist
      const totalElement = document.getElementById("totalRequestsStat");
      if (totalElement) totalElement.textContent = data.totalLoans;

      const pendingElement = document.getElementById("pendingRequestsStat");
      if (pendingElement) pendingElement.textContent = data.pendingLoans;

      const approvedElement = document.getElementById("approvedRequestsStat");
      if (approvedElement) approvedElement.textContent = data.approvedLoans;

      const rejectedElement = document.getElementById("rejectedRequestsStat");
      if (rejectedElement) rejectedElement.textContent = data.rejectedLoans;
    })
    .catch((error) => {
      console.error("Error in loan-stats.js while fetching statistics:", error);
      // Only update elements that exist
      [
        "totalRequestsStat",
        "pendingRequestsStat",
        "approvedRequestsStat",
        "rejectedRequestsStat",
      ].forEach((id) => {
        const element = document.getElementById(id);
        if (element) element.textContent = "Error";
      });
    });
}
