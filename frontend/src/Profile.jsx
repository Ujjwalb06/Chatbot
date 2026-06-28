import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import "./Profile.css";

function Profile() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [role, setRole] = useState("");

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [profileMsg, setProfileMsg] = useState("");
  const [profileMsgType, setProfileMsgType] = useState("");

  const [passwordMsg, setPasswordMsg] = useState("");
  const [passwordMsgType, setPasswordMsgType] = useState("");

  useEffect(() => {
    loadProfile();
  }, []);

  function getToken() {
    return localStorage.getItem("token");
  }

  async function loadProfile() {
    let token = getToken();
    if (!token) {
      window.location.href = "/login.html";
      return;
    }

    try {
      let res = await fetch("/api/profile", {
        headers: { Authorization: "Bearer " + token },
      });

      if (res.status === 401 || res.status === 403) {
        window.location.href = "/login.html";
        return;
      }

      let data = await res.json();
      setName(data.name || "");
      setEmail(data.email || "");
      setUsername(data.username || "");
      setRole(data.role || "");
    } catch (err) {
      showProfileMsg("Failed to load profile", "error");
    }
  }

  function showProfileMsg(text, type) {
    setProfileMsg(text);
    setProfileMsgType(type);
    setTimeout(() => setProfileMsg(""), 3000);
  }

  function showPasswordMsg(text, type) {
    setPasswordMsg(text);
    setPasswordMsgType(type);
    setTimeout(() => setPasswordMsg(""), 3000);
  }

  async function saveProfile() {
    if (!name.trim() || !email.trim()) {
      return showProfileMsg("Name and email cannot be empty", "error");
    }

    try {
      let res = await fetch("/api/profile", {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer " + getToken(),
        },
        body: JSON.stringify({ name, email }),
      });

      let data = await res.json();
      showProfileMsg(data.message, data.success ? "success" : "error");

      if (data.success) {
        localStorage.setItem("name", name);
      }
    } catch (err) {
      showProfileMsg("Server error. Please try again.", "error");
    }
  }

  async function changePassword() {
    if (!currentPassword || !newPassword || !confirmPassword) {
      return showPasswordMsg("Please fill all password fields", "error");
    }
    if (newPassword !== confirmPassword) {
      return showPasswordMsg("New passwords do not match", "error");
    }
    if (newPassword.length < 4) {
      return showPasswordMsg("Password must be at least 4 characters", "error");
    }

    try {
      let res = await fetch("/api/profile/password", {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer " + getToken(),
        },
        body: JSON.stringify({ currentPassword, newPassword }),
      });

      let data = await res.json();
      showPasswordMsg(data.message, data.success ? "success" : "error");

      if (data.success) {
        setCurrentPassword("");
        setNewPassword("");
        setConfirmPassword("");
      }
    } catch (err) {
      showPasswordMsg("Server error. Please try again.", "error");
    }
  }

  return (
    <div className="profilePage">
      <div className="bgGlow glowOne"></div>
      <div className="bgGlow glowTwo"></div>

      <div className="profileContainer">
        <motion.div
          className="profileHeader"
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <div className="brand">
            <div className="logo">AI</div>
            <h2>Profile Settings</h2>
          </div>
          <button className="backBtn" onClick={() => (window.location.href = "/index.html")}>
            Back to Chat
          </button>
        </motion.div>

        <div className="profileGrid">
          {/* Profile Info Card */}
          <motion.div
            className="profileCard"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
          >
            <div className="avatarLarge">{name.charAt(0).toUpperCase() || "?"}</div>

            <h3>Account Information</h3>

            <label>Full Name</label>
            <input value={name} onChange={(e) => setName(e.target.value)} />

            <label>Email</label>
            <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" />

            <label>Username</label>
            <input value={username} disabled className="disabledInput" />

            <label>Role</label>
            <input value={role} disabled className="disabledInput" />

            <button className="saveBtn" onClick={saveProfile}>
              Save Changes
            </button>

            {profileMsg && <div className={`profileMsg ${profileMsgType}`}>{profileMsg}</div>}
          </motion.div>

          {/* Change Password Card */}
          <motion.div
            className="profileCard"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
          >
            <h3>Change Password</h3>

            <label>Current Password</label>
            <input
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
            />

            <label>New Password</label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />

            <label>Confirm New Password</label>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />

            <button className="saveBtn" onClick={changePassword}>
              Update Password
            </button>

            {passwordMsg && <div className={`profileMsg ${passwordMsgType}`}>{passwordMsg}</div>}
          </motion.div>
        </div>
      </div>
    </div>
  );
}

export default Profile;