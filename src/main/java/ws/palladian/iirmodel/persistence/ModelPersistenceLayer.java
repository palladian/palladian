/**
 * Created on: 20.11.2009 23:53:26
 */
package ws.palladian.iirmodel.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;

import ws.palladian.iirmodel.Author;
import ws.palladian.iirmodel.Item;
import ws.palladian.iirmodel.ItemRelation;
import ws.palladian.iirmodel.ItemStream;
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
public final class ModelPersistenceLayer extends AbstractPersistenceLayer {

    /**
     * <p>
     * The logger for objects of this class. Configure it using <tt>src/main/resources/log4j.xml</tt>.
     * </p>
     */
    private static final Logger LOGGER = Logger.getLogger(ModelPersistenceLayer.class);

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
        commitTransaction(openedTransaction);
    }

    protected void saveStreamGroup(final StreamGroup streamGroup) {
        Boolean openedTransaction = openTransaction();
        Collection<Author> authors = getAllAuthors(streamGroup);
        for (Author author : authors) {
            getManager().persist(author);
        }
        getManager().persist(streamGroup);
        commitTransaction(openedTransaction);
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
        commitTransaction(openedTransaction);
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
    protected void saveItem(final Item item) {
        Author savedAuthor = saveAuthor(item.getAuthor());
        item.setAuthor(savedAuthor);
        Item existingItem = loadItem(item.getIdentifier());
        final Boolean openedTransaction = openTransaction();
        if (existingItem == null) {
            getManager().persist(item);
        } else {
            item.setIdentifier(existingItem.getIdentifier());
            getManager().merge(item);
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
        // final Author existingUser = loadAuthor(author.getUsername(), author.getStreamSource());
        final Boolean openedTransaction = openTransaction();

        Author result;
        // if (existingUser == null) {
        getManager().persist(author);
        result = author;
        // } else {
        // author.setIdentifier(existingUser.getIdentifier());
        // result = getManager().merge(author);
        // }
        commitTransaction(openedTransaction);
        return result;
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
        List<Author> authors = query.getResultList();
        Author ret = getFirst(authors);
        commitTransaction(openedTransaction);
        return ret;
    }

    public void saveItemRelation(ItemRelation itemRelation) {
        TypedQuery<ItemRelation> relationExistsQuery = getManager().createQuery(
                "SELECT r FROM ItemRelation r WHERE (r.firstItem=:firstItem AND r.secondItem=:secondItem) "
                        + "OR (r.firstItem=:secondItem AND r.secondItem=:firstItem) " + "AND r.type=:type",
                ItemRelation.class);
        relationExistsQuery.setParameter("firstItem", itemRelation.getFirstItem());
        relationExistsQuery.setParameter("secondItem", itemRelation.getSecondItem());
        relationExistsQuery.setParameter("type", itemRelation.getType());
        Boolean openedTransaction = openTransaction();
        List<ItemRelation> relations = relationExistsQuery.getResultList();
        if (relations.isEmpty()) {
            getManager().persist(itemRelation);
        } else {
            ItemRelation existingRelation = relations.get(0);
            existingRelation.setType(itemRelation.getType());
            existingRelation.setComment(itemRelation.getComment());
        }
        commitTransaction(openedTransaction);
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
    public ItemRelation loadRelation(String relationIdentifier) {
        getManager().getTransaction().begin();
        ItemRelation ret = getManager().find(ItemRelation.class, relationIdentifier);
        getManager().getTransaction().commit();
        return ret;
    }

    /**
     * @return All {@link ItemRelation} instances from the database.
     */
    public List<ItemRelation> loadRelations() {
        return loadAll(ItemRelation.class);
    }

    private RelationType loadRelationType(String name) {
        EntityManager em = getManager();
        TypedQuery<RelationType> query = em.createQuery("SELECT rt FROM RelationType rt WHERE rt.name=:name",
                RelationType.class);
        query.setParameter("name", name);
        em.getTransaction().begin();
        List<RelationType> resultList = query.getResultList();
        em.getTransaction().commit();
        return getFirst(resultList);
    }

    /**
     * A relation type describes a relation that can exist between two contributions.
     * 
     * @return All relation types from the database.
     */
    public Collection<RelationType> loadRelationTypes() {
        return loadAll(RelationType.class);
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
        TypedQuery<StreamSource> query = getManager().createQuery(
                "select t from StreamSource t where t.sourceAddress=:sourceAddress", StreamSource.class);
        query.setParameter("sourceAddress", sourceAddress);
        List<StreamSource> result = query.getResultList();
        commitTransaction(openedTransaction);
        return getFirst(result);
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
        return loadAll(StreamSource.class);
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
        List<StreamSource> result = query.getResultList();
        commitTransaction(openedTransaction);

        if (result.size() > 1) {
            throw new IllegalStateException("There are " + result.size() + " stream sources named " + streamSourceName
                    + ". There should be only one.");
        } else if (result.size() == 1) {
            return result.get(0);
        } else {
            return null;
        }
    }

    /**
     * @param forumInternalIdentifier
     * @param parentStream
     * @return
     */
    public Item loadItem(String forumInternalIdentifier, ItemStream parentStream) {
        TypedQuery<Item> query = getManager().createQuery(
                "SELECt i FROM Item i WHERE i.sourceInternalIdentifier=:sourceInternalIdentifier AND parent=:parent",
                Item.class);
        query.setParameter("sourceInternalIdentifier", forumInternalIdentifier);
        query.setParameter("parent", parentStream);
        final Boolean openedTransaction = openTransaction();
        List<Item> result = query.getResultList();
        commitTransaction(openedTransaction);
        if (result.size() == 1) {
            return result.get(0);
        } else {
            throw new IllegalStateException("Found " + result.size() + " items with internal identifier "
                    + forumInternalIdentifier + "in item stream " + parentStream);
        }
    }

    /**
     * <p>
     * Delete all data.
     * </p>
     */
    public void deleteAll() {
        EntityManager entityManager = getManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        entityManager.createQuery("DELETE FROM Item").executeUpdate();
        entityManager.createQuery("DELETE FROM StreamSource").executeUpdate();
        entityManager.createQuery("DELETE FROM ItemStream").executeUpdate();
        entityManager.createQuery("DELETE FROM StreamGroup").executeUpdate();
        entityManager.createQuery("DELETE FROM Author").executeUpdate();
        entityManager.createQuery("DELETE FROM ItemRelation").executeUpdate();
        entityManager.createQuery("DELETE FROM RelationType").executeUpdate();
        transaction.commit();
    }
}