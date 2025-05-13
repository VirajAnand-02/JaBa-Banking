/**
 * Employee Dashboard JavaScript
 */

document.addEventListener("DOMContentLoaded", function () {
  console.log("Employee dashboard script loaded.");
  // Initialize any dashboard specific components here if needed

  // Example: Make the first ticket active by default if not already handled by JSP
  const firstTicket = document.querySelector(".ticket-list li");
  if (firstTicket && !document.querySelector(".ticket-list li.active-ticket")) {
    // firstTicket.classList.add('active-ticket'); // This is now handled by JSP example
  }
});

/**
 * Flags a transaction.
 * Makes a real API call to flag a transaction as suspicious.
 * @param {string} transactionId - The ID of the transaction to flag.
 */
function flagTransaction(transactionId) {
  if (!transactionId) {
    console.error("Transaction ID is missing for flagging.");
    alert("Error: Cannot flag transaction without an ID.");
    return;
  }

  const confirmation = confirm(
    `Are you sure you want to flag transaction ID: ${transactionId} as suspicious?`
  );

  if (confirmation) {
    // Get context path dynamically from the page
    const contextPath =
      document
        .querySelector("a[href*='/logout']")
        ?.getAttribute("href")
        ?.split("/logout")[0] || "";

    // Make the API call to flag the transaction
    fetch(`${contextPath}/api/employee/flag-transaction`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        transactionId: transactionId,
        reason: "Flagged as suspicious by employee",
      }),
    })
      .then((response) => {
        if (!response.ok) {
          if (response.status === 409) {
            throw new Error("This transaction has already been flagged");
          }
          throw new Error(`Server error: ${response.status}`);
        }
        return response.json();
      })
      .then((data) => {
        if (data.success) {
          alert(`Transaction ${transactionId} has been successfully flagged.`);

          // Update the UI to show the transaction is now flagged
          const flagButton = document.querySelector(
            `button[onclick="flagTransaction('${transactionId}')"]`
          );
          if (flagButton) {
            flagButton.innerHTML = '<i class="fas fa-check"></i> Flagged';
            flagButton.classList.remove("danger");
            flagButton.classList.add("secondary");
            flagButton.disabled = true;
          }
        } else {
          alert(`Failed to flag transaction: ${data.message}`);
        }
      })
      .catch((error) => {
        console.error("Error flagging transaction:", error);
        alert(`Error: ${error.message || "Failed to flag transaction"}`);
      });
  } else {
    console.log(`Flagging cancelled for transaction ${transactionId}.`);
  }
}

/**
 * Handles selection of a ticket from the list.
 * Updates the chat window and ticket details panel. (UI only for now)
 * @param {string} ticketId - The ID of the selected ticket.
 * @param {string} customerName - The name of the customer for the selected ticket.
 */
function selectTicket(ticketId, customerName) {
  console.log(`Selected ticket: ${ticketId} for customer: ${customerName}`);

  // Update active state in ticket list
  document.querySelectorAll(".ticket-list li").forEach((item) => {
    item.classList.remove("active-ticket");
  });
  // Find the clicked li element to make it active. This is a bit simplistic.
  // A better way would be to pass 'this' from the onclick or find by ticketId if they have unique IDs.
  event.currentTarget.classList.add("active-ticket");

  // Update chat window header
  document.getElementById("chattingWith").textContent = customerName;
  document.getElementById("chatTicketId").textContent = `#${ticketId}`;

  // Update ticket details panel
  document.getElementById("detailCustomerName").textContent = customerName;
  document.getElementById("detailTicketId").textContent = `#${ticketId}`;
  // For a real app, fetch full ticket details and chat history via API
  document.getElementById("detailTicketStatus").textContent = "Open"; // Placeholder

  // Clear and populate message area (placeholder)
  const messageArea = document.getElementById("messageArea");
  messageArea.innerHTML = `
        <div class="message received">
            <p>Hi, this is ${customerName}. I need help with ticket ${ticketId}.</p>
            <span class="timestamp">11:00 AM</span>
        </div>
        <div class="message sent">
            <p>Hello ${customerName}, how can I assist you with ticket ${ticketId}?</p>
            <span class="timestamp">11:01 AM</span>
        </div>
    `; // Replace with actual message loading
  messageArea.scrollTop = messageArea.scrollHeight; // Scroll to bottom
}

/**
 * Handles sending a chat message. (UI only for now)
 */
function sendMessage() {
  const messageInput = document.getElementById("chatMessageInput");
  const messageText = messageInput.value.trim();

  if (messageText === "") {
    return; // Don't send empty messages
  }

  const messageArea = document.getElementById("messageArea");
  const newMessageDiv = document.createElement("div");
  newMessageDiv.classList.add("message", "sent");

  const messageParagraph = document.createElement("p");
  messageParagraph.textContent = messageText;

  const timestampSpan = document.createElement("span");
  timestampSpan.classList.add("timestamp");
  timestampSpan.textContent = new Date().toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });

  newMessageDiv.appendChild(messageParagraph);
  newMessageDiv.appendChild(timestampSpan);
  messageArea.appendChild(newMessageDiv);

  // Scroll to the bottom of the message area
  messageArea.scrollTop = messageArea.scrollHeight;

  messageInput.value = ""; // Clear the input field
  messageInput.focus();

  console.log("Message sent (UI only):", messageText);
  // In a real app, send the message to the backend via WebSocket or API call
}

// Add event listener for Enter key in chat input
document
  .getElementById("chatMessageInput")
  ?.addEventListener("keypress", function (event) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault(); // Prevent new line on Enter
      sendMessage();
    }
  });
