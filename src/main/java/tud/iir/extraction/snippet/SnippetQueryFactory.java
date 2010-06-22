package tud.iir.extraction.snippet;

import tud.iir.knowledge.Entity;
import tud.iir.knowledge.KnowledgeManager;

/**
 * This class acts as query template builder factory. Given an entity, it generates a set of queries, which are sent to the search engines.
 * 
 * @author Christopher Friedrich
 */
public class SnippetQueryFactory {

    private static SnippetQueryFactory instance = null;

    private SnippetQueryFactory() {
    }

    public static SnippetQueryFactory getInstance() {
        if (instance == null)
            instance = new SnippetQueryFactory();
        return instance;
    }

    /**
     * Given an entity, this method returns a SnippetQuery object, which is a set of search engine queries for a given entity.
     */
    public SnippetQuery createEntityQuery(Entity entity) {
        SnippetQuery sq = new SnippetQuery(entity);

        String queryEntity = "\"" + entity.getName() + "\"";
        String queryEntityConcept = "\"" + entity.getName() + "\" \"" + entity.getConcept().getName() + "\"";

        String[] querySet = { queryEntity, queryEntityConcept };
        sq.setQuerySet(querySet);

        return sq;
    }

    public static void main(String[] args) {

        KnowledgeManager knowledgeManager = new KnowledgeManager();
        knowledgeManager.createBenchmarkConcepts();
        knowledgeManager.getConcepts().get(0);
        SnippetQuery sq = SnippetQueryFactory.getInstance().createEntityQuery(new Entity("iPhone 3GS", knowledgeManager.getConcepts().get(0)));
        System.out.println(sq.getQuerySet().length);
        for (int i = 0, l = sq.getQuerySet().length; i < l; ++i) {
            System.out.println(sq.getQuerySet()[i]);
        }
    }
}