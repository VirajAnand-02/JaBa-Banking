/**
 * JaBa Banking Login & Registration
 * This file contains all the JavaScript functionality for the login and registration page
 */

// Tab switching functionality
function switchTab(tab) {
  const loginTab = document.getElementById("login-tab");
  const registerTab = document.getElementById("register-tab");
  const loginForm = document.getElementById("login-form");
  const registerForm = document.getElementById("register-form");
  const descText = document.getElementById("desc-text");

  if (tab === "login") {
    loginTab.classList.add("active");
    registerTab.classList.remove("active");
    loginForm.style.display = "block";
    registerForm.style.display = "none";
    descText.textContent = "Sign in to your account";
  } else {
    loginTab.classList.remove("active");
    registerTab.classList.add("active");
    loginForm.style.display = "none";
    registerForm.style.display = "block";
    descText.textContent = "Create your account";
  }
}

// Dark mode toggle functionality
function initDarkMode() {
  const darkModeToggle = document.getElementById("darkModeToggle");

  // Check for saved preference in localStorage
  const isDarkMode = localStorage.getItem("darkMode") === "true";

  // Set initial state based on localStorage or system preference
  if (
    isDarkMode ||
    (window.matchMedia &&
      window.matchMedia("(prefers-color-scheme: dark)").matches &&
      localStorage.getItem("darkMode") === null)
  ) {
    document.body.classList.add("dark-mode");
  }

  // Add click event listener for toggling
  darkModeToggle.addEventListener("click", () => {
    document.body.classList.toggle("dark-mode");

    // Save preference to localStorage
    localStorage.setItem(
      "darkMode",
      document.body.classList.contains("dark-mode")
    );
  });
}

// Handle URL parameters for success/error messages
function displayMessages() {
  const urlParams = new URLSearchParams(window.location.search);
  const error = urlParams.get("error");
  const success = urlParams.get("success");
  const messageContainer = document.getElementById("message-container");

  if (error) {
    const errorMsg = error.replace(/_/g, " ");
    messageContainer.innerHTML = `<div class="alert alert-error">${errorMsg}</div>`;
  } else if (success) {
    const successMsg = success.replace(/_/g, " ");
    messageContainer.innerHTML = `<div class="alert alert-success">${successMsg}</div>`;
  }
}

// Initialize when the DOM is fully loaded
document.addEventListener("DOMContentLoaded", function () {
  // Initialize dark mode
  initDarkMode();

  // Display messages
  displayMessages();
});
