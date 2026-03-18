package com.pickleball.pickleball_backend.service.impl;

import com.pickleball.pickleball_backend.entity.User;
import com.pickleball.pickleball_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        // Never log email — PII protection
        log.debug("Loading user details for authentication");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    // Never log the email that was not found — security protection
                    // Logging emails of failed lookups can expose valid accounts
                    log.warn("Authentication failed — user not found");
                    return new UsernameNotFoundException("User not found: " + email);
                });

        // Safe to log userId and role
        log.debug("User details loaded — userId: {}, role: {}",
                user.getId(), user.getRole());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}