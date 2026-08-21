package com.autopro.backend.payment;

import com.autopro.backend.exception.PaymentGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Client pour l'API "Checkout Invoice" de PayDunya (agrégateur de paiement Mobile Money /
 * Orange Money / carte utilisé en Afrique de l'Ouest). Doc : https://paydunya.com/developers
 */
@Component
public class PayDunyaClient {

    private static final Logger log = LoggerFactory.getLogger(PayDunyaClient.class);

    private final RestClient restClient;

    @Value("${paydunya.master-key}")
    private String masterKey;

    @Value("${paydunya.private-key}")
    private String privateKey;

    @Value("${paydunya.public-key}")
    private String publicKey;

    @Value("${paydunya.token}")
    private String token;

    @Value("${paydunya.store-name:AutoPro}")
    private String storeName;

    public PayDunyaClient(@Value("${paydunya.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Crée une facture de paiement (checkout invoice) chez PayDunya et retourne le token
     * de la facture ainsi que l'URL de paiement à présenter au client.
     */
    @SuppressWarnings("unchecked")
    public PayDunyaCheckoutResult createInvoice(BigDecimal amount, String description,
                                                 String callbackUrl, String returnUrl, String cancelUrl) {
        Map<String, Object> body = Map.of(
                "invoice", Map.of(
                        "total_amount", amount.intValue(),
                        "description", description
                ),
                "store", Map.of(
                        "name", storeName
                ),
                "actions", Map.of(
                        "callback_url", callbackUrl,
                        "return_url", returnUrl,
                        "cancel_url", cancelUrl
                )
        );

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri("/checkout-invoice/create")
                    .header("PAYDUNYA-MASTER-KEY", masterKey)
                    .header("PAYDUNYA-PRIVATE-KEY", privateKey)
                    .header("PAYDUNYA-PUBLIC-KEY", publicKey)
                    .header("PAYDUNYA-TOKEN", token)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException ex) {
            log.error("Appel PayDunya createInvoice en échec", ex);
            throw new PaymentGatewayException("Impossible de contacter le prestataire de paiement", ex);
        }

        if (response == null || !"00".equals(String.valueOf(response.get("response_code")))) {
            String reason = response != null ? String.valueOf(response.get("response_text")) : "réponse vide";
            throw new PaymentGatewayException("Échec de création de la facture PayDunya : " + reason);
        }

        String invoiceToken = String.valueOf(response.get("token"));
        String checkoutUrl = "https://paydunya.com/checkout/invoice/" + invoiceToken;
        return new PayDunyaCheckoutResult(invoiceToken, checkoutUrl);
    }

    /**
     * PayDunya envoie dans l'IPN un hash égal à SHA-512(private_key), utilisé comme jeton
     * d'intégrité pour vérifier que la notification vient bien de PayDunya (et pas d'un tiers
     * qui devinerait l'URL du webhook).
     */
    public boolean verifyWebhookHash(String receivedHash) {
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }
        String expected = sha512(privateKey);
        return expected.equalsIgnoreCase(receivedHash);
    }

    private static String sha512(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 non disponible", e);
        }
    }
}
