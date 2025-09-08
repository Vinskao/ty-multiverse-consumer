package com.vinskao.ty_multiverse_consumer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.core.task.TaskExecutor;

@Configuration
@EnableRabbit
public class RabbitMQConfig {
    
    // 在開發環境中，如果隊列已存在但配置不一致，可以設置為 true 來清理隊列
    private static final boolean CLEAR_QUEUES_ON_STARTUP = false;

    // 添加調試日誌
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

    // People 隊列名稱
    public static final String PEOPLE_INSERT_QUEUE = "people-insert";
    public static final String PEOPLE_UPDATE_QUEUE = "people-update";
    public static final String PEOPLE_INSERT_MULTIPLE_QUEUE = "people-insert-multiple";
    public static final String PEOPLE_GET_ALL_QUEUE = "people-get-all";
    public static final String PEOPLE_GET_BY_NAME_QUEUE = "people-get-by-name";
    public static final String PEOPLE_DELETE_QUEUE = "people-delete";
    public static final String PEOPLE_DELETE_ALL_QUEUE = "people-delete-all";
    public static final String PEOPLE_DAMAGE_CALCULATION_QUEUE = "people-damage-calculation";

    // Weapon 隊列名稱
    public static final String WEAPON_GET_ALL_QUEUE = "weapon-get-all";
    public static final String WEAPON_GET_BY_NAME_QUEUE = "weapon-get-by-name";
    public static final String WEAPON_GET_BY_OWNER_QUEUE = "weapon-get-by-owner";
    public static final String WEAPON_SAVE_QUEUE = "weapon-save";
    public static final String WEAPON_INSERT_MULTIPLE_QUEUE = "weapon-insert-multiple";
    public static final String WEAPON_DELETE_QUEUE = "weapon-delete";
    public static final String WEAPON_DELETE_ALL_QUEUE = "weapon-delete-all";
    public static final String WEAPON_EXISTS_QUEUE = "weapon-exists";
    public static final String WEAPON_UPDATE_ATTRIBUTES_QUEUE = "weapon-update-attributes";
    public static final String WEAPON_UPDATE_BASE_DAMAGE_QUEUE = "weapon-update-base-damage";

    // 交換機名稱
    public static final String MAIN_EXCHANGE = "tymb-exchange";
    public static final String PEOPLE_RESPONSE_EXCHANGE = "people-response";
    public static final String WEAPON_RESPONSE_EXCHANGE = "weapon-response";

    // 路由鍵
    public static final String PEOPLE_INSERT_ROUTING_KEY = "people.insert";
    public static final String PEOPLE_UPDATE_ROUTING_KEY = "people.update";
    public static final String PEOPLE_INSERT_MULTIPLE_ROUTING_KEY = "people.insert.multiple";
    public static final String PEOPLE_GET_ALL_ROUTING_KEY = "people.get.all";
    public static final String PEOPLE_GET_BY_NAME_ROUTING_KEY = "people.get.by.name";
    public static final String PEOPLE_DELETE_ROUTING_KEY = "people.delete";
    public static final String PEOPLE_DELETE_ALL_ROUTING_KEY = "people.delete.all";
    public static final String PEOPLE_DAMAGE_CALCULATION_ROUTING_KEY = "people.damage.calculation";

    public static final String WEAPON_GET_ALL_ROUTING_KEY = "weapon.get.all";
    public static final String WEAPON_GET_BY_NAME_ROUTING_KEY = "weapon.get.by.name";
    public static final String WEAPON_GET_BY_OWNER_ROUTING_KEY = "weapon.get.by.owner";
    public static final String WEAPON_SAVE_ROUTING_KEY = "weapon.save";
    public static final String WEAPON_INSERT_MULTIPLE_ROUTING_KEY = "weapon.insert.multiple";
    public static final String WEAPON_DELETE_ROUTING_KEY = "weapon.delete";
    public static final String WEAPON_DELETE_ALL_ROUTING_KEY = "weapon.delete.all";
    public static final String WEAPON_EXISTS_ROUTING_KEY = "weapon.exists";
    public static final String WEAPON_UPDATE_ATTRIBUTES_ROUTING_KEY = "weapon.update.attributes";
    public static final String WEAPON_UPDATE_BASE_DAMAGE_ROUTING_KEY = "weapon.update.base.damage";
    
    // 回傳路由鍵
    public static final String PEOPLE_GET_ALL_RESPONSE_ROUTING_KEY = "people.get-all.response";
    public static final String PEOPLE_RESPONSE_ROUTING_KEY = "people.response";
    public static final String WEAPON_RESPONSE_ROUTING_KEY = "weapon.response";
    
    // 回傳隊列名稱
    public static final String PEOPLE_RESPONSE_QUEUE = "people.response.queue";
    public static final String WEAPON_RESPONSE_QUEUE = "weapon.response.queue";
    
