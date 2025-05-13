/**
 * Admin Dashboard Statistics Module
 * Fetches real-time statistics from the server
 */
document.addEventListener("DOMContentLoaded", function () {
  loadAdminStats();

  // Refresh stats every 30 seconds
  setInterval(loadAdminStats, 30000);
});

/**
 * Fetches admin dashboard statistics from the server
 */
function loadAdminStats() {
  fetch(contextPath + "/api/admin-stats", {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      "X-Requested-With": "XMLHttpRequest",
    },
    credentials: "same-origin",
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error("Network response was not ok");
      }
      return response.json();
    })
    .then((data) => {
      // Update stats in the UI
      document.getElementById("totalUsers").textContent = data.totalUsers;
      document.getElementById("activeSessions").textContent =
        data.activeSessions;
      document.getElementById("pendingLoans").textContent = data.pendingLoans;
      document.getElementById("recentAlerts").textContent = data.recentAlerts;
    })
    .catch((error) => {
      console.error("Error fetching admin statistics:", error);
      document.getElementById("totalUsers").textContent = "Error";
      document.getElementById("activeSessions").textContent = "Error";
      document.getElementById("pendingLoans").textContent = "Error";
      document.getElementById("recentAlerts").textContent = "Error";
    });
}
