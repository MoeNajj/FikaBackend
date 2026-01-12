package com.bikeparking.backend.service;

import com.bikeparking.backend.dto.AuthResponse;
import com.bikeparking.backend.dto.LoginRequest;
import com.bikeparking.backend.dto.RegisterRequest;
import com.bikeparking.backend.model.Role;
import com.bikeparking.backend.model.User;
import com.bikeparking.backend.repository.RoleRepository;
import com.bikeparking.backend.repository.UserRepository;
import com.bikeparking.backend.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setIsActive(true);
        user.setIsEmailVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Assign default USER role
        Role userRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default USER role not found"));
        user.getRoles().add(userRole);

        // Save user
        User savedUser = userRepository.save(user);

        // Generate tokens
        List<String> roles = savedUser.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        String accessToken = jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getUsername(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId(), savedUser.getUsername());

        // Build response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getJwtProperties().getAccessTokenExpiration() / 1000) // Convert to seconds
                .user(AuthResponse.UserInfo.builder()
                        .id(savedUser.getId())
                        .username(savedUser.getUsername())
                        .email(savedUser.getEmail())
                        .roles(roles)
                        .build())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        // Check if user is active
        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is deactivated");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // Build response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getJwtProperties().getAccessTokenExpiration() / 1000) // Convert to seconds
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .roles(roles)
                        .build())
                .build();
    }
}

