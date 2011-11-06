package ws.palladian.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

public class DatabaseManagerTest {

    private static DatabaseManager databaseManager;
    
    private static final String JDBC_DRIVER = "org.h2.Driver";
    private static final String JDBC_URL = "jdbc:h2:mem:test";
    private static final String JDBC_USERNAME = "sa";
    private static final String JDBC_PASSWORD = "";
    
    private static final RowConverter<String> CONVERTER = new RowConverter<String>() {
        @Override
        public String convert(ResultSet resultSet) throws SQLException {
            return resultSet.getString("name");
        }
    };

    @BeforeClass
    public static void beforeClass() {
        databaseManager = DatabaseManagerFactory.create(DatabaseManager.class, JDBC_DRIVER, JDBC_URL, JDBC_USERNAME, JDBC_PASSWORD);
        databaseManager
                .runUpdate("CREATE TABLE test (id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT, name VARCHAR(255), PRIMARY KEY (id));");
    }

    @Test
    public void testRunInsert() {
        String sql = "INSERT INTO test (name) VALUES (?)";
        assertEquals(1, databaseManager.runInsertReturnId(sql, "bob"));
        assertEquals(2, databaseManager.runInsertReturnId(sql, "mary"));
    }
    
    @Test
    public void testRunQuery() {
        String sql = "SELECT * FROM test";
        List<String> result = databaseManager.runQuery(CONVERTER, sql);
        assertEquals(2, result.size());
        assertEquals("bob", result.get(0));
        assertEquals("mary", result.get(1));
    }
    
    @Test
    public void testRunSingleQuery() {
        String sql = "SELECT * FROM test WHERE name = ?";
        String result = databaseManager.runSingleQuery(CONVERTER, sql, "bob");
        assertEquals("bob", result);
    }
    
    @Test
    public void testRunQueryWithIterator() {
        String sql = "SELECT * FROM test";
        ResultIterator<String> iterator = databaseManager.runQueryWithIterator(CONVERTER, sql);
        assertTrue(iterator.hasNext());
        assertEquals("bob", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("mary", iterator.next());
        assertFalse(iterator.hasNext());
    }

}
