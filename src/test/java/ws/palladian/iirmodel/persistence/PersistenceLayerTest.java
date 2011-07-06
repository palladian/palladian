/**
 * Created on: 01.07.2011 16:34:00
 */
package ws.palladian.iirmodel.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Date;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.iirmodel.Author;
import ws.palladian.iirmodel.Item;
import ws.palladian.iirmodel.ItemStream;
import ws.palladian.iirmodel.ItemType;

/**
 * @author Klemens Muthmann
 * @author Philipp Katz
 * 
 */
public class PersistenceLayerTest {

    private ModelPersistenceLayer persistenceLayer;
    private EntityManagerFactory emFactory;
    private static final String TEST_PERSISTENCE_UNIT_NAME = "iirmodel";

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        emFactory = Persistence.createEntityManagerFactory(TEST_PERSISTENCE_UNIT_NAME);
        persistenceLayer = new ModelPersistenceLayer(emFactory.createEntityManager());
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        persistenceLayer.shutdown();
        persistenceLayer = null;
        if (emFactory != null && emFactory.isOpen()) {
            emFactory.close();
        }
        emFactory = null;
    }

    @Test
    public final void testSaveItemStream() {
        try {
            ItemStream stream = new ItemStream("testSource", "http://testSource.de/testStream", "testChannel");
            persistenceLayer.saveItemStream(stream);
        } catch (Exception e) {
            fail("Unable to save new ItemStream due to: " + e.getStackTrace());
        }
    }

    @Test
    public final void testSaveComplexItemStream() {
        ItemStream stream = new ItemStream("testSource", "http://testSource.de/testStream", "testChannel");
        Author author1 = new Author("a1@testSource", "a1", 10, 2, 5, new Date(), "testSource");
        Item item1 = new Item("i1@" + stream.getIdentifier(), "i1", author1, "http://testSource.de/testStream/i1",
                "i1", new Date(), new Date(), "i1text", null, ItemType.QUESTION);
        stream.addItem(item1);
        Author author2 = new Author("a2@testSource", "a2", 4, 0, 3, new Date(), "testSource");
        Item item2 = new Item("i2@" + stream.getIdentifier(), "i2", author2, "http://testSource.de/testStream/i2",
                "i2", new Date(), new Date(), "i2text", item1, ItemType.OTHER);
        stream.addItem(item2);

        persistenceLayer.saveItemStream(stream);
        ItemStream result = persistenceLayer.loadItemStreamBySourceAddress(stream.getSourceAddress());
        assertEquals(stream, result);
    }

    @Test
    public void testSaveChangeItemStream() throws Exception {
        ItemStream stream = new ItemStream("testSource", "http://testSource.de/testStream", "testChannel");
        Author author1 = new Author("a1@testSource", "a1", 10, 2, 5, new Date(), "testSource");
        Item item1 = new Item("i1@" + stream.getIdentifier(), "i1", author1, "http://testSource.de/testStream/i1",
                "i1", new Date(), new Date(), "i1text", null, ItemType.QUESTION);
        stream.addItem(item1);
        Author author2 = new Author("a2@testSource", "a2", 4, 0, 3, new Date(), "testSource");
        Date author2RegistrationDate = new Date();
        Item item2 = new Item("i2@" + stream.getIdentifier(), "i2", author2, "http://testSource.de/testStream/i2",
                "i2", author2RegistrationDate, new Date(), "i2text", item1, ItemType.OTHER);
        stream.addItem(item2);

        persistenceLayer.saveItemStream(stream);

        ItemStream changedStream = new ItemStream("testSource", "http://testSource.de/testStream",
                "testChannel");

        Author changedAuthor1 = new Author("a2@testSource", "a2", 11, 3, 5, author2RegistrationDate, "testSource");
        Item changedItem1 = new Item("i2@" + changedStream.getIdentifier(), "i2", changedAuthor1,
                "http://testSource.de/testStream/i2", "i2", item2.getPublicationDate(), item2.getUpdateDate(),
                "i2text", null, ItemType.OTHER);
        changedStream.addItem(changedItem1);
        Author changedAuthor2 = new Author("a3@testSource", "a3", 11, 3, 5, new Date(), "testSource");
        Item changedItem2 = new Item("i3@" + changedStream.getIdentifier(), "i3", changedAuthor2,
                "http://testSource.de/testStream/i3", "i3", new Date(), new Date(), "i3text", changedItem1,
                ItemType.OTHER);
        changedStream.addItem(changedItem2);

        persistenceLayer.saveItemStream(changedStream);

        ItemStream loadedStream = persistenceLayer.loadItemStreamBySourceAddress("http://testSource.de/testStream");
        Item deletedItem = persistenceLayer.loadItem("i1@" + loadedStream.getIdentifier());
        assertNull(deletedItem);
        assertEquals(2, loadedStream.getItems().size());
        assertEquals("http://testSource.de/testStream", loadedStream.getSourceAddress());
        assertEquals("i2", loadedStream.getItems().get(0).getStreamSourceInternalIdentifier());
        assertEquals("i3", loadedStream.getItems().get(1).getStreamSourceInternalIdentifier());
    }

    @Test
    public void testSaveAuthor() throws Exception {
        Date registrationDate = new Date();
        Author testAuthor = new Author("test@testSource", "test", 10, 2, 3, registrationDate, "testSource");
        persistenceLayer.saveAuthor(testAuthor);
        Author result = persistenceLayer.loadAuthor(testAuthor.getUsername() + "@testSource");
        assertNotNull(result);
        assertEquals("test", result.getUsername());
        assertEquals(Integer.valueOf(10), result.getCountOfItems());
        assertEquals(Integer.valueOf(2), result.getCountOfStreamsStarted());
        assertEquals(Integer.valueOf(3), result.getAuthorRating());
        assertEquals(registrationDate, result.getRegisteredSince());
        assertEquals("testSource", result.getStreamSource());
    }

    @Test
    public void testUpdateAuthor() throws Exception {
        Date registrationDate = new Date();
        Author testAuthor = new Author("test@testSource", "test", 10, 2, 3, registrationDate, "testSource");
        persistenceLayer.saveAuthor(testAuthor);
        Author originalResult = persistenceLayer.loadAuthor("test@testSource");
        assertEquals(originalResult, testAuthor);

        Author changedAuthor = new Author("test@testSource", "test", 30, 5, 4, registrationDate, "testSource");
        persistenceLayer.saveAuthor(changedAuthor);
        Author result = persistenceLayer.loadAuthor("test@testSource");
        assertEquals(result, changedAuthor);
    }

    @Test
    public void testSaveItem() throws Exception {
        Author testAuthor = new Author("testUser@testSource", "testUser", 3, 2, 1, new Date(), "testSource");
        Item testItem = new Item("testItem", "testItem", testAuthor, "http://testSource.de/testItem", "testItem",
                new Date(), new Date(), "testItemText", null, ItemType.OTHER);

        persistenceLayer.saveItem(testItem);
    }
}
