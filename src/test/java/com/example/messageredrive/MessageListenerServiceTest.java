package com.example.messageredrive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageListenerServiceTest {

    @InjectMocks
    private MessageListenerService messageListenerService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void whenProcessingFails_shouldSendToRetryQueue() {
        // Arrange
        Map<String, Object> payload = Collections.singletonMap("test", "value");
        Message message = new Message("{}".getBytes(), new MessageProperties());

        // Act
        messageListenerService.onMessage(message, payload);

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(
                eq(RabbitConfig.MAIN_EXCHANGE_NAME),
                eq(RabbitConfig.RETRY_QUEUE_NAME),
                messageCaptor.capture()
        );

        Message sentMessage = messageCaptor.getValue();
        assertEquals(1, sentMessage.getMessageProperties().getHeaders().get("x-retry-count"));
    }

    @Test
    void whenMaxRetriesReached_shouldSendToFailedQueue() {
        // Arrange
        Map<String, Object> payload = Collections.singletonMap("test", "value");
        MessageProperties properties = new MessageProperties();
        properties.setHeader("x-retry-count", 3); // MAX_RETRIES is 3
        Message message = new Message("{}".getBytes(), properties);

        // Act
        messageListenerService.onMessage(message, payload);

        // Assert
        verify(rabbitTemplate).send(
                eq(RabbitConfig.MAIN_EXCHANGE_NAME),
                eq(RabbitConfig.FAILED_QUEUE_NAME),
                any(Message.class)
        );
    }
}
