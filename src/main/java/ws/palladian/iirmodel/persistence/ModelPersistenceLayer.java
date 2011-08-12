/**
 * Created on: 20.11.2009 23:53:26
 */
package ws.palladian.iirmodel.persistence;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.log4j.Logger;

import ws.palladian.iirmodel.Author;
import ws.palladian.iirmodel.Item;
import ws.palladian.iirmodel.ItemRelation;
import ws.palladian.iirmodel.ItemStream;
import ws.palladian.iirmodel.RelationType;
import ws.palladian.iirmodel.StreamGroup;
import ws.palladian.iirmodel.StreamSource;

/**
 * <p>
 * Instances of this class represent the interface between an application and the Palladian IIR model.
 * </p>
 * <p>
 * Object of this class are not thread safe. Create them inside each thread on the fly if you need to load or persist
 * something.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 3.0
 * @since 1.0
 * 
 */
public class ModelPersistenceLayer extends AbstractPersistenceLayer {

    /**
     * <p>
     * The logger for objects of this class. Configure it using <tt>src/main/resources/log4j.xml</tt>.
     * </p>
     */
    private static final Logger LOGGER = Logger.getLogger(ModelPersistenceLayer.class);
    /**
     * <p>
     * The query for getting all pairs of {@code Item}s from different {@code ItemStream}s that have no relation yet.
     * </p>
     * 
     * @see #getNonAnnotatedInterThreadRelation(Map)
     */
    private static final String GET_NON_ANNOTATED_INTER_STREAM_RELATIONS = "SELECT i1.identifier,i2.identifier "
            + "FROM Item i1, Item i2 " + "WHERE i1.identifier <> i2.identifier " + "AND i1.identifier NOT IN "
            + "(SELECT DISTINCT ir1.firstEntry.identifier FROM ItemRelation ir1) " + "AND i1.identifier NOT IN "
            + "(SELECT DISTINCT ir2.secondEntry.identifier FROM ItemRelation ir2) " + "AND i2.identifier NOT IN "
            + "(SELECT DISTINCT ir3.firstEntry.identifier FROM ItemRelation ir3) " + "AND i2.identifier NOT IN "
            + "(SELECT DISTINCT ir4.secondEntry.identifier FROM ItemRelation ir4) " + "AND i1.parent<>i2.parent";
    /**
     * <p>
     * The query for getting all pairs of {@code Item}s from the same {@code ItemStream} that have no relation yet.
     * </p>
     * 
     * @see #getNonAnnotatedIntraThreadRelation(Map)
     */
    private static final String GET_NON_ANNOTATED_INTRA_STREAM_RELATIONS = "SELECT i1.identifier,i2.identifier "
            + "FROM Item i1, Item i2 " + "WHERE i1.identifier <> i2.identifier " + "AND i1.identifier NOT IN "
            + "(SELECT DISTINCT ir1.firstEntry.identifier FROM ForumEntryRelation ir1) " + "AND i1.identifier NOT IN "
            + "(SELECT DISTINCT ir2.secondEntry.identifier FROM ForumEntryRelation ir2) " + "AND i2.identifier NOT IN "
            + "(SELECT DISTINCT ir3.firstEntry.identifier FROM ForumEntryRelation ir3) " + "AND i2.identifier NOT IN "
            + "(SELECT DISTINCT ir4.secondEntry.identifier FROM ForumEntryRelation ir4) " + "AND i1.parent=i2.parent";

    /**
     * <p>
     * The maximum count of non annotated relations that are returned during one query.
     * </p>
     * 
     * @see #getRandomNonAnnotatedRelation(String)
     */
    private static final Integer MAX_NUMBER_OF_NON_ANNOTATED_RELATIONS = 100;

    /**
     * <p>
     * Creates a new {@code ModelPersistenceLayer} working on the provided JPA {@code EntityManager}. To acquire an
     * {@code EntityManager} do something like:
     * </p>
     * <code>
     * EntityManagerFactory factory = Persistence.createEntityManagerFactory("iirmodel");<br/>
     * ModelPersistenceLayer pl = new ModelPersistenceLayer(factory.createEntityManager());
     * </code>
     * 
     * <p>
     * The parameter of {@link Persistence#createEntityManagerFactory(String)} must be the identifier of a valid
     * persistence unit, such as the one provided in <tt>src/main/resources/persistence.xml</tt>.
     * </p>
     * 
     * @param manager The {@code EntityManager} this persistence layer should use to access the database.
     */
    public ModelPersistenceLayer(final EntityManager manager) {
        super(manager);
    }

