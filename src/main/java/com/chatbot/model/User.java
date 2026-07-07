package com.chatbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username",    columnList = "username"),
    @Index(name = "idx_users_email",       columnList = "email"),
    @Index(name = "idx_users_reset_token", columnList = "resetToken")
})
@Getter @Setter @NoArgsConstructor
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "USER";

    @Column
    private String resetToken;

    @Column
    private Long resetTokenExpiry;

    public User(String name, String email, String username, String password) {
        this.name = name; this.email = email;
        this.username = username; this.password = password;
        this.role = "USER";
    }
}