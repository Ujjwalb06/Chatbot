package com.chatbot.util;

public class Validator {

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }

    public static void validateEmail(String email) {
        requireNonBlank(email, "Email");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email format.");
        }
    }

    public static void validatePassword(String password) {
        requireNonBlank(password, "Password");
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
    }

    public static void validateUsername(String username) {
        requireNonBlank(username, "Username");
        if (username.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters.");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, and underscores.");
        }
    }
}