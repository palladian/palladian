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
     * Retrieve MIOs.
     * 
     * @param entity the entity
     * @return the list
     */
    public List<MIOPage> retrieveMIOPages(final Entity entity, boolean weakFlag) {

        List<MIOPage> mioPages;
       
//        final long timeStamp1 = System.currentTimeMillis();

        // generate searchQueries
        final List<String> searchQueries = generateSearchQueries(entity, weakFlag);

        // initiate search with searchEngines
        final List<String> mioPageCandidates = getMIOPageCandidates(searchQueries);
        
//        System.out.println("GENERATING SearchQueries and GETTING mioPageCandidates finished in: "+ DateHelper.getRuntime(timeStamp1));
        LOGGER.info("Analyzing MIOPageCandidates startet..for " + entity.getName() + " Count: "
                + mioPageCandidates.size());
        
//        final long timeStamp2 = System.currentTimeMillis();
        // analyze the MIOPageCandidates for MIO-existence
        mioPages = analyzeMIOPageCandidates(mioPageCandidates, entity);
        
//        System.out.println("Analyzing MIOPAGECANDIDATES finished in: "+ DateHelper.getRuntime(timeStamp2));
        LOGGER.info("MIOPageCandidateAnalysis finished, DedicatedPage-Calculation starts..for " + entity.getName());
      
//        final long timeStamp3 = System.currentTimeMillis();
        // detect DedicatedPages
        final DedicatedPageDetector dpDetector = new DedicatedPageDetector();
        for (MIOPage mioPage : mioPages) {            
            dpDetector.calculateDedicatedPageTrust(mioPage);
            // System.out.println(entity.getName() + "  " + mioPage.getUrl());
        }
//        System.out.println("DedicatedPageDetection finished in: "+ DateHelper.getRuntime(timeStamp3));
        LOGGER.info("DedicatedPage-Calculation finished..for " + entity.getName());

        return mioPages;
    }

    /**
     * Generate specific SearchQueries for every entity.
     *
     * @param entity the entity
     * @return the list
     */
    private List<String> generateSearchQueries(final Entity entity, boolean weakFlag) {
        final MIOQueryFactory searchQueryFac = new MIOQueryFactory(entity, weakFlag);
        final List<String> searchQueries = searchQueryFac.generateSearchQueries();

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
