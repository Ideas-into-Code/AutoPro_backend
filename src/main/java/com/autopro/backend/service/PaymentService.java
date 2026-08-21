package com.autopro.backend.service;

import com.autopro.backend.dto.payment.InitiatePaymentRequest;
import com.autopro.backend.dto.payment.PayDunyaWebhookPayload;
import com.autopro.backend.dto.payment.PaymentResponse;
import com.autopro.backend.entity.Payment;
import com.autopro.backend.entity.PaymentProvider;
import com.autopro.backend.entity.PaymentStatus;
import com.autopro.backend.entity.User;
import com.autopro.backend.exception.ResourceNotFoundException;
import com.autopro.backend.payment.PayDunyaCheckoutResult;
import com.autopro.backend.payment.PayDunyaClient;
import com.autopro.backend.repository.PaymentRepository;
import com.autopro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PayDunyaClient payDunyaClient;

    @Transactional
    public PaymentResponse initiatePayment(String email, InitiatePaymentRequest request,
                                            String callbackUrl, String returnUrl, String cancelUrl) {
        User payer = getUser(email);

        PayDunyaCheckoutResult checkout = payDunyaClient.createInvoice(
                request.getAmount(), request.getDescription(), callbackUrl, returnUrl, cancelUrl);

        Payment payment = Payment.builder()
                .serviceRequestId(request.getServiceRequestId())
                .payer(payer)
                .amount(request.getAmount())
                .provider(PaymentProvider.PAYDUNYA)
                .providerReference(checkout.token())
                .checkoutUrl(checkout.checkoutUrl())
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);
        return toResponse(payment);
    }

    public List<PaymentResponse> getMyPayments(String email) {
        User user = getUser(email);
        return paymentRepository.findByPayerIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PaymentResponse getById(String email, Long id) {
        User user = getUser(email);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable : " + id));
        boolean isOwner = payment.getPayer().getId().equals(user.getId());
        boolean isAdmin = "ROLE_ADMIN".equals(user.getRole().getName());
        if (!isOwner && !isAdmin) {
            throw new SecurityException("Accès non autorisé à ce paiement");
        }
        return toResponse(payment);
    }

    /**
     * Traite la notification (IPN) envoyée par PayDunya. Idempotent : un paiement déjà
     * traité (statut != PENDING) n'est pas re-traité, pour tolérer les renvois du webhook.
     */
    @Transactional
    public void handleWebhook(PayDunyaWebhookPayload payload) {
        if (!payDunyaClient.verifyWebhookHash(payload.getHash())) {
            log.warn("Webhook PayDunya rejeté : hash invalide pour le token {}", payload.getToken());
            throw new SecurityException("Signature du webhook invalide");
        }

        Payment payment = paymentRepository.findByProviderReference(payload.getToken())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun paiement associé au token PayDunya : " + payload.getToken()));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Webhook PayDunya ignoré : paiement {} déjà au statut {}", payment.getId(), payment.getStatus());
            return;
        }

        if ("completed".equalsIgnoreCase(payload.getStatus())) {
            payment.setStatus(PaymentStatus.SUCCESS);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(payload.getResponseText() != null
                    ? payload.getResponseText() : "Paiement refusé par le prestataire");
        }
        paymentRepository.save(payment);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + email));
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .serviceRequestId(payment.getServiceRequestId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .provider(payment.getProvider())
                .status(payment.getStatus())
                .checkoutUrl(payment.getCheckoutUrl())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
