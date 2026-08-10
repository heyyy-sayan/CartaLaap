package com.cartalaap.comment;

import jakarta.validation.constraints.Size;

public record CommentRequest(@Size(max = 2000) String body, @Size(max = 2048) String imageUrl) {
}
