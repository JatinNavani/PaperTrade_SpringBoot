package paper_trade.paper_trade;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import com.zerodhatech.models.Tick;

@Service
public class PriceService {
	
	@Async("taskExecutor")
    public void processTicks(List<Tick> ticks) {
        NumberFormat formatter = new DecimalFormat();
        
        long threadId = Thread.currentThread().getId(); // Get the ID of the current thread
        System.out.println("Thread ID: " + threadId);
        System.out.println("ticks size " + ticks.size());
        if (ticks.size() > 0) {
            System.out.println("last price " + ticks.get(0).getLastTradedPrice());
            System.out.println("last traded time " + ticks.get(0).getLastTradedTime());
           
        }
        
    }
	
}
