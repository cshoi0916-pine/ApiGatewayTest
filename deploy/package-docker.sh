#!/bin/bash
# Docker 이미지 빌드 + 폐쇄망 배포용 tar.gz 생성
# 실행: ApiGatewayTest/ 또는 어디서든 OK

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
OUTPUT="$SCRIPT_DIR/gateway-images.tar.gz"

echo "=== 1. JAR 빌드 ==="
cd "$ROOT_DIR/api-gateway"
./mvnw.cmd package -DskipTests -q
echo "  JAR 빌드 완료"

echo "=== 2. Docker 이미지 빌드 ==="
cd "$ROOT_DIR"
docker build -t api-gateway:latest api-gateway/
echo "  api-gateway:latest 빌드 완료"

echo "=== 3. 베이스 이미지 pull (이미 있으면 스킵) ==="
docker pull redis:7
docker pull postgres:16
docker pull prom/prometheus
docker pull grafana/grafana

echo "=== 4. 이미지 저장: $OUTPUT ==="
docker save \
  api-gateway:latest \
  redis:7 \
  postgres:16 \
  prom/prometheus \
  grafana/grafana \
  | gzip > "$OUTPUT"

SIZE=$(du -sh "$OUTPUT" | cut -f1)
echo ""
echo "====================================="
echo " 완료: gateway-images.tar.gz ($SIZE)"
echo " 배포 대상 서버에서 실행:"
echo "   docker load < gateway-images.tar.gz"
echo "   docker-compose up -d"
echo "====================================="
