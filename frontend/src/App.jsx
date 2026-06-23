import { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import ReactMarkdown from "react-markdown";
import "./App.css";

function App() {
  const [messages, setMessages] = useState([]);
  const [question, setQuestion] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [username, setUsername] = useState("");
  const [role, setRole] = useState("");
  const chatboxRef = useRef(null);

  useEffect(() => {
    checkLogin();
  }, []);

  useEffect(() => {
    if (chatboxRef.current) {
      chatboxRef.current.scrollTop = chatboxRef.current.scrollHeight;
    }
  }, [messages, isTyping]);

  function getTime() {
    let now = new Date();
    return now.getHours().toString().padStart(2, "0") + ":" + now.getMinutes().toString().padStart(2, "0");
  }

  function checkLogin() {
    let token = localStorage.getItem("token");
    let user = localStorage.getItem("username");
    let userRole = localStorage.getItem("role");

    if (!token || !user) {
      window.location.href = "/login.html";
      return;
    }

    setUsername(user);
    setRole(userRole);
    loadHistory(user, token);
}

async function loadHistory(user, token) {
    try {
        let res = await fetch("/api/history", {
            headers: { Authorization: "Bearer " + token },
        });

        if (!res.ok) throw new Error("Failed to load history");

        let data = await res.json();

        if (data.length === 0) {
            setMessages([{ role: "bot", content: `Hi ${user}! How can I help you today?`, time: getTime() }]);
        } else {
            let formatted = data.map((m) => ({
                role: m.role === "user" ? "user" : "bot",
                content: m.content,
                time: new Date(m.timestamp).getHours().toString().padStart(2, "0") + ":" +
                      new Date(m.timestamp).getMinutes().toString().padStart(2, "0"),
            }));
            setMessages(formatted);
        }
    } catch (err) {
        setMessages([{ role: "bot", content: `Hi ${user}! How can I help you today?`, time: getTime() }]);
    }
}

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    localStorage.removeItem("name");
    localStorage.removeItem("role");
    window.location.href = "/login.html";
  }

  async function ask() {
    if (!question.trim()) return;

    let q = question.trim();
    let newMessages = [...messages, { role: "user", content: q, time: getTime() }];
    setMessages(newMessages);
    setQuestion("");
    setIsTyping(true);

    try {
      let token = localStorage.getItem("token");

      let res = await fetch("/api/chat", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": "Bearer " + token,   // ✅ attach JWT
        },
        body: JSON.stringify({
          messages: newMessages.map((m) => ({
            role: m.role === "user" ? "user" : "assistant",
            content: m.content,
          })),
        }),
      });

      if (res.status === 401 || res.status === 403) {
        logout(); // token invalid/expired
        return;
      }

      if (!res.ok) throw new Error("Server error");

      let text = await res.text();
      setMessages([...newMessages, { role: "bot", content: text, time: getTime() }]);
    } catch (err) {
      setMessages([
        ...newMessages,
        { role: "bot", content: "Could not reach server. Please try again.", time: getTime(), error: true },
      ]);
    }

    setIsTyping(false);
  }

  function handleKey(e) {
    if (e.key === "Enter") ask();
  }

  async function clearChat() {
    let token = localStorage.getItem("token");

    try {
        await fetch("/api/history", {
            method: "DELETE",
            headers: { Authorization: "Bearer " + token },
        });
    } catch (err) {
        console.error("Failed to clear history on server");
    }

    setMessages([{ role: "bot", content: `Hi ${username}! How can I help you today?`, time: getTime() }]);
}

  function copyMessage(text, index) {
    navigator.clipboard.writeText(text);
    const el = document.getElementById(`copy-${index}`);
    if (el) {
      el.innerText = "Copied";
      setTimeout(() => (el.innerText = "Copy"), 1500);
    }
  }

  return (
    <div className="page">
      <div className="bgGlow glowOne"></div>
      <div className="bgGlow glowTwo"></div>

      <div className="app">
        <motion.div
          className="header"
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
        >
          <div className="brand">
            <div className="logo">AI</div>
            <div>
              <h2>Assistant</h2>
              <span className="username">Signed in as {username} {role === "ADMIN" && "(Admin)"}</span>
            </div>
          </div>
          <div style={{ display: "flex", gap: "10px" }}>
            {role === "ADMIN" && (
              <button className="logoutBtn" onClick={() => (window.location.href = "/users.html")}>
                Manage Users
              </button>
            )}
            <button className="logoutBtn" onClick={logout}>Logout</button>
          </div>
        </motion.div>

        <div className="chatbox" ref={chatboxRef}>
          <AnimatePresence initial={false}>
            {messages.map((m, i) => (
              <motion.div
                key={i}
                className={`messageRow ${m.role}`}
                initial={{ opacity: 0, y: 14, scale: 0.98 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ duration: 0.28, ease: "easeOut" }}
              >
                {m.role === "bot" && <div className="avatar bot-avatar">AI</div>}

                <div className={`message ${m.role} ${m.error ? "error" : ""}`}>
                  <div className="messageText">
                    {m.role === "bot" ? (
                      <ReactMarkdown>{m.content}</ReactMarkdown>
                    ) : (
                      m.content
                    )}
                  </div>
                  <div className="messageFooter">
                    <span className="time">{m.time}</span>
                    {m.role === "bot" && !m.error && (
                      <button
                        id={`copy-${i}`}
                        className="copyBtn"
                        onClick={() => copyMessage(m.content, i)}
                      >
                        Copy
                      </button>
                    )}
                  </div>
                </div>

                {m.role === "user" && (
                  <div className="avatar user-avatar">{username.charAt(0).toUpperCase()}</div>
                )}
              </motion.div>
            ))}

            {isTyping && (
              <motion.div
                className="messageRow bot"
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
              >
                <div className="avatar bot-avatar">AI</div>
                <div className="message bot typingBubble">
                  <span className="dot"></span>
                  <span className="dot"></span>
                  <span className="dot"></span>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <motion.div
          className="inputBox"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.1 }}
        >
          <input
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={handleKey}
            placeholder="Message Assistant..."
          />
          <button className="sendBtn" onClick={ask} disabled={!question.trim()}>
            Send
          </button>
          <button className="clearBtn" onClick={clearChat}>
            Clear
          </button>
        </motion.div>
      </div>
    </div>
  );
}

export default App;