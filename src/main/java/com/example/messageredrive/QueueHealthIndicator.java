package com.example.messageredrive;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class QueueHealthIndicator implements HealthIndicator {

    private final AmqpAdmin amqpAdmin;

    public QueueHealthIndicator(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    @Override
    public Health health() {
        try {
            Properties mainQueueProps = amqpAdmin.getQueueProperties(RabbitConfig.MAIN_QUEUE_NAME);
            Properties retryQueueProps = amqpAdmin.getQueueProperties(RabbitConfig.RETRY_QUEUE_NAME);
            Properties failedQueueProps = amqpAdmin.getQueueProperties(RabbitConfig.FAILED_QUEUE_NAME);

            int mainQueueCount = (int) mainQueueProps.get("QUEUE_MESSAGE_COUNT");
            int retryQueueCount = (int) retryQueueProps.get("QUEUE_MESSAGE_COUNT");
            int failedQueueCount = (int) failedQueueProps.get("QUEUE_MESSAGE_COUNT");

            Health.Builder builder = Health.up()
                    .withDetail("main_queue_messages", mainQueueCount)
                    .withDetail("retry_queue_messages", retryQueueCount)
                    .withDetail("failed_queue_messages", failedQueueCount);

            if (failedQueueCount > 0) {
                builder.down().withDetail("reason", "Messages found in the failed queue.");
            }

            return builder.build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
