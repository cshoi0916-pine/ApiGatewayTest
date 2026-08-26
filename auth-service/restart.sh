#!/bin/bash
pkill -f auth-service 2>/dev/null
sleep 2
cd /opt/apigw/auth-service
nohup java -jar auth-service-1.0.0.jar --spring.profiles.active=prod > app.log 2>&1 &
echo "Started PID: $!"
tail -f app.log
