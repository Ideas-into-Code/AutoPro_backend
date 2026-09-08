package com.autopro.backend;

import com.autopro.backend.dto.payment.PaymentResponse;
import com.autopro.backend.entity.*;
import com.autopro.backend.exception.ResourceNotFoundException;
import com.autopro.backend.repository.InterventionRepository;
import com.autopro.backend.repository.MechanicRepository;
import com.autopro.backend.repository.PaymentRepository;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private MechanicRepository mechanicRepository;
    @Mock private InterventionRepository interventionRepository;
    @Mock private com.autopro.backend.service.NotificationService notificationService;

    @InjectMocks private PaymentService paymentService;

    private User user(Long id, String role) {
        return User.builder().id(id).firstName("A").lastName("B")
                .email("user" + id + "@example.com")
                .role(Role.builder().id(1L).name(role).build()).build();
    }

    private ServiceRequest request(Long id, User client, Mechanic mechanic, BigDecimal price) {
        return ServiceRequest.builder().id(id).client(client).mechanic(mechanic)
                .description("Vidange").status(ServiceRequestStatus.COMPLETED).price(price).build();
    }

    @Test
    void createPending_createsCashPaymentWithRequestPrice() {
        ServiceRequest sr = request(1L, user(1L, "ROLE_CLIENT"), null, new BigDecimal("15000.00"));
        when(paymentRepository.findByServiceRequestId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.createPendingForCompletedRequest(sr);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getAmount()).isEqualByComparingTo("15000.00");
        assertThat(saved.getCurrency()).isEqualTo("XOF");
    }

    @Test
    void createPending_isIdempotent() {
        ServiceRequest sr = request(1L, user(1L, "ROLE_CLIENT"), null, new BigDecimal("15000"));
        when(paymentRepository.findByServiceRequestId(1L))
                .thenReturn(Optional.of(Payment.builder().serviceRequest(sr).build()));

        paymentService.createPendingForCompletedRequest(sr);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPending_throwsWhenPriceMissing() {
        ServiceRequest sr = request(1L, user(1L, "ROLE_CLIENT"), null, null);
        when(paymentRepository.findByServiceRequestId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPendingForCompletedRequest(sr))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void collect_marksCollectedAndRecordsInterventionForEarnings() {
        User mechUser = user(2L, "ROLE_MECHANIC");
        Mechanic mechanic = Mechanic.builder().id(10L).user(mechUser).build();
        ServiceRequest sr = request(1L, user(1L, "ROLE_CLIENT"), mechanic, new BigDecimal("15000.00"));
        Payment payment = Payment.builder().id(5L).serviceRequest(sr).amount(new BigDecimal("15000.00"))
                .method(PaymentMethod.CASH).status(PaymentStatus.PENDING).currency("XOF").build();

        when(paymentRepository.findByServiceRequestId(1L)).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(mechUser));
        when(mechanicRepository.findByUserId(2L)).thenReturn(Optional.of(mechanic));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.collect("user2@example.com", 1L, "payé");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COLLECTED);
        assertThat(response.getCollectedAt()).isNotNull();

        ArgumentCaptor<Intervention> captor = ArgumentCaptor.forClass(Intervention.class);
        verify(interventionRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("15000.00");
        assertThat(captor.getValue().getStatus()).isEqualTo(InterventionStatus.COMPLETED);
        assertThat(captor.getValue().getMechanic().getId()).isEqualTo(10L);
    }

    @Test
    void collect_rejectsMechanicNotAssigned() {
        User mechUser = user(3L, "ROLE_MECHANIC");
        Mechanic assigned = Mechanic.builder().id(10L).user(user(2L, "ROLE_MECHANIC")).build();
        Mechanic other = Mechanic.builder().id(11L).user(mechUser).build();
        ServiceRequest sr = request(1L, user(1L, "ROLE_CLIENT"), assigned, new BigDecimal("15000"));
        Payment payment = Payment.builder().id(5L).serviceRequest(sr).amount(new BigDecimal("15000"))
                .status(PaymentStatus.PENDING).build();

        when(paymentRepository.findByServiceRequestId(1L)).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("user3@example.com")).thenReturn(Optional.of(mechUser));
        when(mechanicRepository.findByUserId(3L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> paymentService.collect("user3@example.com", 1L, null))
                .isInstanceOf(SecurityException.class);
        verify(interventionRepository, never()).save(any());
    }

    @Test
    void collect_rejectsAlreadyCollectedPayment() {
        User mechUser = user(2L, "ROLE_MECHANIC");
        Mechanic mechanic = Mechanic.builder().id(10L).user(mechUser).build();
        ServiceRequest sr = request(1L, user(1L, "ROLE_CLIENT"), mechanic, new BigDecimal("15000"));
        Payment payment = Payment.builder().id(5L).serviceRequest(sr).amount(new BigDecimal("15000"))
                .status(PaymentStatus.COLLECTED).build();

        when(paymentRepository.findByServiceRequestId(1L)).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(mechUser));
        when(mechanicRepository.findByUserId(2L)).thenReturn(Optional.of(mechanic));

        assertThatThrownBy(() -> paymentService.collect("user2@example.com", 1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void collect_throwsWhenNoPayment() {
        when(paymentRepository.findByServiceRequestId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.collect("user2@example.com", 1L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelForRequest_cancelsPendingPayment() {
        ServiceRequest sr = request(1L, user(1L, "ROLE_CLIENT"), null, new BigDecimal("15000"));
        Payment payment = Payment.builder().id(5L).serviceRequest(sr).status(PaymentStatus.PENDING).build();
        when(paymentRepository.findByServiceRequestId(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.cancelForRequest(sr);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void getForRequest_deniesUnrelatedClient() {
        User owner = user(1L, "ROLE_CLIENT");
        User stranger = user(9L, "ROLE_CLIENT");
        ServiceRequest sr = request(1L, owner, null, new BigDecimal("15000"));
        Payment payment = Payment.builder().id(5L).serviceRequest(sr).status(PaymentStatus.PENDING).build();
        when(paymentRepository.findByServiceRequestId(1L)).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("user9@example.com")).thenReturn(Optional.of(stranger));

        assertThatThrownBy(() -> paymentService.getForRequest("user9@example.com", 1L))
                .isInstanceOf(SecurityException.class);
    }
}
