import { useState } from "react";
import { motion } from "framer-motion";
import "./Auth.css";

function Register() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [msg, setMsg] = useState("");
  const [msgType, setMsgType] = useState("");

  async function handleRegister() {
    if (!name || !email || !username || !password) {
      return showMsg("Please fill all fields!", "error");
    }

    try {
      let res = await fetch("/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, email, username, password }),
      });

      let data = await res.json();

      if (data.success) {
        showMsg("Registered successfully! Redirecting to login...", "success");
        setTimeout(() => (window.location.href = "/login.html"), 1200);
      } else {
        showMsg(data.message, "error");
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
    if (e.key === "Enter") handleRegister();
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
        <h2>Create your account</h2>
        <p className="subtitle">Sign up to start chatting</p>

        <input
          placeholder="Full Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={handleKey}
        />
        <input
          placeholder="Email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          onKeyDown={handleKey}
        />
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

        <button className="primaryBtn" onClick={handleRegister}>
          Register
        </button>

        {msg && <div className={`authMsg ${msgType}`}>{msg}</div>}

        <p className="switchLink">
          Already have an account? <a href="/login.html">Login</a>
        </p>
      </motion.div>
    </div>
  );
}

export default Register;