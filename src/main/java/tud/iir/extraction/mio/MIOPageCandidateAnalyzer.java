/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import tud.iir.knowledge.Entity;
import tud.iir.web.Crawler;

/**
 * The PageAnalyzer analyzes MIOPageCandidates for MIO-Existence. Also some links and IFRAMEs are analyzed.
 * 
 * @author Martin Werner
 */
public class MIOPageCandidateAnalyzer {

    /** The MIOPageCandidates. */
    private final transient List<String> mioPageCandidates;

    /** The MIOPages. */
    private final transient List<MIOPage> mioPages;

    /**
     * Instantiates a new PageAnalyzer.
     * 
     * @param mioPageCandidates the mIO page candidates
     */
    public MIOPageCandidateAnalyzer(final List<String> mioPageCandidates) {

        this.mioPageCandidates = mioPageCandidates;
        mioPages = new ArrayList<MIOPage>();

    }

    /**
     * This central method identifies entity-relevant MIOPages.
     * 
     * @param entity the entity
     * @return the identified mioPages as list
     */
    public final List<MIOPage> identifyMIOPages(final Entity entity) {

        final Crawler craw = new Crawler();
        // initialize SearchWordMatcher
        final SearchWordMatcher swMatcher = new SearchWordMatcher(entity.getName());
        final FastMIODetector fMIODec = new FastMIODetector();
        final IFrameAnalyzer iframeAnalyzer = new IFrameAnalyzer(swMatcher);
        final LinkAnalyzer linkAnalyzer = new LinkAnalyzer(swMatcher);

        for (String mioPageCandidate : mioPageCandidates) {

            final Document webDocument = craw.getWebDocument(mioPageCandidate);
            final String pageContent = Crawler.documentToString(webDocument);

            if (("").equals(pageContent)) {
                continue;
            }

            if (fMIODec.containsMIO(pageContent)) {
                final MIOPage mioPage = new MIOPage(mioPageCandidate, webDocument);
                mioPages.add(mioPage);
            }

            // IFRAME-Analysis
            mioPages.addAll(iframeAnalyzer.getIframeMioPages(pageContent, mioPageCandidate));

            // Link-Analysis
            mioPages.addAll(linkAnalyzer.getLinkedMioPages(pageContent, mioPageCandidate));

        }

        return removeDuplicates(mioPages);
    }

    /**
     * check if a MIO-Indicator is contained.
     * 
     * @param mioPages the mio pages
     * @return true, if successful
     */
    // private boolean containsMIOIndicator(final String mioPageContent) {
    // String modPageContent = mioPageContent.toLowerCase(Locale.ENGLISH);
    // boolean returnValue = false;
    // for (String mioInd : mioIndicators) {
    // if (modPageContent.contains(mioInd)) {
    // // break after a first indicator was detected
    // returnValue = true;
    //
    // }
    // }
    //
    // return returnValue;
    //
    // }

    /**
     * Remove duplicates, but pay attention to the different ways of retrieving a MIO.
     * 
     * @param mioPages the mioPages
     * @return the list without duplicates
     */
    private List<MIOPage> removeDuplicates(final List<MIOPage> mioPages) {

        final List<MIOPage> resultList = new ArrayList<MIOPage>();

        final Map<String, MIOPage> tempMap = new HashMap<String, MIOPage>();
        for (MIOPage mioPage : mioPages) {

            if (tempMap.containsKey(mioPage.getUrl())) {

                final MIOPage tempMIOPage = tempMap.get(mioPage.getUrl());

                // organize the different ways of finding the same miopage
                if (mioPage.isLinkedPage()) {
                    if (tempMIOPage.getLinkName().equals("")) {
                        tempMIOPage.setLinkName(mioPage.getLinkName());
                    }
                    if (tempMIOPage.getLinkTitle().equals("")) {
                        tempMIOPage.setLinkTitle(mioPage.getLinkTitle());
                    }
                    if (!tempMIOPage.isLinkedPage()) {
                        tempMIOPage.setLinkedPage(mioPage.isLinkedPage());
                        tempMIOPage.setLinkParentPage(mioPage.getLinkParentPage());
                    }
                }

                if (mioPage.isIFrameSource() && !tempMIOPage.isIFrameSource()) {
                    tempMIOPage.setIFrameSource(mioPage.isIFrameSource());
                    tempMIOPage.setIframeParentPage(mioPage.getIframeParentPage());
                }

            } else {
                tempMap.put(mioPage.getUrl(), mioPage);
            }
        }
        for (MIOPage mioPage : tempMap.values()) {
            resultList.add(mioPage);

        }
        // System.out.println("Anzahl MIOPAGES nach DublicateRemoiving: " + resultList.size());
        return resultList;

    }

//    /**
//     * The main method.
//     * 
//     * @param args the arguments
//     */
//    public static void main(String[] args) {
//        // List<String> testList = new ArrayList<String>();
//        // testList.add("http://www.canon.co.uk/for_home/product_finder/multifunctionals/inkjet/pixma_mp990/index.aspx");
//        // PageAnalyzer pageAnalyzer = new PageAnalyzer(testList);
//        // // start and get Results of PageAnalyzing
//        // Concept pconcept = new Concept("printer");
//        // Entity printer = new Entity("canon mp990", pconcept);
//        // List<MIOPage> MIOPages = pageAnalyzer.analyzePages(printer);
//        //
//        // for (MIOPage mioPage : MIOPages) {
//        // System.out.println(mioPage.getUrl() + "  linkName: " + mioPage.getLinkName());
//        // }
//    }
}
