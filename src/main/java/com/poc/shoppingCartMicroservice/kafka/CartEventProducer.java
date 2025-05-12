package com.poc.shoppingCartMicroservice.kafka;

import com.poc.dto.CartCheckoutRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CartEventProducer {

    private static final String TOPIC = "order-topic";

    @Autowired
    private KafkaTemplate<String, CartCheckoutRequest> kafkaTemplate;

    public void sendOrder(CartCheckoutRequest orderRequest) {
        log.info("Sending checkout request to Kafka: {}", orderRequest);
        kafkaTemplate.send(TOPIC, orderRequest);
    }
}