package com.rumi.maintenance.infrastructure.web;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User forwarded by rumi-api-gateway in the X-Role, X-Building-Id and X-User-Id headers after it validated
 * the JWT. The gateway lets any authenticated user reach inspections and damage reports; the rules of this
 * context belong to the service: only administrators manage inspections and the damage board, and a
 * resident only reports damage in the buildings of their token. A request without the headers did not go
 * through the gateway (for example, a local test) and is not restricted.
 */
record GatewayUser(String role, Set<UUID> buildingIds) {

    static final String ROLE_HEADER = "X-Role";
    static final String BUILDING_IDS_HEADER = "X-Building-Id";
    static final String USER_ID_HEADER = "X-User-Id";
    static final String RESIDENT = "RESIDENT";

    static GatewayUser from(String role, String buildingIdsHeader) {
        if (buildingIdsHeader == null || buildingIdsHeader.isBlank()) {
            return new GatewayUser(role, Set.of());
        }
        try {
            Set<UUID> ids = Arrays.stream(buildingIdsHeader.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(UUID::fromString)
                    .collect(Collectors.toUnmodifiableSet());
            return new GatewayUser(role, ids);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Header " + BUILDING_IDS_HEADER + " must hold building ids");
        }
    }

    static GatewayUser fromRole(String role) {
        return new GatewayUser(role, Set.of());
    }

    static UUID parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(userIdHeader.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Header " + USER_ID_HEADER + " must hold a user id");
        }
    }

    /**
     * Inspections, the damage board and the review of damage reports are for the administrator.
     */
    void requireAdministrator(String action) {
        if (RESIDENT.equals(role)) {
            throw new MaintenanceAccessDeniedException("Only the building administrator can " + action);
        }
    }

    /**
     * A resident can only report damage in a building of their token.
     */
    void requireAccessTo(UUID buildingId) {
        if (RESIDENT.equals(role) && !buildingIds.contains(buildingId)) {
            throw new MaintenanceAccessDeniedException(
                    "You cannot report damage in building " + buildingId + ": it is not one of your buildings");
        }
    }
}
