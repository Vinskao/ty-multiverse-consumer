package com.vinskao.ty_multiverse_consumer.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // People 隊列名稱
    public static final String PEOPLE_INSERT_QUEUE = "people.insert.queue";
    public static final String PEOPLE_UPDATE_QUEUE = "people.update.queue";
    public static final String PEOPLE_INSERT_MULTIPLE_QUEUE = "people.insert.multiple.queue";
    public static final String PEOPLE_GET_ALL_QUEUE = "people.get.all.queue";
    public static final String PEOPLE_GET_BY_NAME_QUEUE = "people.get.by.name.queue";
    public static final String PEOPLE_DELETE_QUEUE = "people.delete.queue";
    public static final String PEOPLE_DAMAGE_CALCULATION_QUEUE = "people.damage.calculation.queue";

    // Weapon 隊列名稱
    public static final String WEAPON_GET_ALL_QUEUE = "weapon.get.all.queue";
    public static final String WEAPON_GET_BY_NAME_QUEUE = "weapon.get.by.name.queue";
    public static final String WEAPON_GET_BY_OWNER_QUEUE = "weapon.get.by.owner.queue";
    public static final String WEAPON_SAVE_QUEUE = "weapon.save.queue";
    public static final String WEAPON_DELETE_QUEUE = "weapon.delete.queue";
    public static final String WEAPON_DELETE_ALL_QUEUE = "weapon.delete.all.queue";
    public static final String WEAPON_EXISTS_QUEUE = "weapon.exists.queue";
    public static final String WEAPON_UPDATE_ATTRIBUTES_QUEUE = "weapon.update.attributes.queue";
    public static final String WEAPON_UPDATE_BASE_DAMAGE_QUEUE = "weapon.update.base.damage.queue";

    // 交換機名稱
    public static final String PEOPLE_EXCHANGE = "people.exchange";
    public static final String WEAPON_EXCHANGE = "weapon.exchange";
    public static final String PEOPLE_RESPONSE_EXCHANGE = "people-response";
    public static final String WEAPON_RESPONSE_EXCHANGE = "weapon-response";

    // 路由鍵
    public static final String PEOPLE_INSERT_ROUTING_KEY = "people.insert";
    public static final String PEOPLE_UPDATE_ROUTING_KEY = "people.update";
    public static final String PEOPLE_INSERT_MULTIPLE_ROUTING_KEY = "people.insert.multiple";
    public static final String PEOPLE_GET_ALL_ROUTING_KEY = "people.get.all";
    public static final String PEOPLE_GET_BY_NAME_ROUTING_KEY = "people.get.by.name";
    public static final String PEOPLE_DELETE_ROUTING_KEY = "people.delete";
    public static final String PEOPLE_DAMAGE_CALCULATION_ROUTING_KEY = "people.damage.calculation";

    public static final String WEAPON_GET_ALL_ROUTING_KEY = "weapon.get.all";
    public static final String WEAPON_GET_BY_NAME_ROUTING_KEY = "weapon.get.by.name";
    public static final String WEAPON_GET_BY_OWNER_ROUTING_KEY = "weapon.get.by.owner";
    public static final String WEAPON_SAVE_ROUTING_KEY = "weapon.save";
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

    // 創建隊列
    @Bean
    public Queue peopleGetAllQueue() {
        return new Queue(PEOPLE_GET_ALL_QUEUE, true);
    }

    // People 隊列
    @Bean
    public Queue peopleInsertQueue() {
        return new Queue(PEOPLE_INSERT_QUEUE, true);
    }

    @Bean
    public Queue peopleUpdateQueue() {
        return new Queue(PEOPLE_UPDATE_QUEUE, true);
    }

    @Bean
    public Queue peopleInsertMultipleQueue() {
        return new Queue(PEOPLE_INSERT_MULTIPLE_QUEUE, true);
    }

    @Bean
    public Queue peopleGetByNameQueue() {
        return new Queue(PEOPLE_GET_BY_NAME_QUEUE, true);
    }

    @Bean
    public Queue peopleDeleteQueue() {
        return new Queue(PEOPLE_DELETE_QUEUE, true);
    }

    @Bean
    public Queue peopleDamageCalculationQueue() {
        return new Queue(PEOPLE_DAMAGE_CALCULATION_QUEUE, true);
    }
    
    // 回傳隊列
    @Bean
    public Queue peopleResponseQueue() {
        return new Queue(PEOPLE_RESPONSE_QUEUE, true);
    }
    
    @Bean
    public Queue weaponResponseQueue() {
        return new Queue(WEAPON_RESPONSE_QUEUE, true);
    }

    // Weapon 隊列
    @Bean
    public Queue weaponGetAllQueue() {
        return new Queue(WEAPON_GET_ALL_QUEUE, true);
    }

    @Bean
    public Queue weaponGetByNameQueue() {
        return new Queue(WEAPON_GET_BY_NAME_QUEUE, true);
    }

    @Bean
    public Queue weaponGetByOwnerQueue() {
        return new Queue(WEAPON_GET_BY_OWNER_QUEUE, true);
    }

    @Bean
    public Queue weaponSaveQueue() {
        return new Queue(WEAPON_SAVE_QUEUE, true);
    }

    @Bean
    public Queue weaponDeleteQueue() {
        return new Queue(WEAPON_DELETE_QUEUE, true);
    }

    @Bean
    public Queue weaponDeleteAllQueue() {
        return new Queue(WEAPON_DELETE_ALL_QUEUE, true);
    }

    @Bean
    public Queue weaponExistsQueue() {
        return new Queue(WEAPON_EXISTS_QUEUE, true);
    }

    @Bean
    public Queue weaponUpdateAttributesQueue() {
        return new Queue(WEAPON_UPDATE_ATTRIBUTES_QUEUE, true);
    }

    @Bean
    public Queue weaponUpdateBaseDamageQueue() {
        return new Queue(WEAPON_UPDATE_BASE_DAMAGE_QUEUE, true);
    }

    // 創建交換機
    @Bean
    public DirectExchange peopleExchange() {
        return new DirectExchange(PEOPLE_EXCHANGE);
    }

    @Bean
    public DirectExchange weaponExchange() {
        return new DirectExchange(WEAPON_EXCHANGE);
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
                .to(peopleExchange())
                .with(PEOPLE_INSERT_ROUTING_KEY);
    }

    @Bean
    public Binding peopleUpdateBinding() {
        return BindingBuilder.bind(peopleUpdateQueue())
                .to(peopleExchange())
                .with(PEOPLE_UPDATE_ROUTING_KEY);
    }

    @Bean
    public Binding peopleInsertMultipleBinding() {
        return BindingBuilder.bind(peopleInsertMultipleQueue())
                .to(peopleExchange())
                .with(PEOPLE_INSERT_MULTIPLE_ROUTING_KEY);
    }

    @Bean
    public Binding peopleGetAllBinding() {
        return BindingBuilder.bind(peopleGetAllQueue())
                .to(peopleExchange())
                .with(PEOPLE_GET_ALL_ROUTING_KEY);
    }

    @Bean
    public Binding peopleGetByNameBinding() {
        return BindingBuilder.bind(peopleGetByNameQueue())
                .to(peopleExchange())
                .with(PEOPLE_GET_BY_NAME_ROUTING_KEY);
    }

    @Bean
    public Binding peopleDeleteBinding() {
        return BindingBuilder.bind(peopleDeleteQueue())
                .to(peopleExchange())
                .with(PEOPLE_DELETE_ROUTING_KEY);
    }

    @Bean
    public Binding peopleDamageCalculationBinding() {
        return BindingBuilder.bind(peopleDamageCalculationQueue())
                .to(peopleExchange())
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

    // 綁定 Weapon 隊列到交換機
    @Bean
    public Binding weaponGetAllBinding() {
        return BindingBuilder.bind(weaponGetAllQueue())
                .to(weaponExchange())
                .with(WEAPON_GET_ALL_ROUTING_KEY);
    }

    @Bean
    public Binding weaponGetByNameBinding() {
        return BindingBuilder.bind(weaponGetByNameQueue())
                .to(weaponExchange())
                .with(WEAPON_GET_BY_NAME_ROUTING_KEY);
    }

    @Bean
    public Binding weaponGetByOwnerBinding() {
        return BindingBuilder.bind(weaponGetByOwnerQueue())
                .to(weaponExchange())
                .with(WEAPON_GET_BY_OWNER_ROUTING_KEY);
    }

    @Bean
    public Binding weaponSaveBinding() {
        return BindingBuilder.bind(weaponSaveQueue())
                .to(weaponExchange())
                .with(WEAPON_SAVE_ROUTING_KEY);
    }

    @Bean
    public Binding weaponDeleteBinding() {
        return BindingBuilder.bind(weaponDeleteQueue())
                .to(weaponExchange())
                .with(WEAPON_DELETE_ROUTING_KEY);
    }

    @Bean
    public Binding weaponDeleteAllBinding() {
        return BindingBuilder.bind(weaponDeleteAllQueue())
                .to(weaponExchange())
                .with(WEAPON_DELETE_ALL_ROUTING_KEY);
    }

    @Bean
    public Binding weaponExistsBinding() {
        return BindingBuilder.bind(weaponExistsQueue())
                .to(weaponExchange())
                .with(WEAPON_EXISTS_ROUTING_KEY);
    }

    @Bean
    public Binding weaponUpdateAttributesBinding() {
        return BindingBuilder.bind(weaponUpdateAttributesQueue())
                .to(weaponExchange())
                .with(WEAPON_UPDATE_ATTRIBUTES_ROUTING_KEY);
    }

    @Bean
    public Binding weaponUpdateBaseDamageBinding() {
        return BindingBuilder.bind(weaponUpdateBaseDamageQueue())
                .to(weaponExchange())
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
}
