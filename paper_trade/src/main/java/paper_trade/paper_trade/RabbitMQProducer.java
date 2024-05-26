package paper_trade.paper_trade;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQProducer {

    @Autowired
    private AmqpTemplate rabbitTemplate;
    

    @Value("${price.exchange.name}")
    private String priceExchange;

    public void sendPriceAndToken(String price, long instrumentToken) {
        String message = price + "|" + instrumentToken; // Combine price and instrument token
        rabbitTemplate.convertAndSend("dummyExchange", "dummy.#", message);
        System.out.println("Sent price and instrument token: " + message);
        
        
        
    }
    
    public void sendHello() {
        String message = "Hello From RabbitMQ";
        rabbitTemplate.convertAndSend("HelloExchange1", "hello1.#", message);
        System.out.println("Sent price and instrument token: " + message);
        
        
        
    }
}
