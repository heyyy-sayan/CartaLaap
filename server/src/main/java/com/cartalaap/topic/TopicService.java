package com.cartalaap.topic;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cartalaap.article.ArticleRepository;
import com.cartalaap.article.ArticleService;
import com.cartalaap.common.BadRequestException;
import com.cartalaap.common.NotFoundException;
import com.cartalaap.user.CurrentUserService;

@Service
public class TopicService {
    private final CommunityTopicRepository topics;
    private final ArticleRepository articles;
    private final ArticleService articleService;
    private final CurrentUserService currentUsers;

    public TopicService(CommunityTopicRepository topics, ArticleRepository articles,
            ArticleService articleService, CurrentUserService currentUsers) {
        this.topics = topics;
        this.articles = articles;
        this.articleService = articleService;
        this.currentUsers = currentUsers;
    }

    @Transactional(readOnly = true)
    public List<TopicSummary> list() {
        return topics.findAllByOrderByNameAsc().stream().map(this::summary)
                .sorted(Comparator.comparingLong(TopicSummary::articles).reversed()
                        .thenComparing(TopicSummary::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopicSummary> trending(int limit) {
        return list().stream()
                .filter(topic -> topic.articles() > 0)
                .limit(Math.max(1, Math.min(limit, 8)))
                .toList();
    }

    @Transactional
    public TopicSummary create(CreateTopicRequest request, Authentication authentication) {
        var author = currentUsers.require(authentication);
        String name = request.name().trim().replaceAll("\\s+", " ");
        if (topics.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("That topic already exists. Choose it from the list.");
        }
        String baseSlug = slugify(name);
        if (baseSlug.isBlank()) throw new BadRequestException("Use letters or numbers in the topic name");
        if (baseSlug.length() > 70) baseSlug = baseSlug.substring(0, 70).replaceAll("-$", "");
        String slug = baseSlug;
        int suffix = 2;
        while (topics.existsBySlugIgnoreCase(slug)) slug = baseSlug + "-" + suffix++;
        String description = request.description() == null || request.description().isBlank()
                ? "Articles created by the CartaLaap community."
                : request.description().trim();
        return summary(topics.save(new CommunityTopic(slug, name, description, author)));
    }

    @Transactional(readOnly = true)
    public TopicDetail detail(String slug, Authentication authentication) {
        CommunityTopic topic = definition(slug);
        var topicArticles = articleService.search(topic.getSlug(), 0, 30, authentication).content();
        return new TopicDetail(summary(topic), topicArticles);
    }

    private TopicSummary summary(CommunityTopic topic) {
        long articleCount = articles.countByTopic_SlugIgnoreCase(topic.getSlug());
        return new TopicSummary(topic.getSlug(), topic.getName(), topic.getDescription(), articleCount, 0, articleCount);
    }

    private CommunityTopic definition(String slug) {
        return topics.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new NotFoundException("Topic not found"));
    }

    private String slugify(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
