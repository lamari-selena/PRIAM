package gatling.org;
import java.sql.*;
import java.util.Map;
import io.github.cdimascio.dotenv.Dotenv;

public class DataVerifier {

        private static final Dotenv dotenv = Dotenv.configure()
        .filename(".env") // fichier à la racine du projet
        .ignoreIfMalformed()
        .ignoreIfMissing()
        .load();

    
    private static final String HOST = dotenv.get("DB_HOST");
    private static final String PORT = dotenv.get("DB_PORT");

    // Construction dynamique de l'URL JDBC avec host & port
    private static final String DB_URL = "jdbc:mysql://" + HOST + ":" + PORT + "/teadb?useSSL=false";
    private static final String USER = dotenv.get("TEADB_USERNAME");
    private static final String PASSWORD = dotenv.get("TEADB_PASSWORD");
    //private static final String DB_URL = "jdbc:mysql://localhost:3307/teadb?useSSL=false";
    //private static final String USER ="";
    //private static final String PASSWORD ="";

    public static String getDataValue(String idRef, String dataName, String dataTypeName, Map<String, String> primaryKeys) throws SQLException {
        String value = null;
        StringBuilder query = new StringBuilder("SELECT " + dataName + " FROM " + dataTypeName + " WHERE ID = ?");
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            try (PreparedStatement stmt = conn.prepareStatement(query.toString())) {
                stmt.setString(1, idRef);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    value = rs.getString(dataName);
                }
            }
        }
        return value;
    }
}
