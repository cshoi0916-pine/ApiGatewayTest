package com.example.apigateway.security;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("gateway.ip_blacklist")
public class IpBlacklistEntity {

    @Id
    private Long id;
    private String ipAddress;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;  // NULL이면 영구 차단
    private boolean auto;             // true면 Rate Limit 초과로 자동 등록

    public Long getId() { return id; }
    public String getIpAddress() { return ipAddress; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public boolean isAuto() { return auto; }

    public void setId(Long id) { this.id = id; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setReason(String reason) { this.reason = reason; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }
    public void setAuto(boolean auto) { this.auto = auto; }
}
