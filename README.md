# TY Multiverse Consumer

## Overview
- **Web 層**：Spring WebFlux（Netty）
- **DB 層**：Spring Data R2DBC（PostgreSQL），連線池上限 5（遵循 K8s 限制）
- **MQ 層**：Reactor RabbitMQ + Spring AMQP（雙棧支援），完全 reactive 消息處理
- **OpenAPI**：springdoc-webflux-ui
- **其他**：Virtual Threads 開啟（供一般任務池）

## 不變更承諾 ✅
- **API 規格不動**：所有 REST 路徑、HTTP 方法、JSON 格式維持相同
- **MQ 不動**：保留既有交換器/隊列/路由鍵配置，對外 MQ 規格完全不變
- **DB 連線限制**：R2DBC 連線池 `max-size=5`

## 模組重點

### 實體層（Entity）
- People/Weapon/PeopleImage/DamageCalculationResult：Entity 改為 Spring Data R2DBC 註解
- 移除 JPA 註解（`@Entity`, `@Table`, `@Id`, `@Column`, `@Version`）
- 改用 R2DBC 註解（`@Table`, `@Id`, `@Column`, `@Version` from `org.springframework.data.annotation`）

### 資料層（Repository）
- 改用 `ReactiveCrudRepository`（`Mono` / `Flux`）
- 移除 JPA Specification 和 Pageable 支援（R2DBC 原生不支援）
- 使用 `@Query` 原生 SQL 查詢

### 服務層（Service）
- 全面 reactive，所有方法回傳 `Mono` 或 `Flux`
- 無阻塞 DB 呼叫
- 移除 `Optional` 和 blocking 操作

### 控制層（Controller）
- WebFlux，回傳 `Mono<ResponseEntity<...>>` 或 `Flux<T>`
- 保持 API 路徑和 JSON 格式完全不變

### 異常處理（Exception）
- **責任鏈模式**：實現可擴展的異常處理架構
- WebFlux 風格的全域異常處理
- 移除 `HttpServletRequest` 依賴

### CORS 配置
- 使用 `CorsWebFilter` 取代 WebMVC 配置

## MQ 消費者架構

### 🚀 完全 Reactive MQ 消費者（新增）
使用 **Reactor RabbitMQ** 實現端到端非阻塞消息處理：

#### ReactivePeopleConsumer
- **並發控制**：`flatMap(concurrency=2)` 與 DB 連線池協調
- **背壓管理**：`prefetch=2`，避免耗盡 DB 連線
- **手動 ACK/NACK**：`AcknowledgableDelivery` 精確控制消息確認
- **隊列**：people-get-all, people-get-by-name, people-delete-all

#### ReactiveWeaponConsumer  
- **並發控制**：依操作類型調整（查詢 concurrency=2，寫入 concurrency=1）
- **隊列**：weapon-get-all, weapon-get-by-name, weapon-get-by-owner, weapon-save, weapon-exists

#### ReactiveAsyncResultConsumer
- **高優先級**：使用專用接收器，`prefetch=1` 快速處理
- **監控功能**：記錄成功/失敗指標，支援未來監控集成

### 異常處理責任鏈

```
GlobalExceptionHandler ──► ExceptionHandlerChain ──► 具體處理器
                                        │
                                        ├── ValidationExceptionHandler
                                        ├── BusinessExceptionHandler
                                        ├── DataIntegrityExceptionHandler
                                        ├── ResilienceExceptionHandler
                                        ├── IllegalArgumentExceptionHandler
                                        ├── RuntimeExceptionHandler
                                        └── DefaultExceptionHandler (兜底)
```

**責任鏈設計原則：**
- **優先級排序**：具體異常在前，通用異常在後
- **單一責任**：每個處理器只處理特定類型的異常
- **鏈式傳遞**：無法處理時自動傳遞給下一個處理器
- **可擴展性**：輕鬆添加新的異常處理器

### 🔄 傳統 MQ 消費者（保留，預設禁用）
- 使用 Spring AMQP `@RabbitListener`（同步監聽）
- 內部呼叫 reactive service，於邊界以 `.block()` 收斂結果
- 條件啟用：`spring.rabbitmq.legacy.enabled=true`

### MQ 設定對比

| 特性 | Reactor RabbitMQ | Spring AMQP |
|------|------------------|-------------|
| **I/O 模式** | 完全非阻塞 | 阻塞監聽 + reactive service |
| **背壓控制** | 原生支援 | 無 |
| **並發控制** | `flatMap(concurrency)` | `@RabbitListener(concurrency)` |
| **ACK 策略** | 手動 ACK/NACK | 自動 ACK |
| **資源效率** | 高（事件驅動） | 中（線程池） |
| **複雜度** | 中等 | 低 |

