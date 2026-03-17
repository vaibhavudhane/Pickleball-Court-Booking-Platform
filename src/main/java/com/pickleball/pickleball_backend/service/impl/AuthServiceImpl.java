package com.pickleball.pickleball_backend.service.impl;

import com.pickleball.pickleball_backend.dto.request.LoginRequest;
import com.pickleball.pickleball_backend.dto.request.RegisterRequest;
import com.pickleball.pickleball_backend.dto.response.AuthResponseDTO;
import com.pickleball.pickleball_backend.entity.User;
import com.pickleball.pickleball_backend.repository.UserRepository;
import com.pickleball.pickleball_backend.service.AuthService;
import com.pickleball.pickleball_backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponseDTO register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already registered: " + request.email());
        }

        // Hash the password BEFORE saving to database
        String hashedPassword = passwordEncoder.encode(request.password());

        // Build and save user
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(hashedPassword)
                .role(request.role())
                .build();
        User saved = userRepository.save(user);

        // Generate JWT and return
        String token = jwtUtil.generateToken(saved);
        return new AuthResponseDTO(token, saved.getRole().name(), saved.getName(), saved.getId());
    }

    public AuthResponseDTO login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Compare submitted password with stored hash
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user);
        return new AuthResponseDTO(token, user.getRole().name(), user.getName(), user.getId());
    }
}
