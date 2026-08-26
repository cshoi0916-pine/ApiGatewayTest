#!/bin/bash
pkill -f auth-service 2>/dev/null && echo "auth-service 종료됨" || echo "실행 중인 auth-service 없음"
