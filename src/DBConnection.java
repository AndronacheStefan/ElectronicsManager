import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class DBConnection {
    public static Connection connec () {
        String url = "jdbc:sqlite:identifier.sqlite ";
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println("Eroare la conectare: " + e.getMessage());
            return null;
        }
    }

    public PreparedStatement prepareStatement(String selectSql) {
        return null;
    }
}