## 配置檔重點

### application.yml
```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/peoplesystem
    pool:
      max-size: 5  # 限制為個位數連線
  rabbitmq:
    enabled: true
    legacy.enabled: false  # 禁用傳統 MQ 消費者
```

### ReactiveRabbitMQConfig
- **連接工廠**：Reactive RabbitMQ ConnectionFactory
- **Sender/Receiver**：非阻塞消息發送/接收
- **並發策略**：與 R2DBC 連線池協調（prefetch=3, 保留 2 個連線作緩衝）

## 啟動與運行

### 本地執行
```bash
# 設定 local.properties
cp src/main/resources/env/local.properties.example src/main/resources/env/local.properties

# 啟動（預設使用 Reactive MQ）
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 如需啟用傳統 MQ 消費者
```bash
# 在 application-local.yml 中新增：
spring:
  rabbitmq:
    legacy:
      enabled: true
```

## API 文件
- **Swagger UI**: http://localhost:8081/ty_multiverse_consumer/swagger-ui.html
- **OpenAPI Docs**: http://localhost:8081/ty_multiverse_consumer/v3/api-docs

## 架構優勢

### 🚀 性能提升
- **非阻塞 I/O**：WebFlux + R2DBC + Reactor RabbitMQ 端到端非阻塞
- **連線池效率**：R2DBC 連線池更高效，支援更高併發
- **背壓友善**：自然的背壓機制，避免系統過載
- **記憶體使用**：事件驅動，降低記憶體消耗

### ✅ 遷移安全
- **API 契約不變**：客戶端無需修改
- **MQ 規格不變**：Producer 無需調整
- **雙棧支援**：可隨時回退到傳統 MQ 消費者
- **漸進式遷移**：新舊架構並存

### 🎯 運維友善
- **連線數控制**：嚴格遵循 K8s 環境限制
- **監控就緒**：內建指標記錄點，易於集成 Micrometer
- **日誌清晰**：結構化日誌，便於除錯
- **健康檢查**：R2DBC 連線健康監控

## 下一步建議

1. **性能測試**：高併發場景下的響應時間與資源使用
2. **監控集成**：添加 Micrometer 指標收集
3. **完全移除 Spring AMQP**：生產環境驗證後移除傳統依賴
4. **Producer 整合測試**：確保異步消息流完整性

---

## 📚 Reactive 架構設計概念指南

### 🎯 為什麼選擇 Reactive 架構？

**核心問題分析：**
- **資源瓶頸**：傳統阻塞 I/O 在高併發下造成線程浪費，DB 連線數限制讓問題更嚴重
- **延遲累積**：網路 I/O + DB I/O + 應用邏輯形成串聯延遲，無法有效並行處理
- **擴展限制**：線程池模式在 K8s 環境下無法有效利用有限資源

**Reactive 解決方案：**
- **事件驅動**：從"拉取數據"轉變為"數據推送"，減少等待時間
- **背壓控制**：上游生產者根據下游消費能力自動調整速度
- **資源共享**：少量線程處理大量併發請求，提高資源利用率

### 🔰 Mono 與 Flux 基礎教學

在進入 Reactive 架構設計之前，讓我們先掌握 Mono 與 Flux 的基本概念和寫法。

#### 1. Mono 基礎操作

**Mono<T>**：0-1 個元素的非同步結果，類似 Optional 的非同步版本

```java
// 創建 Mono
Mono<String> mono = Mono.just("Hello");                    // 直接創建
Mono<String> emptyMono = Mono.empty();                      // 空 Mono
Mono<String> errorMono = Mono.error(new RuntimeException()); // 錯誤 Mono

// 基本操作
mono.map(s -> s + " World")                                // 轉換： "Hello World"
    .flatMap(s -> Mono.just(s.toUpperCase()))             // 平坦化轉換： "HELLO WORLD"
    .filter(s -> s.length() > 5)                           // 過濾： 通過
    .defaultIfEmpty("Default")                             // 默認值
    .onErrorResume(e -> Mono.just("Error"))                // 錯誤恢復
    .subscribe(System.out::println);                       // 訂閱並消費
```

**常見使用場景：**
```java
// 單個數據庫查詢
Mono<User> findUserById(Long id) {
    return userRepository.findById(id);
}

// 單個外部 API 調用
Mono<String> callExternalApi(String param) {
    return webClient.get()
        .uri("/api/data/" + param)
        .retrieve()
        .bodyToMono(String.class);
}

