package tud.iir.extraction.fact;

import java.util.HashSet;
import java.util.Iterator;

import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.KnowledgeManager;

/**
 * The FactQueryFactory creates FactQuery objects.
 * 
 * @author David Urbansky
 */
class FactQueryFactory {

    /**
     * TODO test with X + Concept name TODO X has a Y of fact query types "X" facts with "Y is Z" "X" "Y" "Y2" "Y3"... with intitle: higher trust
     * "X('s) Y (is/was) Z" "X Y is Z" "X Y was Z" "(the) Y of (the) X is Z" "X" "Y" TODO: add "intitle:X" to all queries for higher precision
     */
    public static final int TYPE_X = 1; // just the entity name TODO special case of X_YN with N=0
    public static final int TYPE_X_FACTS = 2; // "X" facts with "Y is[ Z]"
    public static final int TYPE_X_YN = 3; // "X" "Y" "Y2" "Y3"...
    public static final int TYPE_X_Y_IS = 4; // "X('s) Y (is/was)[ Z]"
    public static final int TYPE_Y_OF_X_IS = 5; // "(the) Y of (the|a| ) X is[ Z]"
    public static final int TYPE_X_Y = 6; // "X" "Y" (entity and one attribute at a time)

    private static FactQueryFactory instance = null;
    private boolean inTitleRequired = false;

    private FactQueryFactory() {
    }

    public static FactQueryFactory getInstance() {
        if (instance == null)
            instance = new FactQueryFactory();
        return instance;
    }

    public boolean isInTitleRequired() {
        return inTitleRequired;
    }

    public void setInTitleRequired(boolean inTitleRequired) {
        this.inTitleRequired = inTitleRequired;
    }

    /**
     * Fact queries for overview (attributes are automatically chosen or not needed).
     * 
     * @param entity The entity
     * @param type The type.
     * @return The fact query.
     */
    public FactQuery createManyAttributesQuery(Entity entity, int type) {
        switch (type) {
            case FactQueryFactory.TYPE_X:
                return this.createXQuery(entity);
            case FactQueryFactory.TYPE_X_FACTS:
                return this.createXFactsQuery(entity);
            case FactQueryFactory.TYPE_X_YN:
                return this.createXYNQuery(entity);
            default:
                return null;
        }
    }

    /**
     * Specific attribute look ups.
     * 
     * @param entity The entity.
     * @param attributeName The name of the attribute.
     * @param type The type.
     * @return The fact query.
     */
    public FactQuery createSingleAttributeQuery(Entity entity, Attribute attribute, int type) {
        switch (type) {
            case FactQueryFactory.TYPE_X_Y_IS:
                return this.createXYISQuery(entity, attribute);
            case FactQueryFactory.TYPE_Y_OF_X_IS:
                return this.createYOFXISQuery(entity, attribute);
            case FactQueryFactory.TYPE_X_Y:
                return this.createXYQuery(entity, attribute);
            default:
                return null;
        }

    }

    private FactQuery createXQuery(Entity entity) {
        FactQuery fq = new FactQuery(entity);
        fq.setQueryType(TYPE_X);

        // look for all attributes on fact pages
        fq.setAttributes(entity.getConcept().getAttributes());
        fq.setFactStructureType(FactQuery.FREE_TEXT_AND_STRUCTURED);
        String queryString = "\"" + entity.getName() + "\"";
        if (isInTitleRequired())
            queryString += " intitle:" + entity.getName();
        String[] querySet = { queryString };
        fq.setQuerySet(querySet);

        return fq;
    }

    private FactQuery createXFactsQuery(Entity entity) {
        FactQuery fq = new FactQuery(entity);
        fq.setQueryType(TYPE_X_FACTS);

        // look for all attributes on fact pages
        fq.setAttributes(entity.getConcept().getAttributes());
        fq.setFactStructureType(FactQuery.FREE_TEXT_AND_STRUCTURED);
        String queryString = "\"" + entity.getName() + "\" facts";
        if (isInTitleRequired())
            queryString += " intitle:" + entity.getName();
        String[] querySet = { queryString };
        fq.setQuerySet(querySet);

        return fq;
    }

