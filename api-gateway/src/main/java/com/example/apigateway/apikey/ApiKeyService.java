package com.example.apigateway.apikey;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class
ApiKeyService {

    private final ApiClientRepository repo;

    public ApiKeyService(ApiClientRepository repo) {
        this.repo = repo;
    }

    public Mono<Boolean> isValidKey(String apiKey) {
        return repo.findValidByApiKey(apiKey)
                .map(c -> true)
                .defaultIfEmpty(false);
    }

    public Mono<ApiClient> issueKey(String clientName, LocalDateTime expiredAt) {
        ApiClient client = new ApiClient();
        client.setClientName(clientName);
        client.setApiKey(UUID.randomUUID().toString().replace("-", ""));
        client.setCreatedAt(LocalDateTime.now());
        client.setExpiredAt(expiredAt);
        client.setEnabled(true);
        return repo.save(client);
    }

    public Mono<ApiClient> setExpiry(Long id, LocalDateTime expiredAt) {
        return repo.findById(id)
                .flatMap(c -> {
                    c.setExpiredAt(expiredAt);
                    return repo.save(c);
                });
    }

    public Flux<ApiClient> findAll() {
        return repo.findAll();
    }

    public Mono<Void> deleteKey(Long id) {
        return repo.deleteById(id);
    }

    public Mono<ApiClient> toggleKey(Long id) {
        return repo.findById(id)
                .flatMap(c -> {
                    c.setEnabled(!c.isEnabled());
                    return repo.save(c);
                });
    }
}
