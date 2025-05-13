/**
 * User Management functionality for admin interface
 */
document.addEventListener("DOMContentLoaded", function () {
  // Initialize the page
  loadUserStats();
  loadInactiveUsers(); // Add this line to load inactive users
  loadUsers();

  // Set up event listeners
  document
    .getElementById("addUserForm")
    .addEventListener("submit", function (e) {
      e.preventDefault();
      addUser();
    });

  document
    .getElementById("searchUserBtn")
    .addEventListener("click", function () {
      searchUsers();
    });

  document
    .getElementById("searchUserQuery")
    .addEventListener("keypress", function (e) {
      if (e.key === "Enter") {
        searchUsers();
      }
    });

  // Reset password toggler
  const resetPasswordCheckbox = document.getElementById("resetPassword");
  if (resetPasswordCheckbox) {
    resetPasswordCheckbox.addEventListener("change", function () {
      document.getElementById("newPasswordContainer").style.display = this
        .checked
        ? "block"
        : "none";
    });
  }

  // Edit user form submission
  const editUserForm = document.getElementById("editUserForm");
  if (editUserForm) {
    editUserForm.addEventListener("submit", function (e) {
      e.preventDefault();
      updateUser();
    });
  }
});

/**
 * Load user statistics
 */
async function loadUserStats() {
  const contextPath = document.getElementById("contextPath").value;

  try {
    // Show loading placeholders
    document.getElementById("totalUsersStat").textContent = "Loading...";
    document.getElementById("employeesStat").textContent = "Loading...";
    document.getElementById("customersStat").textContent = "Loading...";
    document.getElementById("adminsStat").textContent = "Loading...";

    // Fetch stats from server
    const response = await fetch(`${contextPath}/api/user-stats`, {
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

    const stats = await response.json();

    // Update the stats display
    document.getElementById("totalUsersStat").textContent =
      stats.totalUsers || "0";
    document.getElementById("employeesStat").textContent =
      stats.employeeCount || "0";
    document.getElementById("customersStat").textContent =
      stats.customerCount || "0";
    document.getElementById("adminsStat").textContent = stats.adminCount || "0";
  } catch (error) {
    console.error("Error loading user statistics:", error);
    document.getElementById("totalUsersStat").textContent = "Error";
    document.getElementById("employeesStat").textContent = "Error";
    document.getElementById("customersStat").textContent = "Error";
    document.getElementById("adminsStat").textContent = "Error";
  }
}

/**
 * Load all users with optional filtering
 */
async function loadUsers(search = "", role = "all", page = 1) {
  const tableBody = document.getElementById("userTableBody");
  const contextPath = document.getElementById("contextPath").value;

  try {
    // Show loading indicator
    tableBody.innerHTML =
      '<tr><td colspan="7" class="loading-indicator">Loading users...</td></tr>';

    // Build query parameters for search and filters
    let url = `${contextPath}/api/users?page=${page}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    if (role && role !== "all") url += `&role=${encodeURIComponent(role)}`;

    // Fetch users from server
    const response = await fetch(url, {
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

    const result = await response.json();
    const users = result.users || [];

    if (users.length === 0) {
      tableBody.innerHTML =
        '<tr><td colspan="7" class="no-results">No users found.</td></tr>';
      // Clear pagination
      document.getElementById("userTablePagination").innerHTML = "";
      return;
    }

    // Generate table rows
    let html = "";
    users.forEach((user) => {
      const statusClass = getStatusClass(user.status);
      const roleClass = getRoleClass(user.role);

      html += `
        <tr>
          <td>${user.id}</td>
          <td>${user.name}</td>
          <td>${user.email}</td>
          <td class="${roleClass}">${capitalizeFirstLetter(user.role)}</td>
          <td class="${statusClass}">${capitalizeFirstLetter(user.status)}</td>
          <td>${formatDate(new Date(user.createdAt))}</td>
          <td class="actions-cell">
            <button class="action-button small" onclick="showEditUserModal(${
              user.id
            })">Edit</button>
            ${
              user.status === "active"
                ? `<button class="action-button small warning" onclick="toggleUserStatus(${user.id}, 'inactive')">Disable</button>`
                : `<button class="action-button small success" onclick="toggleUserStatus(${user.id}, 'active')">Enable</button>`
            }
          </td>
        </tr>
      `;
    });

    tableBody.innerHTML = html;

    // Update pagination
    renderPagination(
      result.currentPage,
      result.totalPages,
      "userTablePagination"
    );
  } catch (error) {
    console.error("Error loading users:", error);
    tableBody.innerHTML =
      '<tr><td colspan="7" class="error-message">Failed to load users. Please try again later.</td></tr>';
  }
}

/**
 * Load inactive users for approval
 */
async function loadInactiveUsers() {
  const tableBody = document.getElementById("inactiveUsersTableBody");
  const contextPath = document.getElementById("contextPath").value;

  try {
    // Show loading indicator
    tableBody.innerHTML =
      '<tr><td colspan="6" class="loading-indicator">Loading inactive users...</td></tr>';

    // Fetch inactive users from server
    const response = await fetch(`${contextPath}/api/inactive-users`, {
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

    const inactiveUsers = await response.json();

    if (inactiveUsers.length === 0) {
      tableBody.innerHTML =
        '<tr><td colspan="6" class="no-requests">No pending user approvals found.</td></tr>';
      return;
    }

    let html = "";
    inactiveUsers.forEach((user) => {
      html += `
                <tr>
                    <td>${user.id}</td>
                    <td>${user.name}</td>
                    <td>${user.email}</td>
                    <td class="${getRoleClass(
                      user.role
                    )}">${capitalizeFirstLetter(user.role)}</td>
                    <td>${formatDate(new Date(user.createdAt))}</td>
                    <td class="actions-cell">
                        <button class="action-button success" onclick="approveUser(${
                          user.id
                        })">Approve</button>
                    </td>
                </tr>
            `;
    });

    tableBody.innerHTML = html;
  } catch (error) {
    console.error("Error loading inactive users:", error);
    tableBody.innerHTML =
      '<tr><td colspan="6" class="error-message">Failed to load inactive users. Please try again later.</td></tr>';
  }
}

/**
 * Approve a user
 */
async function approveUser(userId) {
  if (!confirm(`Are you sure you want to approve this user?`)) {
    return;
  }

  const contextPath = document.getElementById("contextPath").value;

  try {
    // Create form data for POST request
    const formData = new URLSearchParams();
    formData.append("userId", userId);

    // Send approval request to server
    const response = await fetch(`${contextPath}/api/approve-user`, {
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
      throw new Error(result.message || "Failed to approve user");
    }

    // Reload relevant data
    loadUserStats();
    loadInactiveUsers();
    loadUsers();

    // Notify user
    alert(`User has been approved successfully.`);
  } catch (error) {
    console.error("Error approving user:", error);
    alert("Failed to approve user. Please try again.");
  }
}

/**
 * Search users based on input and filter
 */
function searchUsers() {
  const searchQuery = document.getElementById("searchUserQuery").value.trim();
  const roleFilter = document.getElementById("searchUserRole").value;
  loadUsers(searchQuery, roleFilter, 1); // Reset to first page on new search
}

/**
 * Add a new user
 */
async function addUser() {
  const form = document.getElementById("addUserForm");
  const contextPath = document.getElementById("contextPath").value;

  try {
    // Create form data
    const formData = new FormData(form);
    const data = {};
    formData.forEach((value, key) => (data[key] = value));

    // Send to server
    const response = await fetch(`${contextPath}/api/users`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest",
      },
      body: JSON.stringify(data),
      credentials: "same-origin",
    });

    const result = await response.json();

    if (!response.ok) {
      throw new Error(result.message || "Failed to create user");
    }

    // Show success notification
    alert("User created successfully!");

    // Reset form
    form.reset();

    // Refresh user list and stats
    loadUserStats();
    loadUsers();
  } catch (error) {
    console.error("Error creating user:", error);
    alert(`Failed to create user: ${error.message}`);
  }
}

/**
 * Show the edit user modal
 */
async function showEditUserModal(userId) {
  const contextPath = document.getElementById("contextPath").value;

  try {
    // Fetch user details
    const response = await fetch(`${contextPath}/api/users/${userId}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest",
      },
      credentials: "same-origin",
    });

    if (!response.ok) {
      throw new Error("Failed to fetch user details");
    }

    const user = await response.json();

    // Populate the form
    document.getElementById("editUserId").value = user.id;
    document.getElementById("editUserName").value = user.name;
    document.getElementById("editUserEmail").value = user.email;
    document.getElementById("editUserRole").value = user.role;
    document.getElementById("editUserStatus").value = user.status;

    // Reset password fields
    document.getElementById("resetPassword").checked = false;
    document.getElementById("newPasswordContainer").style.display = "none";
    document.getElementById("newPassword").value = "";

    // Show the modal
    document.getElementById("editUserModal").style.display = "flex";
  } catch (error) {
    console.error("Error fetching user details:", error);
    alert("Failed to load user details. Please try again.");
  }
}

