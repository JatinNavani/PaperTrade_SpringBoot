package paper_trade.paper_trade;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Quote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class RESTController {

    @Autowired
    private SQLiteService sqliteService;
    @Autowired
    private KiteService kiteService;

    @GetMapping("/api/watchlist")
    public void addWatchlistStock(@RequestParam("id") String id, @RequestParam("instrumentToken") List<Long> instrumentToken) {
        sqliteService.insertIntoWatchlist(id, instrumentToken);
        
        
    }
    
    @GetMapping("/getQuote")
    public QuoteResponse getQuote(@RequestParam("instrumentToken") long instrumentToken) {
        try {
            return kiteService.getQuote(instrumentToken);
        } catch (KiteException e) {
            e.printStackTrace();
            return null;
        }
    

    
    }
    }

