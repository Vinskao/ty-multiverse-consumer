package com.vinskao.ty_multiverse_consumer.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 業務事件（tymb-events stream）專用的 RabbitMQ 設定
 *
 * <p>與既有的 classic queue／RPC 設定完全分開，不修改任何現有 queue 名稱或型別。
 * 這裡的宣告參數必須與叢集上已建立的資源以及 backend 的 RabbitMQConfig 完全一致，
 * 否則 RabbitAdmin 會在啟動時 PRECONDITION_FAILED。</p>
 *
 * @author TY Backend Team
 * @since 2026-08
 */
@Configuration
public class EventRabbitMQConfig {

    private static final Logger logger = LoggerFactory.getLogger(EventRabbitMQConfig.class);

    public static final String TYMB_EVENT_EXCHANGE = "tymb-event-exchange";
    public static final String TYMB_EVENTS_STREAM = "tymb-events";
    public static final String TYMB_EVENTS_ROUTING_PATTERN = "event.#";

    /** Stream retention：保留 30 天 */
    private static final String EVENT_STREAM_MAX_AGE = "30D";
    /** Stream 容量上限：10 GiB */
    private static final long EVENT_STREAM_MAX_LENGTH_BYTES = 10_737_418_240L;

    @Bean
    public TopicExchange tymbEventExchange() {
        return ExchangeBuilder.topicExchange(TYMB_EVENT_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue tymbEventsStream() {
        return QueueBuilder.durable(TYMB_EVENTS_STREAM)
                .stream()
                .withArgument("x-max-age", EVENT_STREAM_MAX_AGE)
                .withArgument("x-max-length-bytes", EVENT_STREAM_MAX_LENGTH_BYTES)
                .build();
    }

    @Bean
    public Binding tymbEventsBinding(Queue tymbEventsStream, TopicExchange tymbEventExchange) {
        return BindingBuilder.bind(tymbEventsStream)
                .to(tymbEventExchange)
                .with(TYMB_EVENTS_ROUTING_PATTERN);
    }

    /**
     * 事件發布專用 RabbitTemplate
     *
     * <p>與預設的 rabbitTemplate 分開，原因：</p>
     * <ul>
     *   <li>開啟 mandatory + returns，讓「路由不到任何 queue」被當成失敗而不是默默丟掉。</li>
     *   <li>不套用 Jackson2JsonMessageConverter／ClassMapper——outbox 裡存的 JSON 會原樣送出，
     *       稽核到的內容與發布出去的內容完全一致，也不會多帶 __TypeId__ header。</li>
     * </ul>
     *
     * <p>publisher confirm 由 {@code spring.rabbitmq.publisher-confirm-type=correlated} 開啟，
     * 由 {@link BusinessEventPublisher} 逐筆等待 ack。</p>
     */
    @Bean
    public RabbitTemplate eventRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        template.setReturnsCallback(returned -> logger.warn(
                "⚠️ 事件無法路由: routingKey={}, replyText={}, messageId={}",
                returned.getRoutingKey(),
                returned.getReplyText(),
                returned.getMessage().getMessageProperties().getMessageId()));
        logger.info("✅ eventRabbitTemplate 已建立（mandatory + publisher confirm）");
        return template;
    }
}
