#!/bin/bash
pkill -f api-gateway 2>/dev/null && echo "api-gateway 종료됨" || echo "실행 중인 api-gateway 없음"
