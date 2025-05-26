package gatling.org;
import java.sql.*;
import java.util.Map;

public class DataVerifier {

    private static final String DB_URL = "jdbc:mysql://localhost:3308/priam-teadb?useSSL=false";
    private static final String USER = "priamu";
    private static final String PASSWORD = "MaiRP_pWd-UsEr";

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
