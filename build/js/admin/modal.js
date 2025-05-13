/**
 * Modal functionality for admin interfaces
 */
document.addEventListener("DOMContentLoaded", function () {
  const createUserModal = document.getElementById("createUserModal");
  const openModalBtn = document.getElementById("openCreateUserModalBtn");
  const closeModalBtn = document.getElementById("closeModalBtn");
  const createUserForm = document.getElementById("createUserForm");
  const modalErrorMessage = document.getElementById("modalErrorMessage");

  // Only initialize if the elements exist on the page
  if (!createUserModal || !openModalBtn) {
    return;
  }

  function openModal() {
    modalErrorMessage.style.display = "none"; // Hide error on open
    createUserForm.reset(); // Clear form fields
    createUserModal.style.display = "flex"; // Show the modal (using flex for centering)
  }

  function closeModal() {
    createUserModal.style.display = "none"; // Hide the modal
  }

  // Event Listeners
  openModalBtn.addEventListener("click", (event) => {
    event.preventDefault(); // Prevent default anchor behavior
    openModal();
  });

  closeModalBtn.addEventListener("click", closeModal);

  // Close modal if user clicks outside the modal content
  createUserModal.addEventListener("click", (event) => {
    // Check if the click is directly on the modal background (overlay)
    if (event.target === createUserModal) {
      closeModal();
    }
  });

  // Close modal on Escape key press
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && createUserModal.style.display === "flex") {
      closeModal();
    }
  });

  // Form validation before submission
  createUserForm.addEventListener("submit", (event) => {
    const password = document.getElementById("modal-password").value;
    const confirmPassword = document.getElementById(
      "modal-confirmPassword"
    ).value;
    const role = document.getElementById("modal-role").value;

    modalErrorMessage.style.display = "none"; // Hide previous error
    let errors = [];

    if (password !== confirmPassword) {
      errors.push("Passwords do not match.");
    }

    if (password.length < 6) {
      // Basic length check
      errors.push("Password must be at least 6 characters long.");
    }

    if (!role) {
      // Check if a role was selected
      errors.push("Please select a role for the user.");
    }

    if (errors.length > 0) {
      event.preventDefault(); // Stop form submission
      modalErrorMessage.textContent = errors.join(" ");
      modalErrorMessage.style.display = "block"; // Show errors
    }
    // If no errors, the form will submit naturally
  });

  // Handle URL error parameters
  function displayModalErrorFromURL() {
    const urlParams = new URLSearchParams(window.location.search);
    const modalError = urlParams.get("modal_error"); // Use a specific param for modal errors

    if (modalError) {
      modalErrorMessage.textContent = decodeURIComponent(
        modalError.replace(/_/g, " ")
      );
      modalErrorMessage.style.display = "block";
      openModal(); // Re-open the modal to show the error
    }
  }

  displayModalErrorFromURL();
});
