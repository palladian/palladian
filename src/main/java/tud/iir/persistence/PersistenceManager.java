package tud.iir.persistence;

import tud.iir.knowledge.KnowledgeManager;

/**
 * The PersistenceManager triggers the DatabaseManager and the OntologyManager.
 * 
 * @author David Urbansky
 */
public class PersistenceManager {

    public static void saveExtractions(KnowledgeManager knowledgeManager) {
        // TODO repair saving into ontology
        // OntologyManager.getInstance().saveExtractions(knowledgeManager);
        DatabaseManager.getInstance().saveExtractions(knowledgeManager);
    }

    public static void cleanKnowledgeBase() {
        OntologyManager.getInstance().clearCompleteKnowledgeBase();
        DatabaseManager.getInstance().clearCompleteDatabase();
    }
}