import java.sql.Connection;

public class TestDB {
    public static void main(String[] args) {
        Connection conn = DBConnection.connec();
        if (conn != null) {
            System.out.println("✅ Baza de date este accesibilă!");
        } else {
            System.out.println("❌ Conexiunea a eșuat!");
        }
    }
}
