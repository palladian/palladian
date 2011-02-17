package ws.palladian.helper;

import ws.palladian.helper.DBStore;
import junit.framework.TestCase;

/**
 * Test cases for the xPath handling.
 * 
 * @author David Urbansky
 */
public class DBStoreTest extends TestCase {

    public DBStoreTest(String name) {
        super(name);
    }

    public void testDBStore() {
        DBStore dbStore = new DBStore("test");
        dbStore.clear();

        dbStore.put("abc", "def");
        dbStore.put("123", "456");
        dbStore.put("1234", 999);
        assertEquals("def", dbStore.get("abc"));
        assertEquals(new Integer(456), new Integer(dbStore.get("123").toString()));
        assertEquals(new Integer(999), new Integer(dbStore.get("1234").toString()));
        assertEquals(3, dbStore.size());

        dbStore.remove("abc");
        assertEquals(null, dbStore.get("abc"));
        assertEquals(2, dbStore.size());

        dbStore.put("abc", "def");
        assertEquals("def", dbStore.get("abc"));
        assertEquals(3, dbStore.size());

        dbStore.clear();
        assertEquals(0, dbStore.size());
    }

}