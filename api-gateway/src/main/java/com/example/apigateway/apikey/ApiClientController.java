package com.example.apigateway.apikey;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/console/api/apikeys")
public class ApiClientController {

    private final ApiKeyService apiKeyService;

    public ApiClientController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public Flux<ApiClient> list() {
        return apiKeyService.findAll();
    }

    @PostMapping
    public Mono<ApiClient> issue(@RequestBody Map<String, String> body) {
        String clientName = body.getOrDefault("clientName", "unknown");
        return apiKeyService.issueKey(clientName, parseDateTime(body.get("expiredAt")));
    }

    @PutMapping("/{id}/expiry")
    public Mono<ResponseEntity<ApiClient>> setExpiry(@PathVariable Long id,
                                                     @RequestBody Map<String, String> body) {
        return apiKeyService.setExpiry(id, parseDateTime(body.get("expiredAt")))
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return apiKeyService.deleteKey(id)
                .thenReturn(ResponseEntity.<Void>noContent().build());
    }

    @PatchMapping("/{id}/toggle")
    public Mono<ApiClient> toggle(@PathVariable Long id) {
        return apiKeyService.toggleKey(id);
    }

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]");

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDateTime.parse(s, DT_FMT);
    }
}
