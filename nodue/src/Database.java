import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** MySQL JDBC connection helper. Configure NODUE_DB_URL, NODUE_DB_USER and NODUE_DB_PASSWORD. */
public final class Database {
    private Database() { }

    public static boolean isConfigured() {
        String url = System.getenv("NODUE_DB_URL");
        return url != null && !url.trim().isEmpty();
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv("NODUE_DB_URL");
        if (url == null || url.trim().isEmpty()) throw new SQLException("NODUE_DB_URL is not configured.");
        String user = System.getenv("NODUE_DB_USER");
        String password = System.getenv("NODUE_DB_PASSWORD");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Connector/J is missing from the Java classpath. Install Maven, run 'mvn dependency:copy-dependencies', then launch with '-cp \"out:target/dependency/*\"'.", e);
        }
        return DriverManager.getConnection(url, user == null ? "" : user, password == null ? "" : password);
    }
}
