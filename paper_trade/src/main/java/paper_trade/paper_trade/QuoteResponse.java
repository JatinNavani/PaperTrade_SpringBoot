package paper_trade.paper_trade;

public class QuoteResponse {
    private long instrumentToken;
    private double buyPrice;
    private double sellPrice;

    public QuoteResponse(long instrumentToken, double buyPrice, double sellPrice) {
        this.instrumentToken = instrumentToken;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }
    public long getInstrumentToken() {
        return instrumentToken;
    }

    public void setInstrumentToken(long instrumentToken) {
        this.instrumentToken = instrumentToken;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }
    
}