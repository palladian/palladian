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
    private List<RolePage> rolePageList;

    /** The instance of MIOPageRetriever. */
    private static MIOPageRetriever instance = null;

    /** The resultCount determines how many sources (URLs) should be retrieved */
    private final static int resultCount = 20;

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
    public List<MIOPage> retrieveMIOs(Entity entity, ConceptSearchVocabulary searchVoc) {

        List<MIOPage> mioPages;

        // RolePageDetector rolePageDet = new RolePageDetector(2);

        // generate searchQueries
        List<String> searchQueries = generateSearchQueries(entity, searchVoc);

        // initiate search with searchEngines
        List<String> mioPageCandidates = startSearchAgent(searchQueries);

        LOGGER.info("Analyzing MIOPageCandidates startet..");

        // analyze the MIOPageCandidates for MIO-existence
        mioPages = analyzeMIOPageCandidates(mioPageCandidates, entity);

        LOGGER.info("PageAnalysis finished, DedicatedPage-Calculation starts..");

        // detect DedicatedPages
        for (MIOPage mioPage : mioPages) {
            DedicatedPageDetector dpDetector = new DedicatedPageDetector();
            dpDetector.calculateDedicatedPageTrust(mioPage);
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
    private List<String> generateSearchQueries(Entity entity, ConceptSearchVocabulary conceptVocabulary) {
        MIOQueryFactory searchQueryFac = new MIOQueryFactory();
        List<String> searchQueries = searchQueryFac.generateSearchQueries(entity.getName(), entity.getConcept(),
                rolePageList, conceptVocabulary);

        return searchQueries;
    }

    /**
     * Initiate a search with generated queries for every entity.
     * 
     * @param searchQueries the search queries
     * @return the list
     */
    private List<String> startSearchAgent(List<String> searchQueries) {
        SearchAgent searchAgent = new SearchAgent(resultCount);
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
        PageAnalyzer pageAnalyzer = new PageAnalyzer(mioPageCandidates);
        // start and get Results of PageAnalyzing
        List<MIOPage> mioPages = pageAnalyzer.analyzePages(entity);

        return mioPages;
    }

    /**
     * Prints the mioPages-URLs
     * 
     * @param MIOPages the mioPages
     * @param entityName the entity name
     */
    // private void printMIOPagesURLs(List<MIOPage> MIOPages, String entityName) {
    // System.out.println("-------fuer " + entityName + " wurden " + MIOPages.size() + " MIOPages gefunden!");
    // for (MIOPage mioPage : MIOPages) {
    // if (mioPage.getDedicatedPageTrust() > 0.6) {
    // System.out.println(mioPage.getHostname() + "  " + entityName + " dpTrust: "
    // + mioPage.getDedicatedPageTrust() + " isIframeSource: " + mioPage.isIFrameSource()
    // + " isLinkedPage: " + mioPage.isLinkedPage());
    // System.out.println(mioPage.getUrl());
    // }
    //
    // }
    // }

    /**
     * Prints the role page ur ls.
     * 
     * @param rolePages the role pages
     */
    // private void printRolePageURLs(List<RolePage> rolePages) {
    // System.out.println("Size: " + rolePages.size());
    // for (RolePage rolePage : rolePages) {
    // System.out.println(rolePage.getHostname() + " Count: " + rolePage.getCount());
    // }
    // }

}
