 import { useState, useEffect, useRef } from "react";
 import { motion, AnimatePresence } from "framer-motion";
 import ReactMarkdown from "react-markdown";
 import "./App.css";
 
 function App() {
   const [sessions, setSessions] = useState([]);
   const [currentSessionId, setCurrentSessionId] = useState(null);
   const [messages, setMessages] = useState([]);
   const [question, setQuestion] = useState("");
   const [isTyping, setIsTyping] = useState(false);
   const [username, setUsername] = useState("");
   const [role, setRole] = useState("");
   const [editingSessionId, setEditingSessionId] = useState(null);
   const [editTitle, setEditTitle] = useState("");
 
   // ✅ RAG state
   const [ragActive, setRagActive] = useState(false);
   const [ragFileName, setRagFileName] = useState("");
   const [isUploading, setIsUploading] = useState(false);
 
   const chatboxRef = useRef(null);
   const fileInputRef = useRef(null); // hidden file input
 
   useEffect(() => { init(); }, []);
 
   useEffect(() => {
     if (chatboxRef.current) {
       chatboxRef.current.scrollTop = chatboxRef.current.scrollHeight;
     }
   }, [messages, isTyping]);
 
   // ✅ Check RAG status whenever session changes
   useEffect(() => {
     if (currentSessionId) checkRagStatus(currentSessionId);
   }, [currentSessionId]);
 
   function getToken() { return localStorage.getItem("token"); }
 
   function getTime() {
     let now = new Date();
     return now.getHours().toString().padStart(2, "0") + ":" + now.getMinutes().toString().padStart(2, "0");
   }
 
   async function init() {
     let token = getToken();
     let user = localStorage.getItem("username");
     let userRole = localStorage.getItem("role");
     if (!token || !user) { window.location.href = "/login.html"; return; }
     setUsername(user);
     setRole(userRole);
     await loadSessions();
   }
 
   async function loadSessions() {
     try {
       let res = await fetch("/api/sessions", {
         headers: { Authorization: "Bearer " + getToken() },
       });
       if (res.status === 401 || res.status === 403) { logout(); return; }
       let data = await res.json();
       setSessions(data);
       if (data.length === 0) { await createNewChat(); }
       else { selectSession(data[0].id); }
     } catch (err) { console.error(err); }
   }
 
   async function createNewChat() {
     try {
       let res = await fetch("/api/sessions", {
         method: "POST",
         headers: { Authorization: "Bearer " + getToken() },
       });
       let newSession = await res.json();
       setSessions((prev) => [newSession, ...prev]);
       setCurrentSessionId(newSession.id);
       setRagActive(false);
       setRagFileName("");
       setMessages([{ role: "bot", content: `Hi ${username}! How can I help you today?`, time: getTime() }]);
     } catch (err) { console.error(err); }
   }
 
   async function selectSession(id) {
     setCurrentSessionId(id);
     try {
       let res = await fetch(`/api/sessions/${id}/messages`, {
         headers: { Authorization: "Bearer " + getToken() },
       });
       let data = await res.json();
       if (data.length === 0) {
         setMessages([{ role: "bot", content: `Hi ${username}! How can I help you today?`, time: getTime() }]);
       } else {
         let formatted = data.map((m) => ({
           role: m.role === "user" ? "user" : "bot",
           content: m.content,
           time: new Date(m.timestamp).getHours().toString().padStart(2, "0") + ":" +
                 new Date(m.timestamp).getMinutes().toString().padStart(2, "0"),
         }));
         setMessages(formatted);
       }
     } catch (err) { console.error(err); }
   }
 
   // ✅ Check if current session has a document uploaded
   async function checkRagStatus(sessionId) {
     try {
       let res = await fetch(`/api/document/status?sessionId=${sessionId}`, {
         headers: { Authorization: "Bearer " + getToken() },
       });
       let data = await res.json();
       setRagActive(data.hasDocument);
       if (!data.hasDocument) setRagFileName("");
     } catch (err) { console.error(err); }
   }
 
   // ✅ Triggered when user clicks 📎 button
   function handleAttachClick() {
     fileInputRef.current.click();
   }
 
   // ✅ Triggered when user selects a file
   async function handleFileChange(e) {
     const file = e.target.files[0];
     if (!file) return;
 
     // Reset input so same file can be re-uploaded
     e.target.value = "";
 
     const validTypes = [".pdf", ".png", ".jpg", ".jpeg"];
     const hasValidType = validTypes.some(ext => file.name.toLowerCase().endsWith(ext));
     if (!hasValidType) {
       alert("Only PDF, PNG, JPG files are supported.");
       return;
     }
 
     const isPdf = file.name.toLowerCase().endsWith(".pdf");
     const maxSize = isPdf ? 10 * 1024 * 1024 : 5 * 1024 * 1024;
     if (file.size > maxSize) {
       alert(isPdf ? "PDF must be under 10MB." : "Image must be under 5MB.");
       return;
     }
 
     setIsUploading(true);
 
     const formData = new FormData();
     formData.append("file", file);
     formData.append("sessionId", currentSessionId);
 
     try {
       let res = await fetch("/api/document/upload", {
         method: "POST",
         headers: { Authorization: "Bearer " + getToken() },
         body: formData,
       });
       let data = await res.json();
 
       if (data.success) {
         setRagActive(true);
         setRagFileName(file.name);
         // Show success message in chat
         setMessages((prev) => [
           ...prev,
           {
             role: "bot",
             content: `📄 **${file.name}** uploaded successfully! ${data.message} You can now ask questions about this document.`,
             time: getTime(),
           },
         ]);
       } else {
         alert("Upload failed: " + data.message);
       }
     } catch (err) {
       alert("Upload failed. Please try again.");
     }
 
     setIsUploading(false);
   }
 
   // ✅ Clear uploaded document
   async function handleClearDoc() {
     if (!window.confirm("Remove the uploaded document? Chat will go back to normal mode.")) return;
     try {
       await fetch(`/api/document/clear?sessionId=${currentSessionId}`, {
         method: "DELETE",
         headers: { Authorization: "Bearer " + getToken() },
       });
       setRagActive(false);
       setRagFileName("");
       setMessages((prev) => [
         ...prev,
         { role: "bot", content: "📄 Document removed. Back to normal chat mode.", time: getTime() },
       ]);
     } catch (err) { console.error(err); }
   }
 
   async function deleteSession(id, e) {
     e.stopPropagation();
     if (!window.confirm("Delete this chat? This cannot be undone.")) return;
     try {
       await fetch(`/api/sessions/${id}`, {
         method: "DELETE",
         headers: { Authorization: "Bearer " + getToken() },
       });
       let updated = sessions.filter((s) => s.id !== id);
       setSessions(updated);
       if (id === currentSessionId) {
         if (updated.length > 0) { selectSession(updated[0].id); }
         else { await createNewChat(); }
       }
     } catch (err) { console.error(err); }
   }
 
   function startRename(session, e) {
     e.stopPropagation();
     setEditingSessionId(session.id);
     setEditTitle(session.title);
   }
 
   async function saveRename(id) {
     if (!editTitle.trim()) { setEditingSessionId(null); return; }
     try {
       await fetch(`/api/sessions/${id}`, {
         method: "PUT",
         headers: { "Content-Type": "application/json", Authorization: "Bearer " + getToken() },
         body: JSON.stringify({ title: editTitle }),
       });
       setSessions((prev) => prev.map((s) => (s.id === id ? { ...s, title: editTitle } : s)));
     } catch (err) { console.error(err); }
     setEditingSessionId(null);
   }
 
   function logout() {
     localStorage.removeItem("token");
     localStorage.removeItem("username");
     localStorage.removeItem("name");
     localStorage.removeItem("role");
     window.location.href = "/login.html";
   }
 
   async function ask() {
     if (!question.trim() || !currentSessionId) return;
     let q = question.trim();
     let newMessages = [...messages, { role: "user", content: q, time: getTime() }];
     setMessages(newMessages);
     setQuestion("");
     setIsTyping(true);
 
     try {
       let res = await fetch("/api/chat", {
         method: "POST",
         headers: { "Content-Type": "application/json", Authorization: "Bearer " + getToken() },
         body: JSON.stringify({
           sessionId: currentSessionId,
           messages: newMessages.map((m) => ({
             role: m.role === "user" ? "user" : "assistant",
             content: m.content,
           })),
         }),
       });
 
       if (res.status === 401 || res.status === 403) { logout(); return; }
       if (!res.ok) throw new Error("Server error");
 
       let data = await res.json();
 
       // ✅ Add RAG badge if context was used
       setMessages([
         ...newMessages,
         {
           role: "bot",
           content: data.answer,
           time: getTime(),
           ragUsed: data.ragUsed,
         },
       ]);
 
       setSessions((prev) =>
         prev.map((s) => (s.id === currentSessionId ? { ...s, title: data.sessionTitle } : s))
       );
     } catch (err) {
       setMessages([
         ...newMessages,
         { role: "bot", content: "Could not reach server. Please try again.", time: getTime(), error: true },
       ]);
     }
 
     setIsTyping(false);
   }
 
   function handleKey(e) { if (e.key === "Enter") ask(); }
 
   function copyMessage(text, index) {
     navigator.clipboard.writeText(text);
     const el = document.getElementById(`copy-${index}`);
     if (el) { el.innerText = "Copied"; setTimeout(() => (el.innerText = "Copy"), 1500); }
   }
 
   return (
     <div className="page">
       <div className="bgGlow glowOne"></div>
       <div className="bgGlow glowTwo"></div>
 
       <div className="layout">
         {/* Sidebar */}
         <motion.div className="sidebar" initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }}>
           <button className="newChatBtn" onClick={createNewChat}>+ New Chat</button>
 
           <div className="sessionList">
             <AnimatePresence>
               {sessions.map((s) => (
                 <motion.div
                   key={s.id}
                   className={`sessionItem ${s.id === currentSessionId ? "active" : ""}`}
                   onClick={() => selectSession(s.id)}
                   initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                 >
                   {editingSessionId === s.id ? (
                     <input
                       className="renameInput"
                       value={editTitle}
                       onChange={(e) => setEditTitle(e.target.value)}
                       onBlur={() => saveRename(s.id)}
                       onKeyDown={(e) => e.key === "Enter" && saveRename(s.id)}
                       autoFocus
                       onClick={(e) => e.stopPropagation()}
                     />
                   ) : (
                     <span className="sessionTitle">{s.title}</span>
                   )}
                   <div className="sessionActions">
                     <button className="iconBtn" onClick={(e) => startRename(s, e)} title="Rename">✎</button>
                     <button className="iconBtn delete" onClick={(e) => deleteSession(s.id, e)} title="Delete">🗑</button>
                   </div>
                 </motion.div>
               ))}
             </AnimatePresence>
           </div>
 
           <div className="sidebarFooter">
             <button className="sidebarBtn" onClick={() => (window.location.href = "/profile.html")}>Profile</button>
             {role === "ADMIN" && (
               <button className="sidebarBtn" onClick={() => (window.location.href = "/users.html")}>Manage Users</button>
             )}
             <button className="sidebarBtn logout" onClick={logout}>Logout</button>
           </div>
         </motion.div>
 
         {/* Main chat area */}
         <div className="app">
           <motion.div className="header" initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}>
             <div className="brand">
               <div className="logo">AI</div>
               <div>
                 <h2>Assistant</h2>
                 <span className="username">Signed in as {username}</span>
               </div>
             </div>
 
             {/* ✅ RAG status badge in header */}
             {ragActive && (
               <div className="ragBadge">
                 <span>📄 {ragFileName || "Document active"}</span>
                 <button className="ragClearBtn" onClick={handleClearDoc} title="Remove document">✕</button>
               </div>
             )}
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
                       {m.role === "bot" ? <ReactMarkdown>{m.content}</ReactMarkdown> : m.content}
                     </div>
                     <div className="messageFooter">
                       <span className="time">{m.time}</span>
                       {/* ✅ Show RAG tag when answer came from document */}
                       {m.ragUsed && <span className="ragTag">📄 from document</span>}
                       {m.role === "bot" && !m.error && (
                         <button id={`copy-${i}`} className="copyBtn" onClick={() => copyMessage(m.content, i)}>Copy</button>
                       )}
                     </div>
                   </div>
 
                   {m.role === "user" && (
                     <div className="avatar user-avatar">{username.charAt(0).toUpperCase()}</div>
                   )}
                 </motion.div>
               ))}
 
               {isTyping && (
                 <motion.div className="messageRow bot" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                   <div className="avatar bot-avatar">AI</div>
                   <div className="message bot typingBubble">
                     <span className="dot"></span><span className="dot"></span><span className="dot"></span>
                   </div>
                 </motion.div>
               )}
             </AnimatePresence>
           </div>
 
           <motion.div className="inputBox" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4, delay: 0.1 }}>
 
             {/* ✅ Hidden file input */}
             <input
               type="file"
               accept=".pdf,.png,.jpg,.jpeg"
               ref={fileInputRef}
               style={{ display: "none" }}
               onChange={handleFileChange}
             />
 
             {/* ✅ Attach button */}
             <button
               className={`attachBtn ${ragActive ? "attachActive" : ""}`}
               onClick={handleAttachClick}
               disabled={isUploading}
               title={ragActive ? "Document uploaded — click to replace" : "Upload a PDF document"}
             >
               {isUploading ? "⏳" : "📎"}
             </button>
 
             <input
               value={question}
               onChange={(e) => setQuestion(e.target.value)}
               onKeyDown={handleKey}
               placeholder={ragActive ? "Ask about your document..." : "Message Assistant..."}
             />
             <button className="sendBtn" onClick={ask} disabled={!question.trim()}>Send</button>
           </motion.div>
         </div>
       </div>
     </div>
   );
 }
 
 export default App;
