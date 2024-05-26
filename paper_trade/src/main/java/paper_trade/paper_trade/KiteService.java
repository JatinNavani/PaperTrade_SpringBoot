package paper_trade.paper_trade;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.neovisionaries.ws.client.WebSocketException;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.SessionExpiryHook;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.models.Tick;
import com.zerodhatech.models.User;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnConnect;
import com.zerodhatech.ticker.OnDisconnect;
import com.zerodhatech.ticker.OnError;
import com.zerodhatech.ticker.OnTicks;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;


@Service
public class KiteService {
	@Autowired
    private SQLiteService sqliteTableCreator;
	@Autowired
    private PriceService priceService;
	@Autowired
    private RabbitMQProducer rabbitMQProducer;
	
	private KiteTicker tickerProvider;
    private ArrayList<Long> tokens;
	
	 @Scheduled(cron = "0 30 8 * * *")
    public void fetchAndStoreInstruments() {
        try {
            String instrumentsFilePath = "D:\\instruments\\instruments_gen.csv";

            KiteConnect kiteConnect = new KiteConnect("api", true);
            

            List<Instrument> nseInstruments = kiteConnect.getInstruments();
            writeInstrumentsToCSV(nseInstruments, instrumentsFilePath);

        } catch (Exception e) {
            e.printStackTrace();
        }catch (KiteException e) {
        	e.printStackTrace();
        }
    }
	 
	 public KiteConnect getKiteConnect(){
		 KiteConnect kiteConnect = new KiteConnect("5kg9ivjlrub91in3", true);
         kiteConnect.setUserId("XRN389");
         kiteConnect.setSessionExpiryHook(new SessionExpiryHook() {
             @Override
             public void sessionExpired() {
            	 sqliteTableCreator.deleteAllTokens();
            	 isConnected = false;
                 System.out.println("session expired");
             }
         });
         
         
         
         kiteConnect.setAccessToken(sqliteTableCreator.getMostRecentAccessToken());
         kiteConnect.setPublicToken(sqliteTableCreator.getMostRecentPublicToken());
		
         
         return kiteConnect;
         
	 }

