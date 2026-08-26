#!/bin/bash
pkill -f test-service 2>/dev/null
sleep 2
cd /opt/apigw/test-service
nohup java -jar test-service-1.0.0.jar --spring.profiles.active=prod > app.log 2>&1 &
echo "Started PID: $!"
tail -f app.log
