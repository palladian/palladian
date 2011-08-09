/**
 * Created on: 28.05.2010 09:56:24
 */
package ws.palladian.iirmodel.persistence;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.0
 */
public abstract class AbstractPersistenceLayer {

    public static final String MYSQL_PERSISTENCE_UNIT_NAME = "de.effingo.persistence";

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
    public void shutdown() {
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
    public EntityManager getManager() {
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
    protected List<Object> mergeCollections(Collection<Object> collection1, Collection<Object> collection2) {
        List<Object> ret = new LinkedList<Object>();
        for (Object entry : collection1) {
            ret.add(entry);
        }
        for (Object obj : collection2) {
            if (!collection1.contains(obj)) {
                ret.add(obj);
            }
        }
        return ret;
    }

    protected Boolean openTransaction() {
        if (manager.getTransaction().isActive()) {
            return false;
        } else {
            manager.getTransaction().begin();
            return true;
        }
    }

    protected void commitTransaction(final Boolean openedTransaction) {
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

    public <T> T load(Object identifier, Class<T> classToLoad) {
        final Boolean openedTransaction = openTransaction();
        T ret = getManager().find(classToLoad, identifier);
        commitTransaction(openedTransaction);
        return ret;
    }

    // public <T> Collection<T> loadAll(Class<T> classToLoad) {
    // Query loadQuery = getManager().createQuery("SELECT t FROM :classToLoad t");
    // loadQuery.setParameter("classToLoad", classToLoad.getName());
    // final Boolean openedTransaction = openTransaction();
    // @SuppressWarnings("unchecked")
    // Collection<T> ret = loadQuery.getResultList();
    // pl.commitTransaction(openedTransaction);
    // return ret;
    // }

    /**
     * Return the first object in a list, or <code>null</code>, if list is empty.
     * 
     * @param list
     * @return
     */
    protected <T> T getFirst(List<T> list) {
        if (list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }
}
