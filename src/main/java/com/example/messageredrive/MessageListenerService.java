package com.example.messageredrive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MessageListenerService {

    private static final Logger log = LoggerFactory.getLogger(MessageListenerService.class);
    private static final int MAX_RETRIES = 3;
    private static final String RETRY_HEADER = "x-retry-count";

    private final RabbitTemplate rabbitTemplate;

    public MessageListenerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitConfig.MAIN_QUEUE_NAME)
    public void onMessage(Message message, Map<String, Object> payload) {
        log.info("Received message from main queue: {}", payload);
        try {
            // Simulate a processing failure
            throw new RuntimeException("Simulating processing failure");
        } catch (Exception e) {
            log.warn("Processing failed. Preparing for retry...");
            Integer retryCount = (Integer) message.getMessageProperties().getHeaders().getOrDefault(RETRY_HEADER, 0);

            if (retryCount >= MAX_RETRIES) {
                log.error("Max retries reached. Sending message to failed queue.");
                rabbitTemplate.send(RabbitConfig.MAIN_EXCHANGE_NAME, RabbitConfig.FAILED_QUEUE_NAME, message);
            } else {
                log.info("Sending message to retry queue. Retry count: {}", retryCount + 1);
                message.getMessageProperties().setHeader(RETRY_HEADER, retryCount + 1);
                rabbitTemplate.send(RabbitConfig.MAIN_EXCHANGE_NAME, RabbitConfig.RETRY_QUEUE_NAME, message);
            }
        }
    }
}
