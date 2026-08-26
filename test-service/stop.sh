#!/bin/bash
pkill -f test-service 2>/dev/null && echo "test-service 종료됨" || echo "실행 중인 test-service 없음"
