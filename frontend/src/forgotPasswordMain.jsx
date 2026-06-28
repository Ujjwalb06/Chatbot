import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import ForgotPassword from "./ForgotPassword.jsx";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <ForgotPassword />
  </StrictMode>
);