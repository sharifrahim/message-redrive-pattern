package com.example.messageredrive;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    private final RabbitTemplate rabbitTemplate;

    public TestController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody Map<String, Object> payload) {
        rabbitTemplate.convertAndSend(RabbitConfig.MAIN_EXCHANGE_NAME, RabbitConfig.MAIN_QUEUE_NAME, payload);
        return ResponseEntity.ok("Message sent successfully!");
    }
}
