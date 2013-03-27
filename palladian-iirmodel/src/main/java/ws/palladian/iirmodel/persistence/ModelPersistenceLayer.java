/**
 * Created on: 20.11.2009 23:53:26
 */
package ws.palladian.iirmodel.persistence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.iirmodel.Author;
import ws.palladian.iirmodel.Item;
import ws.palladian.iirmodel.ItemRelation;
import ws.palladian.iirmodel.ItemStream;
import ws.palladian.iirmodel.Label;
import ws.palladian.iirmodel.LabelType;
import ws.palladian.iirmodel.Labeler;
import ws.palladian.iirmodel.RelationType;
import ws.palladian.iirmodel.StreamGroup;
import ws.palladian.iirmodel.StreamSource;
import ws.palladian.iirmodel.helper.DefaultStreamVisitor;

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
public final class ModelPersistenceLayer extends AbstractPersistenceLayer implements Serializable {

    /**
     * <p>
     * The logger for objects of this class. Configure it using <tt>src/main/resources/log4j.xml</tt>.
     * </p>
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelPersistenceLayer.class);

    /**
     * <p>
     * Used for serialization and deserialization of objects of this class. Should only change if the attribute set of
     * this class changes. In this case old serializations of objects of this class may not be read any more.
     * </p>
     */
    private static final long serialVersionUID = -1692254286550452271L;

    // SQL and Native Queries used by the methods of this class.
    /**
     * <p>
     * A native SQL query that counts the amount of {@link Label}s provided by one {@link Labeler}. If that
     * {@code Labeler} annotated the same {@link Item} twice it will be counted twice.
     * </p>
     */
    private static final String COUNT_LABELED_ITEMS_BY_LABELER = "SELECT a, COUNT(a) FROM Labeler lr JOIN lr.labels l JOIN l.type a WHERE lr.name = :labelerName GROUP BY a";

    /**
     * <p>
     * A JPQL query that counts the {@link Label}s by some type. If an {@link Item} has multiple {@code Label}s of the
     * same type they are counted twice.
     * </p>
     */
    private static final String COUNT_LABELED_ITEMS_BY_TYPES = "SELECT LabelType.name, COUNT(Label.identifier) FROM Label INNER JOIN LabelType ON Label.type_identifier=LabelType.identifier GROUP BY LabelType.name";

    /**
     * <p>
     * A JPQL query that counts all {@link Label}s provided by the system.
     * </p>
     */
    private static final String COUNT_LABELS = "SELECT COUNT(l) FROM Label l";

    /**
     * <p>
     * A JPQL query loading all {@link Label}s provided by a single {@link Labeler}.
     * </p>
     */
    private static final String LOAD_ITEMS_LABELED_BY = "SELECT i FROM Labeler lr JOIN lr.labels l JOIN l.labeledItem i WHERE lr=:labeler";

    /**
     * <p>
     * A JPQL query loading only {@link Item}s not yet labeled by {@code :labeler} but by other {@link Labeler}s.
     * </p>
     */
    private static final String LOAD_ITEMS_ONLY_LABELED_BY_OTHERS_EXCLUDING_LABELERS = "SELECT l.labeledItem FROM Labeler lr JOIN lr.labels l WHERE l.labeledItem NOT IN ( SELECT l.labeledItem FROM Labeler lr JOIN lr.labels l WHERE lr=:labeler AND lr NOT IN(:excludedLabelers))";

    /**
     * <p>
     * A JPQL query loading only {@link Item}s not yet labeled by {@code :labeler} but by other {@link Labeler}s.
     * </p>
     */
    private static final String LOAD_ITEMS_ONLY_LABELED_BY_OTHERS = "SELECT l.labeledItem FROM Labeler lr JOIN lr.labels l WHERE l.labeledItem NOT IN ( SELECT l.labeledItem FROM Labeler lr JOIN lr.labels l WHERE lr=:labeler)";

    /**
     * <p>
     * A JPQL query loading all {@link Item}s not yet labeled at all.
     * </p>
     */
    private static final String LOAD_NON_LABELED_ITEMS = "SELECT i FROM Item i WHERE i.identifier NOT IN (SELECT DISTINCT a.labeledItem.identifier FROM Label a)";

    /**
     * <p>
     * A native SQL query loading a random {@link Item} having {@link Label} of a certain {@link LabelType} provided by
     * a certain {@link Labeler}.
     * </p>
     */
    private static final String LOAD_RANDOM_ITEM_OF_TYPE = "SELECT Item.* FROM Item INNER JOIN Label ON Label.labeledItem_identifier=Item.identifier INNER JOIN LabelType ON LabelType.identifier=Label.type_identifier WHERE LabelType.name=:typeName ORDER BY RAND() LIMIT 1";

    /**
     * <p>
     * A JPQL query loading a {@link RelationType}.
     * </p>
     */
    private static final String LOAD_RELATION_TYPE_BY_NAME = "SELECT rt FROM RelationType rt WHERE rt.name=:typeName";

