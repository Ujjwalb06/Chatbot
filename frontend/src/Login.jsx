import { useState } from "react";
import { motion } from "framer-motion";
import "./Auth.css";

function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [msg, setMsg] = useState("");
  const [msgType, setMsgType] = useState("");

  async function handleLogin() {
    if (!username || !password) {
      return showMsg("Please fill all fields!", "error");
    }

    try {
      let res = await fetch("/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });

      let data = await res.json();

      if (data.success) {
        // ✅ Store JWT token and user info in localStorage
        localStorage.setItem("token", data.token);
        localStorage.setItem("username", data.username);
        localStorage.setItem("name", data.name);
        localStorage.setItem("role", data.role);

        showMsg("Login successful! Redirecting...", "success");
        setTimeout(() => (window.location.href = "/index.html"), 800);
      } else {
        showMsg(data.message || "Invalid credentials!", "error");
      }
    } catch (err) {
      showMsg("Server error. Please try again.", "error");
    }
  }

  function showMsg(text, type) {
    setMsg(text);
    setMsgType(type);
  }

  function handleKey(e) {
    if (e.key === "Enter") handleLogin();
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
        <h2>Welcome back</h2>
        <p className="subtitle">Login to continue chatting</p>

        <input
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          onKeyDown={handleKey}
        />
        <input
          placeholder="Password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={handleKey}
        />

        <button className="primaryBtn" onClick={handleLogin}>
          Login
        </button>

        {msg && <div className={`authMsg ${msgType}`}>{msg}</div>}

        <p className="switchLink">
          Don't have an account? <a href="/register.html">Register</a>
        </p>
      </motion.div>
    </div>
  );
}

export default Login;