package gatling.org;

import java.sql.SQLException;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws SQLException {
        Map<String, String> primaryKeys = null;//Map.of(); // ajoute les bonnes PK si besoin
        String result = DataVerifier.getDataValue("508", "REALNAME", "persistenceuser", primaryKeys);
        System.out.println("Valeur actuelle : " + result);
    }
}
