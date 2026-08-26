# Release Note：業務事件 Outbox（第二批）

日期：2026-08-26
影響服務：`ty-multiverse-consumer`、`ty-multiverse-backend`、`ty-multiverse-common`

---

## ⚠️ 行為變更（部署前必須明確接受）

### `weapon-delete` 與 `weapon-delete-all` 會開始「真的刪資料」

這**不是** audit 相關的變更，而是一個既有 bug 的修正，必須單獨評估與測試。

**修正前**（[`WeaponConsumer.java`](../src/main/java/com/vinskao/ty_multiverse_consumer/core/consumer/WeaponConsumer.java)）：

```java
// 處理請求
weaponService.deleteWeapon(name);      // Mono 未被訂閱 -> 刪除從未執行
...
weaponService.deleteAllWeapons();      // 同上
```

`deleteWeapon` / `deleteAllWeapons` 回傳 `Mono<Void>`，但呼叫端沒有 `.block()` 也沒有
`.subscribe()`。Reactive chain 沒有訂閱者就不會執行，因此這兩個端點過去**一律回報成功、
但實際上沒有刪除任何武器**。（`PeopleConsumer` 的 delete-all 早先已經修過同一類問題，
原始碼留有「修正：必須 .block() 才會真正執行」的註解。）

**修正後**：兩者都會被訂閱並執行，刪除與 outbox 事件在同一個 transaction 內完成。

#### 風險

- 任何目前依賴「呼叫 delete 但資料不會真的消失」這個行為的流程都會壞掉。
- 前端／腳本若曾誤打 `weapon-delete-all`，過去無害，之後會清空整張 weapon 表。
- `delete-all` 端點在 backend 需要 `ROLE_manage-users`，但 `weapon-delete` 只需要一般寫入權限。

#### 上線前必做的端點測試

在非正式環境（或先備份 weapon 表）依序驗證：

1. **單筆刪除生效**
   - 建立測試武器 → 呼叫 `weapon-delete` → 查 DB 確認該筆已消失。
2. **刪除不存在的武器**
   - 呼叫 `weapon-delete` 傳入不存在的名稱 → 確認回傳失敗或成功但不影響其他資料，且
     產生 `weapon.delete.failed` 或 `weapon.delete.succeeded` 事件。
3. **delete-all 權限把關**
   - 無 JWT／無內部 token 呼叫 `weapon/delete-all` → 預期 401/403。
   - 低權限身分呼叫 → 預期 403。
4. **delete-all 生效且可稽核**
   - 以 `manage-users` 身分呼叫 → 確認 weapon 表清空，且 `business_event_outbox` 有一筆
     `weapon.delete-all.succeeded`，`tymb-events` 收到對應事件。
5. **回滾演練**
   - 確認已有 weapon 表的備份與還原步驟。

#### 若還不想接受這個變更

可以在部署前把 `WeaponConsumer` 的兩處改回不訂閱（並移除對應的事件記錄），
其餘 outbox 功能不受影響。但請注意：維持原狀等於這兩個端點持續「假成功」。

---

## 新增功能

### 1. 業務事件 Stream（RabbitMQ）

- 新增 `tymb-event-exchange`（topic、durable）與 `tymb-events`（stream，
  retention 30 天、上限 10 GiB），binding routing key 為 `event.#`。
- **完全不影響**既有 classic queue 與 RPC 流程；事件 routing key 一律帶 `event.` 前綴。
- 宣告同時存在於 backend 的 `RabbitMQConfig` 與 consumer 的 `EventRabbitMQConfig`，
  參數必須逐字一致，否則 `RabbitAdmin` 會在啟動時 `PRECONDITION_FAILED`。

### 2. `BusinessEvent` 統一事件格式（`ty-multiverse-common` 2.2.2 → 2.2.3）

- `tw.com.ty.common.event.BusinessEvent` / `BusinessEventType`。
- `eventId` 為去重鍵，`requestId` 串起 requested / succeeded / failed，
  `schemaVersion` 供未來欄位演進。
- payload 不放機密；更新事件只保留 before/after 差異；failed 事件只留錯誤代碼與訊息首行。
- **backend 與 consumer 的 `pom.xml` 必須同步升到 2.2.3 後一起部署。**

### 3. Transactional Outbox

- 新表 `business_event_outbox`（DDL：`ty-multiverse-backend/db/business_event_outbox.sql`，
  已於 2026-08-26 對正式 `peoplesystem` 執行完成）。
- 業務寫入與 outbox 事件在同一個 transaction 內完成（`BusinessEventTransaction`）。
- 背景 publisher 輪詢未發布事件，逐筆等待 RabbitMQ publisher confirm 的 ack
  才回填 `published_at`；失敗則指數退避重試，事件不會被丟棄。
