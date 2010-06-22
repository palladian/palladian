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

    ArrayList<String> SearchQueries;

    public MIOQueryFactory() {

    }

    public List<String> generateSearchQueries(String entityName, Concept concept, List<RolePage> rolePages, ConceptSearchVocabulary searchVoc) {

        SearchQueries = new ArrayList<String>();
        List<String> conceptVocabulary = searchVoc.getVocByConceptName(concept.getName());

        SearchQueries.add(entityName);
        for (String searchWord : conceptVocabulary) {

            if (!searchWord.endsWith("_")) {
                SearchQueries.add(entityName + " \"" + searchWord + "\"");
                // System.out.println(entityName + " \"" + searchWord + "\"");

            } else {
                // for the case: "play Quantum of Solice"
                int pos = searchWord.lastIndexOf("_");
                String modSearchWord = searchWord.substring(0, pos--);
                SearchQueries.add("\"" + modSearchWord + " " + entityName + "\"");
            }
        }

        //		
        // SearchQueries.add(entityName + " \"360 view\"");
        // SearchQueries.add(entityName + " overview");

        // add rolePages to Searchquery
        if (!rolePages.isEmpty()) {
            for (RolePage rolePage : rolePages) {
                SearchQueries.add(rolePage.getHostname() + " " + entityName);
            }

        }

        return SearchQueries;
    }

}
