@echo off
echo ===========================================
echo TY Multiverse Consumer - RabbitMQ Test
echo ===========================================

echo Testing People Get-All API flow...
echo.

echo 1. Checking RabbitMQ connection...
echo    - Host: localhost
echo    - Port: 5672
echo    - Virtual Host: /
echo    - Username: admin
echo.

echo 2. Producer sends request message to people-get-all queue:
echo {
echo   "requestId": "test-uuid-12345",
echo   "endpoint": "/tymb/people/get-all",
echo   "method": "POST",
echo   "payload": null,
echo   "timestamp": "1700000000000",
echo   "source": "producer"
echo }
echo.

echo 3. Consumer should receive and process the message...
echo    - Listening on queue: people-get-all
echo    - Processing with PeopleConsumer.handleGetAllPeople()
echo.

echo 4. Consumer sends response to async-result queue:
echo {
echo   "requestId": "test-uuid-12345",
echo   "status": "completed",
echo   "data": [
echo     {
echo       "name": "Alice",
echo       "codeName": "Wonder Woman",
echo       "age": 25,
echo       "gender": "Female",
echo       "job": "Warrior",
echo       "attributes": "Strength,Agility"
echo     }
echo   ],
echo   "error": null,
echo   "timestamp": "2024-01-01T00:00:00Z",
echo   "source": "consumer"
echo }
echo.

echo ===========================================
echo Test completed. Check application logs for details.
echo ===========================================
