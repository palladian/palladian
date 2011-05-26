package ws.palladian.persistence;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

public class DatabaseManagerTest {

    private static DatabaseManager databaseManager;

    @BeforeClass
    public static void beforeClass() {
        databaseManager = DatabaseManagerFactory.getInstance().create(DatabaseManager.class.getName());
        databaseManager
                .runUpdate("CREATE TABLE test (id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT, name VARCHAR(255), PRIMARY KEY (id));");
    }

    @Test
    public void testDBManager() {
        DatabaseManager manager = DatabaseManagerFactory.getInstance().create(
                "ws.palladian.persistence.DatabaseManager", "org.h2.Driver", "jdbc:h2:mem:palladian", "sa");
        assertNotNull(manager);
        assertTrue(manager.getClass().equals(DatabaseManager.class));
    }

    @Test
    public void testInsert() {
        String sql = "INSERT INTO test (name) VALUES (?)";
        Assert.assertEquals(1, databaseManager.runInsertReturnId(sql, "bob"));
        Assert.assertEquals(2, databaseManager.runInsertReturnId(sql, "mary"));
    }

}
