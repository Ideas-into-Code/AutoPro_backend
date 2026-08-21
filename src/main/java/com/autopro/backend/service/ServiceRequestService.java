package com.autopro.backend.service;

import com.autopro.backend.dto.servicerequest.CreateServiceRequestRequest;
import com.autopro.backend.dto.servicerequest.ServiceRequestResponse;
import com.autopro.backend.dto.servicerequest.UpdateServiceRequestStatusRequest;
import com.autopro.backend.entity.*;
import com.autopro.backend.exception.ResourceNotFoundException;
import com.autopro.backend.repository.MechanicRepository;
import com.autopro.backend.repository.ServiceRequestRepository;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private static final Map<ServiceRequestStatus, Set<ServiceRequestStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(ServiceRequestStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(ServiceRequestStatus.PENDING,
                EnumSet.of(ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(ServiceRequestStatus.ACCEPTED,
                EnumSet.of(ServiceRequestStatus.IN_PROGRESS, ServiceRequestStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(ServiceRequestStatus.IN_PROGRESS,
                EnumSet.of(ServiceRequestStatus.COMPLETED, ServiceRequestStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(ServiceRequestStatus.COMPLETED, EnumSet.noneOf(ServiceRequestStatus.class));
        ALLOWED_TRANSITIONS.put(ServiceRequestStatus.CANCELLED, EnumSet.noneOf(ServiceRequestStatus.class));
    }

    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final MechanicRepository mechanicRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public ServiceRequestResponse create(String email, CreateServiceRequestRequest request) {
        User client = getUser(email);

        Vehicle vehicle = null;
        if (request.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable: " + request.getVehicleId()));
            if (!vehicle.getOwner().getId().equals(client.getId())) {
                throw new SecurityException("Ce véhicule ne vous appartient pas");
            }
        }

        ServiceRequest serviceRequest = ServiceRequest.builder()
                .client(client)
                .vehicle(vehicle)
                .description(request.getDescription())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(ServiceRequestStatus.PENDING)
                .build();

        serviceRequestRepository.save(serviceRequest);
        return toResponse(serviceRequest);
    }

    public List<ServiceRequestResponse> getMyRequests(String email) {
        User user = getUser(email);
        String roleName = user.getRole().getName();

        List<ServiceRequest> requests;
        if ("ROLE_ADMIN".equals(roleName)) {
            requests = serviceRequestRepository.findAll();
        } else if ("ROLE_MECHANIC".equals(roleName)) {
            Long mechanicId = mechanicRepository.findByUserId(user.getId())
                    .map(Mechanic::getId)
                    .orElse(null);

            Map<Long, ServiceRequest> merged = new LinkedHashMap<>();
            if (mechanicId != null) {
                for (ServiceRequest sr : serviceRequestRepository.findByMechanicIdOrderByCreatedAtDesc(mechanicId)) {
                    merged.put(sr.getId(), sr);
                }
            }
            for (ServiceRequest sr : serviceRequestRepository
                    .findByStatusOrderByCreatedAtDesc(ServiceRequestStatus.PENDING)) {
                merged.put(sr.getId(), sr);
            }
            requests = new ArrayList<>(merged.values());
        } else {
            requests = serviceRequestRepository.findByClientIdOrderByCreatedAtDesc(user.getId());
        }

        return requests.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ServiceRequestResponse getById(String email, Long id) {
        ServiceRequest sr = findByIdOrThrow(id);
        checkReadAccess(email, sr);
        return toResponse(sr);
    }

    @Transactional
    public ServiceRequestResponse updateStatus(String email, Long id, UpdateServiceRequestStatusRequest request) {
        ServiceRequest sr = findByIdOrThrow(id);
        User user = getUser(email);
        String roleName = user.getRole().getName();

        if ("ROLE_CLIENT".equals(roleName)) {
            boolean isOwner = sr.getClient().getId().equals(user.getId());
            if (!isOwner) {
                throw new SecurityException("Action non autorisée pour ce client");
            }
            boolean cancelling = request.getStatus() == ServiceRequestStatus.CANCELLED;
            if (!cancelling) {
                throw new IllegalArgumentException("Un client ne peut qu'annuler sa demande");
            }
        } else if ("ROLE_MECHANIC".equals(roleName)) {
            Mechanic mechanic = mechanicRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profil mécanicien introuvable"));
            if (sr.getMechanic() == null) {
                sr.setMechanic(mechanic); // le mécanicien prend en charge la demande
            } else if (!sr.getMechanic().getId().equals(mechanic.getId())) {
                throw new SecurityException("Cette demande est assignée à un autre mécanicien");
            }
        }
        // ROLE_ADMIN : autorisé sans restriction

        validateTransition(sr.getStatus(), request.getStatus());
        sr.setStatus(request.getStatus());
        serviceRequestRepository.save(sr);
        return toResponse(sr);
    }

    private void validateTransition(ServiceRequestStatus from, ServiceRequestStatus to) {
        if (from == to) {
            return;
        }
        Set<ServiceRequestStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalArgumentException("Transition de statut invalide : " + from + " -> " + to);
        }
    }

    private void checkReadAccess(String email, ServiceRequest sr) {
        User user = getUser(email);
        String roleName = user.getRole().getName();
        if ("ROLE_ADMIN".equals(roleName))
            return;
        if ("ROLE_CLIENT".equals(roleName) && sr.getClient().getId().equals(user.getId()))
            return;
        if ("ROLE_MECHANIC".equals(roleName)) {
            boolean isAssigned = sr.getMechanic() != null
                    && mechanicRepository.findByUserId(user.getId())
                            .map(m -> m.getId().equals(sr.getMechanic().getId()))
                            .orElse(false);
            boolean isPending = sr.getStatus() == ServiceRequestStatus.PENDING;
            if (isAssigned || isPending)
                return;
        }
        throw new SecurityException("Accès non autorisé à cette demande");
    }

    private ServiceRequest findByIdOrThrow(Long id) {
        return serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de service introuvable: " + id));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + email));
    }

    private ServiceRequestResponse toResponse(ServiceRequest sr) {
        return ServiceRequestResponse.builder()
                .id(sr.getId())
                .clientId(sr.getClient().getId())
                .clientName(sr.getClient().getFirstName() + " " + sr.getClient().getLastName())
                .mechanicId(sr.getMechanic() != null ? sr.getMechanic().getId() : null)
                .mechanicName(sr.getMechanic() != null
                        ? sr.getMechanic().getUser().getFirstName() + " " + sr.getMechanic().getUser().getLastName()
                        : null)
                .vehicleId(sr.getVehicle() != null ? sr.getVehicle().getId() : null)
                .vehicleLabel(sr.getVehicle() != null
                        ? sr.getVehicle().getBrand() + " " + sr.getVehicle().getModel()
                        : null)
                .description(sr.getDescription())
                .status(sr.getStatus())
                .address(sr.getAddress())
                .latitude(sr.getLatitude())
                .longitude(sr.getLongitude())
                .createdAt(sr.getCreatedAt())
                .updatedAt(sr.getUpdatedAt())
                .build();
    }
}