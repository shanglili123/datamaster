import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class PgJdbcTest {
    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "jdbc:postgresql://127.0.0.1:5432/data_master?currentSchema=public&sslmode=disable";
        try (Connection connection = DriverManager.getConnection(url, "datamaster", "datamaster");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select current_database(), current_user")) {
            rs.next();
            System.out.println("OK " + rs.getString(1) + " " + rs.getString(2));
        }
    }
}
