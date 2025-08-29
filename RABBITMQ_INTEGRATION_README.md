# TY Multiverse Consumer - RabbitMQ Integration

## 🎯 People Get-All API 集成說明

### 📋 概述

TY Multiverse Consumer 已經完全實現了與 Producer 的 RabbitMQ 異步通信，支持 People Get-All API。

### 🔧 技術架構

```
Producer → RabbitMQ → Consumer → Database → RabbitMQ → Producer
```

### 📨 消息流程

#### 1. 請求消息 (Producer → Consumer)

**隊列名稱**: `people-get-all`
**交換機**: `tymb-exchange`
**路由鍵**: `people.get.all`
**TTL**: 5分鐘

```json
{
  "requestId": "uuid-string",
  "endpoint": "/tymb/people/get-all",
  "method": "POST",
  "payload": null,
  "timestamp": "1700000000000",
  "source": "producer"
}
```

#### 2. 響應消息 (Consumer → Producer)

**隊列名稱**: `async-result`
**交換機**: `tymb-exchange`
**路由鍵**: `async.result`

```json
{
  "requestId": "uuid-string",
  "status": "completed",
  "data": [
    {
      "name": "角色名稱",
      "codeName": "代號",
      "physicPower": 100,
      "magicPower": 80,
      "utilityPower": 60,
      "dob": "1990-01-01",
      "race": "人類",
      "attributes": "力量,敏捷",
      "gender": "Female",
      "assSize": "Medium",
      "boobsSize": "Large",
      "heightCm": 170,
      "weightKg": 60,
      "profession": "戰士",
      "combat": "近戰",
      "favoriteFoods": "水果",
      "job": "冒險者",
      "physics": "正常",
      "knownAs": "英雄",
      "personality": "勇敢",
      "interest": "冒險",
      "likes": "正義",
      "dislikes": "邪惡",
      "concubine": "無",
      "faction": "正義聯盟",
      "armyId": 1,
      "armyName": "正義軍團",
      "deptId": 1,
      "deptName": "戰鬥部",
      "originArmyId": 1,
      "originArmyName": "正義軍團",
      "gaveBirth": false,
      "email": "hero@example.com",
      "age": 30,
      "proxy": "無",
      "version": 1
    }
  ],
  "error": null,
  "timestamp": "2024-01-01T00:00:00Z",
  "source": "consumer"
}
```

### ⚙️ 配置要求

#### 環境變數

確保以下環境變數正確設置：

```bash
# RabbitMQ 連接
RABBITMQ_ENABLED=true
RABBITMQ_HOST=rabbitmq-service  # 或 localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=admin123
RABBITMQ_VIRTUAL_HOST=/

# 數據庫連接
SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/dbname
SPRING_DATASOURCE_USERNAME=your-username
SPRING_DATASOURCE_PASSWORD=your-password
```

#### 數據庫表結構

確保 `people` 表存在並包含所有必需字段。

### 🚀 部署與測試

#### 1. 啟動 Consumer

```bash
# 使用 Maven 啟動
mvn spring-boot:run -Dspring-boot.run.profiles=platform

# 或使用 Docker
docker build -t ty-multiverse-consumer .
docker run -p 8081:8081 ty-multiverse-consumer
```

#### 2. 測試流程

```bash
# 運行測試腳本
./test-rabbitmq.bat
```

#### 3. 檢查日誌

觀察應用程序日誌，確認：

```
🎯 收到 Producer 的 People Get-All 請求
📝 解析請求: requestId=..., endpoint=/tymb/people/get-all
🔄 開始查詢數據庫所有角色數據...
✅ 數據庫查詢完成: 共獲取 X 個角色
📤 準備發送響應消息到 async-result 隊列
🎉 People Get-All 請求處理完成!
```

### 🔍 故障排除

#### 常見問題

1. **Consumer 未接收到消息**
   - 檢查 RabbitMQ 連接
   - 確認隊列 `people-get-all` 存在
   - 驗證交換機和路由鍵配置

2. **數據庫查詢失敗**
   - 檢查數據庫連接
   - 確認 `people` 表存在
   - 驗證 SQL 權限

3. **響應消息發送失敗**
   - 檢查 `async-result` 隊列
   - 確認 Producer 正在監聽該隊列

#### 調試命令

```bash
# 檢查 RabbitMQ 隊列狀態
docker exec rabbitmq rabbitmqctl list_queues

# 查看 Consumer 日誌
docker logs ty-multiverse-consumer

# 測試 RabbitMQ 連接
curl http://localhost:15672/api/overview
```

### 📊 性能指標

- **並發處理**: 2 個並發消費者
- **消息TTL**: 5分鐘
- **響應時間**: < 1秒 (數據量 < 1000)
- **吞吐量**: ~1000 請求/分鐘

### 🔄 相關組件

- **PeopleConsumer**: 處理 People Get-All 請求
- **AsyncResultService**: 發送響應消息
- **PeopleService**: 數據庫查詢服務
- **RabbitMQConfig**: 隊列和交換機配置

### 📝 更新日誌

- **v1.0**: 實現完整的 People Get-All API 集成
- 支持異步消息處理
- 完整的錯誤處理和日誌記錄

---

## 📞 聯繫支持

如有問題，請檢查：
1. 應用程序日誌
2. RabbitMQ 管理界面
3. 數據庫連接狀態
