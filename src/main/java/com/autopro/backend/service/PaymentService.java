package com.autopro.backend.service;

import com.autopro.backend.dto.payment.PaymentResponse;
import com.autopro.backend.entity.*;
import com.autopro.backend.exception.ResourceNotFoundException;
import com.autopro.backend.repository.InterventionRepository;
import com.autopro.backend.repository.MechanicRepository;
import com.autopro.backend.repository.PaymentRepository;
import com.autopro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Gestion des paiements des demandes de service.
 *
 * <p>Aujourd'hui, tous les paiements sont en espèces : un enregistrement
 * {@link Payment} est créé automatiquement (statut {@code PENDING}) quand une
 * demande passe à {@code COMPLETED}, puis le mécanicien confirme l'encaissement
 * via {@link #collect}. Aucune passerelle externe n'est appelée.</p>
 *
 * <p>Pour ajouter un moyen de paiement en ligne plus tard : introduire une
 * constante dans {@link PaymentMethod}, un {@code PaymentGateway} dédié, et
 * router {@code collect}/la création selon la méthode. Le modèle, les endpoints
 * et le frontend actuels restent inchangés.</p>
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final MechanicRepository mechanicRepository;
    private final InterventionRepository interventionRepository;

    /**
     * Crée le paiement en attente d'une demande qui vient d'être terminée.
     * Idempotent : ne fait rien si un paiement existe déjà.
     *
     * @throws IllegalStateException si le prix n'a pas été fixé
     */
    @Transactional
    public void createPendingForCompletedRequest(ServiceRequest request) {
        if (paymentRepository.findByServiceRequestId(request.getId()).isPresent()) {
            return;
        }
        if (request.getPrice() == null) {
            throw new IllegalStateException(
                    "Le prix doit être fixé avant de terminer l'intervention");
        }
        paymentRepository.save(Payment.builder()
                .serviceRequest(request)
                .amount(request.getPrice())
                .currency("XOF")
                .method(PaymentMethod.CASH)
                .status(PaymentStatus.PENDING)
                .build());
    }

    /** Annule le paiement en attente d'une demande annulée. */
    @Transactional
    public void cancelForRequest(ServiceRequest request) {
        paymentRepository.findByServiceRequestId(request.getId()).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(payment);
            }
        });
    }

    /**
     * Le mécanicien assigné (ou un admin) confirme avoir reçu les espèces.
     * Enregistre en même temps une {@link Intervention} pour que les gains du
     * mécanicien reflètent le travail payé.
     */
    @Transactional
    public PaymentResponse collect(String email, Long serviceRequestId, String notes) {
        Payment payment = paymentRepository.findByServiceRequestId(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun paiement pour la demande " + serviceRequestId));

        ServiceRequest request = payment.getServiceRequest();
        User user = getUser(email);
        boolean isAdmin = "ROLE_ADMIN".equals(user.getRole().getName());
        boolean isAssignedMechanic = request.getMechanic() != null
                && mechanicRepository.findByUserId(user.getId())
                        .map(m -> m.getId().equals(request.getMechanic().getId()))
                        .orElse(false);
        if (!isAdmin && !isAssignedMechanic) {
            throw new SecurityException("Seul le mécanicien assigné peut confirmer l'encaissement");
        }

        if (payment.getStatus() == PaymentStatus.COLLECTED) {
            throw new IllegalArgumentException("Ce paiement a déjà été encaissé");
        }
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new IllegalArgumentException("Ce paiement a été annulé");
        }

        payment.setStatus(PaymentStatus.COLLECTED);
        payment.setCollectedAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) {
            payment.setNotes(notes.trim());
        }
        paymentRepository.save(payment);

        if (request.getMechanic() != null) {
            interventionRepository.save(Intervention.builder()
                    .mechanic(request.getMechanic())
                    .description("Paiement espèces - demande #" + request.getId())
                    .amount(payment.getAmount())
                    .status(InterventionStatus.COMPLETED)
                    .interventionDate(payment.getCollectedAt())
                    .build());
        }

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getForRequest(String email, Long serviceRequestId) {
        Payment payment = paymentRepository.findByServiceRequestId(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun paiement pour la demande " + serviceRequestId));
        assertReadAccess(email, payment.getServiceRequest());
        return PaymentResponse.from(payment);
    }

    private void assertReadAccess(String email, ServiceRequest request) {
        User user = getUser(email);
        String role = user.getRole().getName();
        if ("ROLE_ADMIN".equals(role)) {
            return;
        }
        if ("ROLE_CLIENT".equals(role) && request.getClient().getId().equals(user.getId())) {
            return;
        }
        if ("ROLE_MECHANIC".equals(role) && request.getMechanic() != null
                && mechanicRepository.findByUserId(user.getId())
                        .map(m -> m.getId().equals(request.getMechanic().getId()))
                        .orElse(false)) {
            return;
        }
        throw new SecurityException("Accès non autorisé à ce paiement");
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + email));
    }
}
