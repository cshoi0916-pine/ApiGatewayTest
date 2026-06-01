package com.example.apigateway.admin;

import com.example.apigateway.admin.ApiKeyAdminService;
import com.example.apigateway.admin.dto.CreateApiKeyRequest;
import com.example.apigateway.admin.dto.CreateApiKeyResponse;
import com.example.apigateway.apikey.ApiClient;
import com.example.apigateway.apikey.ApiClientRepository;
import com.example.apigateway.metrics.ApiMetricsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gateway/admin")
public class AdminController {

    private final ApiKeyAdminService service;
    private final ApiClientRepository repository;
    private final ApiMetricsService metricsService;

    public AdminController(
            ApiKeyAdminService service,
            ApiClientRepository repository,
            ApiMetricsService metricsService
    ) {
        this.service = service;
        this.repository = repository;
        this.metricsService = metricsService;
    }

    @PostMapping("/apikey")
    public CreateApiKeyResponse createApiKey(
            @RequestBody CreateApiKeyRequest request
    ) {

        String apiKey = service.createApiKey(
                request.getName(),
                request.getLimitPerMinute()
        );

        return new CreateApiKeyResponse(apiKey);
    }

    @GetMapping("/apikeys")
    public List<ApiClient> getApiKeys() {
        return repository.findAll();
    }

    @DeleteMapping("/apikey/{id}")
    public ResponseEntity<Map<String, Object>> deleteApiKey(
            @PathVariable Long id
    ) {

        ApiClient client = repository
                .findById(id)
                .orElse(null);

        if (client == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "API Key not found"
                    ));
        }

        repository.delete(client);

        return ResponseEntity.ok(
                Map.of(
                        "message", "API Key deleted successfully",
                        "deletedId", client.getId(),
                        "deletedName", client.getName(),
                        "deletedApiKey", client.getApiKey()
                )
        );
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return metricsService.getStats();
    }
}