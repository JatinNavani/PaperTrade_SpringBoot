package paper_trade.paper_trade;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import com.zerodhatech.models.Quote;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnConnect;
import com.zerodhatech.ticker.OnDisconnect;
import com.zerodhatech.ticker.OnError;
import com.zerodhatech.ticker.OnTicks;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import paper_trade.payload.PricePayload;


@Service
public class KiteService {
	@Autowired
    private SQLiteService sqliteTableCreator;
	@Autowired
    private PriceService priceService;
	@Autowired
    private RabbitMQProducer rabbitMQProducer;
	@Autowired
    private S3Client s3Client;
	
	private KiteTicker tickerProvider;
    private ArrayList<Long> tokens;
// This saves the instruments CSV in AWS S3 Bucket   
    @Scheduled(cron = "0 30 8 * * *")
    public void fetchAndStoreInstruments() {
        String bucketName = "jatinnavanibucket";
        String keyName = "instruments/instruments_gen.csv";
        LocalDate currentDate = LocalDate.now();
        LocalDate expiryWithinAMonth = currentDate.plusMonths(1);

        try {
            KiteConnect kiteConnect = new KiteConnect("api", true);
            List<Instrument> nseInstruments = kiteConnect.getInstruments();
            
            nseInstruments = filterInstruments(nseInstruments, currentDate, expiryWithinAMonth);
            
            if (nseInstruments.size() > 700) {
                nseInstruments = nseInstruments.subList(0, 700);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8)) {
                // Write header
                writer.println("exchange,exchange_token,expiry,instrument_token,instrument_type,last_price,lot_size,name,segment,strike,tick_size,tradingsymbol");

                // Write instrument data
                for (Instrument instrument : nseInstruments) {
                    writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                            instrument.getExchange(),
                            instrument.getExchange_token(),
                            instrument.getExpiry(),
                            instrument.getInstrument_token(),
                            instrument.getInstrument_type(),
                            instrument.getLast_price(),
                            instrument.getLot_size(),
                            instrument.getName(),
                            instrument.getSegment(),
                            instrument.getStrike(),
                            instrument.getTick_size(),
                            instrument.getTradingsymbol()
                    ));
                }
                
              
            }

            

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(outputStream.toByteArray()));
            System.out.println("File uploaded to S3 successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        } catch (KiteException e) {
            e.printStackTrace();
        }
    }
    
  
    
    

    private List<Instrument> filterInstruments(List<Instrument> instruments, LocalDate currentDate, LocalDate expiryWithinAMonth) {
        return instruments.stream()
                .filter(instrument -> "nfo".equalsIgnoreCase(instrument.getExchange()) &&
                        instrument.getExpiry() != null &&
                        convertToLocalDate(instrument.getExpiry()).isAfter(currentDate) &&
                        convertToLocalDate(instrument.getExpiry()).isBefore(expiryWithinAMonth))
                .toList();
    }

    private LocalDate convertToLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private String formatDate(Date date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return formatter.format(convertToLocalDate(date));
    }
    
    
    
    public QuoteResponse getQuote(long instrumentToken) throws KiteException {
	        try {
	            String[] instruments = {String.valueOf(instrumentToken)};
	           
	            KiteConnect kiteConnect = getKiteConnect();
	            Map<String, Quote> quotes = kiteConnect.getQuote(instruments);
	            Quote quote = quotes.get(String.valueOf(instrumentToken));
	            
	            double sellPrice = quote.depth.buy.get(4).getPrice();
	            double buyPrice = quote.depth.sell.get(4).getPrice();
	            return new QuoteResponse(instrumentToken, buyPrice, sellPrice);
	        } catch (IOException e) {
	            e.printStackTrace();
	            // Handle exceptions
	            return null;
	        }
	    }
	 
	 
	
	 public KiteConnect getKiteConnect(){
		 KiteConnect kiteConnect = new KiteConnect("api_key", true);//Put your api_Key
         kiteConnect.setUserId("XXXXXX");//Put your Zerodha UserID
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
	 
	 public void sendDummyTicksToRabbitMQ() throws IOException, WebSocketException, KiteException {
	        /** To get live price use websocket connection.
	         * It is recommended to use only one websocket connection at any point of time and make sure you stop connection, once user goes out of app.
	         * custom url points to new endpoint which can be used till complete Kite Connect 3 migration is done. */
	        
		 KiteConnect kiteConnect = getKiteConnect();
		
		 ArrayList<Long> tokens = loadTokensFromS3("jatinnavanibucket", "instruments/instruments_MCX.csv");

	        

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
	            
	            	List<String> uniqueDeviceUUIDs = sqliteTableCreator.getUniqueDeviceUUIDs();
		            // Send the ticks to RabbitMQ
		            for (Tick tick : ticks) {
		            	double price = tick.getLastTradedPrice();
	                    long instrumentToken = tick.getInstrumentToken();
		                PricePayload message = new PricePayload();
		                
		                message.setInstrumentToken(instrumentToken);
	                    message.setPrice(price);
		                
		                
		                for (String deviceUUID : uniqueDeviceUUIDs) {
		                
		                rabbitMQProducer.sendPriceAndToken(message,deviceUUID);
		                }
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
		    


	 
	 public boolean isConnected = false;
	 
	 
	 
	 public void startStreamingPrices() throws IOException, WebSocketException, KiteException {
	        /** To get live price use websocket connection.
	         * It is recommended to use only one websocket connection at any point of time and make sure you stop connection, once user goes out of app.
	         * custom url points to new endpoint which can be used till complete Kite Connect 3 migration is done. */
	        
		 KiteConnect kiteConnect = getKiteConnect();
		
		 ArrayList<Long> tokens = loadTokensFromS3("jatinnavanibucket", "instruments/instruments_gen.csv");


	        

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
	            	
	            	List<String> uniqueDeviceUUIDs = sqliteTableCreator.getUniqueDeviceUUIDs();
		            // Send the ticks to RabbitMQ
		            for (Tick tick : ticks) {
		            	double price = tick.getLastTradedPrice();
	                    long instrumentToken = tick.getInstrumentToken();
		                PricePayload message = new PricePayload();
		                
		                message.setInstrumentToken(instrumentToken);
	                    message.setPrice(price);
		                
		                
		                for (String deviceUUID : uniqueDeviceUUIDs) {
		                
		                rabbitMQProducer.sendPriceAndToken(message,deviceUUID);
		                }
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
	 
	
	 
	 public ArrayList<Long> loadTokensFromS3(String bucketName, String keyName) throws IOException {
		    ArrayList<Long> tokens = new ArrayList<>();
		    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
		            .bucket(bucketName)
		            .key(keyName)
		            .build();

		    try (InputStream inputStream = s3Client.getObject(getObjectRequest);
		         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

		        String line;
		        reader.readLine(); // skip header line

		        while ((line = reader.readLine()) != null) {
		            String[] parts = line.split(",");
		            long instrumentToken = Long.parseLong(parts[3].trim()); // assuming the token is in the fourth column
		            tokens.add(instrumentToken);
		        }
		    }

		    return tokens;
		}

	
	 
	 
}
