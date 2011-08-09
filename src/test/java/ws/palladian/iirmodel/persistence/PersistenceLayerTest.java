/**
 * Created on: 01.07.2011 16:34:00
 */
package ws.palladian.iirmodel.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.iirmodel.Author;
import ws.palladian.iirmodel.Item;
import ws.palladian.iirmodel.ItemRelation;
import ws.palladian.iirmodel.ItemStream;
import ws.palladian.iirmodel.ItemType;
import ws.palladian.iirmodel.RelationType;
import ws.palladian.iirmodel.StreamGroup;
import ws.palladian.iirmodel.StreamSource;

/**
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 3.0
 * @since 1.0
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

        DriverManager.registerDriver(new org.hsqldb.jdbcDriver());
        final Connection conn = DriverManager.getConnection("jdbc:hsqldb:mem:testdb", "sa", "");
        try {
            if (conn != null) {
                conn.createStatement().execute("SHUTDOWN");
                conn.commit();
            }
        } finally {
            conn.close();
        }
    }

    /**
     * Test to save {@link ItemStream} without any {@link Item}s.
     */
    @Test
    public final void testSaveStreamSource() {
        try {
            ItemStream stream = new ItemStream("testSource", "http://testSource.de/testStream");
            persistenceLayer.saveStreamSource(stream);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unable to save new ItemStream due to: " + e.getStackTrace());
        }
    }

    /**
     * Test to save {@link ItemStream} with attached {@link Item}s.
     */
    @Test
    public final void testSaveComplexItemStream() {
        ItemStream stream = new ItemStream("testSource", "http://testSource.de/testStream");
        Author author1 = new Author("a1", 10, 2, 5, new Date(), stream);
        Item item1 = new Item("i1", author1, "http://testSource.de/testStream/i1", "i1", new Date(), new Date(),
                "i1text", null, ItemType.QUESTION);
        stream.addItem(item1);
        Author author2 = new Author("a2", 4, 0, 3, new Date(), stream);
        Item item2 = new Item("i2", author2, "http://testSource.de/testStream/i2", "i2", new Date(), new Date(),
                "i2text", item1, ItemType.OTHER);
        stream.addItem(item2);

        persistenceLayer.saveStreamSource(stream);
        StreamSource result = persistenceLayer.loadStreamSourceByAddress(stream.getSourceAddress());
        assertEquals(stream, result);
    }

    @Test
    public void testSaveChangeItemStream() throws Exception {

        // save an ItemStream
        ItemStream stream = new ItemStream("testSource", "http://testSource.de/testStream");
        Author author1 = new Author("a1", 10, 2, 5, new Date(), stream);
        Item item1 = new Item("i1", author1, "http://testSource.de/testStream/i1", "i1", new Date(), new Date(),
                "i1text", null, ItemType.QUESTION);
        stream.addItem(item1);
        Author author2 = new Author("a2", 4, 0, 3, new Date(), stream);
        Date author2RegistrationDate = new Date();
        Item item2 = new Item("i2", author2, "http://testSource.de/testStream/i2", "i2", author2RegistrationDate,
                new Date(), "i2text", item1, ItemType.OTHER);
        stream.addItem(item2);

        persistenceLayer.saveStreamSource(stream);

        // save the same ItemStream again; ItemStream gets updated
        ItemStream changedStream = new ItemStream("testSource", "http://testSource.de/testStream");

        Author changedAuthor1 = new Author("a2", 11, 3, 5, author2RegistrationDate, changedStream);
        Item changedItem1 = new Item("i2", changedAuthor1, "http://testSource.de/testStream/i2", "i2",
                item2.getPublicationDate(), item2.getUpdateDate(), "i2text", null, ItemType.OTHER);
        changedStream.addItem(changedItem1);
        Author changedAuthor2 = new Author("a3", 11, 3, 5, new Date(), changedStream);
        Item changedItem2 = new Item("i3", changedAuthor2, "http://testSource.de/testStream/i3", "i3", new Date(),
                new Date(), "i3text", changedItem1, ItemType.OTHER);
        changedStream.addItem(changedItem2);

        persistenceLayer.saveStreamSource(changedStream);

        ItemStream loadedStream = (ItemStream)persistenceLayer
                .loadStreamSourceByAddress("http://testSource.de/testStream");

        Item deletedItem = persistenceLayer.loadItem(loadedStream.getIdentifier());
        assertNull(deletedItem);
        assertEquals(2, loadedStream.getItems().size());
        assertEquals("http://testSource.de/testStream", loadedStream.getSourceAddress());
        assertEquals("i2", loadedStream.getItems().get(0).getSourceInternalIdentifier());
        assertEquals("i3", loadedStream.getItems().get(1).getSourceInternalIdentifier());
    }

    @Test
    public void testSaveAuthor() throws Exception {
        Date registrationDate = new Date();
        StreamSource testSource = new StreamGroup("testGroup", "http://testGroup1.de");
        Author testAuthor = new Author("test", 10, 2, 3, registrationDate, testSource);
        persistenceLayer.saveAuthor(testAuthor);
        Author result = persistenceLayer.loadAuthor(testAuthor.getUsername(), testAuthor.getStreamSource());
        assertNotNull(result);
        assertEquals("test", result.getUsername());
        assertEquals(Integer.valueOf(10), result.getCountOfItems());
        assertEquals(Integer.valueOf(2), result.getCountOfStreamsStarted());
        assertEquals(Integer.valueOf(3), result.getAuthorRating());
        assertEquals(registrationDate, result.getRegisteredSince());
        assertEquals("testGroup", result.getStreamSource().getSourceName());
    }

    @Test
    public void testUpdateAuthor() throws Exception {
        Date registrationDate = new Date();
        StreamSource testSource = new StreamGroup("testGroup", "http://testGroup2.de");
        Author testAuthor = new Author("test", 10, 2, 3, registrationDate, testSource);
        persistenceLayer.saveAuthor(testAuthor);
        Author originalResult = persistenceLayer.loadAuthor(testAuthor.getUsername(), testAuthor.getStreamSource());
        assertEquals(originalResult, testAuthor);

        Author changedAuthor = new Author("test", 30, 5, 4, registrationDate, testSource);
        persistenceLayer.saveAuthor(changedAuthor);
        Author result = persistenceLayer.loadAuthor(changedAuthor.getUsername(), changedAuthor.getStreamSource());
        assertEquals(result, changedAuthor);

    }

    @Test
    public void testSaveStreamWithAuthors() {

        ItemStream itemStream = new ItemStream("testSource", "http://testSource1.de");

        Author author1 = new Author("author1", itemStream);
        Item item1 = new Item("id1", author1, "http://testSource1.de/item1", "title1", new Date(), new Date(), "");
        itemStream.addItem(item1);

        Author author2 = new Author("author2", itemStream);
        Item item2 = new Item("id2", author2, "http://testSource1.de/item2", "title2", new Date(), new Date(), "");
        itemStream.addItem(item2);

        // author3 is actually == author1,
        // so the existing author should just be updated
        Author author3 = new Author("author1", itemStream);
        Item item3 = new Item("id3", author3, "http://testSource1.de/item3", "title3", new Date(), new Date(), "");
        itemStream.addItem(item3);

        persistenceLayer.saveStreamSource(itemStream);

        ItemStream loadedStream = (ItemStream)persistenceLayer.loadStreamSourceByAddress("http://testSource1.de");
        Assert.assertEquals(2, loadedStream.getAuthors().size());
        List<Item> items = loadedStream.getItems();
        Assert.assertEquals("author1", items.get(0).getAuthor().getUsername());
        Assert.assertEquals("author2", items.get(1).getAuthor().getUsername());
        Assert.assertEquals("author1", items.get(2).getAuthor().getUsername());

        Author loadedAuthor = persistenceLayer.loadAuthor("author1", itemStream);
        Assert.assertEquals(1, loadedAuthor.getItems().size());
    }

    @Test
    public void testSaveItem() throws Exception {
        ItemStream testSource = new ItemStream("testGroup", "http://testGroup3.de");
        Author testAuthor = new Author("testUser", 3, 2, 1, new Date(), testSource);
        Item testItem = new Item("testItem", testAuthor, "http://testSource.de/testItem", "testItem", new Date(),
                new Date(), "testItemText", null, ItemType.OTHER);
        testSource.addItem(testItem);

        persistenceLayer.saveStreamSource(testSource);
        persistenceLayer.saveItem(testItem);
    }

    @Test
    public void testSaveRelationType() throws Exception {
        RelationType relationType = new RelationType("duplicate");
        persistenceLayer.saveRelationType(relationType);

        ItemStream testSource = new ItemStream("testGroup", "http://testGroup4.de");

        Author testAuthor = new Author("testUser", 3, 2, 1, new Date(), testSource);
        Item testItem1 = new Item("testItem1", testAuthor, "http://testSource.de/testItem", "testItem", new Date(),
                new Date(), "testItemText");
        Item testItem2 = new Item("testItem2", testAuthor, "http://testSource.de/testItem2", "testItem2", new Date(),
                new Date(), "testItemText");
        testSource.addItem(testItem1);
        testSource.addItem(testItem2);
        persistenceLayer.saveStreamSource(testSource);
        // persistenceLayer.saveItem(testItem1);
        // persistenceLayer.saveItem(testItem2);

        // persistenceLayer.createItemRelation(testItem1, testItem2, relationType, "duplicates");
        ItemRelation itemRelation = new ItemRelation(testItem1, testItem2, relationType, "duplicates");
        persistenceLayer.saveItemRelation(itemRelation);

    }

    /**
     * Test to save composite {@link StreamSource} structures.
     */
    @Test
    public void testSaveStreamGroup() {
        StreamGroup grandParentGroup = new StreamGroup("testSource", "http://testSource.de/testStream");

        StreamGroup parentGroup1 = new StreamGroup("testSource1", "http://testSource.de/testStream1");
        StreamGroup parentGroup2 = new StreamGroup("testSource2", "http://testSource.de/testStream2");
        grandParentGroup.addChild(parentGroup1);
        grandParentGroup.addChild(parentGroup2);

        StreamGroup childGroup1 = new StreamGroup("testSource11", "http://testSource.de/testStream11");
        StreamGroup childGroup2 = new StreamGroup("testSource12", "http://testSource.de/testStream12");
        StreamGroup childGroup3 = new StreamGroup("testSource13", "http://testSource.de/testStream13");
        parentGroup1.addChild(childGroup1);
        parentGroup1.addChild(childGroup2);
        parentGroup1.addChild(childGroup3);

        persistenceLayer.saveStreamSource(grandParentGroup);

        StreamSource streamSource = persistenceLayer.loadStreamSourceByAddress("http://testSource.de/testStream1");
        assertTrue(streamSource instanceof StreamGroup);
        StreamGroup streamGroup = (StreamGroup)streamSource;
        assertEquals(3, streamGroup.getChildren().size());
        assertEquals("testSource", streamGroup.getParentSource().getSourceName());
        System.out.println(childGroup3.getQualifiedSourceName());

        streamSource = persistenceLayer.loadStreamSourceByAddress("http://testSource.de/testStream");
        assertNull(streamSource.getParentSource());
    }

    @Test
    public void testSaveTwoStreamsSharingAuthor() throws Exception {
        StreamGroup forum = new StreamGroup("Test Forum", "http://testforum.de/");
        ItemStream firstStream = new ItemStream("Title1", "http://testforum.de/t1");
        ItemStream secondStream = new ItemStream("Title2", "http://testforum.de/t2");

        forum.addChild(firstStream);
        forum.addChild(secondStream);

        Author firstAsker = new Author("a1", 1, 1, 0, new Date(), forum);
        Item item11 = new Item("0", firstAsker, "http://testforum.de/t1/i1", "Title1", new Date(), new Date(),
                "Hello World?", null, ItemType.QUESTION);
        firstStream.addItem(item11);

        Author answerer = new Author("a2", 2, 0, 1, new Date(), forum);
        Item item12 = new Item("1", answerer, "http://testforum.de/t1/i2", "Title1", new Date(), new Date(),
                "Hello World!", null, ItemType.CORRECT_ANSWER);
        firstStream.addItem(item12);

        Author secondAsker = new Author("a3", 1, 1, 0, new Date(), forum);
        Item item21 = new Item("2", secondAsker, "http://testforum.de/t2/i1", "Title2", new Date(), new Date(),
                "Hello World again?", null, ItemType.QUESTION);
        secondStream.addItem(item21);
        Item item22 = new Item("3", answerer, "http://testforum.de/t2/i2", "Title2", new Date(), new Date(),
                "Hello World again!", item21, ItemType.CORRECT_ANSWER);
        secondStream.addItem(item22);

        persistenceLayer.saveStreamSource(forum);
    }
}