/**
 * Close the edit user modal
 */
function closeEditUserModal() {
  document.getElementById("editUserModal").style.display = "none";
}

/**
 * Update a user
 */
async function updateUser() {
  const form = document.getElementById("editUserForm");
  const userId = document.getElementById("editUserId").value;
  const contextPath = document.getElementById("contextPath").value;

  try {
    // Create form data
    const formData = new FormData(form);
    const data = {};
    formData.forEach((value, key) => {
      // Only include password if reset is checked
      if (
        key === "newPassword" &&
        !document.getElementById("resetPassword").checked
      ) {
        return;
      }
      data[key] = value;
    });

    // Send to server
    const response = await fetch(`${contextPath}/api/users/${userId}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest",
      },
      body: JSON.stringify(data),
      credentials: "same-origin",
    });

    const result = await response.json();

    if (!response.ok) {
      throw new Error(result.message || "Failed to update user");
    }

    // Close modal and notify
    closeEditUserModal();
    alert("User updated successfully!");

    // Refresh user list and stats
    loadUserStats();
    loadUsers();
  } catch (error) {
    console.error("Error updating user:", error);
    alert(`Failed to update user: ${error.message}`);
  }
}

/**
 * Toggle a user's status (active/inactive)
 */
async function toggleUserStatus(userId, newStatus) {
  if (
    !confirm(
      `Are you sure you want to change this user's status to ${newStatus}?`
    )
  ) {
    return;
  }

  const contextPath = document.getElementById("contextPath").value;

  try {
    const response = await fetch(`${contextPath}/api/users/${userId}/status`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest",
      },
      body: JSON.stringify({ status: newStatus }),
      credentials: "same-origin",
    });

    const result = await response.json();

    if (!response.ok) {
      throw new Error(result.message || "Failed to update status");
    }

    // Notify success
    alert(`User status updated to ${newStatus}`);

    // Refresh user list
    loadUsers();
  } catch (error) {
    console.error("Error updating user status:", error);
    alert(`Failed to update status: ${error.message}`);
  }
}

