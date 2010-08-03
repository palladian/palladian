/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;

import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;

/**
 * The MIOQueryFactory creates a List of specific SearchQueries for a given entity and concept
 * 
 * @author Martin Werner
 */
public class MIOQueryFactory {

    /** The search queries. */
    private List<String> searchQueries;

    /** The role pages. */
    private List<RolePage> rolePages;

    /** The entity. */
    Entity entity;

    /** The concept. */
    Concept concept;

    /** The value specifies how much a rolePage must be counted to be relevant. */
    private int rolePageRelevanceValue = 5;

    /**
     * Instantiates a new mIO query factory.
     * 
     * @param entity the entity
     */
    MIOQueryFactory(Entity entity) {
        this.entity = entity;
        this.concept = entity.getConcept();

        // load the RolePages from Database that where not already used with this entity
        rolePages = new ArrayList<RolePage>();
        RolePageDatabase rolePageDB = new RolePageDatabase();
        rolePages = rolePageDB.loadNotUsedRolePagesForEntity(entity);
    }

    /**
     * Generate search queries.
     * 
     * @param searchVoc the searchVocabulary
     * @return the list
     */
    public List<String> generateSearchQueries(ConceptSearchVocabulary searchVoc) {

        searchQueries = new ArrayList<String>();
        String entityName = entity.getName();
        List<String> conceptVocabulary = searchVoc.getVocByConceptName(concept.getName());

        searchQueries.add(entityName);
        for (String searchWord : conceptVocabulary) {

            if (!searchWord.endsWith("_")) {
                searchQueries.add(entityName + " \"" + searchWord + "\"");
                // System.out.println(entityName + " \"" + searchWord + "\"");

            } else {
                // for the case: "play Quantum of Solice"
                int pos = searchWord.lastIndexOf("_");
                String modSearchWord = searchWord.substring(0, pos--);
                searchQueries.add("\"" + modSearchWord + " " + entityName + "\"");
            }
        }

        // add rolePages to Searchquery
        if (!rolePages.isEmpty()) {
            for (RolePage rolePage : rolePages) {
                if (rolePage.getCount() >= rolePageRelevanceValue) {
                    searchQueries.add(rolePage.getHostname() + " " + entityName);

                    // add RolePageUsage information to database
                    RolePageDatabase rpDB = new RolePageDatabase();
                    rpDB.insertRolePageUsage(rolePage, entity);
                }

            }

        }

        return searchQueries;
    }

}
