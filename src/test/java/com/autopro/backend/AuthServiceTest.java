package com.autopro.backend;

import com.autopro.backend.dto.auth.PasswordResetRequest;
import com.autopro.backend.dto.auth.SignUpRequest;
import com.autopro.backend.entity.PasswordResetToken;
import com.autopro.backend.entity.Role;
import com.autopro.backend.entity.User;
import com.autopro.backend.repository.PasswordResetTokenRepository;
import com.autopro.backend.repository.RoleRepository;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.security.JwtService;
import com.autopro.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordResetTokenRepository resetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private SignUpRequest buildRequest(String role) {
        SignUpRequest request = new SignUpRequest();
        request.setFirstName("Jean");
        request.setLastName("Dupont");
        request.setEmail("jean@example.com");
        request.setPassword("password123");
        request.setRole(role);
        return request;
    }

    @Test
    void signUp_rejectsSelfAssignedAdminRole() {
        SignUpRequest request = buildRequest("ROLE_ADMIN");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ROLE_ADMIN");

        verify(roleRepository, never()).findByName(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void signUp_rejectsUnknownRole() {
        SignUpRequest request = buildRequest("ROLE_SUPERUSER");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void signUp_allowsMechanicRole() {
        SignUpRequest request = buildRequest("ROLE_MECHANIC");
        Role role = Role.builder().id(2L).name("ROLE_MECHANIC").build();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName("ROLE_MECHANIC")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("token");

        var response = authService.signUp(request);

        assertThat(response.getRole()).isEqualTo("ROLE_MECHANIC");
    }

    @Test
    void requestPasswordReset_doesNotRevealWhetherEmailExists() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("unknown@example.com");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        String result = authService.requestPasswordReset(request);

        assertThat(result).doesNotContainPattern("[0-9a-f]{8}-[0-9a-f]{4}");
        verify(resetTokenRepository, never()).save(any());
    }

    @Test
    void requestPasswordReset_generatesTokenWhenUserExists() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("jean@example.com");
        User user = User.builder().id(1L).email("jean@example.com").build();
        when(userRepository.findByEmail("jean@example.com")).thenReturn(Optional.of(user));
        when(resetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String token = authService.requestPasswordReset(request);

        assertThat(token).isNotBlank();
        verify(resetTokenRepository).save(any(PasswordResetToken.class));
    }
}
