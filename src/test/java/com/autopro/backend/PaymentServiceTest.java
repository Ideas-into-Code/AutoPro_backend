package com.autopro.backend;

import com.autopro.backend.dto.payment.InitiatePaymentRequest;
import com.autopro.backend.dto.payment.PayDunyaWebhookPayload;
import com.autopro.backend.dto.payment.PaymentResponse;
import com.autopro.backend.entity.Payment;
import com.autopro.backend.entity.PaymentProvider;
import com.autopro.backend.entity.PaymentStatus;
import com.autopro.backend.entity.Role;
import com.autopro.backend.entity.User;
import com.autopro.backend.exception.PaymentGatewayException;
import com.autopro.backend.exception.ResourceNotFoundException;
import com.autopro.backend.payment.PayDunyaCheckoutResult;
import com.autopro.backend.payment.PayDunyaClient;
import com.autopro.backend.repository.PaymentRepository;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PayDunyaClient payDunyaClient;

    @InjectMocks
    private PaymentService paymentService;

    private User buildUser(Long id, String roleName) {
        Role role = Role.builder().id(1L).name(roleName).build();
        return User.builder().id(id).firstName("Jean").lastName("Dupont")
                .email("user" + id + "@example.com").role(role).build();
    }

    private Payment buildPayment(Long id, User payer, String reference, PaymentStatus status) {
        return Payment.builder().id(id).payer(payer).amount(new BigDecimal("15000"))
                .currency("XOF").provider(PaymentProvider.PAYDUNYA)
                .providerReference(reference).status(status).build();
    }

    @Test
    void initiatePayment_createsInvoiceAndSavesPendingPayment() {
        User payer = buildUser(1L, "ROLE_CLIENT");
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(payer));
        when(payDunyaClient.createInvoice(any(), any(), any(), any(), any()))
                .thenReturn(new PayDunyaCheckoutResult("tok_123", "https://paydunya.com/checkout/invoice/tok_123"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setAmount(new BigDecimal("15000"));
        request.setDescription("Vidange moteur");

        PaymentResponse response = paymentService.initiatePayment(
                "user1@example.com", request, "cb", "ret", "cancel");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getCheckoutUrl()).isEqualTo("https://paydunya.com/checkout/invoice/tok_123");
        assertThat(response.getProvider()).isEqualTo(PaymentProvider.PAYDUNYA);
    }

    @Test
    void initiatePayment_propagatesGatewayFailure() {
        User payer = buildUser(1L, "ROLE_CLIENT");
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(payer));
        when(payDunyaClient.createInvoice(any(), any(), any(), any(), any()))
                .thenThrow(new PaymentGatewayException("PayDunya indisponible"));

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setAmount(new BigDecimal("5000"));
        request.setDescription("Diagnostic");

        assertThatThrownBy(() -> paymentService.initiatePayment("user1@example.com", request, "cb", "ret", "cancel"))
                .isInstanceOf(PaymentGatewayException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handleWebhook_marksPaymentSuccessOnCompletedStatus() {
        User payer = buildUser(1L, "ROLE_CLIENT");
        Payment payment = buildPayment(100L, payer, "tok_123", PaymentStatus.PENDING);
        when(payDunyaClient.verifyWebhookHash("valid-hash")).thenReturn(true);
        when(paymentRepository.findByProviderReference("tok_123")).thenReturn(Optional.of(payment));

        PayDunyaWebhookPayload payload = new PayDunyaWebhookPayload();
        payload.setToken("tok_123");
        payload.setStatus("completed");
        payload.setHash("valid-hash");

        paymentService.handleWebhook(payload);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository).save(payment);
    }

    @Test
    void handleWebhook_marksPaymentFailedOnNonCompletedStatus() {
        User payer = buildUser(1L, "ROLE_CLIENT");
        Payment payment = buildPayment(100L, payer, "tok_123", PaymentStatus.PENDING);
        when(payDunyaClient.verifyWebhookHash("valid-hash")).thenReturn(true);
        when(paymentRepository.findByProviderReference("tok_123")).thenReturn(Optional.of(payment));

        PayDunyaWebhookPayload payload = new PayDunyaWebhookPayload();
        payload.setToken("tok_123");
        payload.setStatus("cancelled");
        payload.setHash("valid-hash");
        payload.setResponseText("Paiement annulé par l'utilisateur");

        paymentService.handleWebhook(payload);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Paiement annulé par l'utilisateur");
    }

    @Test
    void handleWebhook_rejectsInvalidHash() {
        when(payDunyaClient.verifyWebhookHash("bad-hash")).thenReturn(false);

        PayDunyaWebhookPayload payload = new PayDunyaWebhookPayload();
        payload.setToken("tok_123");
        payload.setStatus("completed");
        payload.setHash("bad-hash");

        assertThatThrownBy(() -> paymentService.handleWebhook(payload))
                .isInstanceOf(SecurityException.class);

        verify(paymentRepository, never()).findByProviderReference(any());
    }

    @Test
    void handleWebhook_throwsWhenNoMatchingPayment() {
        when(payDunyaClient.verifyWebhookHash("valid-hash")).thenReturn(true);
        when(paymentRepository.findByProviderReference("unknown")).thenReturn(Optional.empty());

        PayDunyaWebhookPayload payload = new PayDunyaWebhookPayload();
        payload.setToken("unknown");
        payload.setStatus("completed");
        payload.setHash("valid-hash");

        assertThatThrownBy(() -> paymentService.handleWebhook(payload))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void handleWebhook_isIdempotentForAlreadyProcessedPayment() {
        User payer = buildUser(1L, "ROLE_CLIENT");
        Payment payment = buildPayment(100L, payer, "tok_123", PaymentStatus.SUCCESS);
        when(payDunyaClient.verifyWebhookHash("valid-hash")).thenReturn(true);
        when(paymentRepository.findByProviderReference("tok_123")).thenReturn(Optional.of(payment));

        PayDunyaWebhookPayload payload = new PayDunyaWebhookPayload();
        payload.setToken("tok_123");
        payload.setStatus("completed");
        payload.setHash("valid-hash");

        paymentService.handleWebhook(payload);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void getById_throwsSecurityExceptionWhenNotOwnerAndNotAdmin() {
        User owner = buildUser(1L, "ROLE_CLIENT");
        User other = buildUser(2L, "ROLE_CLIENT");
        Payment payment = buildPayment(100L, owner, "tok_123", PaymentStatus.SUCCESS);
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(other));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getById("user2@example.com", 100L))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void getMyPayments_returnsOnlyOwnPayments() {
        User payer = buildUser(1L, "ROLE_CLIENT");
        Payment payment = buildPayment(100L, payer, "tok_123", PaymentStatus.SUCCESS);
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(payer));
        when(paymentRepository.findByPayerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(payment));

        List<PaymentResponse> result = paymentService.getMyPayments("user1@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
    }
}
