import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import "./Users.css";

function Users() {
  const [users, setUsers] = useState([]);
  const [editingUser, setEditingUser] = useState(null);
  const [msg, setMsg] = useState("");
  const [msgType, setMsgType] = useState("");

  useEffect(() => {
    checkAdminAndLoad();
  }, []);

  function getToken() {
    return localStorage.getItem("token");
  }

  async function checkAdminAndLoad() {
    let token = getToken();
    let role = localStorage.getItem("role");

    if (!token) {
      window.location.href = "/login.html";
      return;
    }
    if (role !== "ADMIN") {
      window.location.href = "/index.html";
      return;
    }

    loadUsers();
  }

  async function loadUsers() {
    try {
      let res = await fetch("/api/users", {
        headers: { Authorization: "Bearer " + getToken() },
      });

      if (res.status === 401 || res.status === 403) {
        window.location.href = "/index.html";
        return;
      }

      let data = await res.json();
      setUsers(data);
    } catch (err) {
      showMsg("Failed to load users", "error");
    }
  }

  function showMsg(text, type) {
    setMsg(text);
    setMsgType(type);
    setTimeout(() => setMsg(""), 3000);
  }

  function startEdit(user) {
    setEditingUser({ ...user });
  }

  function cancelEdit() {
    setEditingUser(null);
  }

  async function saveEdit() {
    try {
      let res = await fetch(`/api/users/${editingUser.id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer " + getToken(),
        },
        body: JSON.stringify(editingUser),
      });

      let text = await res.text();
      showMsg(text, text.includes("success") ? "success" : "error");
      setEditingUser(null);
      loadUsers();
    } catch (err) {
      showMsg("Failed to update user", "error");
    }
  }

  async function deleteUser(id, username) {
    if (!window.confirm(`Delete user "${username}"? This cannot be undone.`)) return;

    try {
      let res = await fetch(`/api/users/${id}`, {
        method: "DELETE",
        headers: { Authorization: "Bearer " + getToken() },
      });

      let text = await res.text();
      showMsg(text, text.includes("success") ? "success" : "error");
      loadUsers();
    } catch (err) {
      showMsg("Failed to delete user", "error");
    }
  }

  return (
    <div className="usersPage">
      <div className="bgGlow glowOne"></div>
      <div className="bgGlow glowTwo"></div>

      <div className="usersContainer">
        <motion.div
          className="usersHeader"
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <div className="brand">
            <div className="logo">AI</div>
            <h2>Manage Users</h2>
          </div>
          <button className="backBtn" onClick={() => (window.location.href = "/index.html")}>
            Back to Chat
          </button>
        </motion.div>

        {msg && <div className={`toast ${msgType}`}>{msg}</div>}

        <motion.div
          className="usersTableWrapper"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <table className="usersTable">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Username</th>
                <th>Role</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <AnimatePresence>
                {users.map((u) => (
                  <motion.tr
                    key={u.id}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                  >
                    <td>{u.name}</td>
                    <td>{u.email}</td>
                    <td>{u.username}</td>
                    <td>
                      <span className={`roleBadge ${u.role.toLowerCase()}`}>{u.role}</span>
                    </td>
                    <td>
                      <button className="editBtn" onClick={() => startEdit(u)}>Edit</button>
                      <button className="deleteBtn" onClick={() => deleteUser(u.id, u.username)}>Delete</button>
                    </td>
                  </motion.tr>
                ))}
              </AnimatePresence>
            </tbody>
          </table>
        </motion.div>
      </div>

      {/* Edit Modal */}
      <AnimatePresence>
        {editingUser && (
          <motion.div
            className="modalOverlay"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <motion.div
              className="modalBox"
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
            >
              <h3>Edit User</h3>

              <label>Name</label>
              <input
                value={editingUser.name}
                onChange={(e) => setEditingUser({ ...editingUser, name: e.target.value })}
              />

              <label>Email</label>
              <input
                value={editingUser.email}
                onChange={(e) => setEditingUser({ ...editingUser, email: e.target.value })}
              />

              <label>Username</label>
              <input
                value={editingUser.username}
                onChange={(e) => setEditingUser({ ...editingUser, username: e.target.value })}
              />

              <label>Role</label>
              <select
                value={editingUser.role}
                onChange={(e) => setEditingUser({ ...editingUser, role: e.target.value })}
              >
                <option value="USER">USER</option>
                <option value="ADMIN">ADMIN</option>
              </select>

              <div className="modalActions">
                <button className="cancelBtn" onClick={cancelEdit}>Cancel</button>
                <button className="saveBtn" onClick={saveEdit}>Save Changes</button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

export default Users;