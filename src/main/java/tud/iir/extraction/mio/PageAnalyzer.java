/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tud.iir.knowledge.Entity;

/**
 * The PageAnalyzer analyzes MIOPageCandidates for MIO-Existence. Also some links and IFRAMEs are analyzed.
 * 
 * @author Martin Werner
 */
public class PageAnalyzer {

    private List<String> mioPageCandidates;
    private List<MIOPage> mioPages;

    /**
     * Instantiates a new page analyzer.
     * 
     * @param MIOPageCandidates the mIO page candidates
     */
    public PageAnalyzer(List<String> MIOPageCandidates) {

        this.mioPageCandidates = MIOPageCandidates;
        mioPages = new ArrayList<MIOPage>();

    }

    /**
     * the central method.
     * 
     * @param entity the entity
     * @return the list
     */
    public List<MIOPage> analyzePages(Entity entity) {

        // initialize SearchWordMatcher
        final SearchWordMatcher swMatcher = new SearchWordMatcher(entity.getName());

        for (String mioPageCandidate : mioPageCandidates) {

            // System.out.println("PageAnalysing started for: " +
            // mioPageCandidate);
            String pageContent = getPage(mioPageCandidate);
            if (!("").equals(pageContent)) {

                // use fast MIO-Detection
                final FastMIODetector fMIODec = new FastMIODetector();
                mioPages.addAll(fMIODec.getMioPages(pageContent, mioPageCandidate));

                // IFRAME-Analysis
                IFrameAnalyzer iframeAnalyzer = new IFrameAnalyzer(swMatcher);
                mioPages.addAll(iframeAnalyzer.getIframeMioPages(pageContent, mioPageCandidate));

                // Link-Analysis
                LinkAnalyzer linkAnalyzer = new LinkAnalyzer(swMatcher);
                mioPages.addAll(linkAnalyzer.getLinkedMioPages(pageContent, mioPageCandidate));
            }
        }

        return removeDuplicates(mioPages);
    }

    /**
     * get WebPage as String.
     * 
     * @param URLString the uRL string
     * @return the page
     */
    private String getPage(String URLString) {
        GeneralAnalyzer generalAnalyzer = new GeneralAnalyzer();
        return generalAnalyzer.getPage(URLString, false);
    }

    /**
     * remove duplicates, but pay attention to different ways of finding.
     * 
     * @param mioPages the mio pages
     * @return the list
     */
    private List<MIOPage> removeDuplicates(List<MIOPage> mioPages) {
        List<MIOPage> resultList = new ArrayList<MIOPage>();

        Map<String, MIOPage> tempMap = new HashMap<String, MIOPage>();
        for (MIOPage mioPage : mioPages) {

            if (tempMap.containsKey(mioPage.getUrl())) {

                MIOPage tempMIOPage = tempMap.get(mioPage.getUrl());

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

                if (mioPage.isIFrameSource()) {
                    if (!tempMIOPage.isIFrameSource()) {
                        tempMIOPage.setIFrameSource(mioPage.isIFrameSource());
                        tempMIOPage.setIframeParentPage(mioPage.getIframeParentPage());
                    }
                }

            } else {
                tempMap.put(mioPage.getUrl(), mioPage);
            }
        }
        for (MIOPage mioPage : tempMap.values()) {
            resultList.add(mioPage);

        }

        return resultList;

    }

    public static void main(String[] args) {
        // List<String> testList = new ArrayList<String>();
        // testList.add("http://www.canon.co.uk/for_home/product_finder/multifunctionals/inkjet/pixma_mp990/index.aspx");
        // PageAnalyzer pageAnalyzer = new PageAnalyzer(testList);
        // // start and get Results of PageAnalyzing
        // Concept pconcept = new Concept("printer");
        // Entity printer = new Entity("canon mp990", pconcept);
        // List<MIOPage> MIOPages = pageAnalyzer.analyzePages(printer);
        //
        // for (MIOPage mioPage : MIOPages) {
        // System.out.println(mioPage.getUrl() + "  linkName: " + mioPage.getLinkName());
        // }
    }
}
