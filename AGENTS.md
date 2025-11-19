# TY Multiverse Consumer - Agent Guide

## 📁 文档组织规定

**重要**：所有非 `AGENTS.md` 和 `README.md` 的 Markdown 文档都必须放在项目的 `/docs` 目录下。

- ✅ **允许在根目录**：`AGENTS.md`、`README.md`
- ✅ **必须放在 `/docs`**：所有其他 `.md` 文件
- 📂 **文档目录结构**：`/docs/` 目录下可以创建子目录来组织相关文档

## Project Overview

TY Multiverse Consumer is a message-driven service that processes asynchronous tasks and handles background operations for the TY Multiverse system. It consumes messages from RabbitMQ, processes business logic, and integrates with other services via REST APIs and gRPC.

### Architecture
- **Framework**: Spring Boot 3.2.7 with Java 21
- **Message Queue**: RabbitMQ for async processing
- **Protocol**: REST API + Message consumers
- **Database**: PostgreSQL for state management
- **Integration**: Connects to Backend and Gateway services

### Key Components
- **Message Consumers**: Process async tasks and events
- **Card System**: Blackjack game logic and card management
- **State Management**: Application state persistence
- **Async Processing**: Background job execution
- **Service Integration**: Communicate with other TY Multiverse services

## Build and Test Commands

### Prerequisites
```bash
# Ensure common module is built first
cd ../ty-multiverse-common
./mvnw clean install

# Verify dependencies
mvn dependency:tree | grep ty-multiverse-common
```

### Build Commands
```bash
# Clean build
./mvnw clean compile

# Full build with tests
./mvnw clean compile test

# Package (creates JAR)
./mvnw package -DskipTests

# Run in development mode
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Test Commands
```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=CardServiceTest

# Run integration tests
./mvnw test -Pintegration

# Generate test report
./mvnw test surefire-report:report
```

### Development Workflow
```bash
# Start dependencies (RabbitMQ, Redis, etc.)
docker-compose -f docker-compose.dev.yml up -d

# Start consumer service
./mvnw spring-boot:run

# Monitor logs
tail -f target/surefire-reports/*.txt
```

## Code Style Guidelines

### Java Code Style
- **Language Level**: Java 21
- **Formatting**: Standard Java conventions with 4-space indentation
- **Naming**: camelCase for methods/variables, PascalCase for classes
- **Line Length**: Max 120 characters
- **Imports**: Organized by standard java, third party, then project packages

### Async/Event Handling Conventions
```java
// ✅ Good: Proper async handling
@Service
@RequiredArgsConstructor
public class CardEventConsumer {

    private final CardService cardService;
    private final ApplicationEventPublisher eventPublisher;

    @RabbitListener(queues = "card.events")
    public void handleCardEvent(CardEvent event) {
        try {
            cardService.processCardEvent(event);
            eventPublisher.publishEvent(new CardProcessedEvent(event.getId()));
        } catch (Exception e) {
            log.error("Failed to process card event: {}", event.getId(), e);
            // Implement retry logic or dead letter queue
        }
    }
}

// ❌ Avoid: Poor error handling
@RabbitListener public void handleEvent(Event e){service.process(e);}
```

### Package Structure
```
src/main/java/tw/com/tymultiverse/consumer/
├── config/           # Configuration classes
├── consumer/         # Message consumers
├── service/          # Business logic services
├── repository/       # Data access layer
└── dto/             # Data transfer objects
```

## Testing Instructions

### Unit Tests
- **Focus**: Test individual services and utilities
- **Mocking**: Use Mockito for external dependencies
- **Async Testing**: Use `@Async` and test completion
- **Message Testing**: Mock RabbitMQ message consumers

### Integration Tests
- **Scope**: Test complete message processing flows
- **Setup**: Use test containers for RabbitMQ
- **Verification**: Ensure messages are properly consumed and processed

### Test Data Setup
```java
@BeforeEach
void setUp() {
    // Setup test data
    cardRepository.deleteAll();
    cardRepository.save(testCard);
}

@Test
@DirtiesContext
void testMessageConsumer() {
    // Send test message
    rabbitTemplate.convertAndSend("card.events", testMessage);

    // Verify processing
    await().atMost(5, SECONDS)
           .until(() -> cardRepository.findById(testCard.getId()).isPresent());
}
```

## Security Considerations

### Message Security
- **Message Validation**: Validate all incoming message payloads
- **Origin Verification**: Ensure messages come from trusted sources
- **Poison Message Handling**: Implement dead letter queues for bad messages

### Service Integration Security
- **API Authentication**: Use proper authentication when calling other services
- **Token Management**: Securely manage service-to-service tokens
- **Network Security**: Use TLS for inter-service communication

### Data Protection
- **Sensitive Data**: Avoid logging sensitive information from messages
- **Encryption**: Encrypt sensitive data at rest and in transit
- **Access Control**: Implement proper authorization for state changes

## Additional Instructions

### Commit Message Guidelines
```bash
# Format: <type>(<scope>): <description>

feat(cards): implement blackjack game logic
fix(consumer): resolve message processing deadlock
docs(readme): update consumer configuration guide
test(async): add tests for async message processing
refactor(service): improve error handling in CardService
```

### Pull Request Process
1. **Feature Branches**: Use `feature/`, `fix/`, `refactor/` prefixes
2. **Small Changes**: Keep PRs focused and reviewable
3. **Testing**: Ensure all tests pass and add new tests for features
4. **Documentation**: Update README and API docs for changes

### Deployment Steps

#### Development Deployment
```bash
# 1. Start message queue
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# 2. Build and start consumer
./mvnw clean package -DskipTests
java -jar target/ty-multiverse-consumer.jar
```

#### Production Deployment
```bash
# 1. Build optimized JAR
./mvnw clean package -Pprod -DskipTests

# 2. Deploy with proper configuration
java -jar target/ty-multiverse-consumer.jar \
  --spring.profiles.active=prod \
  --rabbitmq.host=your-rabbitmq-host
```

### Message Queue Configuration
```yaml
# application.yml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: ${RABBITMQ_VHOST:/}

# Queue configuration
card:
  queue:
    events: card.events
    dead-letter: card.dead-letter
```

### Monitoring and Observability
- **Health Checks**: Implement readiness and liveness probes
- **Metrics**: Expose JVM and application metrics
- **Logging**: Structured logging with correlation IDs
- **Tracing**: Distributed tracing for message flows

### Troubleshooting
- **Message Backlog**: Monitor queue depths and processing rates
- **Consumer Lag**: Check for slow consumers or processing errors
- **Connection Issues**: Verify RabbitMQ connectivity and credentials
- **Dead Letter Queues**: Monitor and handle failed messages appropriately

### Performance Optimization
- **Batch Processing**: Process multiple messages in single transactions
- **Connection Pooling**: Configure optimal connection pool sizes
- **Concurrent Consumers**: Adjust based on system resources
- **Message Acknowledgment**: Use manual acknowledgment for reliability

### Environment Variables
```bash
# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# Service Integration
BACKEND_URL=http://localhost:8080
GATEWAY_URL=http://localhost:8082

# Application
SPRING_PROFILES_ACTIVE=dev
LOGGING_LEVEL_COM_TY=DEBUG
```
