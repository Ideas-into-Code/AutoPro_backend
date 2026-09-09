package com.autopro.backend.service;

import com.autopro.backend.dto.payment.PaymentResponse;
import com.autopro.backend.dto.servicerequest.CreateServiceRequestRequest;
import com.autopro.backend.dto.servicerequest.ServiceRequestResponse;
import com.autopro.backend.dto.servicerequest.UpdateServiceRequestStatusRequest;
import com.autopro.backend.entity.*;
import com.autopro.backend.exception.ResourceNotFoundException;
import com.autopro.backend.repository.MechanicRepository;
import com.autopro.backend.repository.PaymentRepository;
import com.autopro.backend.repository.ServiceRequestRepository;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.repository.VehicleRepository;

import java.math.BigDecimal;
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
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

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
                .problemType(request.getProblemType() != null
                        ? request.getProblemType() : ProblemType.OTHER)
                .contactPhone(request.getContactPhone() != null && !request.getContactPhone().isBlank()
                        ? request.getContactPhone().trim()
                        : client.getPhone())
                .isEmergency(Boolean.TRUE.equals(request.getIsEmergency()))
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(ServiceRequestStatus.PENDING)
                .build();

        serviceRequestRepository.save(serviceRequest);
        return toResponse(serviceRequest);
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

        if (request.getStatus() == ServiceRequestStatus.COMPLETED && sr.getPrice() == null) {
            throw new IllegalArgumentException(
                    "Fixez le prix de l'intervention avant de la marquer comme terminée");
        }

        sr.setStatus(request.getStatus());
        serviceRequestRepository.save(sr);

        // Effets de bord liés au paiement en espèces.
        if (request.getStatus() == ServiceRequestStatus.COMPLETED) {
            paymentService.createPendingForCompletedRequest(sr);
        } else if (request.getStatus() == ServiceRequestStatus.CANCELLED) {
            paymentService.cancelForRequest(sr);
        }

        notifyStatusChange(sr, request.getStatus(), user);

        return toResponse(sr);
    }

    /** Prévient la partie concernée d'un changement de statut de la demande. */
    private void notifyStatusChange(ServiceRequest sr, ServiceRequestStatus to, User actor) {
        User client = sr.getClient();
        User mechanicUser = sr.getMechanic() != null ? sr.getMechanic().getUser() : null;
        // Le lien dépend du destinataire : le client suit sa demande dans l'espace
        // client, le mécanicien dans son propre espace. Un lien unique enverrait
        // le mécanicien dans la coquille cliente (« on se perd dans les rôles »).
        String clientLink = "/demandes/" + sr.getId();
        String mechanicLink = "/mecanicien/demandes/" + sr.getId();

        switch (to) {
            case ACCEPTED -> notificationService.notify(client, NotificationType.REQUEST_ACCEPTED,
                    "Demande acceptée",
                    (mechanicUser != null ? mechanicUser.getFirstName() : "Un mécanicien")
                            + " prend en charge votre demande.", clientLink);
            case IN_PROGRESS -> notificationService.notify(client, NotificationType.REQUEST_IN_PROGRESS,
                    "Intervention démarrée", "Le mécanicien a commencé l'intervention.", clientLink);
            case COMPLETED -> notificationService.notify(client, NotificationType.REQUEST_COMPLETED,
                    "Intervention terminée",
                    "Montant à régler en espèces : "
                            + (sr.getPrice() != null ? sr.getPrice().toPlainString() : "-") + " FCFA.", clientLink);
            case CANCELLED -> {
                // Prévenir l'autre partie que celle qui a annulé.
                if (actor.getId().equals(client.getId())) {
                    notificationService.notify(mechanicUser, NotificationType.REQUEST_CANCELLED,
                            "Demande annulée", "Le client a annulé sa demande.", mechanicLink);
                } else {
                    notificationService.notify(client, NotificationType.REQUEST_CANCELLED,
                            "Demande annulée", "Votre demande a été annulée.", clientLink);
                }
            }
            default -> { /* PENDING : rien */ }
        }
    }

    /**
     * Fixe le prix convenu de l'intervention. Réservé au mécanicien assigné et à
     * l'admin, uniquement tant que la demande est acceptée ou en cours.
     */
    @Transactional
    public ServiceRequestResponse setPrice(String email, Long id, BigDecimal amount) {
        ServiceRequest sr = findByIdOrThrow(id);
        User user = getUser(email);
        String roleName = user.getRole().getName();

        if ("ROLE_MECHANIC".equals(roleName)) {
            Mechanic mechanic = mechanicRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profil mécanicien introuvable"));
            if (sr.getMechanic() == null || !sr.getMechanic().getId().equals(mechanic.getId())) {
                throw new SecurityException("Cette demande ne vous est pas assignée");
            }
        } else if (!"ROLE_ADMIN".equals(roleName)) {
            throw new SecurityException("Seul le mécanicien assigné peut fixer le prix");
        }

        if (sr.getStatus() != ServiceRequestStatus.ACCEPTED
                && sr.getStatus() != ServiceRequestStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Le prix ne peut être fixé que sur une demande acceptée ou en cours");
        }

        sr.setPrice(amount);
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
                .mechanicUserId(sr.getMechanic() != null ? sr.getMechanic().getUser().getId() : null)
                .mechanicName(sr.getMechanic() != null
                        ? sr.getMechanic().getUser().getFirstName() + " " + sr.getMechanic().getUser().getLastName()
                        : null)
                .vehicleId(sr.getVehicle() != null ? sr.getVehicle().getId() : null)
                .vehicleLabel(sr.getVehicle() != null
                        ? sr.getVehicle().getBrand() + " " + sr.getVehicle().getModel()
                        : null)
                .description(sr.getDescription())
                .problemType(sr.getProblemType())
                .contactPhone(sr.getContactPhone())
                .isEmergency(sr.getIsEmergency())
                .status(sr.getStatus())
                .price(sr.getPrice())
                .payment(paymentRepository.findByServiceRequestId(sr.getId())
                        .map(PaymentResponse::from)
                        .orElse(null))
                .address(sr.getAddress())
                .latitude(sr.getLatitude())
                .longitude(sr.getLongitude())
                .createdAt(sr.getCreatedAt())
                .updatedAt(sr.getUpdatedAt())
                .build();
    }
}