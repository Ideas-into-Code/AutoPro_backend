package com.autopro.backend.dto.admin;

import com.autopro.backend.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserDetailDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Boolean isActive;
    private String role;
    private LocalDateTime createdAt;

    public static UserDetailDTO from(User user) {
        return UserDetailDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .isActive(user.getIsActive())
                .role(user.getRole().getName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
