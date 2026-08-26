package com.example.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        String serviceId = extractServiceId(exchange);
        HttpStatus status = resolveStatus(ex);
        String message = resolveMessage(status, ex, serviceId);

        log.error("[ERROR] path={} service={} status={} cause={}",
                exchange.getRequest().getURI().getPath(), serviceId, status.value(), ex.getMessage());

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (serviceId != null) body.put("serviceId", serviceId);
        body.put("path", exchange.getRequest().getURI().getPath());
        body.put("timestamp", LocalDateTime.now().toString());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":500,\"message\":\"Internal Server Error\"}").getBytes();
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String extractServiceId(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) return null;
        URI uri = route.getUri();
        return "lb".equals(uri.getScheme()) ? uri.getHost() : null;
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof GatewayServiceException gse) {
            return switch (gse.getReason()) {
                case NOT_FOUND -> HttpStatus.NOT_FOUND;
                case SERVICE_DISABLED, NO_INSTANCES, ALL_DOWN -> HttpStatus.SERVICE_UNAVAILABLE;
            };
        }
        if (ex instanceof ResponseStatusException rse) {
            return HttpStatus.valueOf(rse.getStatusCode().value());
        }
        String cn = ex.getClass().getName();
        if (cn.contains("ConnectException") || cn.contains("TimeoutException")) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.BAD_GATEWAY;
    }

    private String resolveMessage(HttpStatus status, Throwable ex, String serviceId) {
        String svc = serviceId != null ? " [" + serviceId + "]" : "";
        if (ex instanceof GatewayServiceException gse) {
            return switch (gse.getReason()) {
                case NOT_FOUND        -> "서비스를 찾을 수 없습니다" + svc;
                case SERVICE_DISABLED -> "서비스가 비활성화 상태입니다" + svc;
                case NO_INSTANCES     -> "등록된 인스턴스가 없습니다" + svc;
                case ALL_DOWN         -> "모든 인스턴스가 응답하지 않습니다" + svc + ". 잠시 후 다시 시도해주세요.";
            };
        }
        return switch (status) {
            case SERVICE_UNAVAILABLE -> "서비스를 일시적으로 이용할 수 없습니다" + svc + ". 잠시 후 다시 시도해주세요.";
            case BAD_GATEWAY        -> "백엔드 서버에 연결할 수 없습니다" + svc;
            case TOO_MANY_REQUESTS  -> "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.";
            case NOT_FOUND          -> "요청한 경로를 찾을 수 없습니다.";
            case UNAUTHORIZED       -> "인증이 필요합니다. X-API-Key 헤더를 확인해주세요.";
            case FORBIDDEN          -> "접근 권한이 없습니다.";
            default                 -> "요청을 처리하는 중 오류가 발생했습니다.";
        };
    }
}