    public final void saveStreamSource(final StreamGroup streamGroup) {
//        Boolean openedTransaction = openTransaction();
//        ItemStream existingStream = (ItemStream)loadStreamSourceByAddress(stream.getSourceAddress());
//
//        if (existingStream == null) {
//            getManager().persist(stream);
//        } else {
//            stream.setIdentifier(existingStream.getIdentifier());
//            Collection<Item> removedItems = getRemovedItems(existingStream, stream);
//            getManager().merge(stream);
//            removeItems(removedItems);
//        }
//        updateAuthors(stream.getItems());
//        commitTransaction(openedTransaction);
    }

    /**
     * <p>
     * Saves a {@code StreamSource}. If the StreamSource is an {@link ItemStream}, all its {@link Item}s are also saved
     * to the database. If the stream already exists, the representation in the database is updated with the new values
     * from this {@code ItemStream}. This means old values are overwritten if changed and the collections of
     * {@code Item}s are merged. Existing {@code Item}s are updated with the new values from the new {@code ItemStream}
     * and new {@code Item}s are added. However old {@code Item}s not in the new {@code ItemStream} are not deleted but
     * simply kept. For each {@code Item} the operation also updates the {@link Author} information if necessary. This
     * means that new {@code Author}s are added to the database and existing ones are updated with new values.
     * </p>
     * 
     * @param stream The {@link StreamSource} to save to the database.
     */
    public final void saveStreamSource(final ItemStream itemStream) {
        Boolean openedTransaction = openTransaction();
        StreamSource existingStreamSource = loadStreamSourceByAddress(itemStream.getSourceAddress());
        for (Item item : itemStream.getItems()) {
            getManager().persist(item.getAuthor());

            // Item existingItem = loadItem(item.getSourceInternalIdentifier(),item.getParent());
            // getManager().persist(item);
        }

        if (existingStreamSource == null) {
            getManager().persist(itemStream);
        } else {
            itemStream.setIdentifier(existingStreamSource.getIdentifier());
            getManager().merge(itemStream);
        }

        //
        // for (Item item : itemStream.getItems()) {
        // getManager().persist(item);
        // }
        // if (streamSource instanceof ItemStream) {
        // ItemStream itemStream = (ItemStream)streamSource;
        // saveItemStream(itemStream);
        // } else {
        // StreamSource existingStream = loadStreamSourceByAddress(streamSource.getSourceAddress());
        // if (existingStream == null) {
        // getManager().persist(streamSource);
        // } else {
        // getManager().merge(existingStream);
        // }
        // }
        commitTransaction(openedTransaction);
    }

    /**
     * @param removedItems
     */
    private void removeItems(Collection<Item> removedItems) {
        final Boolean openedTransaction = openTransaction();
        for (Item item : removedItems) {
            getManager().remove(item);
        }
        commitTransaction(openedTransaction);
    }

    /**
     * @param existingStream
     * @param stream
     * @return
     */
    protected Collection<Item> getRemovedItems(ItemStream existingStream, ItemStream stream) {
        Collection<Item> ret = new HashSet<Item>();
        for (Item item : existingStream.getItems()) {
            if (!stream.getItems().contains(item)) {
                ret.add(item);
            }
        }
        return ret;
    }

    //
    // /**
    // * @param items
    // */
    // private void saveItems(List<Item> items) {
    // for (Item item : items) {
    // saveItem(item);
    // }
    // }

    /**
     * <p>
     * Updates all {@code Author}s occuring in the provided list of items.
     * </p>
     * 
     * @param items The items containing possibly new or changed {@code Author} information.
     */
    private void updateAuthors(final List<Item> items) {
        for (Item item : items) {
            item.setAuthor(saveAuthor(item.getAuthor()));
        }
    }

    // /**
    // * <p>
    // * Removes all {@code Item}s from an existing stream, that are not also present in a new stream.
    // * </p>
    // *
    // * @param existingStream The original stream that should be adapted to the current state in {@code newStream}.
    // * @param newStream The {@code ItemStream} representing the new content.
    // */
    // private void removeNonExistingItems(final ItemStream existingStream, final ItemStream newStream) {
    // for (Item item : existingStream.getItems()) {
    // if (!newStream.getItems().contains(item)) {
    // // List<Item> successorItems = getSuccessorItems(newStream, item);
    // // for (Item successorItem : successorItems) {
    // // successorItem.setPredecessor(null);
    // // }
    // removeItem(item);
    // }
    // }
    // }

