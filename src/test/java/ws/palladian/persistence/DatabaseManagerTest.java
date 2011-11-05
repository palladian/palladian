package ws.palladian.persistence;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

public class DatabaseManagerTest {

    private static DatabaseManager databaseManager;

    @BeforeClass
    public static void beforeClass() {
        databaseManager = DatabaseManagerFactory.create(DatabaseManager.class);
        databaseManager
                .runUpdate("CREATE TABLE test (id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT, name VARCHAR(255), PRIMARY KEY (id));");
    }

    @Test
    public void testInsert() {
        String sql = "INSERT INTO test (name) VALUES (?)";
        assertEquals(1, databaseManager.runInsertReturnId(sql, "bob"));
        assertEquals(2, databaseManager.runInsertReturnId(sql, "mary"));
    }

}
