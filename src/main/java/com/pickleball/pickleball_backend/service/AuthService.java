package com.pickleball.pickleball_backend.service;


import com.pickleball.pickleball_backend.dto.request.LoginRequest;
import com.pickleball.pickleball_backend.dto.request.RegisterRequest;
import com.pickleball.pickleball_backend.dto.response.AuthResponseDTO;

public interface AuthService {
    AuthResponseDTO register(RegisterRequest request);
    AuthResponseDTO login(LoginRequest request);
}
