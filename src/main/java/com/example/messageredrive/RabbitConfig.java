package com.example.messageredrive;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String MAIN_EXCHANGE_NAME = "main-exchange";
    public static final String MAIN_QUEUE_NAME = "main-queue";
    public static final String RETRY_QUEUE_NAME = "retry-queue";
    public static final String FAILED_QUEUE_NAME = "failed-queue";

    private static final Integer RETRY_TTL = 5000; // 5 seconds

    @Bean
    DirectExchange mainExchange() {
        return new DirectExchange(MAIN_EXCHANGE_NAME);
    }

    @Bean
    Queue mainQueue() {
        return QueueBuilder.durable(MAIN_QUEUE_NAME)
                .build();
    }

    @Bean
    Queue retryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MAIN_EXCHANGE_NAME);
        args.put("x-dead-letter-routing-key", MAIN_QUEUE_NAME);
        args.put("x-message-ttl", RETRY_TTL);
        return new Queue(RETRY_QUEUE_NAME, true, false, false, args);
    }

    @Bean
    Queue failedQueue() {
        return new Queue(FAILED_QUEUE_NAME, true);
    }

    @Bean
    Binding mainBinding(Queue mainQueue, DirectExchange mainExchange) {
        return BindingBuilder.bind(mainQueue).to(mainExchange).with(MAIN_QUEUE_NAME);
    }

    @Bean
    Binding retryBinding(Queue retryQueue, DirectExchange mainExchange) {
        return BindingBuilder.bind(retryQueue).to(mainExchange).with(RETRY_QUEUE_NAME);
    }

    @Bean
    Binding failedBinding(Queue failedQueue, DirectExchange mainExchange) {
        return BindingBuilder.bind(failedQueue).to(mainExchange).with(FAILED_QUEUE_NAME);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
