package com.autopro.backend;

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
import com.autopro.backend.service.PaymentService;
import com.autopro.backend.service.ServiceRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MechanicRepository mechanicRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private ServiceRequestService serviceRequestService;

    @BeforeEach
    void setUp() {
        lenient().when(paymentRepository.findByServiceRequestId(any())).thenReturn(Optional.empty());
    }

    private User buildUser(Long id, String roleName) {
        Role role = Role.builder().id(1L).name(roleName).build();
        return User.builder().id(id).firstName("Jean").lastName("Dupont")
                .email("user" + id + "@example.com").role(role).build();
    }

    private Mechanic buildMechanic(Long id, User user) {
        return Mechanic.builder().id(id).user(user).build();
    }

    private ServiceRequest buildRequest(Long id, User client, Mechanic mechanic, ServiceRequestStatus status) {
        return ServiceRequest.builder().id(id).client(client).mechanic(mechanic)
                .description("Vidange").status(status).build();
    }

    @Test
    void create_savesRequestWithoutVehicle() {
        User client = buildUser(1L, "ROLE_CLIENT");
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(client));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateServiceRequestRequest request = new CreateServiceRequestRequest();
        request.setDescription("Vidange");

        ServiceRequestResponse response = serviceRequestService.create("user1@example.com", request);

        assertThat(response.getStatus()).isEqualTo(ServiceRequestStatus.PENDING);
        assertThat(response.getClientId()).isEqualTo(1L);
        assertThat(response.getProblemType()).isEqualTo(ProblemType.OTHER);
        assertThat(response.getIsEmergency()).isFalse();
    }

    @Test
    void create_keepsProblemTypeContactPhoneAndEmergencyFlag() {
        User client = buildUser(1L, "ROLE_CLIENT");
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(client));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateServiceRequestRequest request = new CreateServiceRequestRequest();
        request.setDescription("Batterie à plat depuis ce matin");
        request.setProblemType(ProblemType.BATTERY);
        request.setContactPhone("770001122");
        request.setIsEmergency(true);

        ServiceRequestResponse response = serviceRequestService.create("user1@example.com", request);

        assertThat(response.getProblemType()).isEqualTo(ProblemType.BATTERY);
        assertThat(response.getContactPhone()).isEqualTo("770001122");
        assertThat(response.getIsEmergency()).isTrue();
    }

    @Test
    void create_throwsResourceNotFoundWhenVehicleMissing() {
        User client = buildUser(1L, "ROLE_CLIENT");
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(client));
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        CreateServiceRequestRequest request = new CreateServiceRequestRequest();
        request.setDescription("Vidange");
        request.setVehicleId(99L);

        assertThatThrownBy(() -> serviceRequestService.create("user1@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_throwsSecurityExceptionWhenVehicleNotOwnedByClient() {
        User client = buildUser(1L, "ROLE_CLIENT");
        User otherOwner = buildUser(2L, "ROLE_CLIENT");
        Vehicle vehicle = Vehicle.builder().id(5L).owner(otherOwner).brand("Toyota").model("Yaris").build();
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(client));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));

        CreateServiceRequestRequest request = new CreateServiceRequestRequest();
        request.setDescription("Vidange");
        request.setVehicleId(5L);

        assertThatThrownBy(() -> serviceRequestService.create("user1@example.com", request))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void updateStatus_clientCanCancelOwnRequest() {
        User client = buildUser(1L, "ROLE_CLIENT");
        ServiceRequest sr = buildRequest(100L, client, null, ServiceRequestStatus.PENDING);
        when(serviceRequestRepository.findById(100L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(client));
        when(serviceRequestRepository.save(sr)).thenReturn(sr);

        UpdateServiceRequestStatusRequest request = new UpdateServiceRequestStatusRequest();
        request.setStatus(ServiceRequestStatus.CANCELLED);

        ServiceRequestResponse response = serviceRequestService.updateStatus("user1@example.com", 100L, request);

        assertThat(response.getStatus()).isEqualTo(ServiceRequestStatus.CANCELLED);
    }

    @Test
    void updateStatus_clientCannotAcceptOwnRequest() {
        User client = buildUser(1L, "ROLE_CLIENT");
        ServiceRequest sr = buildRequest(100L, client, null, ServiceRequestStatus.PENDING);
        when(serviceRequestRepository.findById(100L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(client));

        UpdateServiceRequestStatusRequest request = new UpdateServiceRequestStatusRequest();
        request.setStatus(ServiceRequestStatus.ACCEPTED);

        assertThatThrownBy(() -> serviceRequestService.updateStatus("user1@example.com", 100L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateStatus_clientCannotCancelSomeoneElsesRequest() {
        User owner = buildUser(1L, "ROLE_CLIENT");
        User other = buildUser(2L, "ROLE_CLIENT");
        ServiceRequest sr = buildRequest(100L, owner, null, ServiceRequestStatus.PENDING);
        when(serviceRequestRepository.findById(100L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(other));

        UpdateServiceRequestStatusRequest request = new UpdateServiceRequestStatusRequest();
        request.setStatus(ServiceRequestStatus.CANCELLED);

        assertThatThrownBy(() -> serviceRequestService.updateStatus("user2@example.com", 100L, request))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void updateStatus_mechanicClaimsUnassignedRequest() {
        User client = buildUser(1L, "ROLE_CLIENT");
        User mechanicUser = buildUser(2L, "ROLE_MECHANIC");
        Mechanic mechanic = buildMechanic(10L, mechanicUser);
        ServiceRequest sr = buildRequest(100L, client, null, ServiceRequestStatus.PENDING);

        when(serviceRequestRepository.findById(100L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(mechanicUser));
        when(mechanicRepository.findByUserId(2L)).thenReturn(Optional.of(mechanic));
        when(serviceRequestRepository.save(sr)).thenReturn(sr);

        UpdateServiceRequestStatusRequest request = new UpdateServiceRequestStatusRequest();
        request.setStatus(ServiceRequestStatus.ACCEPTED);

        ServiceRequestResponse response = serviceRequestService.updateStatus("user2@example.com", 100L, request);

        assertThat(response.getMechanicId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(ServiceRequestStatus.ACCEPTED);
    }

    @Test
    void updateStatus_mechanicCannotTouchRequestAssignedToAnother() {
        User client = buildUser(1L, "ROLE_CLIENT");
        User mechanicUser1 = buildUser(2L, "ROLE_MECHANIC");
        User mechanicUser2 = buildUser(3L, "ROLE_MECHANIC");
        Mechanic mechanic1 = buildMechanic(10L, mechanicUser1);
        Mechanic mechanic2 = buildMechanic(11L, mechanicUser2);
        ServiceRequest sr = buildRequest(100L, client, mechanic1, ServiceRequestStatus.ACCEPTED);

        when(serviceRequestRepository.findById(100L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("user3@example.com")).thenReturn(Optional.of(mechanicUser2));
        when(mechanicRepository.findByUserId(3L)).thenReturn(Optional.of(mechanic2));

        UpdateServiceRequestStatusRequest request = new UpdateServiceRequestStatusRequest();
        request.setStatus(ServiceRequestStatus.IN_PROGRESS);

        assertThatThrownBy(() -> serviceRequestService.updateStatus("user3@example.com", 100L, request))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void updateStatus_rejectsInvalidTransition() {
        User client = buildUser(1L, "ROLE_CLIENT");
        User adminUser = buildUser(9L, "ROLE_ADMIN");
        ServiceRequest sr = buildRequest(100L, client, null, ServiceRequestStatus.COMPLETED);

        when(serviceRequestRepository.findById(100L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("user9@example.com")).thenReturn(Optional.of(adminUser));

        UpdateServiceRequestStatusRequest request = new UpdateServiceRequestStatusRequest();
        request.setStatus(ServiceRequestStatus.PENDING);

        assertThatThrownBy(() -> serviceRequestService.updateStatus("user9@example.com", 100L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transition de statut invalide");

        verify(serviceRequestRepository, never()).save(any());
    }

    @Test
    void updateStatus_adminBypassesOwnershipChecks() {
        User client = buildUser(1L, "ROLE_CLIENT");
        User adminUser = buildUser(9L, "ROLE_ADMIN");
        ServiceRequest sr = buildRequest(100L, client, null, ServiceRequestStatus.PENDING);

        when(serviceRequestRepository.findById(100L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("user9@example.com")).thenReturn(Optional.of(adminUser));
        when(serviceRequestRepository.save(sr)).thenReturn(sr);

        UpdateServiceRequestStatusRequest request = new UpdateServiceRequestStatusRequest();
        request.setStatus(ServiceRequestStatus.ACCEPTED);

        ServiceRequestResponse response = serviceRequestService.updateStatus("user9@example.com", 100L, request);

        assertThat(response.getStatus()).isEqualTo(ServiceRequestStatus.ACCEPTED);
    }

    @Test
    void setPrice_assignedMechanicSetsPriceOnAcceptedRequest() {
        User mechanicUser = buildUser(2L, "ROLE_MECHANIC");
        Mechanic mechanic = buildMechanic(10L, mechanicUser);
        ServiceRequest sr = buildRequest(100L, buildUser(1L, "ROLE_CLIENT"), mechanic, ServiceRequestStatus.ACCEPTED);
        when(serviceRequestRepository.findById(100L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(mechanicUser));
        when(mechanicRepository.findByUserId(2L)).thenReturn(Optional.of(mechanic));
        when(serviceRequestRepository.save(sr)).thenReturn(sr);

        ServiceRequestResponse response =
                serviceRequestService.setPrice("user2@example.com", 100L, new java.math.BigDecimal("15000"));

        assertThat(response.getPrice()).isEqualByComparingTo("15000");
    }

    @Test
    void updateStatus_cannotCompleteWithoutPrice() {
        User mechanicUser = buildUser(2L, "ROLE_MECHANIC");
        Mechanic mechanic = buildMechanic(10L, mechanicUser);
        ServiceRequest sr = buildRequest(100L, buildUser(1L, "ROLE_CLIENT"), mechanic, ServiceRequestStatus.IN_PROGRESS);
        when(serviceRequestRepository.findById(100L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(mechanicUser));
        when(mechanicRepository.findByUserId(2L)).thenReturn(Optional.of(mechanic));

        UpdateServiceRequestStatusRequest request = new UpdateServiceRequestStatusRequest();
        request.setStatus(ServiceRequestStatus.COMPLETED);

        assertThatThrownBy(() -> serviceRequestService.updateStatus("user2@example.com", 100L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prix");
    }

    @Test
    void updateStatus_completingWithPriceTriggersPaymentCreation() {
        User mechanicUser = buildUser(2L, "ROLE_MECHANIC");
        Mechanic mechanic = buildMechanic(10L, mechanicUser);
        ServiceRequest sr = buildRequest(100L, buildUser(1L, "ROLE_CLIENT"), mechanic, ServiceRequestStatus.IN_PROGRESS);
        sr.setPrice(new java.math.BigDecimal("15000"));
        when(serviceRequestRepository.findById(100L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(mechanicUser));
        when(mechanicRepository.findByUserId(2L)).thenReturn(Optional.of(mechanic));
        when(serviceRequestRepository.save(sr)).thenReturn(sr);

        UpdateServiceRequestStatusRequest request = new UpdateServiceRequestStatusRequest();
        request.setStatus(ServiceRequestStatus.COMPLETED);

        ServiceRequestResponse response = serviceRequestService.updateStatus("user2@example.com", 100L, request);

        assertThat(response.getStatus()).isEqualTo(ServiceRequestStatus.COMPLETED);
        verify(paymentService).createPendingForCompletedRequest(sr);
    }

    @Test
    void getById_throwsResourceNotFoundWhenMissing() {
        when(serviceRequestRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceRequestService.getById("user1@example.com", 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_throwsSecurityExceptionWhenClientNotOwnerAndNotPending() {
        User owner = buildUser(1L, "ROLE_CLIENT");
        User other = buildUser(2L, "ROLE_CLIENT");
        ServiceRequest sr = buildRequest(100L, owner, null, ServiceRequestStatus.COMPLETED);

        when(serviceRequestRepository.findById(100L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> serviceRequestService.getById("user2@example.com", 100L))
                .isInstanceOf(SecurityException.class);
    }
}
