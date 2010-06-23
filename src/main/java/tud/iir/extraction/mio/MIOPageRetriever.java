package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.knowledge.Entity;

/**
 * The MIOPageRetriever finds pages from the web that have a relative high probability of containing relevant MIO(s) for
 * a given entity
 * 
 * @author Martin Werner
 */
public class MIOPageRetriever {

    private static final Logger logger = Logger.getLogger(MIOPageRetriever.class);

    private List<RolePage> rolePageList;
    private static MIOPageRetriever instance = null;

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
     * Retrieve mi os.
     *
     * @param entity the entity
     * @param searchVoc the search voc
     * @return the list
     */
    public List<MIOPage> retrieveMIOs(Entity entity, ConceptSearchVocabulary searchVoc) {

        List<MIOPage> MIOPages;

        // RolePageDetector rolePageDet = new RolePageDetector(2);

        // generate searchQueries
        List<String> searchQueries = generateSearchQueries(entity, searchVoc);

        // initiate search with searchEngines
        List<String> MIOPageCandidates = startSearchAgent(searchQueries);

        logger.info("Analyzing MIOPageCandidates startet..");

        // analyze the MIOPageCandidates for MIO-existence
        MIOPages = analyzeMIOPageCandidates(MIOPageCandidates, entity);

        logger.info("PageAnalysis finished, DedicatedPage-Calculation starts..");

        // detect DedicatedPages
        for (MIOPage mioPage : MIOPages) {
            DedicatedPageDetector dpDetector = new DedicatedPageDetector();
            dpDetector.calculateDedicatedPageTrust(mioPage);
        }
        logger.info("DedicatedPage-Calculation finished");

        // printMIOPagesURLs(MIOPages, entity.getName());
        //			
        // //check for Role-Pages
        // rolePageList = rolePageDet.analyzeForRolePages(MIOPages);
        // }
        // printRolePageURLs(rolePageList);
        return MIOPages;
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
        SearchAgent searchAgent = new SearchAgent(20);
        List<String> MIOPageCandidates = searchAgent.initiateSearch(searchQueries);

        return MIOPageCandidates;
    }

    /**
     * Do the Webpage-Analysis.
     *
     * @param MIOPageCandidates the mIO page candidates
     * @param entity the entity
     * @return the list
     */
    private List<MIOPage> analyzeMIOPageCandidates(List<String> MIOPageCandidates, Entity entity) {
        PageAnalyzer pageAnalyzer = new PageAnalyzer(MIOPageCandidates);
        // start and get Results of PageAnalyzing
        List<MIOPage> MIOPages = pageAnalyzer.analyzePages(entity);

        return MIOPages;
    }

    /**
     * Prints the mio pages ur ls.
     *
     * @param MIOPages the mIO pages
     * @param entityName the entity name
     */
    private void printMIOPagesURLs(List<MIOPage> MIOPages, String entityName) {
        System.out.println("-------fuer " + entityName + " wurden " + MIOPages.size() + " MIOPages gefunden!");
        for (MIOPage mioPage : MIOPages) {
            if (mioPage.getDedicatedPageTrust() > 0.6) {
                System.out.println(mioPage.getHostname() + "  " + entityName + " dpTrust: "
                        + mioPage.getDedicatedPageTrust() + " isIframeSource: " + mioPage.isIFrameSource()
                        + " isLinkedPage: " + mioPage.isLinkedPage());
                System.out.println(mioPage.getUrl());
            }

        }
    }

    /**
     * Prints the role page ur ls.
     *
     * @param rolePages the role pages
     */
    private void printRolePageURLs(List<RolePage> rolePages) {
        System.out.println("Size: " + rolePages.size());
        for (RolePage rolePage : rolePages) {
            System.out.println(rolePage.getHostname() + " Count: " + rolePage.getCount());
        }
    }

}
