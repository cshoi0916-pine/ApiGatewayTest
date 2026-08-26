#!/bin/bash
# auth-service 배포 스크립트
# 사용법: bash deploy-auth-server.sh [서버IP] [SSH유저]

SERVER=${1:-192.168.1.221}
USER=${2:-root}
REMOTE_DIR=/opt/apigw/auth-service
JAR=auth-service-1.0.0.jar

echo "=== auth-service 배포 → $USER@$SERVER ==="

# 원격 디렉토리 생성
ssh $USER@$SERVER "mkdir -p $REMOTE_DIR"

# JAR 전송
echo "[1/2] JAR 전송 중..."
scp ../auth-service/target/$JAR $USER@$SERVER:$REMOTE_DIR/$JAR

# 실행 스크립트 전송 및 실행
echo "[2/2] 서비스 재시작..."
ssh $USER@$SERVER "
  PID=\$(pgrep -f 'auth-service')
  if [ -n \"\$PID\" ]; then
    echo 'Stopping existing process (PID='\$PID')...'
    kill \$PID
    sleep 3
  fi

  cd $REMOTE_DIR
  nohup java -jar $JAR \
    --spring.profiles.active=prod \
    > app.log 2>&1 &

  sleep 2
  echo 'Started PID='$(pgrep -f 'auth-service')
"

echo ""
echo "=== 배포 완료 ==="
echo "  로그: ssh $USER@$SERVER 'tail -f $REMOTE_DIR/app.log'"
