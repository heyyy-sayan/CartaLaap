package com.cartalaap.article;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArticleRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 50000) String body,
        @URL @Size(max = 2048) String coverImageUrl,
        @NotBlank @Size(max = 80) String topicSlug) {}
