import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import "./Auth.css";

function ResetPassword() {
  const [token, setToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [msg, setMsg] = useState("");
  const [msgType, setMsgType] = useState("");
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const t = params.get("token");
    if (!t) {
      showMsg("Invalid reset link", "error");
    } else {
      setToken(t);
    }
  }, []);

  function showMsg(text, type) {
    setMsg(text);
    setMsgType(type);
  }

  async function handleSubmit() {
    if (!newPassword || !confirmPassword) {
      return showMsg("Please fill all fields", "error");
    }
    if (newPassword !== confirmPassword) {
      return showMsg("Passwords do not match", "error");
    }
    if (newPassword.length < 4) {
      return showMsg("Password must be at least 4 characters", "error");
    }

    try {
      let res = await fetch("/auth/reset-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token, newPassword }),
      });

      let data = await res.json();
      showMsg(data.message, data.success ? "success" : "error");

      if (data.success) {
        setSuccess(true);
        setTimeout(() => (window.location.href = "/login.html"), 2000);
      }
    } catch (err) {
      showMsg("Server error. Please try again.", "error");
    }
  }

  function handleKey(e) {
    if (e.key === "Enter") handleSubmit();
  }

  return (
    <div className="authPage">
      <div className="bgGlow glowOne"></div>
      <div className="bgGlow glowTwo"></div>

      <motion.div
        className="authBox"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
      >
        <div className="logo">AI</div>
        <h2>Reset Password</h2>
        <p className="subtitle">Enter your new password</p>

        {!success && (
          <>
            <input
              placeholder="New Password"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              onKeyDown={handleKey}
            />
            <input
              placeholder="Confirm New Password"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              onKeyDown={handleKey}
            />

            <button className="primaryBtn" onClick={handleSubmit}>
              Reset Password
            </button>
          </>
        )}

        {msg && <div className={`authMsg ${msgType}`}>{msg}</div>}

        {!success && (
          <p className="switchLink">
            <a href="/login.html">Back to Login</a>
          </p>
        )}
      </motion.div>
    </div>
  );
}

export default ResetPassword;