    // /**
    // * @param newStream
    // * @param item
    // * @return
    // */
    // private List<Item> getSuccessorItems(ItemStream newStream, Item item) {
    // List<Item> ret = new LinkedList<Item>();
    // for (Item streamItem : newStream.getItems()) {
    // if (item.equals(streamItem.getPredecessor())) {
    // ret.add(streamItem);
    // }
    // }
    // return ret;
    // }

    /**
     * <p>
     * Completely removes an {@code Item} from the database.
     * </p>
     * 
     * @param item The {@code Item} to remove.
     */
    public void removeItem(Item item) {
        final Boolean openedTransaction = openTransaction();
        getManager().remove(item);
        commitTransaction(openedTransaction);
    }

    // /**
    // * @param existingThread
    // * @param thread
    // */
    // private void addNewItems(final ItemStream existingThread, final ItemStream thread) {
    // for (Item newEntry : thread.getItems()) {
    // saveItem(newEntry);
    // }
    // }

    /**
     * @param newEntry
     */
    protected void saveItem(final Item entry) {
        Author savedAuthor = saveAuthor(entry.getAuthor());
        entry.setAuthor(savedAuthor);
        Item existingEntry = loadItem(entry.getIdentifier());
        final Boolean openedTransaction = openTransaction();
        if (existingEntry == null) {
            getManager().persist(entry);
        } else {
            // entry.setIdentifier(existingEntry.getIdentifier());
            // getManager().merge(entry);
            existingEntry.setAuthor(entry.getAuthor());
            existingEntry.setLink(entry.getLink());
            existingEntry.setParent(entry.getParent());
            existingEntry.setPredecessor(entry.getPredecessor());
            existingEntry.setPublicationDate(entry.getPublicationDate());
            existingEntry.setSourceInternalIdentifier(entry.getSourceInternalIdentifier());
            existingEntry.setText(entry.getText());
            existingEntry.setTitle(entry.getText());
            existingEntry.setType(entry.getType());
            existingEntry.setUpdateDate(entry.getUpdateDate());
            existingEntry = getManager().merge(existingEntry);
        }
        commitTransaction(openedTransaction);
    }

    /**
     * <p>
     * Saves this {@code Author} to the database or updates its state if it already exists.
     * </p>
     * 
     * @param author The {@code Author} to save.
     */
    public Author saveAuthor(final Author author) {
        // final Author existingUser = loadAuthor(author.getIdentifier());
        final Boolean openedTransaction = openTransaction();
        final Author existingUser = loadAuthor(author.getUsername(), author.getStreamSource());
        if (existingUser == null) {
            getManager().persist(author);
            // getManager().persist(author.getStreamSource());
        } else {
            author.setIdentifier(existingUser.getIdentifier());
            getManager().merge(author);
        }
        // saveStreamSource(author.getStreamSource());

        //
        Author ret = null;
        //
        // if (existingUser == null) {
        // getManager().persist(author);
        // ret = author;
        // } else {
        // // author.setIdentifier(existingUser.getIdentifier());
        // // ret = getManager().merge(author);
        // existingUser.setAuthorRating(author.getAuthorRating());
        // existingUser.setCountOfItems(author.getCountOfItems());
        // existingUser.setCountOfStreamsStarted(author.getCountOfItems());
        // // TODO need to merge Items?
        // existingUser.setItems(author.getItems());
        // existingUser.setRegisteredSince(author.getRegisteredSince());
        // existingUser.setStreamSource(author.getStreamSource());
        // existingUser.setUsername(author.getUsername());
        // ret = getManager().merge(existingUser);
        // }
        commitTransaction(openedTransaction);
        return ret;
    }

    /**
     * @param username
     * @param streamSource
     * @return
     */
    public Author loadAuthor(String username, String streamSource) {
        Query authorQuery = getManager().createQuery(
                "SELECT a FROM Author a WHERE a.username=:username AND a.streamSourceAddress=:streamSource");
        authorQuery.setParameter("username", username);
        authorQuery.setParameter("streamSource", streamSource);

        Boolean openedTransaction = openTransaction();
        // EntityTransaction tx = getManager().getTransaction();

        // tx.begin();
        @SuppressWarnings("unchecked")
        List<Author> authors = authorQuery.getResultList();
        Author ret = getFirst(authors);
        commitTransaction(openedTransaction);
        return ret;
    }

