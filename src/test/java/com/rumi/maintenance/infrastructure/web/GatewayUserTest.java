package com.rumi.maintenance.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayUserTest {

    private static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");
    private static final UUID OTHER_BUILDING_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");

    @Test
    void readsTheCommaSeparatedBuildingsOfTheToken() {
        GatewayUser user = GatewayUser.from("RESIDENT", " " + BUILDING_ID + " ," + OTHER_BUILDING_ID + ",");

        assertThat(user.buildingIds()).isEqualTo(Set.of(BUILDING_ID, OTHER_BUILDING_ID));
    }

    @Test
    void aResidentOnlyReportsDamageInTheBuildingsOfTheToken() {
        GatewayUser user = GatewayUser.from("RESIDENT", BUILDING_ID.toString());

        user.requireAccessTo(BUILDING_ID);
        assertThatThrownBy(() -> user.requireAccessTo(OTHER_BUILDING_ID))
                .isInstanceOf(MaintenanceAccessDeniedException.class)
                .hasMessage("You cannot report damage in building " + OTHER_BUILDING_ID
                        + ": it is not one of your buildings");
        assertThatThrownBy(() -> GatewayUser.from("RESIDENT", null).requireAccessTo(BUILDING_ID))
                .isInstanceOf(MaintenanceAccessDeniedException.class);
    }

    @Test
    void onlyAResidentIsKeptOutOfTheAdministratorTasks() {
        assertThatThrownBy(() -> GatewayUser.fromRole("RESIDENT").requireAdministrator("manage inspections"))
                .isInstanceOf(MaintenanceAccessDeniedException.class)
                .hasMessage("Only the building administrator can manage inspections");
        GatewayUser.fromRole("ADMINISTRATOR").requireAdministrator("manage inspections");
        GatewayUser.fromRole(null).requireAdministrator("manage inspections");
    }

    @Test
    void administratorsAndRequestsWithoutGatewayHeadersReachAnyBuilding() {
        GatewayUser.from("ADMINISTRATOR", OTHER_BUILDING_ID.toString()).requireAccessTo(BUILDING_ID);
        GatewayUser.from(null, null).requireAccessTo(BUILDING_ID);
        assertThat(GatewayUser.from(null, " ").buildingIds()).isEmpty();
    }

    @Test
    void rejectsMalformedHeaders() {
        assertThatThrownBy(() -> GatewayUser.from("RESIDENT", "tower-a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Header X-Building-Id must hold building ids");
        assertThatThrownBy(() -> GatewayUser.parseUserId("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Header X-User-Id must hold a user id");
    }

    @Test
    void readsTheUserIdOfTheToken() {
        UUID userId = UUID.randomUUID();

        assertThat(GatewayUser.parseUserId(" " + userId + " ")).isEqualTo(userId);
        assertThat(GatewayUser.parseUserId(null)).isNull();
        assertThat(GatewayUser.parseUserId("")).isNull();
    }
}
