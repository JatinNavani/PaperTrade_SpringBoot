package paper_trade.payload;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PricePayload implements Serializable{
	private double price;
    private long instrumentToken;
    private LocalDateTime time;
    
    // Constructor
    public PricePayload(double price, long instrumentToken) {
        this.price = price;
        this.instrumentToken = instrumentToken;
    }
    public PricePayload() {
        // Initialize default values if needed
    }
    public PricePayload(long instrumentToken, double price) {
        this.instrumentToken = instrumentToken;
        this.price = price;
    }

    // Getters and setters
    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getInstrumentToken() {
        return instrumentToken;
    }

    public void setInstrumentToken(long instrumentToken) {
        this.instrumentToken = instrumentToken;
    }
    
    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }


}
