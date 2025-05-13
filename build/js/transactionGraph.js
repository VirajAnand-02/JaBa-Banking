/**
 * Transaction Graph Visualization
 * Fetches transaction data and renders a line chart for the past 7 days
 */
document.addEventListener("DOMContentLoaded", async function () {
  // Get the chart container
  const chartContainer = document.getElementById("transaction-chart");
  if (!chartContainer) return;

  try {
    // Show loading state
    chartContainer.innerHTML =
      '<div class="loading-indicator">Loading transaction data...</div>';

    // Fetch transaction data from server
    const response = await fetch("api/transaction-data");
    if (!response.ok) {
      throw new Error(
        `Server returned ${response.status}: ${response.statusText}`
      );
    }

    const data = await response.json();

    // Clear loading message
    chartContainer.innerHTML = "";

    // Create chart
    const ctx = chartContainer.getContext("2d");
    new Chart(ctx, {
      type: "line",
      data: {
        labels: data.labels,
        datasets: [
          {
            label: "Transaction Volume",
            data: data.values,
            backgroundColor: "rgba(0, 112, 243, 0.2)", // Uses primary color with transparency
            borderColor: "var(--primary)",
            borderWidth: 2,
            tension: 0.3,
            fill: true,
            pointBackgroundColor: "var(--primary)",
            pointRadius: 4,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: function (value) {
                return "$" + value.toFixed(2);
              },
            },
            grid: {
              color: "rgba(200, 200, 200, 0.2)",
            },
          },
          x: {
            grid: {
              color: "rgba(200, 200, 200, 0.2)",
            },
          },
        },
        plugins: {
          tooltip: {
            callbacks: {
              label: function (context) {
                return "$" + context.parsed.y.toFixed(2);
              },
            },
          },
          legend: {
            display: false,
          },
        },
      },
    });
  } catch (error) {
    console.error("Error loading transaction chart:", error);
    chartContainer.innerHTML =
      '<p class="chart-error">Failed to load transaction data</p>';
  }
});

// Add support for dark mode by refreshing the chart when theme changes
document.addEventListener("themeChanged", function () {
  // Force reload of the chart when theme changes
  location.reload();
});
