package paper_trade.paper_trade;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;


@Service
public class SQLiteService {

    // JDBC URL for SQLite database
    public static final String JDBC_URL = "jdbc:sqlite:tokens.db";

    // SQL statement to create the table
    public static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS tokens (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "requestToken TEXT," +
            "accessToken TEXT," +
            "publicToken TEXT," +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +  // Automatically captures the insertion time
            ");";

    public static final String CREATE_TABLE_SQL_watchlist = "CREATE TABLE IF NOT EXISTS watchlist (" +
            "id INTEGER ," +
            "instrument_token TEXT," +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +  // Automatically captures the insertion time
            "PRIMARY KEY (id, instrument_token)" +
            ");";
    
 

    public void createTable() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement statement = connection.prepareStatement(CREATE_TABLE_SQL)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error creating table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean hasTable() {
        String sql = "SELECT COUNT(*) AS table_count FROM sqlite_master WHERE type='table' AND name='tokens'";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            return resultSet.next() && resultSet.getInt("table_count") > 0;
        } catch (SQLException e) {
            System.err.println("Error checking if table exists: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    public String getMostRecentRequestToken() {
        String sql = "SELECT requestToken FROM tokens ORDER BY id DESC LIMIT 1";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            
            // If there is a result, return it
            if (resultSet.next()) {
                return resultSet.getString("requestToken");
            }
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
        // Return null if no data is found or if there is an exception
        return null;
    }
    
    
    public void deleteAllTokens() {
        String sql = "DELETE FROM tokens";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            int affectedRows = preparedStatement.executeUpdate();
            System.out.println("Deleted " + affectedRows + " entries from the database.");
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean hasValidToken() {
        String sqlCountEntries = "SELECT COUNT(*) AS entry_count FROM tokens";
        String sqlCountOldEntries = "SELECT COUNT(*) AS old_entries_count FROM tokens WHERE created_at <= datetime('now', '-1 day')";

        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement psCountEntries = connection.prepareStatement(sqlCountEntries);
             ResultSet rsEntries = psCountEntries.executeQuery()) {

            // Check if there are any entries in the table
            if (rsEntries.next() && rsEntries.getInt("entry_count") > 0) {
                // Table has entries, check for older entries
                try (PreparedStatement psCountOldEntries = connection.prepareStatement(sqlCountOldEntries);
                     ResultSet rsOldEntries = psCountOldEntries.executeQuery()) {
                    
                    // If there are older entries, return false
                    if (rsOldEntries.next() && rsOldEntries.getInt("old_entries_count") > 0) {
                        return false; // There are entries older than 24 hours
                    }
                }
            } else {
                // Table is empty, return false
                return false;
            }
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
        // If control reaches here, it means either the table is empty or all entries are recent
        return true;
    }

    public String getMostRecentAccessToken() {
        String sql = "SELECT accessToken FROM tokens ORDER BY id DESC LIMIT 1";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            // If there is a result, return the access token
            if (resultSet.next()) {
                return resultSet.getString("accessToken");
            }
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
        // Return null if no data is found or if there is an exception
        return null;
    }

    public String getMostRecentPublicToken() {
        String sql = "SELECT publicToken FROM tokens ORDER BY id DESC LIMIT 1";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            // If there is a result, return the public token
            if (resultSet.next()) {
                return resultSet.getString("publicToken");
            }
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
        // Return null if no data is found or if there is an exception
        return null;
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
    
    public void createWatchlistTable() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement statement = connection.prepareStatement(CREATE_TABLE_SQL_watchlist)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error creating watchlist table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean hasWatchlistTable() {
        String sql = "SELECT COUNT(*) AS table_count FROM sqlite_master WHERE type='table' AND name='watchlist'";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            return resultSet.next() && resultSet.getInt("table_count") > 0;
        } catch (SQLException e) {
            System.err.println("Error checking if watchlist table exists: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public void insertIntoWatchlist(String id, List<Long> instrumentTokens) {
        try {
            // Connect to the SQLite database
            Connection connection = DriverManager.getConnection(JDBC_URL);
            // Create a prepared statement object to delete existing entries for the provided id
            PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM watchlist WHERE id = ?");

            // Set the value for the prepared statement parameter
            deleteStatement.setString(1, id);

            // Execute the SQL DELETE statement
            deleteStatement.executeUpdate();

            // Close the delete statement
            deleteStatement.close();

            // Create a prepared statement object
            PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO watchlist (id, instrument_token) VALUES (?, ?)");

            // Set values for the prepared statement parameters
            for (Long token : instrumentTokens) {
                // Set values for the prepared statement parameters
                insertStatement.setString(1, id);
                insertStatement.setLong(2, token);

                // Execute the SQL INSERT statement
                insertStatement.executeUpdate();
            }

            // Close the insert statement
            insertStatement.close();

            // Close the connection
            connection.close();

            System.out.println("Watchlist stock inserted into database successfully.");
        } catch (SQLException e) {
            System.err.println("Error inserting watchlist stock into database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    
    
    public List<String> getInstrumentTokensById(int id) {
        List<String> instrumentTokens = new ArrayList<>();
        String sql = "SELECT instrument_token FROM watchlist WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            // Set the value for the parameterized query
            preparedStatement.setInt(1, id);
            
            // Execute the query
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                // Iterate through the result set and add instrument tokens to the list
                while (resultSet.next()) {
                    instrumentTokens.add(resultSet.getString("instrument_token"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving instrument tokens by id: " + e.getMessage());
            e.printStackTrace();
        }
        return instrumentTokens;
    }
    
    public boolean isInstrumentInWatchlist(long instrumentToken, String deviceUUID) {
        String sql = "SELECT COUNT(*) AS token_count FROM watchlist WHERE id = ? AND instrument_token = ?";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            // Set values for the prepared statement parameters
            preparedStatement.setString(1, deviceUUID);
            preparedStatement.setLong(2, instrumentToken);

            // Execute the query
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                // If there is a result, return true
                if (resultSet.next() && resultSet.getInt("token_count") > 0) {
                    return true; // Instrument token is in the watchlist for the device
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking if instrument is in watchlist: " + e.getMessage());
            e.printStackTrace();
        }
        // Return false if the instrument token is not found in the watchlist or if there is an exception
        return false;
    }

    public int getUniqueDeviceCount() {
        String sql = "SELECT COUNT(DISTINCT id) AS unique_device_count FROM watchlist";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            // If there is a result, return the count of unique device IDs
            if (resultSet.next()) {
                return resultSet.getInt("unique_device_count");
            }
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
        // Return 0 if no data is found or if there is an exception
        return 0;
    }
    
    public List<String> getUniqueDeviceUUIDs() {
        List<String> uniqueDeviceUUIDs = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT DISTINCT id FROM watchlist");
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                String deviceUUID = resultSet.getString("id");
                uniqueDeviceUUIDs.add(deviceUUID);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return uniqueDeviceUUIDs;
    }


}