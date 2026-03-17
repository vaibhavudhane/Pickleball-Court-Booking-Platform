package com.pickleball.pickleball_backend.enums;

public enum SlotStatus {
    AVAILABLE,    // Can be booked
    BOOKED,       // Already confirmed by a user
    UNAVAILABLE   // Past time slot (cannot book slots in the past)
}