	 private void writeInstrumentsToCSV(List<Instrument> instruments, String fileName) {
		    
		 List<Instrument> nullExpiryInstruments = new ArrayList<>();
		    List<Instrument> nonNullExpiryInstruments = new ArrayList<>();
		    for (Instrument instrument : instruments) {
		        if (instrument.getExpiry() == null) {
		            nullExpiryInstruments.add(instrument);
		        } else {
		            nonNullExpiryInstruments.add(instrument);
		        }
		    }
		    
		    
		    nonNullExpiryInstruments.sort(Comparator.comparing(Instrument::getExpiry));
		    
		    
		    List<Instrument> sortedInstruments = new ArrayList<>(nullExpiryInstruments);
		    sortedInstruments.addAll(nonNullExpiryInstruments);
		    
		    
		    
		    
		    File file = new File(fileName);
		    if (file.exists()) {
		        // If file already exists, delete it
		        file.delete();
		    }

		    try (FileWriter writer = new FileWriter(fileName)) {
		        // Write header
		        writer.append("exchange,exchange_token,expiry,instrument_token,instrument_type,last_price,lot_size,name,segment,strike,tick_size,tradingsymbol\n");

		        // Write instruments (limit to 2000 entries)
		        int count = 0;
		        for (Instrument instrument : instruments) {
		            // Check if instrument name starts with "nifty"
		            if (instrument.name != null && instrument.name.toLowerCase().startsWith("nifty")) {
		                writer.append(instrument.exchange).append(",");
		                writer.append(String.valueOf(instrument.exchange_token)).append(",");
		                
		                
		                
		                String formattedDate = "";
		                if (instrument.getExpiry() != null) {
		                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
		                    formattedDate = dateFormat.format(instrument.getExpiry());
		                }
		                writer.append(formattedDate).append(",");
		                
		                writer.append(String.valueOf(instrument.instrument_token)).append(",");
		                writer.append(instrument.instrument_type).append(",");
		                writer.append(String.valueOf(instrument.last_price)).append(",");
		                writer.append(String.valueOf(instrument.lot_size)).append(",");
		                writer.append(instrument.name).append(",");
		                writer.append(instrument.segment).append(",");
		                writer.append(String.valueOf(instrument.strike)).append(",");
		                writer.append(String.valueOf(instrument.tick_size)).append(",");
		                writer.append(instrument.tradingsymbol).append("\n");

		                count++;
		                if (count >= 2000) {
		                    break; // Break loop after writing 2000 entries
		                }
		            }
		        }

		        System.out.println("Instruments written to CSV file successfully.");
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}
	 
	 private boolean dummyContinueSending = true; 
	 
	 public void sendDummyTicksToRabbitMQ() {
		    while (dummyContinueSending) { // Continue sending as long as continueSending is true
		        try {
		            // Load instrument tokens from CSV file
		            ArrayList<Long> tokens = new ArrayList<>();
		            File csvFile = new File("D:/instruments/instruments_gen.csv");
		            Scanner scanner = new Scanner(new FileReader(csvFile));

		            // Skip the header row (if present)
		            if (scanner.hasNextLine()) {
		                scanner.nextLine();
		            }

		            // Read each line of the CSV file
		            while (scanner.hasNextLine()) {
		                String line = scanner.nextLine();
		                String[] values = line.split(",");

		                // Assuming "instrument_token" is at index 3 (change index if needed)
		                if (values.length > 3) {
		                    try {
		                        long token = Long.parseLong(values[3]); // Convert String to long
		                        tokens.add(token);
		                    } catch (NumberFormatException e) {
		                        // Handle potential parsing errors (optional)
		                        System.err.println("Error parsing instrument_token: " + values[3]);
		                    }
		                }
		            }

		            scanner.close();

		            // Generate dummy ticks for each instrument token
		            Random random = new Random();
		            ArrayList<Tick> dummyTicks = new ArrayList<>();
		            for (long instrumentToken : tokens) {
		                double lastTradedPrice = 100 + (200 - 100) * random.nextDouble();

		                Tick tick = new Tick();
		                tick.setInstrumentToken(instrumentToken);
		                tick.setLastTradedPrice(lastTradedPrice);

		                dummyTicks.add(tick);
		            }

		            // Process the dummy ticks (optional)
		            priceService.processTicks(dummyTicks);

		            // Send the ticks to RabbitMQ
		            for (Tick tick : dummyTicks) {
		                String price = String.valueOf(tick.getLastTradedPrice());
		                long instrumentToken = tick.getInstrumentToken();
		                rabbitMQProducer.sendPriceAndToken(price, instrumentToken);
		            }

		            // Sleep for a specified duration before sending the next batch of ticks (optional)
		            Thread.sleep(1000); // Sleep for 1 second (adjust as needed)
		        } catch (IOException | InterruptedException e) {
		            e.printStackTrace();
		        }
		    }
		}


	 
	 public boolean isConnected = false;
	 
	 public void startStreamingPrices() throws IOException, WebSocketException, KiteException {
	        /** To get live price use websocket connection.
	         * It is recommended to use only one websocket connection at any point of time and make sure you stop connection, once user goes out of app.
	         * custom url points to new endpoint which can be used till complete Kite Connect 3 migration is done. */
	        
		 KiteConnect kiteConnect = getKiteConnect();
		 ArrayList<Long> tokens = new ArrayList<>(); // Temporary ArrayList for Strings
	        File csvFile = new File("D:/instruments/instruments_gen.csv");
	        Scanner scanner = new Scanner(new FileReader(csvFile));

	        // Skip the header row (if present)
	        if (scanner.hasNextLine()) {
	            scanner.nextLine();
	        }

	        while (scanner.hasNextLine()) {
	            String line = scanner.nextLine();
	            String[] values = line.split(",");

	            // Assuming "instrument_token" is at index 2 (change index if needed)
	            if (values.length > 3) {
	                try {
	                    long token = Long.parseLong(values[3]); // Convert String to long
	                    tokens.add(token);
	                } catch (NumberFormatException e) {
	                    // Handle potential parsing errors (optional)
	                    System.err.println("Error parsing instrument_token: " + values[3]);
	                }
	            }
	        }

	        scanner.close();

	        // Convert ArrayList<Long> to long array
	        long[] tokensArray = new long[tokens.size()];
	        for (int i = 0; i < tokens.size(); i++) {
	            tokensArray[i] = tokens.get(i);
	        }
	        
		 final KiteTicker tickerProvider = new KiteTicker(kiteConnect.getAccessToken(), kiteConnect.getApiKey());

	        tickerProvider.setOnConnectedListener(new OnConnect() {
	            @Override
	            public void onConnected() {
	                /** Subscribe ticks for token.
	                 * By default, all tokens are subscribed for modeQuote.
	                 * */
	            	isConnected = true;
	            	
	            	tickerProvider.subscribe(tokens);
	              
	            }
	        });

	        tickerProvider.setOnDisconnectedListener(new OnDisconnect() {
	            @Override
	            public void onDisconnected() {
	                // your code goes here
	            	isConnected = false;
	            }
	        });

	        

	        /** Set error listener to listen to errors.*/
	        tickerProvider.setOnErrorListener(new OnError() {
	            @Override
	            public void onError(Exception exception) {
	                //handle here.
	            }
	            public void onError(WebSocketException webexception) {
	                //handle here.
	            }

	            @Override
	            public void onError(KiteException kiteException) {
	                //handle here.
	            }

	            @Override
	            public void onError(String error) {
	                System.out.println(error);
	            }
	        });

	        tickerProvider.setOnTickerArrivalListener(new OnTicks() {
	            @Override
	            public void onTicks(ArrayList<Tick> ticks) {
	            	priceService.processTicks(ticks);
	            	
	            	for (Tick tick : ticks) {
	                    String price = String.valueOf(tick.getLastTradedPrice());
	                    long instrumentToken = tick.getInstrumentToken();
	                    //rabbitMQProducer.sendPriceAndToken(price, instrumentToken);
	                }
	            }
	        });
	        // Make sure this is called before calling connect.
	        tickerProvider.setTryReconnection(true);
	        //maximum retries and should be greater than 0
	        tickerProvider.setMaximumRetries(10);
	        //set maximum retry interval in seconds
	        tickerProvider.setMaximumRetryInterval(30);

	        /** connects to com.zerodhatech.com.zerodhatech.ticker server for getting live quotes*/
	        
	        tickerProvider.connect();
	        
	        

	        boolean isConnected = tickerProvider.isConnectionOpen();
	        System.out.println(isConnected);

	        /** set mode is used to set mode in which you need tick for list of tokens.
	         * Ticker allows three modes, modeFull, modeQuote, modeLTP.
	         * For getting only last traded price, use modeLTP
	         * For getting last traded price, last traded quantity, average price, volume traded today, total sell quantity and total buy quantity, open, high, low, close, change, use modeQuote
	         * For getting all data with depth, use modeFull*/
	        tickerProvider.setMode(tokens, KiteTicker.modeFull);
	        

	        
	        //tickerProvider.unsubscribe(tokens);


	        //tickerProvider.disconnect();
	    }
	 public void stopStreamingPrices() {
		 
		 KiteConnect kiteConnect = getKiteConnect();
		 ArrayList<Long> tokens = new ArrayList<>();
	        tokens.add(109320711L);
	        tokens.add(66021383L);
	        
		 final KiteTicker tickerProvider = new KiteTicker(kiteConnect.getAccessToken(), kiteConnect.getApiKey());
		 tickerProvider.unsubscribe(tokens);


	     tickerProvider.disconnect();
	 }
	 
}
