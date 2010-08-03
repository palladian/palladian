/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.knowledge.Entity;

/**
 * The MIOPageRetriever finds pages from the web that have a relative high probability of containing relevant MIO(s) for
 * a given entity.
 * 
 * @author Martin Werner
 */
public class MIOPageRetriever {

    /** The LOGGER. */
    private static final Logger LOGGER = Logger.getLogger(MIOPageRetriever.class);

    /**
     * The rolePage-List.
     * 
     * @param entity the entity
     */
    // private List<RolePage> rolePageList;

    /**
     * Instantiates a new MIOPageRetriever.
     */
    public MIOPageRetriever(Entity entity) {
        // rolePageList = new ArrayList<RolePage>();
        // RolePageDatabase rolePageDB = new RolePageDatabase();
        // rolePageList = rolePageDB.loadNotUsedRolePagesForEntity(entity);
    }

    /**
     * Retrieve MIOs.
     * 
     * @param entity the entity
     * @param searchVoc the search voc
     * @return the list
     */
    public List<MIOPage> retrieveMIOPages(final Entity entity, final ConceptSearchVocabulary searchVoc) {

        List<MIOPage> mioPages;

        // generate searchQueries
        final List<String> searchQueries = generateSearchQueries(entity, searchVoc);

        // initiate search with searchEngines
        final List<String> mioPageCandidates = getMIOPageCandidates(searchQueries);

        LOGGER.info("Analyzing MIOPageCandidates startet..for " + entity.getName() + " Count: "
                + mioPageCandidates.size());

        // analyze the MIOPageCandidates for MIO-existence
        mioPages = analyzeMIOPageCandidates(mioPageCandidates, entity);

        LOGGER.info("MIOPageCandidateAnalysis finished, DedicatedPage-Calculation starts..for " + entity.getName());

        // detect DedicatedPages
        for (MIOPage mioPage : mioPages) {
            final DedicatedPageDetector dpDetector = new DedicatedPageDetector();
            dpDetector.calculateDedicatedPageTrust(mioPage);
            // System.out.println(entity.getName() + "  " + mioPage.getUrl());
        }
        LOGGER.info("DedicatedPage-Calculation finished..for " + entity.getName());

        return mioPages;
    }

    /**
     * Generate specific SearchQueries for every entity.
     * 
     * @param entity the entity
     * @param conceptVocabulary the concept vocabulary
     * @return the list
     */
    private List<String> generateSearchQueries(final Entity entity, final ConceptSearchVocabulary conceptVocabulary) {
        final MIOQueryFactory searchQueryFac = new MIOQueryFactory(entity);
        final List<String> searchQueries = searchQueryFac.generateSearchQueries(conceptVocabulary);

        return searchQueries;
    }

    /**
     * Initiate a search with generated queries for every entity.
     * 
     * @param searchQueries the search queries
     * @return the list
     */
    private List<String> getMIOPageCandidates(final List<String> searchQueries) {
        final SearchAgent searchAgent = new SearchAgent();
        final List<String> mioPageCandidates = searchAgent.initiateSearch(searchQueries);

        return mioPageCandidates;
    }

    /**
     * Do the Webpage-Analysis.
     * 
     * @param mioPageCandidates the mioPage-candidates
     * @param entity the entity
     * @return the list
     */
    private List<MIOPage> analyzeMIOPageCandidates(final List<String> mioPageCandidates, final Entity entity) {
        final MIOPageCandidateAnalyzer candidateAnalyzer = new MIOPageCandidateAnalyzer(mioPageCandidates);
        // start and get Results of PageAnalyzing
        final List<MIOPage> mioPages = candidateAnalyzer.identifyMIOPages(entity);

        return mioPages;
    }

}