    /**
     * <p>
     * A JPQL query that loads all {@link ItemRelation}s provided by a certain {@link Labeler} on a pair of {@link Item}
     * s.
     * </p>
     */
    private static final String LOAD_RELATIONS_FOR_ITEM = "SELECT r FROM Labeler l JOIN l.relations r WHERE r.firstItem=:firstItem AND r.secondItem=:secondItem AND l=:labeler";

    /**
     * <p>
     * The maximum count of non annotated relations that are returned during one query.
     * </p>
     * 
     * @see #loadRandomNonAnnotatedRelation(String)
     */
    private static final Integer MAX_NUMBER_OF_RESULTS = 100;
    /**
     * <p>
     * Random object to choose random items from results.
     * </p>
     */
    private static final Random random = new Random(Calendar.getInstance().getTimeInMillis());

    /**
     * <p>
     * Creates a new {@code ModelPersistenceLayer} working on the provided JPA {@code EntityManager}. The
     * {@code EntityManager} may be created using the JPA API like so:
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
     * <p>
     * Be careful. Since {@code EntityManager}s are not thread safe always use only one instance
     * {@code WebPersistenceUtils} object per thread. The best practice is to always create a new instance of this class
     * whenever you need to do some persistence work.
     * </p>
     * 
     * @param manager The {@code EntityManager} this persistence layer should use to access the database.
     */
    public ModelPersistenceLayer(final EntityManager manager) {
        super(manager);
    }

    /**
     * <p>
     * Deep traverse a {@link StreamSource} and return all contained {@link Author}s, i. e. also Authors which are
     * contained in sub-{@link StreamGroup}s and sub-{@link ItemStream}s.
     * </p>
     * 
     * @param streamSource
     * @return
     */
    protected Set<Author> getAllAuthors(StreamSource streamSource) {
        final Set<Author> authors = new HashSet<Author>();
        streamSource.accept(new DefaultStreamVisitor() {

            @Override
            public void visitStreamGroup(StreamGroup streamGroup, int depth) {
                authors.addAll(streamGroup.getAuthors());
            }

            @Override
            public void visitItemStream(ItemStream itemStream, int depth) {
                authors.addAll(itemStream.getAuthors());
            }
        });
        return authors;
    }

