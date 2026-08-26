package com.example.testservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class TestController {

    @Value("${pine-gw.service-id}")
    private String serviceId;

    @Value("${server.port}")
    private int port;

    @GetMapping({"/", ""})
    public Map<String, Object> root() {
        return Map.of(
            "service", serviceId,
            "port", port,
            "status", "UP"
        );
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        return Map.of(
            "message", "Hello from " + serviceId,
            "port", port,
            "time", LocalDateTime.now().toString()
        );
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return List.of(
            Map.of("id", 1, "name", "홍길동", "role", "admin"),
            Map.of("id", 2, "name", "김철수", "role", "user"),
            Map.of("id", 3, "name", "이영희", "role", "user")
        );
    }

    @GetMapping("/echo")
    public Map<String, Object> echo(@RequestParam(defaultValue = "test") String msg) {
        return Map.of(
            "echo", msg,
            "from", serviceId + ":" + port,
            "time", LocalDateTime.now().toString()
        );
    }

    @GetMapping("/slow")
    public ResponseEntity<String> slow(@RequestParam(defaultValue = "10000") long ms)
            throws InterruptedException {
        Thread.sleep(ms);
        return ResponseEntity.ok("done after " + ms + "ms");
    }

    private static final AtomicInteger counter = new AtomicInteger(0);

    @GetMapping("/flaky")
    public ResponseEntity<String> flaky() {
        int count = counter.incrementAndGet();
        if (count % 3 != 0) {          // 3번 중 1번만 성공
            return ResponseEntity.status(502).body("simulated failure #" + count);
        }
        counter.set(0);
        return ResponseEntity.ok("success on attempt #" + count);
    }
}
