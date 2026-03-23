package com.pickleball.pickleball_backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    OWNER,   // Court Owner — can create venues, manage bookings
    BOOKER ;  // Booker — can browse venues and book courts

    @JsonCreator
    public static Role fromValue(Object value) {
        if (value == null) return null;

        String input = value.toString().trim();

        return switch (input) {
            case "1" -> OWNER;
            case "0" -> BOOKER;
            case "OWNER" -> OWNER;
            case "BOOKER" -> BOOKER;
            default -> throw new IllegalArgumentException(
                    "Invalid role — use OWNER or 1 for owner, BOOKER or 0 for booker"
            );
        };
    }
}
