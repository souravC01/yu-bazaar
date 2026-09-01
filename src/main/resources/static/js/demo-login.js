document.addEventListener("DOMContentLoaded", () => {
  const demoButton = document.getElementById("demoLoginButton");
  const emailField = document.getElementById("email");
  const passwordField = document.getElementById("password");

  if (!demoButton || !emailField || !passwordField) {
    return;
  }

  demoButton.addEventListener("click", () => {
    emailField.value = "demo@yubazaar.app";
    passwordField.value = "Demo@YuBazaar2026";
    emailField.focus();
  });
});
