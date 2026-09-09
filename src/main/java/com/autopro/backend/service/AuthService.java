package com.autopro.backend.service;

import com.autopro.backend.dto.auth.*;
import com.autopro.backend.dto.user.UserResponse;
import com.autopro.backend.entity.Mechanic;
import com.autopro.backend.entity.PasswordResetToken;
import com.autopro.backend.entity.Role;
import com.autopro.backend.entity.User;
import com.autopro.backend.repository.MechanicRepository;
import com.autopro.backend.repository.PasswordResetTokenRepository;
import com.autopro.backend.repository.RoleRepository;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MechanicRepository mechanicRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private static final List<String> SELF_SIGNUP_ALLOWED_ROLES = List.of("ROLE_CLIENT", "ROLE_MECHANIC");

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        // ROLE_ADMIN ne doit jamais pouvoir être obtenu via l'inscription publique.
        String requestedRole = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole() : "ROLE_CLIENT";
        if (!SELF_SIGNUP_ALLOWED_ROLES.contains(requestedRole)) {
            throw new IllegalArgumentException("Rôle non autorisé pour l'inscription : " + requestedRole);
        }
        Role role = roleRepository.findByName(requestedRole)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + requestedRole));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .build();
        userRepository.save(user);

        // Un compte mécanicien n'est utilisable qu'avec un profil Mechanic associé.
        // Il est créé ici, en attente de validation par un admin (validationStatus = PENDING)
        // et indisponible (isAvailable = false) tant qu'il n'est pas approuvé.
        if ("ROLE_MECHANIC".equals(requestedRole)) {
            Mechanic mechanic = Mechanic.builder()
                    .user(user)
                    .specialization(request.getSpecialization())
                    .experienceYears(request.getExperienceYears())
                    .bio(request.getBio())
                    .isAvailable(false)
                    .build();
            mechanicRepository.save(mechanic);
            user.setMechanic(mechanic);
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return buildAuthResponse(user);
    }

    /** Message unique renvoyé par {@link #requestPasswordReset}, quel que soit le cas. */
    private static final String RESET_ACK =
            "Si un compte existe pour cet email, un lien de réinitialisation a été envoyé.";

    @Transactional
    public String requestPasswordReset(PasswordResetRequest request) {
        // Ne pas révéler si l'email existe ou non (énumération de comptes) : même réponse dans les deux cas.
        var userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return RESET_ACK;
        }
        String token = UUID.randomUUID().toString();
        resetTokenRepository.save(PasswordResetToken.builder()
                .token(token)
                .user(userOpt.get())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build());

        // Le token n'est JAMAIS renvoyé dans la réponse HTTP : le faire permettait à quiconque
        // de déclencher ET récupérer un token pour n'importe quel email (prise de contrôle de
        // compte). Faute de service d'envoi d'email, il est journalisé côté serveur — un
        // administrateur peut le transmettre manuellement. TODO(#24) : envoi par email.
        log.info("Reset token généré pour {} : {} (à transmettre par email une fois le service en place)",
                userOpt.get().getEmail(), token);

        return RESET_ACK;
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

    private AuthResponse buildAuthResponse(User user) {
        String roleName = user.getRole().getName();
        var springUser = new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPassword(),
                List.of(new SimpleGrantedAuthority(roleName))
        );
        return AuthResponse.builder()
                .token(jwtService.generateToken(springUser))
                .type("Bearer")
                .user(UserResponse.from(user))
                .build();
    }
}
