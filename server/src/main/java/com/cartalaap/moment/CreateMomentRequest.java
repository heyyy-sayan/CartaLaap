package com.cartalaap.moment;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMomentRequest(
        @NotBlank @URL @Size(max = 2048) String imageUrl,
        @Size(max = 300) String caption) {}