- 冪等：`event_id` 唯一索引 + `ON CONFLICT DO NOTHING`；AMQP `messageId = eventId`，
  下游必須以它去重（ack 後、回填前當機會造成同一事件重送一次）。
- **錯誤一律往外傳**：成功事件的序列化或 outbox 寫入失敗會讓業務 transaction 一併回滾。
  吞掉錯誤會造成「業務資料 commit 但 outbox 沒紀錄」，正好破壞 outbox 的核心保證。
  唯一的 quiet failure 是 failed 事件的補記（`recordFailedQuietly`）——此時業務 transaction
  已回滾，稽核寫入再失敗不應蓋掉原本的業務錯誤。
- 事件的 `occurredAt` 是 `Instant`，依賴注入的 `ObjectMapper` 已註冊 `JavaTimeModule`
  （Spring Boot 自動組態提供）。若日後改用自訂的裸 `ObjectMapper` bean，寫入會失敗並回滾。

### 5. 測試

`mvn test`：16 個測試通過。新增覆蓋範圍：

- `BusinessEventRecorderTest`：序列化失敗／outbox 寫入失敗必須往外傳、關閉開關不寫入、
  payload JSON 可還原、failed 事件不含 stack trace。
- `BusinessEventTransactionRollbackTest`：以真的 `R2dbcTransactionManager`（H2）驗證
  事件失敗→業務回滾、業務失敗→無事件、兩者同時 commit。
- `BusinessEventPublisherTest`：ack 才標記已發布、nack／broker 中斷／confirm 逾時
  都保留事件重試、指數退避成長與上限、讀取失敗不中斷排程。

`ON CONFLICT DO NOTHING` 的去重是 PostgreSQL 專屬語意，H2 無法驗證，改由
`verify-outbox-resilience.sh` 對正式資料庫斷言（同一 eventId 在 outbox 只有一筆）。

### 6. 已接入的寫入操作

| 操作 | 保證 |
| --- | --- |
| `people.insert` / `people.update` / `people.delete-all` | 單一 transaction |
| `weapon.save` / `weapon.delete` / `weapon.delete-all` | 單一 transaction |
| `people.insert-multiple` | **較弱**：寫入成功後另開 transaction 記錄 |

`people.insert-multiple` 的 `saveAllPeople` 刻意不加 `@Transactional` 以允許多連線並發
（原始碼有註解說明），硬包進單一 transaction 會踩到 non-multiplexing 錯誤，因此改用
`recordAfter`，存在「寫入成功但事件遺失」的視窗。

**未接入**：`people.batch-damage`。`calculateBatchDamageWithWeapon` 只查詢 people 與 weapon
再計算傷害，不改變任何資料狀態，依「讀取型操作先不記」的原則略過。若之後需要存取稽核，
應另建 access-events，不要混入業務交易 stream。

---

## 設定

consumer 新增（皆有預設值，不設也能運作）：

| 環境變數 | 預設 | 說明 |
| --- | --- | --- |
| `OUTBOX_ENABLED` | `true` | 關閉後不寫入也不發布事件 |
| `OUTBOX_POLL_INTERVAL_MS` | `2000` | 輪詢間隔 |
| `OUTBOX_BATCH_SIZE` | `100` | 每輪最多發布筆數 |
| `OUTBOX_CONFIRM_TIMEOUT_MS` | `5000` | 等待 publisher confirm 的逾時 |
| `OUTBOX_BACKOFF_BASE_MS` | `2000` | 重試退避基準 |
| `OUTBOX_MAX_BACKOFF_MS` | `300000` | 重試退避上限 |
| `OUTBOX_ALERT_AFTER_ATTEMPTS` | `10` | 超過此次數以 ERROR 告警（仍繼續重試） |

另外 `spring.rabbitmq.publisher-confirm-type` 改為 `correlated`、`publisher-returns` 開啟。
這會套用在共用的 connection factory 上，既有 RPC 發送行為不變。

RabbitMQ 帳密改由 Kubernetes Secret `rabbitmq-credentials` 提供，
backend 與 consumer 的 manifest 皆已改為 `secretKeyRef`。

---

## 部署順序

1. 發布 `ty-multiverse-common` 2.2.3。
2. 確認 `business_event_outbox` 已存在（已完成）。
3. 部署 consumer 與 backend（兩者都依賴 common 2.2.3）。
4. 執行 preflight：`bash verify-outbox-resilience.sh`
5. 全數通過後執行故障測試：`bash verify-outbox-resilience.sh --execute`
6. 依上方清單完成 `weapon-delete` / `weapon-delete-all` 端點測試。

驗證腳本：[`ty-multiverse-backend/k8s/verify-outbox-resilience.sh`](../../ty-multiverse-backend/k8s/verify-outbox-resilience.sh)
