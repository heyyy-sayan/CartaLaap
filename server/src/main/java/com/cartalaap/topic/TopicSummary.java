package com.cartalaap.topic;

public record TopicSummary(String slug, String name, String description, long conversations,
        long posts, long articles) {
}
