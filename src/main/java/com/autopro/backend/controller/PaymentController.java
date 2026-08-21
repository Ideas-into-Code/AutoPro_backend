package com.autopro.backend.controller;

import com.autopro.backend.dto.payment.InitiatePaymentRequest;
import com.autopro.backend.dto.payment.PayDunyaWebhookPayload;
import com.autopro.backend.dto.payment.PaymentResponse;
import com.autopro.backend.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Paiements", description = "Initiation et suivi des paiements via PayDunya")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Initier un paiement",
               description = "Crée une facture chez PayDunya et retourne l'URL de paiement à ouvrir côté client.")
    public ResponseEntity<PaymentResponse> initiate(
            Authentication authentication,
            @Valid @RequestBody InitiatePaymentRequest request) {
        String callbackUrl = appBaseUrl + "/api/payments/webhook";
        String returnUrl = appBaseUrl + "/payments/success";
        String cancelUrl = appBaseUrl + "/payments/cancelled";
        return ResponseEntity.ok(
                paymentService.initiatePayment(authentication.getName(), request, callbackUrl, returnUrl, cancelUrl));
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lister mes paiements")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(Authentication authentication) {
        return ResponseEntity.ok(paymentService.getMyPayments(authentication.getName()));
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consulter le détail d'un paiement")
    public ResponseEntity<PaymentResponse> getById(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(authentication.getName(), id));
    }

    /**
     * Endpoint public appelé par PayDunya (pas par le client de l'app) : l'authenticité
     * de la notification est vérifiée via le hash PayDunya, pas via JWT (voir SecurityConfig).
     */
    @PostMapping("/webhook")
    @Operation(summary = "Webhook PayDunya (IPN) — appelé par PayDunya, pas par le client")
    public ResponseEntity<Void> webhook(@RequestBody PayDunyaWebhookPayload payload) {
        paymentService.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