// 異步計算結果
Mono<Integer> calculateAsync(int a, int b) {
    return Mono.fromCallable(() -> a + b);
}
```

#### 2. Flux 基礎操作

**Flux<T>**：0-N 個元素的非同步串流，類似 Stream 的非同步版本

```java
// 創建 Flux
Flux<String> flux = Flux.just("A", "B", "C");              // 多個元素
Flux<String> fromList = Flux.fromIterable(Arrays.asList("X", "Y", "Z")); // 從集合
Flux<Integer> range = Flux.range(1, 5);                    // 1, 2, 3, 4, 5

// 基本操作
flux.map(s -> s.toLowerCase())                             // 轉換每個元素
    .flatMap(s -> Flux.just(s, s + "!"))                   // 每個元素展開為多個
    .filter(s -> !s.contains("B"))                          // 過濾： "a", "c"
    .take(2)                                               // 只取前2個： "a", "c"
    .collectList()                                         // 收集為 List
    .subscribe(list -> System.out.println(list));          // 訂閱
```

**常見使用場景：**
```java
// 多個數據庫查詢
Flux<User> findAllUsers() {
    return userRepository.findAll();
}

// 批量處理
Flux<User> processUsers(List<User> users) {
    return Flux.fromIterable(users)
        .flatMap(user -> userRepository.save(user));
}

// 分頁查詢
Flux<User> findUsersWithPagination(int page, int size) {
    return userRepository.findAll()
        .skip((long) page * size)
        .take(size);
}
```

#### 3. Mono 與 Flux 互轉

```java
// Flux 轉 Mono
Mono<List<String>> listMono = flux.collectList();           // 收集所有元素為 List
Mono<String> firstMono = flux.next();                       // 只取第一個元素
Mono<Boolean> hasElements = flux.hasElements();             // 是否有元素

// Mono 轉 Flux
Flux<String> singleFlux = mono.flux();                      // 單元素 Flux
Flux<String> multipleFlux = mono.flatMapMany(s -> Flux.just(s, s)); // 多元素 Flux
```

#### 4. 錯誤處理

```java
// Mono 錯誤處理
Mono<String> result = service.callApi()
    .onErrorReturn("Default Value")                         // 返回默認值
    .onErrorResume(e -> Mono.just("Fallback"))              // 恢復邏輯
    .doOnError(e -> log.error("Error occurred", e))         // 側邊效果
    .retry(3);                                              // 重試 3 次

// Flux 錯誤處理
Flux<String> stream = service.getDataStream()
    .onErrorContinue((e, item) -> log.warn("Skip item: {}", item)) // 跳過錯誤項
    .doOnError(e -> log.error("Stream error", e));
```

#### 5. 組合操作

```java
// 並行執行
Mono.zip(mono1, mono2)
    .map(tuple -> tuple.getT1() + tuple.getT2());           // 等待兩個 Mono 完成

// 順序執行
mono1.flatMap(result1 ->
    mono2.map(result2 -> result1 + result2));              // mono2 依賴 mono1 結果

// 合併多個 Flux
Flux.merge(flux1, flux2, flux3)                             // 隨機順序合併
    .subscribe(System.out::println);

// 有序合併
Flux.concat(flux1, flux2, flux3)                            // 保持順序合併
    .subscribe(System.out::println);
```

#### 6. 測試 Reactive 程式碼

```java
@Test
void testMonoOperations() {
    StepVerifier.create(
        Mono.just("hello")
            .map(String::toUpperCase)
            .filter(s -> s.length() > 3)
    )
    .expectNext("HELLO")
    .verifyComplete();
}

@Test
void testFluxOperations() {
    StepVerifier.create(
        Flux.just("a", "b", "c")
            .map(String::toUpperCase)
            .collectList()
    )
    .expectNext(Arrays.asList("A", "B", "C"))
    .verifyComplete();
}
```

#### 7. 常見陷阱與最佳實踐

**陷阱 1：阻塞操作**
```java
// ❌ 錯誤：在 Reactive 鏈中阻塞
Mono<String> bad = Mono.fromCallable(() -> {
    Thread.sleep(1000); // 阻塞當前線程
    return "result";
});

// ✅ 正確：使用非阻塞操作
Mono<String> good = Mono.delay(Duration.ofSeconds(1))
    .map(i -> "result");
```

**陷阱 2：忽略訂閱**
```java
// ❌ 忘記訂閱，什麼都不會發生
Mono<String> mono = service.getData();
// 沒有 .subscribe()，不會執行

