#!/bin/bash
# api-gateway 서버 배포 스크립트
# 사용법: bash deploy-api-gateway.sh [서버IP] [SSH유저]

SERVER=${1:-192.168.1.221}
USER=${2:-root}
REMOTE_DIR=/opt/apigw/api-gateway
JAR=api-gateway-0.0.1-SNAPSHOT.jar

# 스크립트 자신의 위치 기준으로 경로를 잡아서, 어느 폴더에서 실행하든 동작하게 함
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCAL_JAR="$SCRIPT_DIR/../api-gateway/target/$JAR"

echo "=== api-gateway 배포 → $USER@$SERVER ==="

# 원격 디렉토리 생성
ssh $USER@$SERVER "mkdir -p $REMOTE_DIR"

# JAR 전송 (restart.sh가 찾는 파일명 그대로 유지)
echo "[1/2] JAR 전송 중..."
scp "$LOCAL_JAR" $USER@$SERVER:$REMOTE_DIR/$JAR

# 서비스 재시작 (restart.sh와 동일한 pkill 패턴 + 파일명 사용)
echo "[2/2] 서비스 재시작..."
ssh $USER@$SERVER "
  pkill -f api-gateway 2>/dev/null
  sleep 2

  cd $REMOTE_DIR
  nohup java -jar $JAR \
    --spring.profiles.active=prod \
    > app.log 2>&1 &

  sleep 2
  echo 'Started PID='\$(pgrep -f api-gateway)
"

echo ""
echo "=== 배포 완료 ==="
echo "  콘솔: http://$SERVER/console.html"
echo "  로그: ssh $USER@$SERVER 'tail -f $REMOTE_DIR/app.log'"
