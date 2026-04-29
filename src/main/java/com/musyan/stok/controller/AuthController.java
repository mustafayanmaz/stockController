package com.musyan.stok.controller;

import com.musyan.stok.dto.AuthRequestDto;
import com.musyan.stok.dto.AuthResponseDto;
import com.musyan.stok.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication APIs", description = "REST API to get JWT tokens")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    @Operation(summary = "Login to get token", description = "Use admin/admin to login and get a JWT token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto requestDto) {

        // Temporary hardcoded basic authentication mechanism
        if ("admin".equals(requestDto.getUsername()) && "admin".equals(requestDto.getPassword())) {
            String token = jwtUtil.generateToken(requestDto.getUsername());
            return ResponseEntity.ok(new AuthResponseDto(token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
