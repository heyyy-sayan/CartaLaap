package com.cartalaap.topic;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/topics")
public class TopicController {
    private final TopicService topics;

    public TopicController(TopicService topics) {
        this.topics = topics;
    }

    @GetMapping
    public List<TopicSummary> list() {
        return topics.list();
    }

    @GetMapping("/trending")
    public List<TopicSummary> trending(@RequestParam(defaultValue = "4") int limit) {
        return topics.trending(limit);
    }

    @PostMapping
    public TopicSummary create(@Valid @RequestBody CreateTopicRequest request, Authentication authentication) {
        return topics.create(request, authentication);
    }

    @GetMapping("/{slug}")
    public TopicDetail detail(@PathVariable String slug, Authentication authentication) {
        return topics.detail(slug, authentication);
    }
}
