package com.autopro.backend;

import com.autopro.backend.entity.Mechanic;
import com.autopro.backend.entity.Role;
import com.autopro.backend.entity.User;
import com.autopro.backend.entity.Vehicle;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityTest {

    @Test
    void roleEntityBuildsCorrectly() {
        Role role = Role.builder()
                .name("ROLE_ADMIN")
                .description("Administrator")
                .build();

        assertThat(role.getName()).isEqualTo("ROLE_ADMIN");
        assertThat(role.getDescription()).isEqualTo("Administrator");
    }

    @Test
    void userEntityBuildsCorrectly() {
        Role role = Role.builder().name("ROLE_CLIENT").build();

        User user = User.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .email("jean.dupont@example.com")
                .password("hashed_pw")
                .phone("+33600000000")
                .role(role)
                .build();

        assertThat(user.getEmail()).isEqualTo("jean.dupont@example.com");
        assertThat(user.getIsActive()).isTrue();
        assertThat(user.getRole()).isEqualTo(role);
    }

    @Test
    void mechanicEntityBuildsCorrectly() {
        User user = User.builder()
                .firstName("Paul")
                .lastName("Martin")
                .email("paul.martin@example.com")
                .password("hashed_pw")
                .role(Role.builder().name("ROLE_MECHANIC").build())
                .build();

        Mechanic mechanic = Mechanic.builder()
                .user(user)
                .specialization("Électricité auto")
                .experienceYears(5)
                .build();

        assertThat(mechanic.getUser()).isEqualTo(user);
        assertThat(mechanic.getIsAvailable()).isTrue();
        assertThat(mechanic.getSpecialization()).isEqualTo("Électricité auto");
    }

    @Test
    void vehicleEntityBuildsCorrectly() {
        User owner = User.builder()
                .firstName("Alice")
                .lastName("Durand")
                .email("alice@example.com")
                .password("hashed_pw")
                .role(Role.builder().name("ROLE_CLIENT").build())
                .build();

        Vehicle vehicle = Vehicle.builder()
                .owner(owner)
                .brand("Renault")
                .model("Clio")
                .year(2020)
                .licensePlate("AB-123-CD")
                .vin("VF1XXXXXXXX000001")
                .color("Blanc")
                .mileage(15000)
                .build();

        assertThat(vehicle.getBrand()).isEqualTo("Renault");
        assertThat(vehicle.getLicensePlate()).isEqualTo("AB-123-CD");
        assertThat(vehicle.getOwner()).isEqualTo(owner);
    }
}