    private FactQuery createXYNQuery(Entity entity) {
        FactQuery fq = new FactQuery(entity);
        fq.setQueryType(TYPE_X_YN);

        fq.setFactStructureType(FactQuery.FREE_TEXT_AND_STRUCTURED);
        HashSet<Attribute> attributes = entity.getConcept().getAttributes();

        // look for all attributes
        fq.setAttributes(attributes);
        int attributeCount = attributes.size();

        // create search queries for all attributes (n) 2^n
        // int totalQueries = (int)Math.pow(2.0,(double)attributeCount); // TODO too many queries!
        int totalQueries = attributeCount; // take as many queries as attributes, first all attributes than all-1, all-2...

        // start with all first than gradually take less
        StringBuilder attributeString = new StringBuilder();
        Iterator<Attribute> attrIterator = attributes.iterator();
        while (attrIterator.hasNext()) {
            Attribute a = attrIterator.next();
            attributeString.append("#").append("\"" + a.getName() + "\"");
        }
        String currentAttributeString = attributeString.toString();

        String[] querySet = new String[totalQueries];

        // create the queries, i+1 equals the number of attributes added to the entity name, start with all first than gradually take less
        for (int i = 0; i < totalQueries; ++i) {

            String queryString = "\"" + entity.getName() + "\"" + currentAttributeString.replaceAll("#", " ");
            if (isInTitleRequired())
                queryString += " intitle:" + entity.getName();
            querySet[i] = queryString;

            int nextIndex = currentAttributeString.lastIndexOf("#");
            if (nextIndex > -1)
                currentAttributeString = currentAttributeString.substring(0, currentAttributeString.lastIndexOf("#"));
        }

        fq.setQuerySet(querySet);

        return fq;
    }

    private FactQuery createXYISQuery(Entity entity, Attribute attribute) {
        FactQuery fq = new FactQuery(entity);
        fq.setQueryType(TYPE_X_Y_IS);

        fq.addAttribute(attribute);
        fq.setFactStructureType(FactQuery.FREE_TEXT_ONLY);
        String queryString1 = "\"" + entity.getName() + "'s " + attribute.getName() + " is\"";
        // String queryString2 = "\""+entity.getName()+" "+attribute.getName()+" is\"";
        if (isInTitleRequired()) {
            queryString1 += " intitle:" + entity.getName();
            // queryString2 += " intitle:"+entity.getName();
        }
        String[] querySet = { queryString1 /* ,queryString2 */};
        fq.setQuerySet(querySet);

        return fq;
    }

    private FactQuery createYOFXISQuery(Entity entity, Attribute attribute) {
        FactQuery fq = new FactQuery(entity);
        fq.setQueryType(TYPE_Y_OF_X_IS);

        fq.addAttribute(attribute);
        fq.setFactStructureType(FactQuery.FREE_TEXT_ONLY);

        String queryString1 = "\"the " + attribute.getName() + " of " + entity.getName() + " is\"";
        String queryString2 = "\"the " + attribute.getName() + " of the " + entity.getName() + " is\"";
        String queryString3 = "\"the " + attribute.getName() + " for " + entity.getName() + " is\"";
        String queryString4 = "\"the " + attribute.getName() + " for the " + entity.getName() + " is\"";

        if (isInTitleRequired()) {
            queryString1 += " intitle:" + entity.getName();
            queryString2 += " intitle:" + entity.getName();
            queryString3 += " intitle:" + entity.getName();
            queryString4 += " intitle:" + entity.getName();
        }
        String[] querySet = { queryString1, queryString2, queryString3, queryString4 };
        fq.setQuerySet(querySet);

        return fq;
    }

    private FactQuery createXYQuery(Entity entity, Attribute attribute) {
        FactQuery fq = new FactQuery(entity);
        fq.setQueryType(TYPE_X_Y);

        fq.addAttribute(attribute);
        fq.setFactStructureType(FactQuery.FREE_TEXT_AND_STRUCTURED);
        String queryString1 = "\"" + entity.getName() + "\" " + "\"" + attribute.getName() + "\"";
        if (isInTitleRequired())
            queryString1 += " intitle:" + entity.getName();
        String[] querySet = { queryString1 };
        fq.setQuerySet(querySet);

        return fq;
    }

    public FactQuery createImageQuery(Entity entity, Attribute attribute) {
        FactQuery fq = new FactQuery(entity);
        String query = "\"" + entity.getName() + "\"";

        // CONVENTION "_entity_image_" is the an image with the entity name
        if (!attribute.getName().equalsIgnoreCase(entity.getName()) && !attribute.getName().equalsIgnoreCase("_entity_image_")) {
            query += " \"" + attribute.getName() + "\"";
        }

        String[] querySet = { query };
        fq.setQuerySet(querySet);
        fq.addAttribute(attribute);
        return fq;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        KnowledgeManager knowledgeManager = new KnowledgeManager();
        knowledgeManager.createBenchmarkConcepts();
        knowledgeManager.getConcepts().get(0);
        FactQuery fq = FactQueryFactory.getInstance().createManyAttributesQuery(new Entity("Australia", knowledgeManager.getConcepts().get(0)),
                FactQueryFactory.TYPE_X_YN);
        System.out.println(fq.getQuerySet().length);
        for (int i = 0, l = fq.getQuerySet().length; i < l; ++i) {
            System.out.println(fq.getQuerySet()[i]);
        }
    }
}