package com.example.addressbook.dto;

import java.time.Instant;

public record ContactResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String address,
        String city,
        String state,
        String country,
        String postalCode,
        String company,
        String jobTitle,
        Instant createdAt,
        Instant updatedAt
) {
}
