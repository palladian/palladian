package ws.palladian.persistence;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author David Urbansky
 */
public class DbStoreTest {

    @Test
    public void testDbStore() {
        DbStore dbStore = new DbStore("test");
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