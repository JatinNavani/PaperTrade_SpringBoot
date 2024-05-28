package paper_trade.paper_trade;

import java.time.LocalDateTime;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import paper_trade.payload.PricePayload;

@Component
public class RabbitMQProducer {

    @Autowired
    private AmqpTemplate rabbitTemplate;
    
    
    
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    

    @Value("${price.exchange.name}")
    private String priceExchange;

    public void sendPriceAndToken(PricePayload payload) {
    	String jsonMessage = gson.toJson(payload); 
        rabbitTemplate.convertAndSend("dummyExchange", "dummy.#", jsonMessage);
        System.out.println("Sent price and instrument token- " + jsonMessage);
        
        
        
    }
    
    public void sendHello() {
        String message = "Hello From RabbitMQ";
        rabbitTemplate.convertAndSend("HelloExchange1", "hello1.#", message);
        System.out.println("Sent price and instrument token- " + message);
        
        
        
    }
}
