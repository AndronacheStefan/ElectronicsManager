import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:sqlite:identifier.sqlite";

    private DBConnection() {
    }

    public static Connection connec() {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.out.println("Eroare la conectare: " + e.getMessage());
            return null;
        }
    }
}
