package com.cartalaap.topic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTopicRequest(
        @NotBlank @Size(min = 3, max = 80) String name,
        @Size(max = 240) String description) {
}
