package com.autopro.backend.exception;

/** Echec de communication avec le prestataire de paiement (PayDunya) : reseau, timeout, reponse invalide. */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