// ✅ 正確訂閱
mono.subscribe(
    data -> System.out.println(data),                      // onNext
    error -> System.err.println(error),                    // onError
    () -> System.out.println("Complete")                   // onComplete
);
```

**最佳實踐：**
- 總是記得訂閱 Reactive 串流
- 使用 `StepVerifier` 進行單元測試
- 避免在 Reactive 鏈中使用阻塞操作
- 善用操作符組合，而非嵌套回調

---

### 🌊 Reactive 編程模型的核心概念

#### 1. 資料流（Data Flow）
```java
// 傳統：同步方法呼叫
List<People> people = peopleService.getAllPeople();

// Reactive：非同步資料流
Flux<People> people = peopleService.getAllPeople();
```
**設計理念：**
- `Mono<T>`：0-1 個元素的非同步結果
- `Flux<T>`：0-N 個元素的非同步串流
- **推模型**：數據主動"推送"給訂閱者，而非被動"拉取"

#### 2. 背壓（Backpressure）
**問題：** 生產者速度 > 消費者速度，造成記憶體累積或系統崩潰

**Reactive 解決方案：**
```java
// 控制上游生產速度
.flatMap(this::processItem, 2)  // 最多同時處理 2 個項目

// 請求式拉取
.subscribe(subscriber, Long.MAX_VALUE);  // 請求無限多數據
```

**設計原則：**
- **請求-響應模式**：消費者主動請求數據量，生產者按需提供
- **流量控制**：自動調整生產速度，防止系統過載

#### 3. 非阻塞 I/O（Non-blocking I/O）
**傳統阻塞 I/O：**
```
線程 A ──► 發送請求 ──► 等待回應 ──► 處理結果
         ▲                                    │
         └────────────────────────────────────┘
                    線程被阻塞無法處理其他任務
```

**Reactive 非阻塞 I/O：**
```
線程 A ──► 發送請求 ──► 註冊回調 ──► 處理其他任務
         ▲                                    │
         └────────────────────────────────────┘
                    線程繼續處理其他請求，回調觸發時再處理結果
```

**設計優勢：**
- **線程複用**：單個線程處理多個 I/O 操作
- **並發提升**：在相同資源下支援更高併發
- **延遲降低**：消除阻塞等待時間

### 🏗️ 架構層次設計理念

#### 1. Web 層：Spring WebFlux
**設計決策：**
- **Netty 替代 Tomcat**：事件驅動的非阻塞服務器
- **Reactive Controller**：所有端點回傳 `Mono<ResponseEntity<T>>`
- **函數式編程**：使用 `map()`, `flatMap()`, `onErrorResume()` 組合操作

**架構優勢：**
- **零阻塞**：請求處理不佔用線程
- **自動擴展**：根據負載動態調整資源
- **背壓友好**：上游壓力會自動傳播到下游

#### 2. 資料層：R2DBC
**設計決策：**
- **驅動級非阻塞**：直接使用非阻塞資料庫協議
- **連線池限制**：`max-size=5` 嚴格控制資源使用
- **Reactive Transaction**：事務操作同樣非阻塞

**架構優勢：**
- **資源節省**：少量連線處理大量請求
- **延遲預測性**：消除連線等待時間
- **K8s 友好**：符合容器環境資源限制

#### 3. 消息層：Reactor RabbitMQ
**設計決策：**
- **串流消費**：消息作為連續事件流處理
- **手動 ACK/NACK**：精確控制消息確認時機
- **並發控制**：`flatMap(concurrency)` 動態調整處理速度

**架構優勢：**
- **端到端背壓**：從 MQ 到 DB 的完整壓力控制
- **故障恢復**：消息處理失敗自動重試和重新入隊
- **資源協調**：MQ 消費速度與 DB 處理能力同步

### 🔄 系統間的背壓傳播設計

```
HTTP 請求 ──► WebFlux ──► Service ──► R2DBC ──► DB
     ▲             ▲           ▲           ▲
     │             │           │           │
     └─────────────┴───────────┴───────────┴─────背壓傳播路徑
```

**設計原則：**
1. **HTTP 層背壓**：Netty 根據處理能力限制新請求接受
2. **應用層背壓**：Service 根據 DB 連線可用性控制處理速度
3. **資料層背壓**：R2DBC 根據連線池狀態限制並發查詢
4. **MQ 層背壓**：Reactor RabbitMQ 根據消費能力調整 prefetch

### 🎨 程式設計模式變革

#### 1. 從命令式到宣告式
```java
// 命令式：告訴電腦"如何做"
for (People person : peopleList) {
    person.setUpdatedAt(now);
    repository.save(person);
}

// 宣告式：告訴電腦"要做什麼"
Flux.fromIterable(peopleList)
    .map(person -> person.setUpdatedAt(now))
    .flatMap(repository::save)
