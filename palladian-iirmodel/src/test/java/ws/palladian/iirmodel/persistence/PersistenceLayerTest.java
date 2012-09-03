/**
 * Created on: 01.07.2011 16:34:00
 */
package ws.palladian.iirmodel.persistence;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.iirmodel.Author;
import ws.palladian.iirmodel.Item;
import ws.palladian.iirmodel.ItemRelation;
import ws.palladian.iirmodel.ItemStream;
import ws.palladian.iirmodel.RelationType;
import ws.palladian.iirmodel.StreamGroup;
import ws.palladian.iirmodel.StreamSource;
import ws.palladian.iirmodel.UndirectedItemRelation;

/**
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 3.0
 * @since 1.0
 */
public class PersistenceLayerTest {

    private ModelPersistenceLayer persistenceLayer;
    private EntityManagerFactory emFactory;
    private static final String TEST_PERSISTENCE_UNIT_NAME = System.getProperty("persistenceunitname");

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        System.err.println("setup");
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
        
        EntityManager entityManager = emFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        entityManager.createNativeQuery("SHUTDOWN").executeUpdate();
        transaction.commit();
        entityManager.close();
        
        if (emFactory != null && emFactory.isOpen()) {
            emFactory.close();
        }
        emFactory = null;

//        DriverManager.registerDriver(new org.hsqldb.jdbcDriver());
//        Connection conn = DriverManager.getConnection("jdbc:hsqldb:mem:testdb", "sa", "");
//        Statement statement = null;
//        try {
//            if (conn != null) {
//                statement = conn.createStatement();
//                statement.execute("SHUTDOWN");
//                conn.commit();
//            }
//        } finally {
//            if (statement != null) {
//                statement.close();
//            }
//            conn.close();
//        }
    }

    /**
     * Test to save {@link ItemStream} without any {@link Item}s.
     */
    @Test
    public void testSaveStreamSource() {
        try {
            ItemStream stream = new ItemStream("testSource", "http://testSource.de/testStream");
            persistenceLayer.saveItemStream(stream);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unable to save new ItemStream due to: " + e.getStackTrace());
        }
    }

    /**
     * Test to save {@link ItemStream} with attached {@link Item}s.
     */
    @Test
    public void testSaveComplexItemStream() {
        ItemStream stream = new ItemStream("testSource", "http://testSource.de/testStream");
        Author author1 = new Author("a1", 10, 2, 5, new Date(), stream.getSourceAddress());
        Item item1 = new Item("i1", author1, "http://testSource.de/testStream/i1", "i1", new Date(), new Date(),
                "i1text", null);
        stream.addItem(item1);
        Author author2 = new Author("a2", 4, 0, 3, new Date(), stream.getSourceAddress());
        Item item2 = new Item("i2", author2, "http://testSource.de/testStream/i2", "i2", new Date(), new Date(),
                "i2text", item1);
        stream.addItem(item2);
        stream.addAuthor(author1);
        stream.addAuthor(author2);

        persistenceLayer.saveItemStream(stream);
        ItemStream result = (ItemStream)persistenceLayer.loadStreamSourceByAddress(stream.getSourceAddress());
        assertEquals(1, persistenceLayer.loadStreamSources().size());
        assertEquals(stream.getSourceAddress(), result.getSourceAddress());
        assertEquals(stream.getSourceName(), result.getSourceName());
        assertEquals(stream.getItems().size(), result.getItems().size());
        Item firstResultItem = stream.getItems().get(0);
        assertNotNull(firstResultItem);
        assertEquals(item1.getLink(), firstResultItem.getLink());
        assertEquals(item1.getPublicationDate(), firstResultItem.getPublicationDate());
        assertEquals(item1.getSourceInternalIdentifier(), firstResultItem.getSourceInternalIdentifier());
        assertEquals(item1.getText(), firstResultItem.getText());
        assertEquals(item1.getTitle(), firstResultItem.getTitle());
        assertEquals(item1.getUpdateDate(), firstResultItem.getUpdateDate());
        Author firstItemResultAuthor = firstResultItem.getAuthor();
        assertEquals(author1.getAuthorRating(), firstItemResultAuthor.getAuthorRating());
        assertEquals(author1.getCountOfItems(), firstItemResultAuthor.getCountOfItems());
        assertEquals(author1.getCountOfStreamsStarted(), firstItemResultAuthor.getCountOfStreamsStarted());
        // assertEquals(author1.getItems().size(), firstItemResultAuthor.getItems().size());
        assertEquals(author1.getRegisteredSince(), firstItemResultAuthor.getRegisteredSince());
        assertNotNull(firstItemResultAuthor.getStreamSource());
        assertEquals(author1.getUsername(), firstItemResultAuthor.getUsername());
    }

    @Test
    @Ignore
    public void testSaveChangedItemStream() throws Exception {

        // save an ItemStream
        ItemStream stream = new ItemStream("testSource", "http://testSource.de/testStream");
        Author author1 = new Author("a1", 10, 2, 5, new Date(), stream.getSourceAddress());
        Item item1 = new Item("i1", author1, "http://testSource.de/testStream/i1", "i1", new Date(), new Date(),
                "i1text", null);
        stream.addItem(item1);
        Author author2 = new Author("a2", 4, 0, 3, new Date(), stream.getSourceAddress());
        Date author2RegistrationDate = new Date();
        Item item2 = new Item("i2", author2, "http://testSource.de/testStream/i2", "i2", author2RegistrationDate,
                new Date(), "i2text", item1);
        stream.addItem(item2);
        stream.addAuthor(author1);
        stream.addAuthor(author2);

        persistenceLayer.saveItemStream(stream);

        // save the same ItemStream again; ItemStream gets updated
        ItemStream changedStream = new ItemStream("testSource", "http://testSource.de/testStream");

        Author changedAuthor1 = new Author("a2", 11, 3, 5, author2RegistrationDate, changedStream.getSourceAddress());
        Item changedItem1 = new Item("i2", changedAuthor1, "http://testSource.de/testStream/i2", "i2",
                item2.getPublicationDate(), item2.getUpdateDate(), "i2text", null);
        changedStream.addItem(changedItem1);
        Author changedAuthor2 = new Author("a3", 11, 3, 5, new Date(), changedStream.getSourceAddress());
        Item changedItem2 = new Item("i3", changedAuthor2, "http://testSource.de/testStream/i3", "i3", new Date(),
                new Date(), "i3text", changedItem1);
        changedStream.addItem(changedItem2);
        changedStream.addAuthor(changedAuthor1);
        changedStream.addAuthor(changedAuthor2);

        persistenceLayer.saveItemStream(changedStream);

        ItemStream loadedStream = (ItemStream)persistenceLayer
                .loadStreamSourceByAddress("http://testSource.de/testStream");

        Item deletedItem = persistenceLayer.loadItem(loadedStream.getIdentifier());
        assertNull(deletedItem);
        assertEquals(2, loadedStream.getItems().size());
        assertEquals("http://testSource.de/testStream", loadedStream.getSourceAddress());
        assertEquals("i2", loadedStream.getItems().get(0).getSourceInternalIdentifier());
        assertEquals("i3", loadedStream.getItems().get(1).getSourceInternalIdentifier());
        assertEquals(2, loadedStream.getAuthors().size());
        assertEquals(Integer.valueOf(5), loadedStream.getItems().get(0).getAuthor().getAuthorRating());
    }

    @Test
    public void testSaveAuthor() throws Exception {
        Date registrationDate = new Date();
        StreamSource testSource = new StreamGroup("testGroup", "http://testGroup.de");
        Author testAuthor = new Author("test", 10, 2, 3, registrationDate, testSource.getSourceAddress());
        persistenceLayer.saveAuthor(testAuthor);
        Author result = persistenceLayer.loadAuthor(testAuthor.getUsername(), testAuthor.getStreamSource());
        assertNotNull(result);
        assertEquals("test", result.getUsername());
        assertEquals(Integer.valueOf(10), result.getCountOfItems());
        assertEquals(Integer.valueOf(2), result.getCountOfStreamsStarted());
        assertEquals(Integer.valueOf(3), result.getAuthorRating());
        assertEquals(registrationDate, result.getRegisteredSince());
        // assertEquals("testGroup", result.getStreamSource().getSourceName());
    }

    @Test
    @Ignore
    public void testUpdateAuthor() throws Exception {
        Date registrationDate = new Date();
        StreamSource testSource = new StreamGroup("testGroup", "http://testGroup.de");
        Author testAuthor = new Author("test", 10, 2, 3, registrationDate, testSource.getSourceAddress());
        persistenceLayer.saveAuthor(testAuthor);
        Author originalResult = persistenceLayer.loadAuthor(testAuthor.getUsername(), testAuthor.getStreamSource());
        assertEquals(originalResult, testAuthor);

        Author changedAuthor = new Author("test", 30, 5, 4, registrationDate, testSource.getSourceAddress());
        persistenceLayer.saveAuthor(changedAuthor);
        Author result = persistenceLayer.loadAuthor(changedAuthor.getUsername(), changedAuthor.getStreamSource());
        assertThat(result.getCountOfItems(), is(30));
        assertThat(result.getCountOfStreamsStarted(), is(5));
        assertThat(result.getAuthorRating(), is(4));
        Collection<Author> authors = persistenceLayer.loadAuthors();
        assertThat(authors.size(), is(1));
    }

    @Test
    public void testSaveSameAuthorTwice() throws Exception {
        try {
            Author firstAuthorObject = new Author("test", 10, 2, 3, new Date(), "http://testGroup.de");
            Author secondAuthorObject = new Author("test", 10, 2, 3, new Date(), "http://testGroup.de");
            persistenceLayer.saveAuthor(firstAuthorObject);
            persistenceLayer.saveAuthor(secondAuthorObject);
            fail("Saving the same author twice should throw a RuntimeException.");
        } catch (RuntimeException e) {
            // Do nothing! This exception is expected in this case.
        }
    }

    // TODO automatic merging of authors is not working at the moment.
    @Test
    @Ignore
    public void testSaveStreamWithAuthors() {

        ItemStream itemStream = new ItemStream("testSource", "http://testSource.de");

        Author author1 = new Author("author1", itemStream.getSourceAddress());
        Item item1 = new Item("id1", author1, "http://testSource.de/item1", "title1", new Date(), new Date(), "");
        itemStream.addItem(item1);
        itemStream.addAuthor(author1);

        Author author2 = new Author("author2", itemStream.getSourceAddress());
        Item item2 = new Item("id2", author2, "http://testSource.de/item2", "title2", new Date(), new Date(), "");
        itemStream.addItem(item2);
        itemStream.addAuthor(author2);

        // author3 is actually == author1,
        // so the existing author should just be updated
        Author author3 = new Author("author1", itemStream.getSourceAddress());
        Item item3 = new Item("id3", author3, "http://testSource.de/item3", "title3", new Date(), new Date(), "");
        itemStream.addItem(item3);
        itemStream.addAuthor(author3);

        persistenceLayer.saveItemStream(itemStream);

        ItemStream loadedStream = (ItemStream)persistenceLayer.loadStreamSourceByAddress("http://testSource.de");
        Assert.assertEquals(2, loadedStream.getAuthors().size());
        List<Item> items = loadedStream.getItems();
        Assert.assertEquals("author1", items.get(0).getAuthor().getUsername());
        Assert.assertEquals("author2", items.get(1).getAuthor().getUsername());
        Assert.assertEquals("author1", items.get(2).getAuthor().getUsername());

        Author loadedAuthor = persistenceLayer.loadAuthor("author1", itemStream.getSourceAddress());
        // Assert.assertEquals(1, loadedAuthor.getItems().size());
    }

    @Test
    public void testSaveItem() throws Exception {
        ItemStream testSource = new ItemStream("testGroup", "http://testGroup.de");
        Author testAuthor = new Author("testUser", 3, 2, 1, new Date(), testSource.getSourceAddress());
        Item testItem = new Item("testItem", testAuthor, "http://testSource.de/testItem", "testItem", new Date(),
                new Date(), "testItemText", null);
        testSource.addItem(testItem);

        persistenceLayer.saveItemStream(testSource);
        persistenceLayer.saveItem(testItem);
    }

    @Test
    public void testSaveRelationType() throws Exception {
        RelationType relationType = new RelationType("duplicate", "duplicate");
        persistenceLayer.saveRelationType(relationType);

        ItemStream testSource = new ItemStream("testGroup", "http://testSource.de");

        Author testAuthor = new Author("testUser", 3, 2, 1, new Date(), testSource.getSourceAddress());
        Item testItem1 = new Item("testItem1", testAuthor, "http://testSource.de/testItem", "testItem", new Date(),
                new Date(), "testItemText");
        Item testItem2 = new Item("testItem2", testAuthor, "http://testSource.de/testItem2", "testItem2", new Date(),
                new Date(), "testItemText");
        testSource.addItem(testItem1);
        testSource.addItem(testItem2);
        persistenceLayer.saveItemStream(testSource);
        // persistenceLayer.saveItem(testItem1);
        // persistenceLayer.saveItem(testItem2);

        // persistenceLayer.createItemRelation(testItem1, testItem2, relationType, "duplicates");
        ItemRelation itemRelation = new UndirectedItemRelation(testItem1, testItem2, relationType, "duplicates");
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

        persistenceLayer.saveStreamGroup(grandParentGroup);

        StreamSource streamSource = persistenceLayer.loadStreamSourceByAddress("http://testSource.de/testStream1");
        assertTrue(streamSource instanceof StreamGroup);
        StreamGroup streamGroup = (StreamGroup)streamSource;
        assertEquals(3, streamGroup.getChildren().size());
        assertEquals("testSource", streamGroup.getParentSource().getSourceName());

        streamSource = persistenceLayer.loadStreamSourceByAddress("http://testSource.de/testStream");
        assertNull(streamSource.getParentSource());
        assertEquals("testSource", streamSource.getSourceName());
        assertEquals("http://testSource.de/testStream", streamSource.getSourceAddress());
        streamGroup = (StreamGroup)streamSource;
        assertEquals(2, streamGroup.getChildren().size());
        assertEquals("testSource1", streamGroup.getChildren().get(0).getSourceName());
    }

    @Test
    public void testSaveTwoStreamsSharingAuthor() throws Exception {
        StreamGroup forum = new StreamGroup("Test Forum", "http://testforum.de/");
        ItemStream firstStream = new ItemStream("Title1", "http://testforum.de/t1");
        ItemStream secondStream = new ItemStream("Title2", "http://testforum.de/t2");

        forum.addChild(firstStream);
        forum.addChild(secondStream);

        Author firstAsker = new Author("a1", 1, 1, 0, new Date(), forum.getSourceAddress());
        Item item11 = new Item("0", firstAsker, "http://testforum.de/t1/i1", "Title1", new Date(), new Date(),
                "Hello World?", null);
        firstStream.addItem(item11);
        firstStream.addAuthor(firstAsker);

        Author answerer = new Author("a2", 2, 0, 1, new Date(), forum.getSourceAddress());
        Item item12 = new Item("1", answerer, "http://testforum.de/t1/i2", "Title1", new Date(), new Date(),
                "Hello World!", null);
        firstStream.addItem(item12);
        firstStream.addAuthor(answerer);

        Author secondAsker = new Author("a3", 1, 1, 0, new Date(), forum.getSourceAddress());
        Item item21 = new Item("2", secondAsker, "http://testforum.de/t2/i1", "Title2", new Date(), new Date(),
                "Hello World again?", null);
        secondStream.addItem(item21);
        secondStream.addAuthor(secondAsker);

        Item item22 = new Item("3", answerer, "http://testforum.de/t2/i2", "Title2", new Date(), new Date(),
                "Hello World again!", item21);
        secondStream.addItem(item22);
        secondStream.addAuthor(answerer);

        persistenceLayer.saveStreamGroup(forum);
    }

    @Test
    public void testSaveStreamWithMultipleAuthorOccurrences() {
        StreamGroup forum = new StreamGroup("testgroup", "http://testgroup.com");

        ItemStream itemStream1 = new ItemStream("teststream1", "http://testgroup.com/teststream1");
        forum.addChild(itemStream1);
        Author author1 = new Author("u1", "http://testgroup.com");
        forum.addAuthor(author1);
        Item item1 = new Item("id1", author1, "http://testgroup.com/teststream1/item1", "item1", new Date(),
                new Date(), "");
        itemStream1.addItem(item1);

        ItemStream itemStream2 = new ItemStream("teststream2", "http://testgroup.com/teststream2");
        forum.addChild(itemStream2);

        // retrieve Author by his username, as we must avoid duplicate author instances!
        Author author2 = forum.getAuthor("u1");
        forum.addAuthor(author2);
        Item item2 = new Item("id2", author2, "http://testgroup.com/teststream1/item2", "item2", new Date(),
                new Date(), "");
        itemStream2.addItem(item2);

        persistenceLayer.saveStreamSource(forum);
    }
}
