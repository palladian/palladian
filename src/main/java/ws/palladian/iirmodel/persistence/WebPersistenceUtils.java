/**
 * Created on: 27.09.2011 07:18:36
 */
package ws.palladian.iirmodel.persistence;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Calendar;
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

    private static final String GET_NON_LABELED_ITEM = "SELECT i FROM Item i WHERE i.identifier NOT IN (SELECT DISTINCT a.annotatedItem.identifier FROM Label a)";

    private final TypedQuery<Long> COUNT_LABELED_ITEMS_QUERY;
    private final Query COUNT_LABELED_ITEM_TYPES;

    /**
     * <p>
     * The maximum count of non annotated relations that are returned during one query.
     * </p>
     * 
     * @see #getRandomNonAnnotatedRelation(String)
     */
    private static final Integer MAX_NUMBER_OF_NON_LABELED_RELATIONS = 100;

    private static final Random random = new Random(Calendar.getInstance().getTimeInMillis());

    /**
     * @param entityManager
     */
    public WebPersistenceUtils(EntityManager entityManager) {
        super(entityManager);
        COUNT_LABELED_ITEMS_QUERY = getManager().createQuery("SELECT COUNT(l) FROM Label l", Long.class);
        COUNT_LABELED_ITEM_TYPES = getManager()
                .createNativeQuery(
                        "SELECT ANNOTATIONTYPE.typeName, COUNT(ANNOTATION.identifier) FROM ANNOTATION INNER JOIN ANNOTATIONTYPE ON ANNOTATION.annotation_identifier=ANNOTATIONTYPE.identifier GROUP BY ANNOTATIONTYPE.typeName;");
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
        TypedQuery<Item> getNonAnnotatedItemQuery = getManager().createQuery(GET_NON_LABELED_ITEM, Item.class);
        getNonAnnotatedItemQuery.setMaxResults(100);

        Boolean openedTransaction = openTransaction();
        try {
            List<Item> nonAnnotateditems = getNonAnnotatedItemQuery.getResultList();

            int countOfNonAnnotatedItems = nonAnnotateditems.size();
            if (countOfNonAnnotatedItems > 0) {
                int randomIndex = random.nextInt(countOfNonAnnotatedItems);
                return nonAnnotateditems.get(randomIndex);
            } else {
                return null;
            }
        } finally {
            commitTransaction(openedTransaction);
        }
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
            return COUNT_LABELED_ITEMS_QUERY.getSingleResult();
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
    public Map<String, String> countLabelTypes() {
        Boolean openedTransaction = openTransaction();
        try {
            Map<String, String> ret = new HashMap<String, String>();
            @SuppressWarnings("unchecked")
            List<Object[]> results = COUNT_LABELED_ITEM_TYPES.getResultList();
            for (Object[] mapping : results) {
                ret.put((String)mapping[0], String.valueOf(mapping[1]));
            }
            return ret;
        } finally {
            commitTransaction(openedTransaction);
        }
    }

}
