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
    @Autowired
    private SQLiteService sqlService; 
    
    
    
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    

    @Value("${price.exchange.name}")
    private String priceExchange;
/*
    public void sendPriceAndToken(PricePayload payload) {
    	String jsonMessage = gson.toJson(payload); 
        rabbitTemplate.convertAndSend("dummyExchange", "dummy.#", jsonMessage);
        System.out.println("Sent price and instrument token- " + jsonMessage);
        
        
        
    }
    */
    /*
    public void sendPriceAndToken(PricePayload payload) {
        try {
            // Assuming you have declared the exchange and queue properly in RabbitMQConfig
        	String jsonMessage = gson.toJson(payload);

            String routingKey = "instrument." + payload.getInstrumentToken();

            
            rabbitTemplate.convertAndSend(priceExchange, routingKey, jsonMessage);

            System.out.println("Sent price and instrument token- " + jsonMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    */
    
    public void sendPriceAndToken(PricePayload payload, String deviceUUID) {
        try {
        	
            // Check if the instrument is in the watchlist for the device
            if (sqlService.isInstrumentInWatchlist(payload.getInstrumentToken(), deviceUUID)) {
                // Send the price only if it's in the watchlist
                String jsonMessage = gson.toJson(payload);
                String routingKey = "device." + deviceUUID; // Modify the routing key

                rabbitTemplate.convertAndSend(priceExchange, routingKey, jsonMessage);

                System.out.println("Sent price and instrument token- " + jsonMessage);
            } else {
                System.out.println("Skipping price for instrument " + payload.getInstrumentToken() + " as it's not in the watchlist for device " + deviceUUID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void sendHello() {
        String message = "Hello From RabbitMQ";
        rabbitTemplate.convertAndSend("HelloExchange1", "hello1.#", message);
        System.out.println("Sent price and instrument token- " + message);
        
        
        
    }
}
