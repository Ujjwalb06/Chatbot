import { useState } from "react";
import { motion } from "framer-motion";
import "./Auth.css";

function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [msg, setMsg] = useState("");
  const [msgType, setMsgType] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit() {
    if (!email.trim()) {
      return showMsg("Please enter your email", "error");
    }

    setLoading(true);

    try {
      let res = await fetch("/auth/forgot-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });

      let data = await res.json();
      showMsg(data.message, data.success ? "success" : "error");
    } catch (err) {
      showMsg("Server error. Please try again.", "error");
    }

    setLoading(false);
  }

  function showMsg(text, type) {
    setMsg(text);
    setMsgType(type);
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
        <h2>Forgot Password?</h2>
        <p className="subtitle">Enter your email to receive a reset link</p>

        <input
          placeholder="Email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          onKeyDown={handleKey}
        />

        <button className="primaryBtn" onClick={handleSubmit} disabled={loading}>
          {loading ? "Sending..." : "Send Reset Link"}
        </button>

        {msg && <div className={`authMsg ${msgType}`}>{msg}</div>}

        <p className="switchLink">
          Remember your password? <a href="/login.html">Login</a>
        </p>
      </motion.div>
    </div>
  );
}

export default ForgotPassword;