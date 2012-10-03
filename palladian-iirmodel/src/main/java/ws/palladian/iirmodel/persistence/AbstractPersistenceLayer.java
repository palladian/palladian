/**
 * Created on: 28.05.2010 09:56:24
 */
package ws.palladian.iirmodel.persistence;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

/**
 * <p>
 * Abstract base class for persistence layers. Provides some convenience methods usually required by a persistence
 * layer.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 3.0
 * @since 1.0
 */
public abstract class AbstractPersistenceLayer {

    public static final String MYSQL_PERSISTENCE_UNIT_NAME = "ws.palladian.iirmodel.persistence";

    /**
     * <p>
     * 
     * </p>
     */
    private transient EntityManager manager;

    /**
     * @param persistenceUnitName The name of the persistence unit used by this persistence layer. This name is used to
     *            load the correct configuration from the persistence.xml file.
     */
    public AbstractPersistenceLayer(final EntityManager entityManager) {
        if (entityManager == null) {
            throw new IllegalArgumentException(
                    "Trying to create ModelPersistenceLayer with null EntityManager. This is not valid.");
        }

        manager = entityManager;
    }

    /**
     * <p>
     * Shuts the persistence layer down. Call this method when you are done using an instance of this class.
     * </p>
     * 
     */
    public final void shutdown() {
        if (manager != null && manager.isOpen()) {
            manager.close();
        }
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @return the manager
     */
    protected final EntityManager getManager() {
        return manager;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param collection1
     * @param collection2
     * @return
     */
    protected final <T> List<T> mergeCollections(Collection<T> collection1, Collection<T> collection2) {
        List<T> ret = new LinkedList<T>();
        for (T entry : collection1) {
            ret.add(entry);
        }
        for (T obj : collection2) {
            if (!collection1.contains(obj)) {
                ret.add(obj);
            }
        }
        return ret;
    }

    /**
     * <p>
     * When no transaction is open, start a new one. This method can be used in conjunction with
     * {@link #commitTransaction(Boolean)} and placed in subclasses to allow more convenient transaction handling among
     * multiple methods. Example:
     * </p>
     * 
     * <pre>
     * boolean openedTransaction = openTransaction();
     * // do persistence work here
     * commitTransaction(openedTransaction);
     * </pre>
     * 
     * <p>
     * In this case, the transaction is only commited, if it was opened at the beginning.
     * </p>
     * 
     * @return <code>true</code> if a new transaction was started, <code>false</code> if transaction was open already.
     */
    public final Boolean openTransaction() {
        if (manager.getTransaction().isActive()) {
            return false;
        } else {
            manager.getTransaction().begin();
            return true;
        }
    }

    /**
     * <p>
     * Commit a transaction, if supplied parameter is <code>true</code>. See {@link #openTransaction()} for more
     * information.
     * </p>
     * 
     * @param openedTransaction
     */
    public final void commitTransaction(final Boolean openedTransaction) {
        if (openedTransaction) {
            manager.getTransaction().commit();
        }
    }

    // public static <T> T save(T t, AbstractPersistenceLayer pl) {
    // final T existingT = load(t, pl);
    // T ret = null;
    // final Boolean openedTransaction = pl.openTransaction();
    // if (existingT == null) {
    // pl.getManager().persist(t);
    // ret = t;
    // } else {
    // ret = pl.getManager().merge(t);
    // }
    // pl.commitTransaction(openedTransaction);
    // return ret;
    // }

    // public static <T> void saveAll(Collection<T> ts, AbstractPersistenceLayer pl) {
    // final Boolean openedTransaction = pl.openTransaction();
    // for (T t : ts) {
    // save(t, pl);
    // }
    // pl.commitTransaction(openedTransaction);
    // }

    /**
     * <p>
     * Load an entity by its identifier.
     * </p>
     * 
     * @param identifier The database wide unique identifier of the entity to load.
     * @param classToLoad The Java class of the entity to load.
     * @return The entity identified by {@code identifier} or {@code null} if it does not exist.
     */
    public final <T> T load(Object identifier, Class<T> classToLoad) {
        final Boolean openedTransaction = openTransaction();
        T ret = getManager().find(classToLoad, identifier);
        commitTransaction(openedTransaction);
        return ret;
    }

    /**
     * <p>
     * Load all entities of a specific type.
     * </p>
     * 
     * @param classToLoad
     * @return
     */
    public final <T> List<T> loadAll(Class<T> classToLoad) {
        Boolean openedTransaction = openTransaction();
        try {
            EntityManager entityManager = getManager();
            String loadQuery = "SELECT o FROM " + classToLoad.getName() + " o";
            TypedQuery<T> query = entityManager.createQuery(loadQuery, classToLoad);
            List<T> result = query.getResultList();
            return result;
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * <p>
     * Return the first object in a list, or <code>null</code>, if list is empty.
     * </p>
     * 
     * @param list
     * @return
     */
    protected final <T> T getFirst(List<T> list) {
        if (list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }

    /**
     * <p>
     * Saves a list of entities to the database, updating already existing contributions.
     * </p>
     * 
     * @param ts The list of new or changed entities.
     */
    public final <T> void update(List<T> ts) {
        for (T t : ts) {
            update(t);
        }
    }

    /**
     * <p>
     * Save an entity to the database, updating if already existing.
     * </p>
     * 
     * @param t The new or changed entity.
     */
    public final <T> void update(T t) {
        final Boolean openedTransaction = openTransaction();
        getManager().merge(t);
        commitTransaction(openedTransaction);
    }
}
