package com.vinskao.ty_multiverse_consumer.config;

import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import reactor.rabbitmq.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Reactive RabbitMQ 配置類
 * 
 * 提供完全 reactive 的 RabbitMQ 連接和消費者
 * 與 DB 連線池 (max-size=5) 協調的並發控制
 */
@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.reactive.enabled", havingValue = "true", matchIfMissing = true)
public class ReactiveRabbitMQConfig {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveRabbitMQConfig.class);

    @Value("${spring.rabbitmq.host:localhost}")
    private String host;

    @Value("${spring.rabbitmq.port:5672}")
    private int port;

    @Value("${spring.rabbitmq.username:admin}")
    private String username;

    @Value("${spring.rabbitmq.password:admin123}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host:/}")
    private String virtualHost;

    /**
     * Reactive RabbitMQ 連接工廠
     */
    @Bean
    public ConnectionFactory reactiveConnectionFactory() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);

        // 連接超時配置 - 解決網絡延遲導致的 TimeoutException
        connectionFactory.setConnectionTimeout(30000); // 連接超時 30 秒（默認 60 秒）
        connectionFactory.setHandshakeTimeout(30000); // 握手超時 30 秒（默認 10 秒）
        connectionFactory.setChannelRpcTimeout(30000); // Channel RPC 超時 30 秒

        // 連接池配置 - 與 R2DBC 連線池協調
        connectionFactory.setRequestedChannelMax(10);
        connectionFactory.setRequestedFrameMax(131072);
        connectionFactory.setRequestedHeartbeat(60);

        // 啟用自動恢復
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setNetworkRecoveryInterval(5000);
        connectionFactory.setTopologyRecoveryEnabled(true); // 啟用拓撲恢復（隊列、交換機等）

        logger.info(
                "✅ 配置 Reactive RabbitMQ 連接工廠: host={}, port={}, virtualHost={}, connectionTimeout=30s, handshakeTimeout=30s",
                host, port, virtualHost);

        return connectionFactory;
    }

    /**
     * Reactive RabbitMQ Sender
     */
    @Bean
    public Sender reactiveSender(ConnectionFactory reactiveConnectionFactory) {
        SenderOptions senderOptions = new SenderOptions()
                .connectionFactory(reactiveConnectionFactory)
                .resourceManagementChannelMono(
                        // 使用專用通道進行資源管理
                        Mono.fromCallable(() -> reactiveConnectionFactory.newConnection().createChannel())
                                .cache());

        Sender sender = RabbitFlux.createSender(senderOptions);
        logger.info("✅ 創建 Reactive RabbitMQ Sender");

        return sender;
    }

    /**
     * Reactive RabbitMQ Receiver - 主要消費者
     * 
     * 配置 prefetch=3，與 DB 連線池 (max-size=5) 協調
     * 保留 2 個連線作為緩衝
     */
    @Bean
    @Lazy // 延遲創建，確保連接完全建立後再創建 Receiver
    public Receiver reactiveReceiver(ConnectionFactory reactiveConnectionFactory) {
        ReceiverOptions receiverOptions = new ReceiverOptions()
                .connectionFactory(reactiveConnectionFactory)
                // 關鍵：設定訂閱排程器；prefetch 請在各 consumer 的 consumeManualAck 時設定
                .connectionSubscriptionScheduler(Schedulers.boundedElastic());

        Receiver receiver = RabbitFlux.createReceiver(receiverOptions);
        logger.info("✅ 創建 Reactive RabbitMQ Receiver (prefetch=3)");

        return receiver;
    }

    /**
     * Reactive RabbitMQ Receiver - 高優先級消費者
     * 
     * 用於重要或輕量級操作，prefetch=1
     */
    @Bean
    @Lazy // 延遲創建，確保連接完全建立後再創建 Receiver
    public Receiver reactiveReceiverHighPriority(ConnectionFactory reactiveConnectionFactory) {
        ReceiverOptions receiverOptions = new ReceiverOptions()
                .connectionFactory(reactiveConnectionFactory)
                .connectionSubscriptionScheduler(Schedulers.boundedElastic());

        Receiver receiver = RabbitFlux.createReceiver(receiverOptions);
        logger.info("✅ 創建高優先級 Reactive RabbitMQ Receiver (prefetch=1)");

        return receiver;
    }
}
