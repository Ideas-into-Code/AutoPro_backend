package com.autopro.backend;

import com.autopro.backend.payment.PayDunyaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PayDunyaClientTest {

    private PayDunyaClient client;

    @BeforeEach
    void setUp() {
        client = new PayDunyaClient("https://app.paydunya.com/sandbox-api/v1");
        ReflectionTestUtils.setField(client, "privateKey", "test-private-key");
    }

    @Test
    void verifyWebhookHash_acceptsCorrectSha512OfPrivateKey() {
        boolean result = client.verifyWebhookHash(computeSha512("test-private-key"));
        assertThat(result).isTrue();
    }

    @Test
    void verifyWebhookHash_rejectsIncorrectHash() {
        assertThat(client.verifyWebhookHash("clairement-pas-le-bon-hash")).isFalse();
    }

    @Test
    void verifyWebhookHash_rejectsNullOrBlankHash() {
        assertThat(client.verifyWebhookHash(null)).isFalse();
        assertThat(client.verifyWebhookHash("")).isFalse();
        assertThat(client.verifyWebhookHash("   ")).isFalse();
    }

    private static String computeSha512(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
