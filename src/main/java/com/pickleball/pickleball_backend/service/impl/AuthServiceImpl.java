package com.pickleball.pickleball_backend.service.impl;

import com.pickleball.pickleball_backend.dto.request.LoginRequest;
import com.pickleball.pickleball_backend.dto.request.RegisterRequest;
import com.pickleball.pickleball_backend.dto.response.AuthResponseDTO;
import com.pickleball.pickleball_backend.entity.User;
import com.pickleball.pickleball_backend.repository.UserRepository;
import com.pickleball.pickleball_backend.service.AuthService;
import com.pickleball.pickleball_backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponseDTO register(RegisterRequest request) {
        // Never log email — PII protection
        log.info("Registration attempt received");

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed — email already exists");
            throw new RuntimeException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(request.email().toLowerCase().trim())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        User saved = userRepository.save(user);

        // Safe to log userId and role — not PII
        log.info("User registered successfully — userId: {}, role: {}",
                saved.getId(), saved.getRole());

        String token = jwtUtil.generateToken(saved);
        return new AuthResponseDTO(token, saved.getRole().name(),
                saved.getName(), saved.getId());
    }

    @Override
    public AuthResponseDTO login(LoginRequest request) {
        // Never log email — PII protection
        log.info("Login attempt received");

        User user = userRepository.findByEmail(
                        request.email().toLowerCase().trim())
                .orElseThrow(() -> {
                    log.warn("Login failed — user not found");
                    return new RuntimeException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            // Never log which email failed — security protection
            log.warn("Login failed — invalid credentials for userId: {}", user.getId());
            throw new RuntimeException("Invalid email or password");
        }

        // Safe to log userId and role
        log.info("Login successful — userId: {}, role: {}", user.getId(), user.getRole());

        String token = jwtUtil.generateToken(user);
        return new AuthResponseDTO(token, user.getRole().name(),
                user.getName(), user.getId());
    }
}