@echo off
echo ========================================
echo RabbitMQ 測試腳本
echo ========================================

echo.
echo 1. 檢查 RabbitMQ 狀態...
curl -X GET "http://localhost:8081/ty_multiverse_consumer/test/rabbitmq-status" -H "accept: application/json"

echo.
echo.
echo 2. 測試發送 People 消息...
curl -X POST "http://localhost:8081/ty_multiverse_consumer/test/send-people-message" -H "accept: application/json"

echo.
echo.
echo 3. 測試發送傷害計算消息...
curl -X POST "http://localhost:8081/ty_multiverse_consumer/test/send-damage-calculation?characterName=TestCharacter" -H "accept: application/json"

echo.
echo.
echo 4. 檢查應用程序健康狀態...
curl -X GET "http://localhost:8081/ty_multiverse_consumer/actuator/health" -H "accept: application/json"

echo.
echo ========================================
echo 測試完成
echo ========================================
pause
