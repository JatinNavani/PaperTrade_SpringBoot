package paper_trade.paper_trade;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import paper_trade.payload.PricePayload;

@Configuration
public class AppConfig {

    @Bean
    public PricePayload pricePayload() {
        return new PricePayload();
    }
}
