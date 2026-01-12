package com.bikeparking.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "Users", schema = "BikeParking")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Integer id;

    @Column(name = "Username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "Email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "FirstName", length = 100)
    private String firstName;

    @Column(name = "LastName", length = 100)
    private String lastName;

    @Column(name = "IsActive", nullable = false)
    private Boolean isActive = true;

    @Column(name = "IsEmailVerified", nullable = false)
    private Boolean isEmailVerified = false;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "LastLogin")
    private LocalDateTime lastLogin;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "UserRoles",
        schema = "BikeParking",
        joinColumns = @JoinColumn(name = "UserID"),
        inverseJoinColumns = @JoinColumn(name = "RoleID")
    )
    private Set<Role> roles = new HashSet<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