    public void saveStreamSource(StreamSource streamSource) {
        Boolean openedTransaction = openTransaction();
        try {
            streamSource.accept(new DefaultStreamVisitor() {

                @Override
                public void visitStreamGroup(StreamGroup streamGroup, int depth) {
                    saveStreamGroup(streamGroup);
                }

                @Override
                public void visitItemStream(ItemStream itemStream, int depth) {
                    saveItemStream(itemStream);
                }
            });
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    protected void saveStreamGroup(final StreamGroup streamGroup) {
        Boolean openedTransaction = openTransaction();
        try {
            Collection<Author> authors = getAllAuthors(streamGroup);
            for (Author author : authors) {
                getManager().persist(author);
            }
            getManager().persist(streamGroup);
        } finally {
            commitTransaction(openedTransaction);
        }
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
    protected void saveItemStream(final ItemStream itemStream) {
        Boolean openedTransaction = openTransaction();
        try {
            StreamSource existingStreamSource = loadStreamSourceByAddress(itemStream.getSourceAddress());

            for (Item item : itemStream.getItems()) {
                getManager().persist(item.getAuthor());
            }

            if (existingStreamSource == null) {
                getManager().persist(itemStream);
            } else {
                itemStream.setIdentifier(existingStreamSource.getIdentifier());
                getManager().merge(itemStream);
            }
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    // /**
    // * @param removedItems
    // */
    // private void removeItems(Collection<Item> removedItems) {
    // final Boolean openedTransaction = openTransaction();
    // for (Item item : removedItems) {
    // getManager().remove(item);
    // }
    // commitTransaction(openedTransaction);
    // }

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

    // /**
    // * <p>
    // * Updates all {@code Author}s occuring in the provided list of items.
    // * </p>
    // *
    // * @param items The items containing possibly new or changed {@code Author} information.
    // */
    // private void updateAuthors(final List<Item> items) {
    // for (Item item : items) {
    // item.setAuthor(saveAuthor(item.getAuthor()));
    // }
    // }

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
        try {
            getManager().remove(item);
        } finally {
            commitTransaction(openedTransaction);
        }
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
    protected void saveItem(final Item item) {
        Author savedAuthor = saveAuthor(item.getAuthor());
        item.setAuthor(savedAuthor);
        Item existingItem = loadItem(item.getIdentifier());
        Boolean openedTransaction = openTransaction();
        try {
            if (existingItem == null) {
                getManager().persist(item);
            } else {
                item.setIdentifier(existingItem.getIdentifier());
                getManager().merge(item);
            }
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Saves this {@code Author} to the database or updates its state if it already exists.
     * </p>
     * 
     * @param author The {@code Author} to save.
     */
    public Author saveAuthor(final Author author) {
        // final Author existingUser = loadAuthor(author.getUsername(), author.getStreamSource());
        final Boolean openedTransaction = openTransaction();
        try {
            Author result;
            // if (existingUser == null) {
            getManager().persist(author);
            result = author;
            // } else {
            // author.setIdentifier(existingUser.getIdentifier());
            // result = getManager().merge(author);
            // }

            return result;
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Save all {@code Author}s of the provided collection to the database.
     * </p>
     * 
     * @param authors The {@code Author}s to save.
     */
    public void saveAuthors(final Collection<Author> authors) {
        for (Author author : authors) {
            saveAuthor(author);
        }
    }

    /**
     * @param username
     * @param streamSourceAddress
     * @return
     */
    public Author loadAuthor(String username, String streamSourceAddress) {
        TypedQuery<Author> query = getManager().createQuery(
                "SELECT a FROM Author a WHERE a.username=:username AND a.streamSourceAddress=:streamSourceAddress",
                Author.class);
        query.setParameter("username", username);
        query.setParameter("streamSourceAddress", streamSourceAddress);

        Boolean openedTransaction = openTransaction();
        try {
            List<Author> authors = query.getResultList();
            Author ret = getFirst(authors);
            return ret;
        } finally {
            commitTransaction(openedTransaction);
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
    public List<ItemStream> loadItemStreams() {
        return loadAll(ItemStream.class);
    }

    // TODO adapt to new implementation
    /**
     * <p>
     * 
     * </p>
     * 
     * @return
     */
    public Collection<String> loadChannelNames() {
        TypedQuery<String> query = getManager().createQuery("SELECT DISTINCT t.channelName FROM ItemStream t",
                String.class);
        getManager().getTransaction().begin();
        List<String> result = query.getResultList();
        getManager().getTransaction().commit();
        return result;
    }

    /**
     * @param identifier
     *            A unique identifier (primary key) for an item.
     * @return The item matching the provided identifier form the database or null if no matching contribution exists.
     */
    public Item loadItem(final Integer identifier) {
        if (identifier != null) {
            Boolean openedTransaction = openTransaction();
            try {
                Item ret = getManager().find(Item.class, identifier);
                return ret;
            } finally {
                commitTransaction(openedTransaction);
            }
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
    public List<Item> loadItems() {
        return loadAll(Item.class);
    }

    /**
     * <p>
     * Provides the set of items corresponding to a set of item identifiers.
     * </p>
     * 
     * @param identifiers The item identifiers to query the database for.
     * @return Items for the provided identifiers.
     */
    public List<Item> loadItems(Collection<Integer> identifiers) {
        List<Item> ret = new ArrayList<Item>(identifiers.size());
        for (Integer identifier : identifiers) {
            ret.add(loadItem(identifier));
        }
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
    public Collection<String> loadStreamSourceNames() {
        TypedQuery<String> query = getManager().createQuery("SELECT DISTINCT t.streamSource FROM ItemStream t",
                String.class);
        getManager().getTransaction().begin();
        List<String> result = query.getResultList();
        getManager().getTransaction().commit();
        return result;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param relationIdentifier
     * @return
     */
    public ItemRelation loadRelation(Integer relationIdentifier) {
        Boolean openedTransaction = openTransaction();
        try {
            ItemRelation ret = getManager().find(ItemRelation.class, relationIdentifier);
            return ret;
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * @return All {@link ItemRelation} instances from the database.
     */
    public List<ItemRelation> loadRelations() {
        return loadAll(ItemRelation.class);
    }

    /**
     * A relation type describes a relation that can exist between two contributions.
     * 
     * @return All relation types from the database.
     */
    public List<RelationType> loadRelationTypes() {
        return loadAll(RelationType.class);
    }

    /**
     * Load and {@link ItemStream} by its source address.
     * 
     * @param sourceAddress
     * @return
     */
    public StreamSource loadStreamSourceByAddress(String sourceAddress) {
        final Boolean openedTransaction = openTransaction();
        try {
            TypedQuery<StreamSource> query = getManager().createQuery(
                    "select t from StreamSource t where t.sourceAddress=:sourceAddress", StreamSource.class);
            query.setParameter("sourceAddress", sourceAddress);
            List<StreamSource> result = query.getResultList();
            return getFirst(result);
        } finally {
            try {
                commitTransaction(openedTransaction);
            } catch (javax.persistence.RollbackException e) {
                System.out.println(e);
                throw e;
            }
        }
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
        return loadAll(Author.class);
    }

    /**
     * @return
     */
    public Collection<StreamSource> loadStreamSources() {
        TypedQuery<StreamSource> query = getManager().createQuery("SELECT ss FROM StreamSource ss", StreamSource.class);
        Boolean openedTransaction = openTransaction();
        try {
            Collection<StreamSource> results = query.getResultList();
            return results;
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Loads a {@code StreamSource} identified by a certain name. Use this method with care. It throws an
     * {@code IllegalStateException} if there are more than one {@code StreamSource} with that name.
     * </p>
     * 
     * @param streamSourceName The name of the {@code StreamSource} to load.
     * @return The {@code StreamSource} carrying the provided {@code streamSourceName} or {@code null} otherwise.
     */
    public StreamSource loadStreamSourceByName(String streamSourceName) {
        TypedQuery<StreamSource> query = getManager().createQuery(
                "SELECT s FROM StreamSource s WHERE s.sourceName=:sourceName", StreamSource.class);
        query.setParameter("sourceName", streamSourceName);

        final Boolean openedTransaction = openTransaction();
        try {
            List<StreamSource> result = query.getResultList();
            if (result.size() > 1) {
                throw new IllegalStateException("There are " + result.size() + " stream sources named "
                        + streamSourceName + ". There should be only one.");
            } else if (result.size() == 1) {
                return result.get(0);
            } else {
                return null;
            }
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Loads an {@link Item} from an {@link ItemStream} based on its forum internal identifier. The method throws an
     * exception if multiple {@code Item}s with the same forum internal identifier are found.
     * </p>
     * 
     * @param forumInternalIdentifier The forum internal identifier of the {@code Item} to load.
     * @param parentStream The {@code ItemStream} to load the {@code Item} from.
     * @return The item to load or {@code null} if no such item is available.
     */
    public Item loadItem(String forumInternalIdentifier, ItemStream parentStream) {
        TypedQuery<Item> query = getManager().createQuery(
                "SELECt i FROM Item i WHERE i.sourceInternalIdentifier=:sourceInternalIdentifier AND parent=:parent",
                Item.class);
        query.setParameter("sourceInternalIdentifier", forumInternalIdentifier);
        query.setParameter("parent", parentStream);
        final Boolean openedTransaction = openTransaction();
        try {
            List<Item> result = query.getResultList();
            if (result.size() == 1) {
                return result.get(0);
            } else if (result.size() > 1) {
                throw new IllegalStateException("Found " + result.size() + " items with internal identifier "
                        + forumInternalIdentifier + "in item stream " + parentStream);
            } else {
                return null;
            }
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Delete all data.
     * </p>
     */
    public void deleteAll() {
        Boolean openedTransaction = openTransaction();
        try {
            getManager().createQuery("DELETE FROM Item").executeUpdate();
            getManager().createQuery("DELETE FROM StreamSource").executeUpdate();
            getManager().createQuery("DELETE FROM ItemStream").executeUpdate();
            getManager().createQuery("DELETE FROM StreamGroup").executeUpdate();
            getManager().createQuery("DELETE FROM Author").executeUpdate();
            getManager().createQuery("DELETE FROM ItemRelation").executeUpdate();
            getManager().createQuery("DELETE FROM RelationType").executeUpdate();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * @return
     */
    public Long countItems() {
        Query query = getManager().createQuery("SELECT COUNT(i) FROM Item i");
        Boolean openedTransaction = openTransaction();
        try {
            return (Long)query.getSingleResult();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Selects a random {@code Item} from a {@code List} of {@code Item}s.
     * </p>
     * 
     * @param items The {@code List} of {@code Item}s to select from.
     * @return a random {@code Item} from the provided {@code List} of {@code Item}s.
     */
    private Item chooseRandomItem(final List<Item> items) {
        int randomIndex = random.nextInt(items.size());
        LOGGER.debug("Choosing item number " + randomIndex + " from a list of " + items.size() + " items.");
        return items.get(randomIndex);
    }

    /**
     * <p>
     * Provides the grouped labels for each label type. The method provides a mapping from {@code String} to
     * {@code String} even though the value is a number. However since different implementations of JPA use different
     * datatypes to count items in the database this is the most generic method.
     * </p>
     * 
     * @return A mapping from the labels name to the amount of available instances of this name.
     */
    public Map<String, String> countLabeledItemsByType() {
        Query countLabeledItemsByTypesQuery = getManager().createNativeQuery(COUNT_LABELED_ITEMS_BY_TYPES);
        Boolean openedTransaction = openTransaction();
        try {
            Map<String, String> ret = new HashMap<String, String>();
            @SuppressWarnings("unchecked")
            List<Object[]> results = countLabeledItemsByTypesQuery.getResultList();
            for (Object[] mapping : results) {
                ret.put((String)mapping[0], String.valueOf(mapping[1]));
            }
            return ret;
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * @param labelerName The name of the {@link Labeler} to count {@link Label}s for.
     * @return A mapping from the {@link LabelType} to the amount of {@code Label}s provided by the {@code Labeler}
     *         specified via {@code labelerName}.
     */
    public Map<LabelType, Long> countLabeledItemsByType(final String labelerName) {
        Map<LabelType, Long> ret = new HashMap<LabelType, Long>();
        TypedQuery<Object[]> countLabeledItemsByLabelerQuery = getManager().createQuery(COUNT_LABELED_ITEMS_BY_LABELER,
                Object[].class);
        countLabeledItemsByLabelerQuery.setParameter("labelerName", labelerName);
        Boolean openedTransaction = openTransaction();
        try {
            List<Object[]> results = countLabeledItemsByLabelerQuery.getResultList();
            for (Object[] mapping : results) {
                ret.put((LabelType)mapping[0], (Long)mapping[1]);
            }
            return ret;
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * @return The count of labels currently available.
     */
    public Long countLabels() {
        TypedQuery<Long> countLabeledItemsQuery = getManager().createQuery(COUNT_LABELS, Long.class);
        Boolean openedTransaction = openTransaction();
        try {
            return countLabeledItemsQuery.getSingleResult();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * @param name The {@code name} of the {@link Labeler} to count {@link Label}s for.
     * @return The {@code Label}s provided by a single {@code Labeler} with {@code name}.
     */
    public Long countLabels(String name) {
        TypedQuery<Long> query = getManager().createQuery(
                "SELECT COUNT(l.labeledItem) FROM Labeler lr JOIN lr.labels l WHERE lr.name=:labelerName", Long.class)
                .setParameter("labelerName", name);
        Boolean openedTransaction = openTransaction();
        try {
            return query.getSingleResult();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Provides all {@link Item}s labeled by some {@link Labeler}.
     * </p>
     * 
     * @param labeler The queried {@code Labeler}.
     * @return The {@code Item}s labeled by {@code Labeler}.
     */
    public List<Item> loadItemsLabeledBy(Labeler labeler) {
        TypedQuery<Item> query = getManager().createQuery(LOAD_ITEMS_LABELED_BY, Item.class);
        query.setParameter("labeler", labeler);
        Boolean openedTransaction = openTransaction();
        try {
            return query.getResultList();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Provides a {@link Collection} of all {@link Labeler}s from the database.
     * </p>
     * 
     * @return A {@code Collection} containing all {@code Labeler}s from the database.
     */
    public List<Labeler> loadLabeler() {
        TypedQuery<Labeler> query = getManager().createQuery("SELECT l FROM Labeler l", Labeler.class);
        Boolean openedTransaction = openTransaction();
        try {
            return query.getResultList();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Provides a {@link Labeler} identified by its {@code name}.
     * </p>
     * 
     * @param name The name of the {@code Labeler} to find.
     * @return The {@code Labeler} identified by {@code name} or {@code null} if no such {@code Labeler} exists.
     */
    public Labeler loadLabeler(final String name) {
        Boolean openedTransaction = openTransaction();
        try {
            return getManager().find(Labeler.class, name);
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Provides all {@link Label}s for one specific {@link Item} created by one specific {@link Labeler}.
     * </p>
     * 
     * @param item The {@code Item} to query {@code Label}s for.
     * @param labeler The {@code Labeler} providing the {@code Label}s for that {@code Item}.
     * @return All the {@code Label}s created by {@code Labeler} for {@code item}.
     */
    public Collection<Label> loadLabelsForItem(Item item, Labeler labeler) {
        TypedQuery<Label> query = getManager().createQuery(
                "SELECT l FROM Labeler lr JOIN lr.labels l JOIN l.labeledItem i WHERE i=:item AND lr=:labeler",
                Label.class);
        query.setParameter("labeler", labeler);
        query.setParameter("item", item);
        Boolean openedTransaction = openTransaction();
        try {
            return query.getResultList();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * @param labelTypeName The name of the {@link LabelType} to load.
     * @return The {@code LabelType} with the name {@code labelTypeName}.
     */
    public LabelType loadLabelType(String labelTypeName) {
        TypedQuery<LabelType> loadQuery = getManager().createQuery("SELECT a FROM LabelType a WHERE a.name = :name",
                LabelType.class);
        loadQuery.setParameter("name", labelTypeName);
        Boolean openedTransaction = openTransaction();
        List<LabelType> result = loadQuery.getResultList();
        commitTransaction(openedTransaction);
        if (result.size() > 1) {
            throw new IllegalStateException("Duplicate label types found");
        } else if (result.size() < 1) {
            return null;
        } else {
            return result.get(0);
        }
    }

    /**
     * @return All {@link LabelType}s available.
     */
    public List<LabelType> loadLabelTypes() {
        TypedQuery<LabelType> query = getManager().createQuery("SELECT lt FROM LabelType lt", LabelType.class);
        Boolean openedTransaction = openTransaction();
        try {
            List<LabelType> ret = query.getResultList();
            return ret;
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Tries to find a random {@link Item} from the database, that was not labeled by {@link Labeler} but was already
     * labeled by another person. If no such {@code Item} exists a random not yet labeled {@code Item} is returned. This
     * method is especially helpful to maximize the amount of labels per {@code Item} and is thus useful for calculating
     * the kappa value of a dataset. The kappa value describes the agreement between {@code Labelers}. It can be used to
     * make assumptions how easy the label task is and how clear the definitions of individual labels are. A dataset has
     * a high kappa value the {@code Labeler} agree on almost all {@code Label}s whereas a low kappa value denotes
     * disagreement between different {@code Labeler}s.
     * </p>
     * 
     * @param labeler The {@code Labeler} denoted by self. The method tries to find items only labeled by other
     *            {@code Labeler}s.
     * @param excludedLabelers A list of {@code Labeler}s whose {@code Label}s are not considered by this method.
     * @return A random {@code Item} not already labeled by {@code labeler}. At first all items are choosen that where
     *         already labeled by other {@code Labeler}s and if no such {@code Item} exists another unlabeled random
     *         {@code Item} is selected.
     */
    public Item loadNextNonSelfLabeledItem(final Labeler labeler, final Collection<Labeler> excludedLabelers) {
        TypedQuery<Item> loadItemsOnlyLabeledByOthersQuery = getManager().createQuery(
                LOAD_ITEMS_ONLY_LABELED_BY_OTHERS_EXCLUDING_LABELERS, Item.class);
        loadItemsOnlyLabeledByOthersQuery.setMaxResults(MAX_NUMBER_OF_RESULTS);
        loadItemsOnlyLabeledByOthersQuery.setParameter("labeler", labeler);
        loadItemsOnlyLabeledByOthersQuery.setParameter("excludedLabelers", excludedLabelers);
        Boolean openedTransaction = openTransaction();
        try {
            List<Item> itemsLabeledByOthers = loadItemsOnlyLabeledByOthersQuery.getResultList();

            if (itemsLabeledByOthers.isEmpty()) {
                LOGGER.debug("Selecting random item from items not labeled by " + labeler.getName()
                        + " but by other labelers");
                return loadRandomNonLabeledItem();
            } else {
                LOGGER.debug("Selecting random item from non labeled items.");
                return chooseRandomItem(itemsLabeledByOthers);
            }
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Tries to find a random {@link Item} from the database, that was not labeled by {@link Labeler} but was already
     * labeled by another person. If no such {@code Item} exists a random not yet labeled {@code Item} is returned. This
     * method is especially helpful to maximize the amount of labels per {@code Item} and is thus useful for calculating
     * the kappa value of a dataset. The kappa value describes the agreement between {@code Labelers}. It can be used to
     * make assumptions how easy the label task is and how clear the definitions of individual labels are. A dataset has
     * a high kappa value the {@code Labeler} agree on almost all {@code Label}s whereas a low kappa value denotes
     * disagreement between different {@code Labeler}s.
     * </p>
     * 
     * @param labeler The {@code Labeler} denoted by self. The method tries to find items only labeled by other
     *            {@code Labeler}s.
     * @return A random {@code Item} not already labeled by {@code labeler}. At first all items are choosen that where
     *         already labeled by other {@code Labeler}s and if no such {@code Item} exists another unlabeled random
     *         {@code Item} is selected.
     */
    public Item loadNextNonSelfLabeledItem(final Labeler labeler) {
        TypedQuery<Item> loadItemsOnlyLabeledByOthersQuery = getManager().createQuery(
                LOAD_ITEMS_ONLY_LABELED_BY_OTHERS, Item.class);
        loadItemsOnlyLabeledByOthersQuery.setMaxResults(MAX_NUMBER_OF_RESULTS);
        loadItemsOnlyLabeledByOthersQuery.setParameter("labeler", labeler);
        Boolean openedTransaction = openTransaction();
        try {
            List<Item> itemsLabeledByOthers = loadItemsOnlyLabeledByOthersQuery.getResultList();

            if (itemsLabeledByOthers.isEmpty()) {
                LOGGER.debug("Selecting random item from items not labeled by " + labeler.getName()
                        + " but by other labelers");
                return loadRandomNonLabeledItem();
            } else {
                LOGGER.debug("Selecting random item from non labeled items.");
                return chooseRandomItem(itemsLabeledByOthers);
            }
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Provides a random {@link Item} with a certain label type provided by some labeler.
     * </p>
     * 
     * @param loadLabelTypeByName
     * @param typeLabeler
     * @return
     */
    public Item loadRandomItemByType(LabelType labelType, Labeler typeLabeler) {
        Query randomItemOfTypeQuery = getManager().createNativeQuery(LOAD_RANDOM_ITEM_OF_TYPE, Item.class);
        randomItemOfTypeQuery.setParameter("typeName", labelType.getName());
        Boolean openedTransaction = openTransaction();
        try {
            return (Item)randomItemOfTypeQuery.getSingleResult();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Provides a not yet annotated {@code Item} from the database.
     * </p>
     * 
     * @return A random not yet annotated {@code Item} or {@code null} if no such item exists.
     */
    public Item loadRandomNonLabeledItem() {
        TypedQuery<Item> loadNonLabeledItemQuery = getManager().createQuery(LOAD_NON_LABELED_ITEMS, Item.class);
        loadNonLabeledItemQuery.setMaxResults(MAX_NUMBER_OF_RESULTS);
        Boolean openedTransaction = openTransaction();
        try {
            List<Item> nonAnnotateditems = loadNonLabeledItemQuery.getResultList();

            int countOfNonAnnotatedItems = nonAnnotateditems.size();
            if (countOfNonAnnotatedItems > 0) {
                return chooseRandomItem(nonAnnotateditems);
            } else {
                return null;
            }
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Loads all {@link ItemRelation}s between two {@link Item}s provided by a {@link Labeler}.
     * </p>
     * 
     * @param lhItem The first {@code Item} in the relationship.
     * @param rhItem The second {@code Item} in the relationship.
     * @param labeler The {@code Labeler} who provided the {@code ItemRelation}s.
     * @return All {@code ItemRelation}s between {@code lhItem} and {@code rhItem} provided by {@code labeler}.
     */
    public List<ItemRelation> loadRelations(Item lhItem, Item rhItem, Labeler labeler) {
        TypedQuery<ItemRelation> loadRelationsQuery = getManager().createQuery(LOAD_RELATIONS_FOR_ITEM,
                ItemRelation.class);
        loadRelationsQuery.setParameter("firstItem", lhItem);
        loadRelationsQuery.setParameter("secondItem", rhItem);
        loadRelationsQuery.setParameter("labeler", labeler);
        Boolean openedTransaction = openTransaction();
        try {
            return loadRelationsQuery.getResultList();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Loads a {@link RelationType} with a certain name.
     * </p>
     * 
     * @param relationTypeName
     * @return
     */
    public RelationType loadRelationType(String relationTypeName) {
        TypedQuery<RelationType> loadRelationTypeByNameQuery = getManager().createQuery(LOAD_RELATION_TYPE_BY_NAME,
                RelationType.class);
        loadRelationTypeByNameQuery.setParameter("typeName", relationTypeName);
        Boolean openedTransaction = openTransaction();
        try {
            List<RelationType> loadedRelationTypes = loadRelationTypeByNameQuery.getResultList();
            if (loadedRelationTypes.size() == 1) {
                return loadedRelationTypes.get(0);
            } else if (loadedRelationTypes.isEmpty()) {
                return null;
            } else {
                throw new IllegalStateException("Relation type name: " + relationTypeName + " is ambiguous.");
            }
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Runs a native SQL query returning a single object.
     * </p>
     * 
     * @param queryString The SQL query {@code String}.
     * @param filler The {@link ParameterFiller} responsible for initializing all parameters in the {@code queryString}
     *            with correct values.
     * @return The object for which {@code queryString} provides the values for all attributes.
     */
    public Object runSingleResultNativeQuery(final String queryString, final ParameterFiller filler) {
        Query query = getManager().createNativeQuery(queryString);
        filler.fillParameter(query);
        Boolean openedTransaction = openTransaction();
        try {
            return query.getSingleResult();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param queryString
     * @param clazz
     * @return
     */
    public Object runSingleResultNativeQuery(String queryString) {
        Query query = getManager().createNativeQuery(queryString);
        Boolean openedTransaction = openTransaction();
        try {
            return query.getSingleResult();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Saves a new {@link Label} to the database.
     * </p>
     * 
     * @param label The {@code Label} to save.
     */
    public void saveLabel(Label label) {
        Boolean openedTransaction = openTransaction();
        try {
            getManager().persist(label);
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Saves a non existing {@link Labeler} to the database or updates an existing one..
     * </p>
     * 
     * @param labeler The {@code Labeler} to save.
     */
    public void saveLabeler(final Labeler labeler) {
        Boolean openedTransaction = openTransaction();
        Labeler existingLabeler = getManager().find(Labeler.class, labeler.getName());
        if (existingLabeler == null) {
            getManager().persist(labeler);
        } else {
            getManager().merge(labeler);
        }
        commitTransaction(openedTransaction);
    }

    /**
     * <p>
     * Saves a non existing {@link LabelType} to the database.
     * </p>
     * 
     * @param labelType The new {@code LabelType} to save.
     */
    public void saveLabelType(LabelType labelType) {
        if (loadLabelType(labelType.getName()) != null) {
            throw new IllegalStateException("Trying to save annotation type: " + labelType
                    + " twice. This would leave the database in an inconsistent state.");
        }

        Boolean openedTransaction = openTransaction();
        try {
            getManager().persist(labelType);
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Saves a {@link RelationType} to the database.
     * </p>
     * 
     * @param relationType The {@code RelationType} to save.
     */
    public void saveRelationType(RelationType relationType) {
        Boolean openedTransaction = openTransaction();
        try {
            RelationType existingRelationType = loadRelationType(relationType.getName());
            if (existingRelationType == null) {
                getManager().persist(relationType);
            } else {
                getManager().merge(relationType);
            }
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * @param persistentRelation
     */
    public void saveItemRelation(ItemRelation relation) {
        Boolean openedTransaction = openTransaction();
        try {
            getManager().persist(relation);
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Interface for a strategy class that describes how to fill the parameters for some query with values.
     * </p>
     * 
     * @author Klemens Muthmann
     * @see ModelPersistenceLayer#runSingleResultNativeQuery(String, ParameterFiller)
     */
    public interface ParameterFiller {
        /**
         * <p>
         * Fills the parameters of the provided JPA {@code Query} with concrete values.
         * </p>
         * 
         * @param query The query to fill with values.
         */
        void fillParameter(final Query query);
    }

    /**
     * <p>
     * Saves all provided relations to the database.
     * </p>
     * 
     * @param relations The relations to save to the database.
     */
    public void saveItemRelations(List<ItemRelation> relations) {
        Boolean openedTransaction = openTransaction();
        try {
            for (ItemRelation relation : relations) {
                getManager().persist(relation);
            }
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Run a JPQL query through this persistence layer.
     * </p>
     * 
     * @param query The query to run.
     * @param parameterFiller A filler for the named parameters in the query.
     * @param clazz The data type of the queried object.
     * @return A list of the queried objects from the database according to the query.
     */
    public <T> List<T> runQuery(final String query, final ParameterFiller parameterFiller, final Class<T> clazz) {
        Boolean openedTransaction = openTransaction();
        try {
            TypedQuery<T> typedQuery = getManager().createQuery(query, clazz);
            parameterFiller.fillParameter(typedQuery);
            return typedQuery.getResultList();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Runs a native SQL query through this persistence layer.
     * </p>
     * 
     * @param query The query to run.
     * @param clazz The {@code Class} of the results.
     */
    public <T> List<T> runNativeQuery(final String query, final Class<T> clazz) {
        Boolean openedTransaction = openTransaction();
        try {
            Query queryObj = getManager().createNativeQuery(query);
            List<T> resultList = queryObj.getResultList();
            return resultList;
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    public void runNativeUpdate(final String query, final ParameterFiller parameterFiller) {
        Boolean openedTransaction = openTransaction();
        try {
            Query queryObj = getManager().createNativeQuery(query);
            parameterFiller.fillParameter(queryObj);
            queryObj.executeUpdate();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Loads all {@link Item}s not yet labeled by a certain {@link Labeler}.
     * </p>
     * 
     * @param labeler The {@code Labeler} to use as reference to search for non labeled {@code Item}s
     * @return A list of the {@code Item}s not labeled by the {@code Labeler}.
     */
    public List<Item> loadItemsNotLabeledBy(final Labeler labeler) {
        Boolean openedTransaction = openTransaction();
        try {
            TypedQuery<Item> query = getManager()
                    .createQuery(
                            "SELECT i FROM Item i WHERE i NOT IN (SELECT labeled FROM Labeler lr JOIN lr.labels l JOIN l.labeledItem labeled WHERE lr=:labeler)",
                            // "SELECT labeled FROM Labeler lr JOIN lr.labels l JOIN l.labeledItem labeled WHERE lr=:labeler",
                            Item.class);
            query.setParameter("labeler", labeler);
            return query.getResultList();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    public List<Object[]> runNativeQuery(String query, ParameterFiller parameterFiller) {
        Query queryObj = getManager().createNativeQuery(query);
        parameterFiller.fillParameter(queryObj);

        Boolean openedTransaction = openTransaction();
        try {
            return queryObj.getResultList();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    public void runNativeUpdate(final String query) {
        Query queryObj = getManager().createNativeQuery(query);
        Boolean openedTransaction = openTransaction();
        try {
            queryObj.executeUpdate();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Loads all {@link Item}s authored by the same person from the database.
     * </p>
     * 
     * @param author The {@link Author} to load the {@link Item}s for.
     * @return The {@link List} of all {@link Item}s authored by the provided {@link Author}.
     */
    public List<Item> loadItemsByAuthor(Author author) {
        TypedQuery<Item> query = getManager().createQuery("SELECT i FROM Item i JOIN i.author a WHERE a=:author",
                Item.class);
        query.setParameter("author", author);
        Boolean openedTransaction = openTransaction();
        try {
            return query.getResultList();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    public Labeler loadLabelerForLabel(final Label label) {
        TypedQuery<Labeler> query = getManager().createQuery(
                "SELECT l FROM Labeler l JOIN l.labels la WHERE la=:label", Labeler.class);
        query.setParameter("label", label);
        Boolean openedTransaction = openTransaction();
        try {
            return query.getSingleResult();
        } finally {
            commitTransaction(openedTransaction);
        }
    }
}