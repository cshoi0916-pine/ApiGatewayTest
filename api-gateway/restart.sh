#!/bin/bash
pkill -f api-gateway 2>/dev/null
sleep 2
cd /opt/apigw/api-gateway
nohup java -jar api-gateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > app.log 2>&1 &
echo "Started PID: $!"
tail -f app.log
