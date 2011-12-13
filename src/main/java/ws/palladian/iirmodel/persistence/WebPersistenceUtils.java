/**
 * Created on: 27.09.2011 07:18:36
 */
package ws.palladian.iirmodel.persistence;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.text.ParseException;
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

    /**
     * 
     */
    private static final long serialVersionUID = -3246653600872101760L;

    /**
     * <p>
     * The logger for objects of this class.
     * </p>
     */
    private final static Logger LOGGER = Logger.getLogger(WebPersistenceUtils.class);

    /**
     * <p>
     * The query for getting all pairs of {@code Item}s from different {@code ItemStream}s that have no relation yet.
     * </p>
     * 
     * @see #getNonAnnotatedInterThreadRelation(Map)
     */
    private static final String GET_NON_LABELED_INTER_STREAM_RELATIONS = "SELECT i1.identifier,i2.identifier "
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
    private static final String GET_NON_LABELED_INTRA_STREAM_RELATIONS = "SELECT i1.identifier,i2.identifier "
            + "FROM Item i1, Item i2 " + "WHERE i1.identifier <> i2.identifier " + "AND i1.identifier NOT IN "
            + "(SELECT DISTINCT ir1.firstEntry.identifier FROM ForumEntryRelation ir1) " + "AND i1.identifier NOT IN "
            + "(SELECT DISTINCT ir2.secondEntry.identifier FROM ForumEntryRelation ir2) " + "AND i2.identifier NOT IN "
            + "(SELECT DISTINCT ir3.firstEntry.identifier FROM ForumEntryRelation ir3) " + "AND i2.identifier NOT IN "
            + "(SELECT DISTINCT ir4.secondEntry.identifier FROM ForumEntryRelation ir4) " + "AND i1.parent=i2.parent";

    private final TypedQuery<Item> getNonLabeledItemQuery;

    private final TypedQuery<Long> countLabeledItemsQuery;
    private final Query countLabeledItemTypes;

    /**
     * <p>
     * The maximum count of non annotated relations that are returned during one query.
     * </p>
     * 
     * @see #getRandomNonAnnotatedRelation(String)
     */
    private static final Integer MAX_NUMBER_OF_NON_LABELED_RELATIONS = 100;

    private static final Random random = new Random(Calendar.getInstance().getTimeInMillis());

    private final TypedQuery<Item> getItemsOnlyLabeledByOthersQuery;

    private final Query countLabeledItemsByLabeler;

    /**
     * @param entityManager
     */
    public WebPersistenceUtils(EntityManager entityManager) {
        super(entityManager);
        countLabeledItemsQuery = getManager().createQuery("SELECT COUNT(l) FROM Label l", Long.class);
        countLabeledItemTypes = getManager()
                .createNativeQuery(
                        "SELECT ANNOTATIONTYPE.typeName, COUNT(ANNOTATION.identifier) FROM ANNOTATION INNER JOIN ANNOTATIONTYPE ON ANNOTATION.annotation_identifier=ANNOTATIONTYPE.identifier GROUP BY ANNOTATIONTYPE.typeName;");
        getNonLabeledItemQuery = getManager()
                .createQuery(
                        "SELECT i FROM Item i WHERE i.identifier NOT IN (SELECT DISTINCT a.annotatedItem.identifier FROM Label a)",
                        Item.class);
        getNonLabeledItemQuery.setMaxResults(100);
        getItemsOnlyLabeledByOthersQuery = getManager()
                .createQuery(
                        // "SELECT i FROM Labeler lr, IN(lr.labels) l, Item i WHERE l.annotatedItem = i AND lr != :labeler",
                        // "SELECT i.* FROM Labeler lr INNER JOIN Labeler_ANNOTATION la ON lr.name=la.Labeler_name INNER JOIN ANNOTATION l ON l.identifier=la.labels_identifier INNER JOIN ITEM i ON i.IDENTIFIER=l.annotatedItem_identifier WHERE lr.name!=:labelerName",
                        "SELECT l.annotatedItem FROM Labeler lr JOIN lr.labels l WHERE l.annotatedItem NOT IN ( SELECT l.annotatedItem FROM Labeler lr JOIN lr.labels l WHERE lr=:labeler)",
                        Item.class);
        getItemsOnlyLabeledByOthersQuery.setMaxResults(100);
        countLabeledItemsByLabeler = getManager()
                .createNativeQuery(
                        "SELECT ANNOTATIONTYPE.typeName, COUNT(ANNOTATION.identifier) FROM ANNOTATION INNER JOIN ANNOTATIONTYPE ON ANNOTATION.annotation_identifier=ANNOTATIONTYPE.identifier INNER JOIN Labeler_ANNOTATION ON ANNOTATION.identifier=Labeler_ANNOTATION.labels_identifier WHERE Labeler_ANNOTATION.Labeler_name = :labelerName GROUP BY ANNOTATIONTYPE.typeName;");
        // COUNT_LABELED_ITEM_TYPES = getManager().createQuery(
        // "SELECT l.annotation, COUNT(l) FROM Label l GROUP BY l.annotation");
        // CriteriaBuilder cb = getManager().getCriteriaBuilder();
        // COUNT_LABELED_ITEM_TYPES = cb.createQuery(LabelType.class);
        // COUNT_LABELED_ITEM_TYPES.from(Label.class);
        // COUNT_LABELED_ITEM_TYPES.gr
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
    public ItemRelation getNonAnnotatedInterThreadRelation(final Map<String, String[]> restrictedOnFieldsWithValues)
            throws IOException, ParseException, URISyntaxException {
        return getRandomNonAnnotatedRelation(createQueryForRandomNonAnnotatedRelations(
                GET_NON_LABELED_INTER_STREAM_RELATIONS, restrictedOnFieldsWithValues));
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
    public ItemRelation getNonAnnotatedIntraThreadRelation(final Map<String, String[]> restrictedOnFieldsWithValues)
            throws IOException, ParseException, URISyntaxException {
        return getRandomNonAnnotatedRelation(createQueryForRandomNonAnnotatedRelations(
                GET_NON_LABELED_INTRA_STREAM_RELATIONS, restrictedOnFieldsWithValues));
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
     * @see #GET_NON_LABELED_INTER_STREAM_RELATIONS
     * @see #GET_NON_LABELED_INTRA_STREAM_RELATIONS
     */
    @SuppressWarnings("unchecked")
    private ItemRelation getRandomNonAnnotatedRelation(final String query) {
        LOGGER.debug("Running getRandomNonAnnotatedRelation with query: " + query);
        Query loadNonEvaluatedForumEntryIDPairs = getManager().createQuery(query);
        loadNonEvaluatedForumEntryIDPairs.setMaxResults(MAX_NUMBER_OF_NON_LABELED_RELATIONS);
        List<Object> results = loadNonEvaluatedForumEntryIDPairs.getResultList();
        LOGGER.debug("Returned results: " + results);
        if (!results.isEmpty()) {
            final Random randomIndexGenerator = new Random();
            int randomIndex = randomIndexGenerator.nextInt(results.size());
            final Object[] idPair = (Object[])results.get(randomIndex);
            // changed from (String) to (Integer); untested
            Item firstEntry = load(idPair[0], Item.class);
            Item secondEntry = load(idPair[1], Item.class);
            return new ItemRelation(firstEntry, secondEntry, null, "");
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Provides a not yet annotated {@code Item} from the database.
     * </p>
     * 
     * @return A random not yet annotated {@code Item} or {@code null} if no such item exists.
     */
    public Item getRandomNonLabeledItem() {
        Boolean openedTransaction = openTransaction();
        try {
            List<Item> nonAnnotateditems = getNonLabeledItemQuery.getResultList();

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
    public Item getNextNonSelfLabeledItem(final Labeler labeler) {
        Boolean openedTransaction = openTransaction();
        try {
            getItemsOnlyLabeledByOthersQuery.setParameter("labeler", labeler);
            List<Item> itemsLabeledByOthers = getItemsOnlyLabeledByOthersQuery.getResultList();

            if (itemsLabeledByOthers.isEmpty()) {
                LOGGER.debug("Selecting random item from items not labeled by " + labeler.getName()
                        + " but by other labelers");
                return getRandomNonLabeledItem();
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
     * @param labelName
     * @return
     */
    public LabelType loadLabelTypeByName(String labelName) {
        TypedQuery<LabelType> loadQuery = getManager().createQuery(
                "SELECT a FROM LabelType a WHERE a.typeName = :typeName", LabelType.class);
        loadQuery.setParameter("typeName", labelName);
        Boolean openedTransaction = openTransaction();
        List<LabelType> result = loadQuery.getResultList();
        commitTransaction(openedTransaction);
        if (result.size() > 1) {
            throw new IllegalStateException("Duplicate annotation types found");
        } else if (result.size() < 1) {
            return null;
        } else {
            return result.get(0);
        }
    }

    public List<LabelType> loadLabelTypes() {
        Query query = getManager().createQuery("SELECT lt FROM LabelType lt");
        Boolean openedTransaction = openTransaction();
        try {
            @SuppressWarnings("unchecked")
            List<LabelType> ret = query.getResultList();
            return ret;
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * @param labelType
     */
    public void saveLabelType(LabelType labelType) {
        if (loadLabelTypeByName(labelType.getTypeName()) != null) {
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

    public Long countLabeledItems() {
        Boolean openedTransaction = openTransaction();
        try {
            return countLabeledItemsQuery.getSingleResult();
        } finally {
            commitTransaction(openedTransaction);
        }
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
        Boolean openedTransaction = openTransaction();
        try {
            Map<String, String> ret = new HashMap<String, String>();
            @SuppressWarnings("unchecked")
            List<Object[]> results = countLabeledItemTypes.getResultList();
            for (Object[] mapping : results) {
                ret.put((String)mapping[0], String.valueOf(mapping[1]));
            }
            return ret;
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    public Map<String, String> countLabeledItemsByType(final String labelerName) {
        Map<String, String> ret = new HashMap<String, String>();
        countLabeledItemsByLabeler.setParameter("labelerName", labelerName);
        Boolean openedTransaction = openTransaction();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> results = countLabeledItemsByLabeler.getResultList();
            for (Object[] mapping : results) {
                ret.put((String)mapping[0], String.valueOf(mapping[1]));
            }
            return ret;
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
     * Provides a {@link Collection} of all {@link Labeler}s from the database.
     * </p>
     * 
     * @return A {@code Collection} containing all {@code Labeler}s from the database.
     */
    @SuppressWarnings("unchecked")
    public Collection<Labeler> loadLabeler() {
        Query query = getManager().createQuery("SELECT l FROM Labeler l");
        Boolean openedTransaction = openTransaction();
        try {
            return query.getResultList();
        } finally {
            commitTransaction(openedTransaction);
        }
    }

    /**
     * @param name
     * @return
     */
    public Long countLabeledItems(String name) {
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

}
