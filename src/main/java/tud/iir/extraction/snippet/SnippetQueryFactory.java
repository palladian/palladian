package tud.iir.extraction.snippet;

import tud.iir.knowledge.Entity;

/**
 * This class acts as query template builder factory. Given an entity, it generates a set of queries, which are sent to
 * the search engines.
 * 
 * @author David Urbansky
 * @author Christopher Friedrich *
 * 
 */
public class SnippetQueryFactory {

    /** The instance of this class. */
    private static SnippetQueryFactory instance = null;

    private SnippetQueryFactory() {
    }

    public static SnippetQueryFactory getInstance() {
        if (instance == null) {
            instance = new SnippetQueryFactory();
        }
        return instance;
    }

    /**
     * Given an entity, this method returns a SnippetQuery object, which is a set of search engine queries for a given
     * entity.
     * 
     * @param entity The entity for which a query should be built.
     */
    public SnippetQuery createEntityQuery(Entity entity) {
        SnippetQuery sq = new SnippetQuery(entity);

        String queryEntity = "\"" + entity.getName() + "\"";
        String queryEntityConcept = "\"" + entity.getName() + "\" \"" + entity.getConcept().getName() + "\"";

        String[] querySet = { queryEntity, queryEntityConcept };
        sq.setQuerySet(querySet);

        return sq;
    }

}