    /**
     * 
     * @param item1
     * @param item2
     * @param relationType
     * @param comment
     * @return
     * @deprecated Create the {@link ItemRelation} with its constructor and use {@link #saveItemRelation(ItemRelation)}
     *             to store them.
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public ItemRelation createItemRelation(Item item1, Item item2, RelationType relationType, String comment) {
        Query checkIfRelationExistsQuery = getManager().createQuery(
                "SELECT r FROM ItemRelation r WHERE r.firstItem=:firstItem AND r.secondItem=:secondItem");
        checkIfRelationExistsQuery.setParameter("firstItem", item1);
        checkIfRelationExistsQuery.setParameter("secondItem", item2);
        getManager().getTransaction().begin();
        List<ItemRelation> relations = checkIfRelationExistsQuery.getResultList();
        ItemRelation ret = null;
        if (relations.isEmpty()) {
            ret = new ItemRelation(item1, item2, relationType, comment);
            getManager().persist(ret);
        } else {
            ret = relations.get(0);
            ret.setType(relationType);
            ret.setComment(comment);
        }
        getManager().getTransaction().commit();

        return ret;
    }

    public void saveItemRelation(ItemRelation itemRelation) {
        Query relationExistsQuery = getManager().createQuery(
                "SELECT r FROM ItemRelation r WHERE (r.firstItem=:firstItem AND r.secondItem=:secondItem) "
                        + "OR (r.firstItem=:secondItem AND r.secondItem=:firstItem) " + "AND r.type=:type");
        relationExistsQuery.setParameter("firstItem", itemRelation.getFirstItem());
        relationExistsQuery.setParameter("secondItem", itemRelation.getSecondItem());
        relationExistsQuery.setParameter("type", itemRelation.getType());
        EntityTransaction tx = getManager().getTransaction();
        tx.begin();
        @SuppressWarnings("unchecked")
        List<ItemRelation> relations = relationExistsQuery.getResultList();
        if (relations.isEmpty()) {
            getManager().persist(itemRelation);
        } else {
            ItemRelation existingRelation = relations.get(0);
            existingRelation.setType(itemRelation.getType());
            existingRelation.setComment(itemRelation.getComment());
        }
        tx.commit();
    }

    /**
     * Adds restriction to the WHERE clause of a base query. These restrictions might be values on some of the threads
     * fields and/or a free text query in Lucene Query Syntax.
     * <p>
     * If one or both of {@code restrictedOnfieldsWithValues} and {@code freeTextQuery} is null, they are silently
     * ignored.
     * 
     * @param baseQuery
     *            The base query to restrict
     * @param restrictedOnFieldsWithValues
     *            Restricts query to a specific set of values
     * @return The complete query as a string.
     */
    private String createQueryForRandomNonAnnotatedRelations(final String baseQuery,
            final Map<String, String[]> restrictedOnFieldsWithValues) {
        StringBuffer query = new StringBuffer(baseQuery);
        if (restrictedOnFieldsWithValues != null && !restrictedOnFieldsWithValues.isEmpty()) {
            query.append(createRestriction(restrictedOnFieldsWithValues));
        }
        return query.toString();
    }

    /**
     * 
     * @param name
     * @return
     * @deprecated Create the {@link RelationType} with its constructor and use {@link #saveRelationType(RelationType)}.
     */
    @Deprecated
    public RelationType createRelationType(String name) {
        RelationType ret = loadRelationType(name);
        if (ret == null) {
            ret = new RelationType(name);
            getManager().getTransaction().begin();
            getManager().persist(ret);
            getManager().getTransaction().commit();
        }
        return ret;
    }

    public void saveRelationType(RelationType relationType) {
        RelationType existingRelationType = loadRelationType(relationType.getName());
        final Boolean openedTransaction = openTransaction();
        if (existingRelationType == null) {
            getManager().persist(relationType);
        } else {
            relationType.setIdentifier(relationType.getIdentifier());
            getManager().merge(relationType);
        }
        commitTransaction(openedTransaction);
    }

