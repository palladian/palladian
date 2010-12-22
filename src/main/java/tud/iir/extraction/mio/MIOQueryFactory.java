/**
 * The MIOQueryFactory creates a List of specific SearchQueries for a given entity and concept
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;

import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;

public class MIOQueryFactory {

    /** The role pages. */
    private transient List<RolePage> rolePages;

    /** The entity. */
    private final transient Entity entity;

    /** The concept. */
    private final transient Concept concept;

    /** The value specifies how much a rolePage must be counted to be relevant. */
    private transient int rolePageRelevanceValue;

    /** The concept search vocabulary. */
    private final List<String> conceptSearchVocabulary;

    /**
     * Instantiates a new MIOQueryFactory.
     *
     * @param entity the entity
     * @param weakFlag the weak flag
     */
    MIOQueryFactory(final Entity entity, boolean weakFlag) {
        this.entity = entity;
        this.concept = entity.getConcept();

        // weakFlag is true than load the vocabulary for weak mios
        if (weakFlag) {
            this.conceptSearchVocabulary = InCoFiConfiguration.getInstance().getWeakMIOVocabulary();
        } else {
            // load conceptSearchVocabulary from InCoFiConfiguration
            String conceptName = concept.getName();
            this.conceptSearchVocabulary = InCoFiConfiguration.getInstance().getVocByConceptName(conceptName);
        }

        // load rolePageRelevanceValue from InCoFiConfiguration
        this.rolePageRelevanceValue = InCoFiConfiguration.getInstance().rolePageRelevanceValue;

        // load the RolePages from Database that where not already used with this entity
        rolePages = new ArrayList<RolePage>();
        final RolePageDatabase rolePageDB = new RolePageDatabase();
        rolePages = rolePageDB.loadNotUsedRolePagesForEntity(entity);
    }

    /**
     * Generate search queries.
     * 
     * @return the list
     */
    public List<String> generateSearchQueries() {

        /** The search queries. */
        List<String> searchQueries = new ArrayList<String>();
        String entityName = entity.getName();
        
        searchQueries.add(entityName);
        for (String searchWord : conceptSearchVocabulary) {
           
            if (searchWord.endsWith("_")) {
                // for the case: "play Quantum of Solice"
                int pos = searchWord.lastIndexOf("_");
                final String modSearchWord = searchWord.substring(0, pos--);
                searchQueries.add("\"" + modSearchWord + " " + entityName + "\"");
            } else {
                searchQueries.add(entityName + " \"" + searchWord + "\"");
            }
        }

        // add rolePages to Searchquery
        if (!rolePages.isEmpty()) {

            for (RolePage rolePage : rolePages) {
                if (rolePage.getCount() >= rolePageRelevanceValue) {
                    searchQueries.add(rolePage.getHostname() + " " + entityName);

                    // add RolePageUsage information to database
                    final RolePageDatabase rpDB = new RolePageDatabase();
                    rpDB.insertRolePageUsage(rolePage, entity);
                }
            }
        }       
        return searchQueries;
    }

}
