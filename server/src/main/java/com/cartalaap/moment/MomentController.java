package com.cartalaap.moment;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/moments")
public class MomentController {
    private final MomentService service;
    public MomentController(MomentService service) { this.service = service; }
    @GetMapping public List<MomentResponse> active(Authentication auth) { return service.active(auth); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public MomentResponse create(@Valid @RequestBody CreateMomentRequest request, Authentication auth) { return service.create(request, auth); }
    @PostMapping("/{id}/view") public MomentResponse view(@PathVariable Long id, Authentication auth) { return service.view(id, auth); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id, Authentication auth) { service.delete(id, auth); }
}
