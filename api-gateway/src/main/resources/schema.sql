CREATE SCHEMA IF NOT EXISTS gateway;

-- API Client
CREATE TABLE IF NOT EXISTS gateway.api_client (
    id          BIGSERIAL PRIMARY KEY,
    client_name VARCHAR(100) NOT NULL,
    api_key     VARCHAR(64)  NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 논리 서비스
CREATE TABLE IF NOT EXISTS gateway.service (
    service_id   VARCHAR(100) PRIMARY KEY,
    service_name VARCHAR(200),
    description  VARCHAR(500),
    enabled      BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 물리 인스턴스
CREATE TABLE IF NOT EXISTS gateway.service_instance (
    id                BIGSERIAL    PRIMARY KEY,
    service_id        VARCHAR(100) NOT NULL REFERENCES gateway.service(service_id) ON DELETE CASCADE,
    host              VARCHAR(255) NOT NULL,
    port              INT          NOT NULL,
    protocol          VARCHAR(10)  NOT NULL DEFAULT 'HTTP',
    status            VARCHAR(20)  NOT NULL DEFAULT 'UP',
    health_fail_count INT          NOT NULL DEFAULT 0,
    last_heartbeat    TIMESTAMP,
    registered_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    registration_type VARCHAR(10)  DEFAULT 'STATIC',
    CONSTRAINT uq_instance_service_host_port UNIQUE (service_id, host, port)
);

-- Gateway Route
CREATE TABLE IF NOT EXISTS gateway.gateway_route (
    id                VARCHAR(100) PRIMARY KEY,
    name              VARCHAR(200),
    target_service_id VARCHAR(100),
    target_port       INT,
    method            VARCHAR(10),
    api_path          VARCHAR(500) NOT NULL,
    priority          INT          NOT NULL DEFAULT 0,
    description       VARCHAR(500),
    route_type        VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',
    strip_prefix      INT,
    enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP
);

-- 타임아웃/재시도 컬럼 (기존 테이블에 없으면 추가)
ALTER TABLE gateway.gateway_route ADD COLUMN IF NOT EXISTS timeout_ms   INTEGER;
ALTER TABLE gateway.gateway_route ADD COLUMN IF NOT EXISTS retry_count  INTEGER DEFAULT 0;

-- Route Condition
CREATE TABLE IF NOT EXISTS gateway.route_condition (
    id              BIGSERIAL    PRIMARY KEY,
    route_id        VARCHAR(100) NOT NULL REFERENCES gateway.gateway_route(id) ON DELETE CASCADE,
    type            VARCHAR(50),
    operation       VARCHAR(50),
    attribute_name  VARCHAR(200),
    attribute_value VARCHAR(500)
);

-- Request Log
CREATE TABLE IF NOT EXISTS gateway.request_log (
    id               BIGSERIAL    PRIMARY KEY,
    method           VARCHAR(10),
    path             VARCHAR(500),
    service_id       VARCHAR(100),
    status_code      INT,
    duration_ms      BIGINT,
    client_ip        VARCHAR(50),
    instance_address VARCHAR(255),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Login Log
CREATE TABLE IF NOT EXISTS gateway.login_log (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(100),
    client_ip  VARCHAR(50),
    result     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- IP 블랙리스트
CREATE TABLE IF NOT EXISTS gateway.ip_blacklist (
    id         BIGSERIAL    PRIMARY KEY,
    ip_address VARCHAR(50)  NOT NULL UNIQUE,
    reason     VARCHAR(200),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    expired_at TIMESTAMP    NULL,          -- NULL이면 영구 차단, 값이 있으면 해당 시각 이후 자동 해제
    auto       BOOLEAN      NOT NULL DEFAULT FALSE  -- 자동 등록(Rate Limit 초과) 여부
);
ALTER TABLE gateway.ip_blacklist ADD COLUMN IF NOT EXISTS expired_at TIMESTAMP NULL;
ALTER TABLE gateway.ip_blacklist ADD COLUMN IF NOT EXISTS auto BOOLEAN NOT NULL DEFAULT FALSE;

-- API 키 만료일
ALTER TABLE gateway.api_client ADD COLUMN IF NOT EXISTS expired_at TIMESTAMP;

-- 서킷브레이커 이벤트 이력
CREATE TABLE IF NOT EXISTS gateway.circuit_event (
    id         BIGSERIAL    PRIMARY KEY,
    service_id VARCHAR(100) NOT NULL,
    event      VARCHAR(30)  NOT NULL,   -- OPEN, CLOSED, FORCED_OPEN, FORCED_CLOSE
    detail     VARCHAR(255),
    event_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Gateway Lifecycle Log
CREATE TABLE IF NOT EXISTS gateway.gateway_lifecycle (
    id        BIGSERIAL    PRIMARY KEY,
    event     VARCHAR(20)  NOT NULL,
    node_name VARCHAR(100),
    event_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
