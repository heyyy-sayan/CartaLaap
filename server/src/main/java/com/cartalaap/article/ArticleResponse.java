package com.cartalaap.article;

import java.time.Instant;

public record ArticleResponse(Long id, Author author, String title, String body, String excerpt,
        String coverImageUrl, String topicSlug, String topicName, Instant createdAt, Instant updatedAt, boolean ownedByCurrentUser) {
    static ArticleResponse from(Article article, Long currentUserId) {
        var user = article.getAuthor();
        String plain = article.getBody()
                .replaceAll("!\\[([^]]*)]\\([^)]+\\)", "$1")
                .replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1")
                .replaceAll("(?m)^\\s*(?:#{1,3}|>|-|\\d+\\.)\\s+", "")
                .replaceAll("(?m)^\\s*---\\s*$", "")
                .replaceAll("[*_`]", "")
                .replaceAll("\\s+", " ")
                .trim();
        String excerpt = plain.length() > 220 ? plain.substring(0, 220) + "…" : plain;
        return new ArticleResponse(article.getId(), new Author(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl()),
                article.getTitle(), article.getBody(), excerpt, article.getCoverImageUrl(), article.getTopicSlug(),
                article.getTopicName(), article.getCreatedAt(), article.getUpdatedAt(),
                currentUserId != null && user.getId().equals(currentUserId));
    }
    public record Author(Long id, String username, String displayName, String avatarUrl) {}
}
