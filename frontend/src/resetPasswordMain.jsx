import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import ResetPassword from "./ResetPassword.jsx";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <ResetPassword />
  </StrictMode>
);