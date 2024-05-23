package paper_trade.paper_trade;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.neovisionaries.ws.client.WebSocketException;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.SessionExpiryHook;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.models.User;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnTicks;
import com.zerodhatech.ticker.*;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.*;

import org.json.JSONObject;
import com.zerodhatech.models.Margin;


@Controller
public class TokenController {
	
	@Autowired
    private KiteService kiteService;
	@Autowired
    private SQLiteService sqliteTableCreator;
	


    // JDBC URL for SQLite database
    public static final String JDBC_URL = "jdbc:sqlite:tokens.db";

    @GetMapping("/updateToken")
    public String updateTokens(@RequestParam(name="request_token") String requestToken) {
    	
    	try {
    	KiteConnect kiteConnect = new KiteConnect("5kg9ivjlrub91in3", true);

    	// Set userId.
    	kiteConnect.setUserId("XRN389");
    	String url = kiteConnect.getLoginURL();
    	
    	kiteConnect.setSessionExpiryHook(new SessionExpiryHook() {
            @Override
            public void sessionExpired() {
                System.out.println("session expired");
            }
        });
    	
    	User user =  kiteConnect.generateSession(requestToken,"hmyvclm4ut5l269no5p4up63ez7a254y");
        kiteConnect.setAccessToken(user.accessToken);
        kiteConnect.setPublicToken(user.publicToken);
        final String request_token = requestToken;
        /*
        ArrayList<Long> inst_tokens = new ArrayList<>();
        inst_tokens.add(12014082L);
        
        tickerUsage(kiteConnect,inst_tokens);
		*/
        insertTokensIntoDatabase(requestToken,user.accessToken,user.publicToken);
        
        
        return "redirect:/LoginSuccess";} catch (KiteException e) {
            System.out.println(e.message+" "+e.code+" "+e.getClass().getName());
        }catch (JSONException | IOException e) {
            e.printStackTrace();
            return "Login Failed";
        }
    	return "Login Failed";
    }
    
    @GetMapping("/RefreshInstruments")
    public String refreshInstruments() {
        kiteService.fetchAndStoreInstruments();
        System.out.println("Instruments Refreshed!");
        return "redirect:/RefreshSuccess";
    }
    @GetMapping("/startPrice")
    public String startPrice() {
        try {
            if (!kiteService.isConnected) {
                kiteService.startStreamingPrices();
                return "redirect:/PriceSuccess";
            } else {
                System.out.println("Restarting");
                kiteService.stopStreamingPrices();
                kiteService.startStreamingPrices();
                return "redirect:/PriceSuccess";
            }
        } catch (WebSocketException | KiteException | IOException e) {
            e.printStackTrace();
            return "Error";
        }
    }
    
   
    @GetMapping("/LoginSuccess")
    public String loginSuccessPage() {
    	return "LoginSuccess.html";
 // return the name of the view you want to show after successful login
    }
    
    @GetMapping("/ZerodhaLogin")
    public String ZerodhaLogin() {
    	
    	if (sqliteTableCreator.hasValidToken()) {
            // If there are valid tokens use those tokens
            String currentRequestToken = sqliteTableCreator.getMostRecentRequestToken();
            if (currentRequestToken != null) {
            	return "redirect:/LoginSuccess";
            }
        } else {
            
        	
        	return "redirect:https://kite.zerodha.com/connect/login?v=3&api_key=5kg9ivjlrub91in3";
            
        }
        
        return "redirect:/LoginSuccess";
    }
    
    
    @GetMapping("/RefreshSuccess")
    public String refreshSuccessPage() {
    	return "RefreshSuccess.html";
 // return the name of the view you want to show after successful login
    }
    @GetMapping("/PriceSuccess")
    public String startPrices() {
    	return "PriceSuccess.html";
 // return the name of the view you want to show after successful login
    }
    
    
    
    
    private void insertTokensIntoDatabase(String requestToken,String accessToken,String publicToken) {
        try {
            // Connect to the SQLite database
            Connection connection = DriverManager.getConnection(JDBC_URL);

            // Create a prepared statement object
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO tokens (requestToken,accessToken,publicToken) VALUES (?,?,?)");

            // Set values for the prepared statement parameters
            preparedStatement.setString(1, requestToken);
            preparedStatement.setString(2, accessToken);
            preparedStatement.setString(3, publicToken);
       

            // Execute the SQL INSERT statement
            preparedStatement.executeUpdate();

            // Close the connection
            connection.close();

            System.out.println("Tokens inserted into database successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    
 }