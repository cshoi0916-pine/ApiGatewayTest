#!/bin/bash
# 221 서버에 Docker로 배포
# 전제: deploy/gateway-images.tar.gz 가 이미 생성되어 있어야 함 (package-docker.sh 실행 후)

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
SERVER="192.168.1.221"
REMOTE_DIR="/opt/api-gateway"

echo "=== 원격 디렉토리 준비 ==="
ssh "$SERVER" "mkdir -p $REMOTE_DIR"

echo "=== 파일 업로드 ==="
scp "$SCRIPT_DIR/gateway-images.tar.gz" "$SERVER:$REMOTE_DIR/"
scp "$ROOT_DIR/docker-compose.yml"      "$SERVER:$REMOTE_DIR/"
scp "$ROOT_DIR/prometheus.yml"          "$SERVER:$REMOTE_DIR/"

echo "=== 서버에서 실행 ==="
ssh "$SERVER" bash << REMOTE
  set -e
  cd $REMOTE_DIR

  echo "-- 이미지 로드 --"
  docker load < gateway-images.tar.gz

  echo "-- 기존 컨테이너 중지 --"
  docker-compose down 2>/dev/null || true

  echo "-- 서비스 시작 --"
  docker-compose up -d

  echo "-- 상태 확인 --"
  sleep 5
  docker-compose ps
REMOTE

echo ""
echo "배포 완료. https://$SERVER:8080/console 접속 확인"
