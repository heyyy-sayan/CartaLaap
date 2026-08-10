package com.cartalaap.article;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cartalaap.post.PagedResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService service;
    public ArticleController(ArticleService service) { this.service = service; }
    @GetMapping public PagedResponse<ArticleResponse> list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="12") int size, Authentication auth) { return service.list(page, size, auth); }
    @GetMapping("/{id}") public ArticleResponse get(@PathVariable Long id, Authentication auth) { return service.get(id, auth); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ArticleResponse create(@Valid @RequestBody ArticleRequest request, Authentication auth) { return service.create(request, auth); }
    @PatchMapping("/{id}") public ArticleResponse update(@PathVariable Long id, @Valid @RequestBody ArticleRequest request, Authentication auth) { return service.update(id, request, auth); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id, Authentication auth) { service.delete(id, auth); }
}
