/**
 * Created on: 28.05.2010 09:56:24
 */
package ws.palladian.iirmodel.persistence;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
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

    protected final Boolean openTransaction() {
        if (manager.getTransaction().isActive()) {
            return false;
        } else {
            manager.getTransaction().begin();
            return true;
        }
    }

    protected final void commitTransaction(final Boolean openedTransaction) {
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
     * Load an entity be its identifier.
     * </p>
     * 
     * @param identifier
     * @param classToLoad
     * @return
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
        EntityManager entityManager = getManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = criteriaBuilder.createQuery(classToLoad);
        List<T> result = entityManager.createQuery(query).getResultList();
        commitTransaction(openedTransaction);
        return result;
    }

    /**
     * Return the first object in a list, or <code>null</code>, if list is empty.
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
     * Saves a list of entities to the database, updating already existing contributions.
     * 
     * @param contributions
     *            The list of new or changed contributions.
     */
    public final <T> void update(List<T> ts) {
        for (T t : ts) {
            update(t);
        }
    }

    public final <T> void update(T t) {
        final Boolean openedTransaction = openTransaction();
        getManager().merge(t);
        commitTransaction(openedTransaction);
    }
}