    /**
     * <p>
     * Creates restrictions as appendix to an existing JPQL query.
     * </p>
     * 
     * @param fieldValues
     *            Map containing the restrictions.
     * @return A query string starting whit "AND" that can be appended to any
     *         JPQL query joining two contributions.
     */
    private String createRestriction(final Map<String, String[]> fieldValues) {
        final StringBuffer retBuffer = new StringBuffer(29);

        for (Map.Entry<String, String[]> entry : fieldValues.entrySet()) {
            final StringBuffer valuesString = new StringBuffer();
            for (int i = 0; i < entry.getValue().length; i++) {
                valuesString.append("\"" + entry.getValue()[i] + "\",");
            }
            valuesString.deleteCharAt(valuesString.length() - 1);
            retBuffer.append(" AND f1." + entry.getKey() + " IN (" + valuesString + ") AND f2." + entry.getKey()
                    + " IN (" + valuesString + ")");
        }
        return retBuffer.toString();
    }

    // @SuppressWarnings("unchecked")
    // public Author createAuthor(String username, Integer absoluteCountOfPosts, Integer absoluteCountOfThreadsStarted,
    // Integer forumPoints, Date registeredSince, String forumType) {
    // Query checkIfUserExists = getManager().createQuery(
    // "SELECT u FROM User u WHERE u.username=:username AND u.forumType = :forumType");
    // checkIfUserExists.setParameter("username", username);
    // checkIfUserExists.setParameter("forumType", forumType);
    // getManager().getTransaction().begin();
    // List<Author> users = checkIfUserExists.getResultList();
    // Author ret = null;
    // if (!users.isEmpty()) {
    // ret = users.get(0);
    // // ret.setCountOfItems(absoluteCountOfPosts);
    // // ret.setCountOfStreamsStarted(absoluteCountOfThreadsStarted);
    // // ret.setAuthorRating(forumPoints);
    // } else {
    // ret = new Author(username + "@" + forumType, username, absoluteCountOfPosts, absoluteCountOfThreadsStarted,
    // forumPoints, registeredSince, forumType);
    // getManager().persist(ret);
    // }
    // getManager().getTransaction().commit();
    // return ret;
    // }

    // public Author createAuthor(Author author) {
    // return createAuthor(author.getUsername(), author.getCountOfItems(), author.getCountOfStreamsStarted(),
    // author.getAuthorRating(), author.getRegisteredSince(), author.getStreamSource());
    // }

    /**
     * Returns a new non existing relation between two contributions from two different threads. The two contributions
     * are selected by random from the pool of existing contributions.
     * <p>
     * The two parameters are optional to restrict the returned results to some search query or specific fields (like
     * taking only contributions from a certain forum or channel).
     * 
     * @param restrictedOnFieldsWithValues
     *            A map from contribution fields to allowed values.
     * @return A new relation of two contributions.
     * @throws IOException
     *             If underlying lucene index is not available.
     * @throws ParseException
     *             If query is not parseable.
     * @throws URISyntaxException
     *             If location of underlying index was not valid.
     */
    public final ItemRelation getNonAnnotatedInterThreadRelation(
            final Map<String, String[]> restrictedOnFieldsWithValues) throws IOException, ParseException,
            URISyntaxException {
        return getRandomNonAnnotatedRelation(createQueryForRandomNonAnnotatedRelations(
                GET_NON_ANNOTATED_INTER_STREAM_RELATIONS, restrictedOnFieldsWithValues));
    }

    /**
     * <p>
     * Returns a new non existing relation between two contributions from the same thread. The two contributions are
     * selected by random from the pool of existing contributions.
     * </p>
     * <p>
     * The two parameters are optional to restrict the returned results to some search query or specific fields (like
     * taking only contributions from a certain forum or channel).
     * </p>
     * 
     * @param restrictedOnFieldsWithValues
     *            A map from contribution fields to allowed values.
     * @return A new relation of two contributions.
     * @throws IOException
     *             If underlying lucene index is not available.
     * @throws ParseException
     *             If query is not parseable.
     * @throws URISyntaxException
     *             If location of underlying index was not valid.
     */
    public final ItemRelation getNonAnnotatedIntraThreadRelation(
            final Map<String, String[]> restrictedOnFieldsWithValues) throws IOException, ParseException,
            URISyntaxException {
        return getRandomNonAnnotatedRelation(createQueryForRandomNonAnnotatedRelations(
                GET_NON_ANNOTATED_INTRA_STREAM_RELATIONS, restrictedOnFieldsWithValues));
    }

