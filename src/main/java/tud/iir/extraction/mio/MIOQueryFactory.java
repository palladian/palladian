/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;

import tud.iir.knowledge.Concept;

/**
 * The MIOQueryFactory creates a List of specific SearchQueries for a given entity and concept
 * 
 * @author Martin Werner
 */
public class MIOQueryFactory {

    private List<String> searchQueries;

    /**
     * Generate search queries.
     * 
     * @param entityName the entity name
     * @param concept the concept
     * @param rolePages the role pages
     * @param searchVoc the search voc
     * @return the list
     */
    public List<String> generateSearchQueries(String entityName, Concept concept, List<RolePage> rolePages,
            ConceptSearchVocabulary searchVoc) {

        searchQueries = new ArrayList<String>();
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

        //
        // SearchQueries.add(entityName + " \"360 view\"");
        // SearchQueries.add(entityName + " overview");

        // add rolePages to Searchquery
        if (!rolePages.isEmpty()) {
            for (RolePage rolePage : rolePages) {
                searchQueries.add(rolePage.getHostname() + " " + entityName);
            }

        }

        return searchQueries;
    }

}
