package com.cartalaap.article;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cartalaap.common.ForbiddenException;
import com.cartalaap.common.NotFoundException;
import com.cartalaap.post.PagedResponse;
import com.cartalaap.user.CurrentUserService;
import com.cartalaap.user.User;
import com.cartalaap.topic.CommunityTopic;
import com.cartalaap.topic.CommunityTopicRepository;

@Service
public class ArticleService {
    private final ArticleRepository articles;
    private final CurrentUserService currentUsers;
    private final CommunityTopicRepository topics;
    public ArticleService(ArticleRepository articles, CurrentUserService currentUsers, CommunityTopicRepository topics) { this.articles = articles; this.currentUsers = currentUsers; this.topics = topics; }

    @Transactional(readOnly = true)
    public PagedResponse<ArticleResponse> list(int page, int size, Authentication auth) {
        User current = currentUsers.optional(auth);
        return PagedResponse.from(articles.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0, page), Math.max(1, Math.min(30, size))))
                .map(article -> ArticleResponse.from(article, current == null ? null : current.getId())));
    }
    @Transactional(readOnly = true)
    public ArticleResponse get(Long id, Authentication auth) {
        User current = currentUsers.optional(auth);
        return ArticleResponse.from(find(id), current == null ? null : current.getId());
    }
    @Transactional(readOnly = true)
    public PagedResponse<ArticleResponse> search(String query, int page, int size, Authentication auth) {
        User current = currentUsers.optional(auth);
        return PagedResponse.from(articles.findByTopic_SlugIgnoreCaseOrderByCreatedAtDesc(
                query, PageRequest.of(Math.max(0, page), Math.max(1, Math.min(30, size))))
                .map(article -> ArticleResponse.from(article, current == null ? null : current.getId())));
    }
    @Transactional
    public ArticleResponse create(ArticleRequest request, Authentication auth) {
        User current = currentUsers.require(auth);
        Article article = articles.save(new Article(current, request.title().trim(), request.body().trim(),
                clean(request.coverImageUrl()), requireTopic(request.topicSlug())));
        return ArticleResponse.from(article, current.getId());
    }
    @Transactional
    public ArticleResponse update(Long id, ArticleRequest request, Authentication auth) {
        Article article = find(id); User current = currentUsers.require(auth); requireOwner(article, current);
        article.update(request.title().trim(), request.body().trim(), clean(request.coverImageUrl()),
                requireTopic(request.topicSlug()));
        return ArticleResponse.from(article, current.getId());
    }
    @Transactional
    public void delete(Long id, Authentication auth) {
        Article article = find(id); User current = currentUsers.require(auth); requireOwner(article, current); articles.delete(article);
    }
    private Article find(Long id) { return articles.findById(id).orElseThrow(() -> new NotFoundException("Article not found")); }
    private void requireOwner(Article article, User user) { if (!article.getAuthor().getId().equals(user.getId())) throw new ForbiddenException("You can only change your own articles"); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private CommunityTopic requireTopic(String slug) {
        if (slug == null || slug.isBlank()) throw new com.cartalaap.common.BadRequestException("Choose or create a topic");
        return topics.findBySlugIgnoreCase(slug.trim())
                .orElseThrow(() -> new com.cartalaap.common.BadRequestException("Choose a valid topic"));
    }
}
