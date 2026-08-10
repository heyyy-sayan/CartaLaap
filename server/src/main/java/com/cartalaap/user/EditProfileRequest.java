package com.cartalaap.user;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditProfileRequest(
        @NotBlank @Size(min = 2, max = 80) String displayName,
        @Size(max = 300) String bio,
        @URL @Size(max = 2048) String avatarUrl,
        @Size(max = 100) String location,
        @Size(max = 300) String vehicleInterests) {
}
