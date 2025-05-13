/**
 * Dark mode functionality for JaBa Banking application
 * Handles theme toggling and persistence via localStorage
 */
function initDarkMode() {
  const darkModeToggle = document.getElementById("darkModeToggle");
  const isDarkMode = localStorage.getItem("darkMode") === "true";

  if (
    isDarkMode ||
    (window.matchMedia &&
      window.matchMedia("(prefers-color-scheme: dark)").matches &&
      localStorage.getItem("darkMode") === null)
  ) {
    document.body.classList.add("dark-mode");
  }

  darkModeToggle.addEventListener("click", () => {
    document.body.classList.toggle("dark-mode");
    localStorage.setItem(
      "darkMode",
      document.body.classList.contains("dark-mode")
    );
  });
}

// Initialize dark mode when the page loads
document.addEventListener("DOMContentLoaded", initDarkMode);
