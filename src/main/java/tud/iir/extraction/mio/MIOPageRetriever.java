/**
 *  The MIOPageRetriever finds pages from the web that have a relative high probability of containing relevant MIO(s) for
 * a given entity.
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.knowledge.Entity;

public class MIOPageRetriever {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(MIOPageRetriever.class);

    /**
     * Retrieve MIOs.
     * 
     * @param entity the entity
     * @return the list
     */
    public List<MIOPage> retrieveMIOPages(Entity entity, boolean weakFlag) {

        List<MIOPage> mioPages;

        // generate searchQueries
        List<String> searchQueries = generateSearchQueries(entity, weakFlag);

        // initiate search with searchEngines
        List<String> mioPageCandidates = getMIOPageCandidates(searchQueries);

        // List<String> mioPageCandidates = new ArrayList<String>();
        // mioPageCandidates.add("http://www.cinefreaks.com/");
        // mioPageCandidates.add("http://www.miniclip.com/games/en/");
        // mioPageCandidates.add("http://www.miniclip.com/games/en/action.php");
        // mioPageCandidates.add("http://www.miniclip.com/games/mini-golf/en/");

        LOGGER.info("Analyzing MIOPageCandidates startet..for " + entity.getName() + " Count: "
                + mioPageCandidates.size());

        // analyze the MIOPageCandidates for MIO-existence
        mioPages = analyzeMIOPageCandidates(mioPageCandidates, entity);

        LOGGER.info("MIOPageCandidateAnalysis finished for " + entity.getName());

        // detect DedicatedPages, this might take some time
        // DedicatedPageDetector dpDetector = new DedicatedPageDetector();
        // for (MIOPage mioPage : mioPages) {
        // dpDetector.calculateDedicatedPageTrust(mioPage);
        // }
        return mioPages;
    }

    /**
     * Generate specific SearchQueries for every entity.
     *
     * @param entity the entity
     * @return the list
     */
    private List<String> generateSearchQueries(Entity entity, boolean weakFlag) {
        MIOQueryFactory searchQueryFac = new MIOQueryFactory(entity, weakFlag);
        List<String> searchQueries = searchQueryFac.generateSearchQueries();

        return searchQueries;
    }

    /**
     * Initiate a search with generated queries for every entity.
     * 
     * @param searchQueries the search queries
     * @return the list
     */
    private List<String> getMIOPageCandidates(List<String> searchQueries) {
        SearchAgent searchAgent = new SearchAgent();
        List<String> mioPageCandidates = searchAgent.initiateSearch(searchQueries);

        return mioPageCandidates;
    }

    /**
     * Do the Webpage-Analysis.
     * 
     * @param mioPageCandidates the mioPage-candidates
     * @param entity the entity
     * @return the list
     */
    private List<MIOPage> analyzeMIOPageCandidates(List<String> mioPageCandidates, Entity entity) {
        MIOPageCandidateAnalyzer candidateAnalyzer = new MIOPageCandidateAnalyzer(mioPageCandidates);
        // start and get Results of PageAnalyzing
        List<MIOPage> mioPages = candidateAnalyzer.identifyMIOPages(entity);

        return mioPages;
    }

}