    /**
     * <p>
     * Provides one non annotated relation between two contributions by random.
     * </p>
     * 
     * @param query
     *            The JPQL Query used to search the set of valid contributions.
     * @return A new {@link ItemRelation} that does not exist in the
     *         database.
     * @see #GET_NON_ANNOTATED_INTER_STREAM_RELATIONS
     * @see #GET_NON_ANNOTATED_INTRA_STREAM_RELATIONS
     */
    @SuppressWarnings("unchecked")
    private ItemRelation getRandomNonAnnotatedRelation(final String query) {
        LOGGER.debug("Running getRandomNonAnnotatedRelation with query: " + query);
        Query loadNonEvaluatedForumEntryIDPairs = getManager().createQuery(query);
        loadNonEvaluatedForumEntryIDPairs.setMaxResults(MAX_NUMBER_OF_NON_ANNOTATED_RELATIONS);
        List<Object> results = loadNonEvaluatedForumEntryIDPairs.getResultList();
        LOGGER.debug("Returned results: " + results);
        if (!results.isEmpty()) {
            final Random randomIndexGenerator = new Random();
            int randomIndex = randomIndexGenerator.nextInt(results.size());
            final Object[] idPair = (Object[])results.get(randomIndex);
            // changed from (String) to (Integer); untested
            Item firstEntry = loadItem((Integer)idPair[0]);
            Item secondEntry = loadItem((Integer)idPair[1]);
            return createItemRelation(firstEntry, secondEntry, null, "");
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Loads all {@code ItemStream}s currently saved in the database.
     * </p>
     * Item
     * 
     * @return A collection containing all ItemStreams currently persisted in the underlying database.
     */
    @SuppressWarnings("unchecked")
    public List<ItemStream> loadItemStreams() {
        final List<ItemStream> ret = new ArrayList<ItemStream>();
        getManager().getTransaction().begin();

        final Query loadQuery = getManager().createQuery("select t from ItemStream t");
        ret.addAll(loadQuery.getResultList());

        getManager().getTransaction().commit();

        return ret;
    }

    // TODO adapt to new implementation
    /**
     * <p>
     * 
     * </p>
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<String> loadChannelNames() {
        Collection<String> ret;
        Query get = getManager().createQuery("SELECT DISTINCT t.channelName FROM ItemStream t");
        getManager().getTransaction().begin();
        ret = get.getResultList();
        getManager().getTransaction().commit();
        return ret;
    }

    /**
     * @param identifier
     *            A unique identifier (primary key) for an item.
     * @return The item matching the provided identifier form the database or null if no matching contribution exists.
     */
    public final Item loadItem(final Integer identifier) {
        if (identifier != null) {
            Boolean openedTransaction = openTransaction();
            Item ret = getManager().find(Item.class, identifier);
            commitTransaction(openedTransaction);
            return ret;
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Loads all contributions from the database.
     * </p>
     * 
     * @return A list of all contributions from the database.
     */
    @SuppressWarnings("unchecked")
    public List<Item> loadItems() {
        LOGGER.debug("Loading contributions!");
        final List<Item> ret = new ArrayList<Item>();
        Query loadQuery = getManager().createQuery("select c from Item c");
        getManager().getTransaction().begin();

        ret.addAll(loadQuery.getResultList());

        getManager().getTransaction().commit();
        LOGGER.debug("Loaded " + ret.size() + "contributions: ");
        return ret;
    }

    // TODO adapt this to new structure
    /**
     * <p>
     * Loads all available stream sources from the database.
     * </p>
     * 
     * @return A set of distinct stream source names.
     */
    @SuppressWarnings("unchecked")
    public Collection<String> loadStreamSourceNames() {
        Collection<String> ret;
        Query get = getManager().createQuery("SELECT DISTINCT t.streamSource FROM ItemStream t");
        getManager().getTransaction().begin();
        ret = get.getResultList();
        getManager().getTransaction().commit();
        return ret;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param relationIdentifier
     * @return
     */
    public ItemRelation loadRelation(String relationIdentifier) {
        getManager().getTransaction().begin();
        ItemRelation ret = getManager().find(ItemRelation.class, relationIdentifier);
        getManager().getTransaction().commit();
        return ret;
    }

    /**
     * @return All {@link ItemRelation} instances from the database.
     */
    @SuppressWarnings("unchecked")
    public List<ItemRelation> loadRelations() {
        List<ItemRelation> ret;
        Query get = getManager().createQuery("SELECT fer FROM ForumEntryRelation fer");
        getManager().getTransaction().begin();
        ret = get.getResultList();
        getManager().getTransaction().commit();
        return ret;
    }

    private RelationType loadRelationType(String name) {
        // return getManager().find(RelationType.class, type);
        EntityManager em = getManager();
        Query query = em.createQuery("SELECT rt FROM RelationType rt WHERE rt.name=:name");
        query.setParameter("name", name);
        em.getTransaction().begin();
        @SuppressWarnings("unchecked")
        List<RelationType> resultList = query.getResultList();
        em.getTransaction().commit();
        return getFirst(resultList);
    }

    /**
     * A relation type describes a relation that can exist between two contributions.
     * 
     * @return All relation types from the database.
     */
    @SuppressWarnings("unchecked")
    public Collection<RelationType> loadRelationTypes() {
        Collection<RelationType> ret;
        Query get = getManager().createQuery("SELECT rt FROM RelationType rt");
        getManager().getTransaction().begin();
        ret = get.getResultList();
        getManager().getTransaction().commit();
        return ret;
    }

    // /**
    // * @param identifier
    // * the unique identifer (primary key) of a thread in the
    // * database.
    // * @return the {@link DiscussionThread} corresponding to the provided
    // * identifier or null if no such thread exists in the database.
    // */
    // public StreamSource loadItemStream(final String identifier) {
    // if (identifier == null) {
    // return null;
    // }
    // final Boolean openedTransaction = openTransaction();
    // final StreamSource ret = getManager().find(ItemStream.class, identifier);
    // commitTransaction(openedTransaction);
    // return ret;
    // }

    /**
     * Load and {@link ItemStream} by its source address.
     * 
     * @param sourceAddress
     * @return
     */
    public StreamSource loadStreamSourceByAddress(String sourceAddress) {
        final Boolean openedTransaction = openTransaction();
        Query query = getManager().createQuery("select t from StreamSource t where t.sourceAddress=:sourceAddress");
        query.setParameter("sourceAddress", sourceAddress);
        @SuppressWarnings("unchecked")
        List<StreamSource> ret = query.getResultList();
        commitTransaction(openedTransaction);
        return getFirst(ret);
    }

    // /**
    // * <p>
    // * Loads an {@code Author} from the database.
    // * </p>
    // *
    // * FIXME : author name is not unique!
    // *
    // * @param author The {@code Author} to fetch from the database.
    // * @return The {@code Author} from the database or {@code null} if no such {@code Author} was saved.
    // */
    // public Author loadAuthor(Author author) {
    // final Boolean openedTransaction = openTransaction();
    // Query query = getManager().createQuery("SELECT a FROM Author a WHERE username=:username");
    // // "select a from Author a where a.username=:username and a.streamSource=:streamSource");
    // query.setParameter("username", author.getUsername());
    // // query.setParameter("streamSource", author.getStreamSource());
    // @SuppressWarnings("unchecked")
    // List<Author> ret = query.getResultList();
    // commitTransaction(openedTransaction);
    // if (ret.isEmpty()) {
    // return null;
    // } else {
    // return ret.get(0);
    // }
    // }

    public Collection<Author> loadAuthors() {
        Query loadUsersQuery = getManager().createQuery("SELECT u FROM Author u");
        getManager().getTransaction().begin();
        @SuppressWarnings("unchecked")
        Collection<Author> ret = loadUsersQuery.getResultList();
        getManager().getTransaction().commit();
        return ret;
    }

    /**
     * Saves a list of contributions to the database, updating already existing contributions.
     * 
     * @param contributions
     *            The list of new or changed contributions.
     */
    public <T> void update(List<T> ts) {
        for (T t : ts) {
            update(t);
        }
    }

    public <T> void update(T t) {
        final Boolean openedTransaction = openTransaction();
        getManager().merge(t);
        commitTransaction(openedTransaction);
    }

    /**
     * @return
     */
    public Collection<StreamSource> loadStreamSources() {
        Query loadQuery = getManager().createQuery("SELECT ss FROM StreamSource ss");
        final Boolean openedTransaction = openTransaction();
        @SuppressWarnings("unchecked")
        Collection<StreamSource> ret = loadQuery.getResultList();
        commitTransaction(openedTransaction);
        return ret;
    }
}