```

#### 2. 從同步錯誤處理到非同步錯誤處理
```java
// 同步：try-catch 包圍
try {
    List<People> people = service.getAllPeople();
    return ResponseEntity.ok(people);
} catch (Exception e) {
    return ResponseEntity.internalServerError().build();
}

// 非同步：串流錯誤處理
return service.getAllPeople()
    .collectList()
    .map(people -> ResponseEntity.ok(people))
    .onErrorResume(error -> Mono.just(
        ResponseEntity.internalServerError().build()));
```

#### 3. 從線程池到事件循環
```java
// 線程池模式：每個請求一個線程
@RequestMapping("/api/people")
public Callable<ResponseEntity> getPeople() {
    return () -> service.getPeopleBlocking();
}

// 事件循環模式：事件驅動處理
@RequestMapping("/api/people")
public Mono<ResponseEntity> getPeople() {
    return service.getPeopleReactive()
        .collectList()
        .map(people -> ResponseEntity.ok(people));
}
```

### 📊 性能模型分析

#### 傳統阻塞架構的限制
```
請求數量 = 線程池大小 × 處理速度
         = 100線程 × 每秒10個請求
         = 1000 RPS
```

**問題：**
- 線程浪費：大多數時間在等待 I/O
- 記憶體壓力：每個線程需獨立棧空間
- 擴展困難：K8s 環境下線程數受限

#### Reactive 架構的優勢
```
請求數量 = 事件循環數量 × 事件處理速度 × 並發度
         = 4核心 × 每秒1000個事件 × 背壓控制
         = 10,000+ RPS（理論值）
```

**優勢：**
- **資源效率**：4個事件循環處理數千請求
- **動態擴展**：根據負載自動調整處理速度
- **故障隔離**：單個請求失敗不影響其他請求

### 🎯 Reactive 架構的成功關鍵

#### 1. 全棧一致性
**設計原則：** 整個應用棧都必須是 reactive 的
- ❌ 混合模式：WebFlux + JPA（會造成阻塞點）
- ✅ 純 Reactive：WebFlux + R2DBC + Reactor RabbitMQ

#### 2. 背壓策略設計
**設計原則：** 明確定義各層的背壓策略
```yaml
# DB 層：連線池限制
r2dbc:
  pool:
    max-size: 5

# MQ 層：prefetch 控制
consumeOptions:
  qos: 2

# 應用層：flatMap 並發控制
flatMap(concurrency=2)
```

#### 3. 錯誤處理重設計
**設計原則：** 從異常拋出到錯誤訊號傳播
```java
// 傳統：異常中斷執行
throw new BusinessException("資料不存在");

// Reactive：錯誤訊號傳播
return Mono.error(new BusinessException("資料不存在"));
```

#### 4. 資源管理重新思考
**設計原則：** 從資源競爭到資源協調
- **連線池**：從"搶連線"到"協調使用"
- **線程**：從"線程池"到"事件循環"
- **記憶體**：從"緩衝區"到"串流處理"

### 🚀 架構演進路徑

#### 階段 1：基礎 Reactive（已完成）
- WebFlux + R2DBC + Reactor RabbitMQ
- 基本背壓控制
- 端到端非阻塞

#### 階段 2：進階優化（建議）
- 智慧背壓：根據系統負載動態調整參數
- 熔斷模式：自動降級保護系統穩定性
- 分散式追蹤：全鏈路性能監控

#### 階段 3：架構升級（未來）
- 事件驅動架構：從請求-響應到事件驅動
- 響應式微服務：服務間的事件流通信
- 雲原生 Reactive：充分利用容器化優勢

### 💡 設計思維轉變

#### 從"同步思考"到"非同步思考"
```java
// 同步思考：線性執行
開始 → 執行任務A → 等待A完成 → 執行任務B → 結束

// 非同步思考：並行優化
開始 → 同時啟動任務A和任務B → 誰先完成就處理誰 → 結束
```

#### 從"資源管理"到"流量控制"
```java
// 資源管理：限制資源使用量
connectionPool.setMaxSize(5);

// 流量控制：協調生產消費節奏
.flatMap(this::process, maxConcurrency)
.onBackpressureBuffer(bufferSize)
```

#### 從"錯誤處理"到"恢復策略"
```java
// 錯誤處理：被動補救
try { doSomething(); } catch (Exception e) { handleError(); }

// 恢復策略：主動適應
doSomething()
    .retryWhen(Retry.backoff(maxAttempts, Duration.ofSeconds(1)))
    .onErrorResume(fallback::handle);
```

---

**🎉 Reactive 架構不僅是技術升級，更是系統設計思維的根本轉變！**