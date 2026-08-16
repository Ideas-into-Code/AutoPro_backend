package com.autopro.backend.service;

import com.autopro.backend.dto.admin.AdminStatsDTO;
import com.autopro.backend.dto.admin.MechanicDetailDTO;
import com.autopro.backend.dto.admin.UserDetailDTO;
import com.autopro.backend.entity.Mechanic;
import com.autopro.backend.entity.ValidationStatus;
import com.autopro.backend.repository.MechanicRepository;
import com.autopro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final MechanicRepository mechanicRepository;

    public List<UserDetailDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDetailDTO::from)
                .collect(Collectors.toList());
    }

    public List<MechanicDetailDTO> getAllMechanics() {
        return mechanicRepository.findAll().stream()
                .map(MechanicDetailDTO::from)
                .collect(Collectors.toList());
    }

    public List<MechanicDetailDTO> getMechanicsByStatus(ValidationStatus status) {
        return mechanicRepository.findByValidationStatus(status).stream()
                .map(MechanicDetailDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MechanicDetailDTO validateMechanic(Long mechanicId, boolean approved) {
        Mechanic mechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new IllegalArgumentException("Mécanicien introuvable : " + mechanicId));
        mechanic.setValidationStatus(approved ? ValidationStatus.APPROVED : ValidationStatus.REJECTED);
        mechanicRepository.save(mechanic);
        return MechanicDetailDTO.from(mechanic);
    }

    public AdminStatsDTO getSystemStats() {
        long totalUsers = userRepository.count();
        long totalMechanics = mechanicRepository.count();
        long pending = mechanicRepository.countByValidationStatus(ValidationStatus.PENDING);
        long approved = mechanicRepository.countByValidationStatus(ValidationStatus.APPROVED);
        long rejected = mechanicRepository.countByValidationStatus(ValidationStatus.REJECTED);
        long activeUsers = userRepository.countByIsActive(true);
        long inactiveUsers = userRepository.countByIsActive(false);

        return AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalMechanics(totalMechanics)
                .pendingMechanics(pending)
                .approvedMechanics(approved)
                .rejectedMechanics(rejected)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .build();
    }
}
