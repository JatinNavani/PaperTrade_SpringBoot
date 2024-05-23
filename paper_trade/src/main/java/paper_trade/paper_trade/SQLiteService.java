package paper_trade.paper_trade;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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


}