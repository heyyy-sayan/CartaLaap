package com.cartalaap.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank @Size(max = 5000) String body,
        @Size(max = 2048) String imageUrl) {
}
