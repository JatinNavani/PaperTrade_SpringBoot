package paper_trade.paper_trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private SQLiteService sqliteService;

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        System.out.println("Application started!");

        // Check if the table exists, create it if it doesn't
        if (!sqliteService.hasTable()) {
            sqliteService.createTable();
            System.out.println("Table created successfully.");
        } else {
            System.out.println("Table already exists.");
        }
        
        if (!sqliteService.hasWatchlistTable()) {
            sqliteService.createWatchlistTable();
            System.out.println("Watchlist table created successfully.");
        } else {
            System.out.println("Watchlist table already exists.");
        }
    }
}
