package com.autopro.backend.service;

import com.autopro.backend.dto.auth.*;
import com.autopro.backend.entity.PasswordResetToken;
import com.autopro.backend.entity.Role;
import com.autopro.backend.entity.User;
import com.autopro.backend.repository.PasswordResetTokenRepository;
import com.autopro.backend.repository.RoleRepository;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        String roleName = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole() : "ROLE_CLIENT";
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .build();
        userRepository.save(user);

        return buildAuthResponse(user.getEmail(), passwordEncoder.encode(request.getPassword()), role.getName());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return buildAuthResponse(user.getEmail(), user.getPassword(), user.getRole().getName());
    }

    @Transactional
    public String requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String token = UUID.randomUUID().toString();
        resetTokenRepository.save(PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build());
        // Production : envoyer le token par email
        return token;
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));
        if (resetToken.getUsed()) {
            throw new IllegalArgumentException("Token already used");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expired");
        }
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }

    private AuthResponse buildAuthResponse(String email, String encodedPassword, String roleName) {
        var springUser = new org.springframework.security.core.userdetails.User(
                email, encodedPassword,
                List.of(new SimpleGrantedAuthority(roleName))
        );
        return AuthResponse.builder()
                .token(jwtService.generateToken(springUser))
                .type("Bearer")
                .email(email)
                .role(roleName)
                .build();
    }
}
