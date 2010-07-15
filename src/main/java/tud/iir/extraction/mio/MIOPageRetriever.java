/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
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

    /** The rolePage-List. */
    final private List<RolePage> rolePageList;

    /** The instance of MIOPageRetriever. */
    private static MIOPageRetriever instance = null;

    /** The resultCount determines how many sources (URLs) should be retrieved */
    private final static int RESULTCOUNT = 30;

    /**
     * Instantiates a new mIO page retriever.
     */
    public MIOPageRetriever() {
        rolePageList = new ArrayList<RolePage>();
    }

    /**
     * Gets the single instance of MIOPageRetriever.
     * 
     * @return single instance of MIOPageRetriever
     */
    public static MIOPageRetriever getInstance() {
        if (instance == null) {
            instance = new MIOPageRetriever();
        }
        return instance;
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

        // RolePageDetector rolePageDet = new RolePageDetector(2);

        // generate searchQueries
        final List<String> searchQueries = generateSearchQueries(entity, searchVoc);

        // initiate search with searchEngines
        final List<String> mioPageCandidates = startSearchAgent(searchQueries);

        LOGGER.info("Analyzing MIOPageCandidates startet..");

        // analyze the MIOPageCandidates for MIO-existence
        mioPages = analyzeMIOPageCandidates(mioPageCandidates, entity);

        LOGGER.info("MIOPageCandidateAnalysis finished, DedicatedPage-Calculation starts..");

        // detect DedicatedPages
        for (MIOPage mioPage : mioPages) {
            final DedicatedPageDetector dpDetector = new DedicatedPageDetector();
            dpDetector.calculateDedicatedPageTrust(mioPage);
            // System.out.println(entity.getName() + "  " + mioPage.getUrl());
        }
        LOGGER.info("DedicatedPage-Calculation finished");

        // printMIOPagesURLs(MIOPages, entity.getName());
        //
        // //check for Role-Pages
        // rolePageList = rolePageDet.analyzeForRolePages(MIOPages);
        // }
        // printRolePageURLs(rolePageList);
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
        final MIOQueryFactory searchQueryFac = new MIOQueryFactory();
        final List<String> searchQueries = searchQueryFac.generateSearchQueries(entity.getName(), entity.getConcept(),
                rolePageList, conceptVocabulary);

        return searchQueries;
    }

    /**
     * Initiate a search with generated queries for every entity.
     * 
     * @param searchQueries the search queries
     * @return the list
     */
    private List<String> startSearchAgent(final List<String> searchQueries) {
        final SearchAgent searchAgent = new SearchAgent(RESULTCOUNT);
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
