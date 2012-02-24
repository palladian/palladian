/**
 * Created on: 27.09.2011 07:18:36
 */
package ws.palladian.iirmodel.persistence;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;

import ws.palladian.iirmodel.Item;
import ws.palladian.iirmodel.ItemRelation;
import ws.palladian.iirmodel.Label;
import ws.palladian.iirmodel.LabelType;
import ws.palladian.iirmodel.Labeler;
import ws.palladian.iirmodel.RelationType;

/**
 * <p>
 * Provides utility functions to carry out certain operations on the underlying database.
 * </p>
 * 
 * @author Klemens Muthmann
 * @since 0.0.1
 * @version 1.0.0
 */
public final class WebPersistenceUtils extends AbstractPersistenceLayer implements Serializable {

    public interface ParameterFiller {
        void fillParameter(final Query query);
    }

    private static final String COUNT_LABELED_ITEMS_BY_LABELER = "SELECT a, COUNT(a) FROM Labeler lr JOIN lr.labels l JOIN l.labelType a WHERE lr.name = :labelerName GROUP BY a";

    private static final String COUNT_LABELED_ITEMS_BY_TYPES = "SELECT LabelType.name, COUNT(Label.identifier) FROM Label INNER JOIN LabelType ON Label.type_identifier=LabelType.identifier GROUP BY LabelType.name";

    private static final String COUNT_LABELS = "SELECT COUNT(l) FROM Label l";

    private static final String LOAD_ITEMS_LABELED_BY = "SELECT i FROM Labeler lr JOIN lr.labels l JOIN l.labeledItem i WHERE lr=:labeler";

    private static final String LOAD_ITEMS_ONLY_LABELED_BY_OTHERS = "SELECT l.labeledItem FROM Labeler lr JOIN lr.labels l WHERE l.labeledItem NOT IN ( SELECT l.labeledItem FROM Labeler lr JOIN lr.labels l WHERE lr=:labeler)";

    private static final String LOAD_NON_LABELED_ITEMS = "SELECT i FROM Item i WHERE i.identifier NOT IN (SELECT DISTINCT a.labeledItem.identifier FROM Label a)";

    private static final String LOAD_RANDOM_ITEM_OF_TYPE = "SELECT Item.* FROM Item INNER JOIN Label ON Label.labeledItem_identifier=Item.identifier INNER JOIN LabelType ON LabelType.identifier=Label.type_identifier WHERE LabelType.name=:typeName ORDER BY RAND() LIMIT 1";

    private static final String LOAD_RELATION_TYPE_BY_NAME = "SELECT rt FROM RelationType rt WHERE rt.name=:typeName";

    private static final String LOAD_RELATIONS_FOR_ITEM = "SELECT r FROM Labeler l JOIN l.relations r WHERE r.firstItem=:firstItem AND r.secondItem=:secondItem AND l=:labeler";
    /**
     * <p>
     * The logger for objects of this class.
     * </p>
     */
    private final static Logger LOGGER = Logger.getLogger(WebPersistenceUtils.class);
    /**
     * <p>
     * The maximum count of non annotated relations that are returned during one query.
     * </p>
     * 
     * @see #loadRandomNonAnnotatedRelation(String)
     */
    private static final Integer MAX_NUMBER_OF_RESULTS = 100;
    private static final Random random = new Random(Calendar.getInstance().getTimeInMillis());
    /**
     * 
     */
    private static final long serialVersionUID = -3246653600872101760L;

    /**
     * <p>
     * Creates a new completely initialized object of this class. The {@code EntityManager} may be created using the JPA
     * API like so:
     * </p>
     * <code>
     * <pre>
     * EntityManagerFactory factory = Persistence.createEntityManagerFactory("somePersistenceUnitName");
     * EntityManager manager = factory.createEntityManager();
     * WebPersistenceUtils persistenceUtils = new WebPersistenceUtils(manager);
     * </pre>
     * </code>
     * <p>
     * Be careful. Since {@code EntityManager}s are not thread safe always use only one instance
     * {@code WebPersistenceUtils} object per thread. The best practice is to always create a new instance of this class
     * whenever you need to do some persistence work.
     * </p>
     * 
     * @param entityManager The JPA {@code EntityManager} handling the database operations.
     */
    public WebPersistenceUtils(EntityManager entityManager) {
        super(entityManager);
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
        TypedQuery<Long> query = getManager()
                .createQuery(
                        "SELECT COUNT(l.annotatedItem) FROM Labeler lr JOIN lr.labels l WHERE lr.name=:labelerName",
                        Long.class).setParameter("labelerName", name);
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
     * @param labelName
     * @return
     */
    public LabelType loadLabelTypeByName(String labelName) {
        TypedQuery<LabelType> loadQuery = getManager().createQuery("SELECT a FROM LabelType a WHERE a.name = :name",
                LabelType.class);
        loadQuery.setParameter("name", labelName);
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
     * @param queryItem
     * @param item
     * @param labeler
     * @return
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
     * @param relationTypeName
     * @return
     */
    public RelationType loadRelationTypeByName(String relationTypeName) {
        TypedQuery<RelationType> loadRelationTypeByNameQuery = getManager().createQuery(LOAD_RELATION_TYPE_BY_NAME,
                RelationType.class);
        loadRelationTypeByNameQuery.setParameter("typeName", relationTypeName);
        Boolean openedTransaction = openTransaction();
        try {
            return loadRelationTypeByNameQuery.getSingleResult();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

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
     * @param label
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
     * Saves a non existing {@link Labeler} to the database. You need to make sure that all labels provided by this
     * labeler are saved in advance or the persistence layer will throw errors.
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
     * @param labelType
     */
    public void saveLabelType(LabelType labelType) {
        if (loadLabelTypeByName(labelType.getName()) != null) {
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
}
