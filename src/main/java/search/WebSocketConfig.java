package search ;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
 
/** 
* @authors
 * Miguel Castela 2022212972 👍
 * Miguel Martins 2022213951 👍
 */

/**
 * WebSocket configuration class for the Meta2 application.
 * This class configures the WebSocket message broker and STOMP endpoints.
 * It enables WebSocket message handling and sets up the message broker.
 */

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures the message broker for WebSocket communication.
     * 
     * @param config The MessageBrokerRegistry to configure.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // Enables broadcasting to /topic/*
        config.setApplicationDestinationPrefixes("/app"); // For @MessageMapping (optional)
    }

    /**
     * Registers the STOMP endpoints for WebSocket communication.
     * 
     * @param registry The StompEndpointRegistry to register endpoints.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/stats").withSockJS(); // This exposes /stats to SockJS clients
    }
}
