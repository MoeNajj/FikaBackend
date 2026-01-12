package com.bikeparking.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "Roles", schema = "BikeParking")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoleID")
    private Integer id;

    @Column(name = "RoleName", nullable = false, unique = true, length = 50)
    private String roleName;

    @Column(name = "Description", length = 255)
    private String description;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();
}

