package paper_trade.paper_trade;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import paper_trade.payload.PricePayload;
import com.zerodhatech.kiteconnect.KiteConnect;

@Configuration
public class AppConfig {

    @Bean
    public PricePayload pricePayload() {
        return new PricePayload();
    }
    
    @Bean
    public KiteConnect kiteConnect() {
        return new KiteConnect("5kg9ivjlrub91in3", true);
    }
}