    // 異步結果隊列
    public static final String ASYNC_RESULT_QUEUE = "async-result";
    public static final String ASYNC_RESULT_ROUTING_KEY = "async.result";

    // 創建隊列
    @Bean
    public Queue peopleGetAllQueue() {
        return QueueBuilder.durable(PEOPLE_GET_ALL_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    // People 隊列
    @Bean
    public Queue peopleInsertQueue() {
        return QueueBuilder.durable(PEOPLE_INSERT_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue peopleUpdateQueue() {
        return QueueBuilder.durable(PEOPLE_UPDATE_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue peopleInsertMultipleQueue() {
        return QueueBuilder.durable(PEOPLE_INSERT_MULTIPLE_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue peopleGetByNameQueue() {
        return QueueBuilder.durable(PEOPLE_GET_BY_NAME_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue peopleDeleteQueue() {
        return QueueBuilder.durable(PEOPLE_DELETE_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue peopleDeleteAllQueue() {
        return QueueBuilder.durable(PEOPLE_DELETE_ALL_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue peopleDamageCalculationQueue() {
        return QueueBuilder.durable(PEOPLE_DAMAGE_CALCULATION_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }
    

    
    // 回傳隊列
    @Bean
    public Queue peopleResponseQueue() {
        return QueueBuilder.durable(PEOPLE_RESPONSE_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }
    
    @Bean
    public Queue weaponResponseQueue() {
        return QueueBuilder.durable(WEAPON_RESPONSE_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }
    
    /**
     * 異步結果隊列
     */
    @Bean
    public Queue asyncResultQueue() {
        Queue queue = QueueBuilder.durable(ASYNC_RESULT_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
        logger.info("✅ 創建異步結果隊列: {}", ASYNC_RESULT_QUEUE);
        return queue;
    }

    // Weapon 隊列
    @Bean
    public Queue weaponGetAllQueue() {
        return QueueBuilder.durable(WEAPON_GET_ALL_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue weaponGetByNameQueue() {
        return QueueBuilder.durable(WEAPON_GET_BY_NAME_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue weaponGetByOwnerQueue() {
        return QueueBuilder.durable(WEAPON_GET_BY_OWNER_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue weaponSaveQueue() {
        return QueueBuilder.durable(WEAPON_SAVE_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue weaponInsertMultipleQueue() {
        return QueueBuilder.durable(WEAPON_INSERT_MULTIPLE_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue weaponDeleteQueue() {
        return QueueBuilder.durable(WEAPON_DELETE_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue weaponDeleteAllQueue() {
        return QueueBuilder.durable(WEAPON_DELETE_ALL_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue weaponExistsQueue() {
        return QueueBuilder.durable(WEAPON_EXISTS_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue weaponUpdateAttributesQueue() {
        return QueueBuilder.durable(WEAPON_UPDATE_ATTRIBUTES_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    @Bean
    public Queue weaponUpdateBaseDamageQueue() {
        return QueueBuilder.durable(WEAPON_UPDATE_BASE_DAMAGE_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL
                .build();
    }

    // 創建交換機
    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(MAIN_EXCHANGE);
    }
    
    @Bean
    public DirectExchange peopleResponseExchange() {
        return new DirectExchange(PEOPLE_RESPONSE_EXCHANGE);
    }
    
    @Bean
    public DirectExchange weaponResponseExchange() {
        return new DirectExchange(WEAPON_RESPONSE_EXCHANGE);
    }

    // 綁定 People 隊列到交換機
    @Bean
    public Binding peopleInsertBinding() {
        return BindingBuilder.bind(peopleInsertQueue())
                .to(mainExchange())
                .with(PEOPLE_INSERT_ROUTING_KEY);
    }

    @Bean
    public Binding peopleUpdateBinding() {
        return BindingBuilder.bind(peopleUpdateQueue())
                .to(mainExchange())
                .with(PEOPLE_UPDATE_ROUTING_KEY);
    }

    @Bean
    public Binding peopleInsertMultipleBinding() {
        return BindingBuilder.bind(peopleInsertMultipleQueue())
                .to(mainExchange())
                .with(PEOPLE_INSERT_MULTIPLE_ROUTING_KEY);
    }

    @Bean
    public Binding peopleGetAllBinding() {
        return BindingBuilder.bind(peopleGetAllQueue())
                .to(mainExchange())
                .with(PEOPLE_GET_ALL_ROUTING_KEY);
    }

    @Bean
    public Binding peopleGetByNameBinding() {
        return BindingBuilder.bind(peopleGetByNameQueue())
                .to(mainExchange())
                .with(PEOPLE_GET_BY_NAME_ROUTING_KEY);
    }

    @Bean
    public Binding peopleDeleteBinding() {
        return BindingBuilder.bind(peopleDeleteQueue())
                .to(mainExchange())
                .with(PEOPLE_DELETE_ROUTING_KEY);
    }

    @Bean
    public Binding peopleDeleteAllBinding() {
        return BindingBuilder.bind(peopleDeleteAllQueue())
                .to(mainExchange())
                .with(PEOPLE_DELETE_ALL_ROUTING_KEY);
    }

    @Bean
    public Binding peopleDamageCalculationBinding() {
        return BindingBuilder.bind(peopleDamageCalculationQueue())
                .to(mainExchange())
                .with(PEOPLE_DAMAGE_CALCULATION_ROUTING_KEY);
    }
    

    
    // 綁定回傳隊列到回傳交換機
    @Bean
    public Binding peopleResponseBinding() {
        return BindingBuilder.bind(peopleResponseQueue())
                .to(peopleResponseExchange())
                .with(PEOPLE_GET_ALL_RESPONSE_ROUTING_KEY);
    }
    
    @Bean
    public Binding peopleResponseBinding2() {
        return BindingBuilder.bind(peopleResponseQueue())
                .to(peopleResponseExchange())
                .with(PEOPLE_RESPONSE_ROUTING_KEY);
    }
    
    @Bean
    public Binding weaponResponseBinding() {
        return BindingBuilder.bind(weaponResponseQueue())
                .to(weaponResponseExchange())
                .with(WEAPON_RESPONSE_ROUTING_KEY);
    }
    
    /**
     * 綁定異步結果隊列到交換機
     */
    @Bean
    public Binding asyncResultBinding() {
        Binding binding = BindingBuilder.bind(asyncResultQueue())
                .to(mainExchange())
                .with(ASYNC_RESULT_ROUTING_KEY);
        logger.info("✅ 創建異步結果綁定: 交換機={}, 路由鍵={}, 隊列={}",
                   MAIN_EXCHANGE, ASYNC_RESULT_ROUTING_KEY, ASYNC_RESULT_QUEUE);
        return binding;
    }

    // 綁定 Weapon 隊列到交換機
    @Bean
    public Binding weaponGetAllBinding() {
        return BindingBuilder.bind(weaponGetAllQueue())
                .to(mainExchange())
                .with(WEAPON_GET_ALL_ROUTING_KEY);
    }

    @Bean
    public Binding weaponGetByNameBinding() {
        return BindingBuilder.bind(weaponGetByNameQueue())
                .to(mainExchange())
                .with(WEAPON_GET_BY_NAME_ROUTING_KEY);
    }

    @Bean
    public Binding weaponGetByOwnerBinding() {
        return BindingBuilder.bind(weaponGetByOwnerQueue())
                .to(mainExchange())
                .with(WEAPON_GET_BY_OWNER_ROUTING_KEY);
    }

    @Bean
    public Binding weaponSaveBinding() {
        return BindingBuilder.bind(weaponSaveQueue())
                .to(mainExchange())
                .with(WEAPON_SAVE_ROUTING_KEY);
    }

    @Bean
    public Binding weaponInsertMultipleBinding() {
        return BindingBuilder.bind(weaponInsertMultipleQueue())
                .to(mainExchange())
                .with(WEAPON_INSERT_MULTIPLE_ROUTING_KEY);
    }

    @Bean
    public Binding weaponDeleteBinding() {
        return BindingBuilder.bind(weaponDeleteQueue())
                .to(mainExchange())
                .with(WEAPON_DELETE_ROUTING_KEY);
    }

    @Bean
    public Binding weaponDeleteAllBinding() {
        return BindingBuilder.bind(weaponDeleteAllQueue())
                .to(mainExchange())
                .with(WEAPON_DELETE_ALL_ROUTING_KEY);
    }

    @Bean
    public Binding weaponExistsBinding() {
        return BindingBuilder.bind(weaponExistsQueue())
                .to(mainExchange())
                .with(WEAPON_EXISTS_ROUTING_KEY);
    }

    @Bean
    public Binding weaponUpdateAttributesBinding() {
        return BindingBuilder.bind(weaponUpdateAttributesQueue())
                .to(mainExchange())
                .with(WEAPON_UPDATE_ATTRIBUTES_ROUTING_KEY);
    }

    @Bean
    public Binding weaponUpdateBaseDamageBinding() {
        return BindingBuilder.bind(weaponUpdateBaseDamageQueue())
                .to(mainExchange())
                .with(WEAPON_UPDATE_BASE_DAMAGE_ROUTING_KEY);
    }

    // 配置消息轉換器
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 配置 RabbitTemplate
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * 使用虛擬線程的 RabbitListener 容器工廠
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            TaskExecutor applicationTaskExecutor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setTaskExecutor(applicationTaskExecutor);
        return factory;
    }
}