/**
 * Render pagination controls
 */
function renderPagination(currentPage, totalPages, containerId) {
  const container = document.getElementById(containerId);
  if (!container) return;

  let html = "";

  // Previous button
  html += `<button class="pagination-btn" ${
    currentPage === 1 ? "disabled" : ""
  } 
            onclick="loadUsers('', '', ${currentPage - 1})">Previous</button>`;

  // Page numbers
  const maxButtons = 5;
  const halfButtons = Math.floor(maxButtons / 2);
  let startPage = Math.max(1, currentPage - halfButtons);
  let endPage = Math.min(totalPages, startPage + maxButtons - 1);

  // Adjust if at edges
  if (endPage - startPage + 1 < maxButtons) {
    startPage = Math.max(1, endPage - maxButtons + 1);
  }

  // First page and ellipsis if needed
  if (startPage > 1) {
    html += `<button class="pagination-btn" onclick="loadUsers('', '', 1)">1</button>`;
    if (startPage > 2) {
      html += `<span class="pagination-ellipsis">...</span>`;
    }
  }

  // Page buttons
  for (let i = startPage; i <= endPage; i++) {
    html += `<button class="pagination-btn ${
      i === currentPage ? "current" : ""
    }" 
              onclick="loadUsers('', '', ${i})">${i}</button>`;
  }

  // Last page and ellipsis if needed
  if (endPage < totalPages) {
    if (endPage < totalPages - 1) {
      html += `<span class="pagination-ellipsis">...</span>`;
    }
    html += `<button class="pagination-btn" onclick="loadUsers('', '', ${totalPages})">${totalPages}</button>`;
  }

  // Next button
  html += `<button class="pagination-btn" ${
    currentPage === totalPages ? "disabled" : ""
  } 
            onclick="loadUsers('', '', ${currentPage + 1})">Next</button>`;

  container.innerHTML = html;
}

/**
 * Helper function to get CSS class for status
 */
function getStatusClass(status) {
  switch (status.toLowerCase()) {
    case "active":
      return "status-active";
    case "inactive":
      return "status-inactive";
    case "locked":
      return "status-locked";
    default:
      return "";
  }
}

/**
 * Helper function to get CSS class for role
 */
function getRoleClass(role) {
  switch (role.toLowerCase()) {
    case "admin":
      return "role-admin";
    case "employee":
      return "role-employee";
    case "customer":
      return "role-customer";
    default:
      return "";
  }
}

/**
 * Format a date object to a readable string
 */
function formatDate(date) {
  if (!(date instanceof Date) || isNaN(date)) {
    return "N/A";
  }

  const options = { year: "numeric", month: "short", day: "numeric" };
  return date.toLocaleDateString(undefined, options);
}

/**
 * Capitalize the first letter of a string
 */
function capitalizeFirstLetter(string) {
  if (!string) return "";
  return string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();
}
