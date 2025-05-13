/**
 * Transaction Volume Graph for Admin Dashboard
 * Displays transaction volume data for the past 7 days
 */

document.addEventListener("DOMContentLoaded", function () {
  initTransactionVolumeChart();
});

function initTransactionVolumeChart() {
  const chartCanvas = document.getElementById("transactionVolumeChart");
  if (!chartCanvas) {
    console.error("Chart canvas element not found");
    return;
  }

  // Clear any existing chart
  if (window.transactionChart) {
    window.transactionChart.destroy();
  }

  // Show loading indicator
  const container = chartCanvas.parentElement;
  container.innerHTML =
    '<div class="loading-chart">Loading transaction data...</div>';
  container.appendChild(chartCanvas);
  chartCanvas.style.display = "none";

  fetchTransactionVolumeData()
    .then((data) => {
      chartCanvas.style.display = "block";
      container.querySelector(".loading-chart")?.remove();

      // Create the chart
      window.transactionChart = new Chart(chartCanvas, {
        type: "bar",
        data: {
          labels: data.labels,
          datasets: [
            {
              label: "Transaction Volume ($)",
              data: data.values,
              backgroundColor: "rgba(0, 112, 243, 0.7)",
              borderColor: "rgba(0, 112, 243, 1)",
              borderWidth: 1,
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
                  return "$" + value.toLocaleString();
                },
              },
            },
          },
          plugins: {
            tooltip: {
              callbacks: {
                label: function (context) {
                  return "$" + context.parsed.y.toLocaleString();
                },
              },
            },
            legend: {
              display: true,
              position: "top",
            },
          },
        },
      });
    })
    .catch((error) => {
      console.error("Error initializing transaction chart:", error);
      showChartError(chartCanvas);
    });
}

/**
 * Fetch transaction volume data from API
 * @returns {Promise<Object>} Transaction data with labels and values
 */
async function fetchTransactionVolumeData() {
  try {
    // Get the context path
    const contextPath =
      window.location.pathname.substring(
        0,
        window.location.pathname.indexOf("/", 1)
      ) || "";

    console.log("Fetching transaction volume data from API");

    // Fetch data from the dedicated transaction volume endpoint
    const response = await fetch(`${contextPath}/api/transaction-volume`);
    if (!response.ok) {
      console.error(`API error: ${response.status} ${response.statusText}`);
      throw new Error(`API error: ${response.status}`);
    }

    const data = await response.json();
    console.log("Transaction volume data received:", data);

    // Validate data format
    if (!Array.isArray(data.labels) || !Array.isArray(data.values)) {
      console.error("Invalid data format received:", data);
      throw new Error("Invalid data format");
    }

    // Check if all values are zero
    const allZeros = data.values.every((value) => value === 0);
    if (allZeros) {
      console.warn(
        "All transaction values are zero - this might indicate a data issue"
      );
    }

    return data;
  } catch (error) {
    console.error("Error fetching transaction volume data:", error);
    throw error;
  }
}

/**
 * Display error message when chart fails to load
 * @param {HTMLElement} canvas The chart canvas element
 */
function showChartError(canvas) {
  // Get parent container
  const container = canvas.parentElement;

  // Create error message
  const errorMessage = document.createElement("div");
  errorMessage.className = "chart-error";
  errorMessage.innerHTML = `
        <p>Failed to load transaction data</p>
        <button onclick="initTransactionVolumeChart()" class="action-button secondary">Retry</button>
    `;

  // Clear container and show error
  container.innerHTML = "";
  container.appendChild(errorMessage);
}

// Update chart colors when dark mode changes
document.addEventListener("darkModeChanged", function (event) {
  // Reinitialize the chart to apply new theme colors
  initTransactionVolumeChart();
});
