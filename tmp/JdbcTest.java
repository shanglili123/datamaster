import java.sql.*;
public class JdbcTest {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://172.21.163.204:5432/datamaster_test";
        System.out.println("URL: " + url);
        try (Connection c = DriverManager.getConnection(url, "datamaster", "datamaster")) {
            System.out.println("OK: " + c.isValid(2));
        } catch (Exception e) {
            System.out.println("FAIL: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
