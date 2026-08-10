package com.cartalaap.topic;

import java.util.List;

import com.cartalaap.article.ArticleResponse;
public record TopicDetail(TopicSummary topic, List<ArticleResponse> articles) {
}
