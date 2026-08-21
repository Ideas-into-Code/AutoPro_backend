package com.autopro.backend.dto.payment;

import lombok.Data;

/**
 * Notification IPN envoyée par PayDunya à la confirmation (ou l'échec) d'un paiement.
 * NOTE : la charge utile réelle de PayDunya est form-urlencoded avec des clés imbriquées
 * (data[token], data[status], data[hash]...). Ce DTO couvre les champs utilisés par
 * PaymentService ; à ajuster une fois testé contre le vrai sandbox PayDunya.
 */
@Data
public class PayDunyaWebhookPayload {

    private String token;

    /** "completed" en cas de succès chez PayDunya, autre chose sinon. */
    private String status;

    /** SHA-512(private_key) envoyé par PayDunya pour authentifier la notification. */
    private String hash;

    private String responseText;
}
