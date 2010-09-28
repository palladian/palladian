/**
 * The PageAnalyzer analyzes MIOPageCandidates for MIO-Existence. Also some links and IFRAMEs are analyzed.
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import tud.iir.knowledge.Entity;
import tud.iir.web.Crawler;
import tud.iir.web.URLDownloader;
import tud.iir.web.URLDownloader.URLDownloaderCallback;

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

        // initialize SearchWordMatcher
        final SearchWordMatcher swMatcher = new SearchWordMatcher(entity.getName());
        final FastMIODetector fMIODec = new FastMIODetector();
        final IFrameAnalyzer iframeAnalyzer = new IFrameAnalyzer(swMatcher);
        final LinkAnalyzer linkAnalyzer = new LinkAnalyzer(swMatcher, entity.getConcept());

        URLDownloader downloader = new URLDownloader();
        // set how many threads the urlDownloader can use (see configuration-file)
        downloader.setMaxThreads(InCoFiConfiguration.getInstance().urlDownloaderThreads);
        // add pages to downloader
        for (String mioPageCandidate : mioPageCandidates) {
            downloader.add(mioPageCandidate);
        }

        downloader.start(new URLDownloaderCallback() {
            @Override
            public void finished(String url, InputStream inputStream) {
                final Document webDocument = Crawler.getWebDocumentFromInputStream(inputStream, url);
                final String pageContent = Crawler.documentToString(webDocument);
                if (!("").equals(pageContent)) {

                    if (fMIODec.containsMIO(pageContent)) {
                        final MIOPage mioPage = new MIOPage(url, webDocument);
                        mioPages.add(mioPage);
                    }

                    // IFRAME-Analysis
                    mioPages.addAll(iframeAnalyzer.getIframeMioPages(pageContent, url));

                    // Link-Analysis
                    mioPages.addAll(linkAnalyzer.getLinkedMioPages(pageContent, url));
                }
            }
        });

        return removeDuplicates(mioPages);
    }

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
        
        return resultList;
    }

    // /**
    // * The main method.
    // *
    // * @param args the arguments
    // */
    // public static void main(String[] args) {
    // // List<String> testList = new ArrayList<String>();
    // // testList.add("http://www.canon.co.uk/for_home/product_finder/multifunctionals/inkjet/pixma_mp990/index.aspx");
    // // PageAnalyzer pageAnalyzer = new PageAnalyzer(testList);
    // // // start and get Results of PageAnalyzing
    // // Concept pconcept = new Concept("printer");
    // // Entity printer = new Entity("canon mp990", pconcept);
    // // List<MIOPage> MIOPages = pageAnalyzer.analyzePages(printer);
    // //
    // // for (MIOPage mioPage : MIOPages) {
    // // System.out.println(mioPage.getUrl() + "  linkName: " + mioPage.getLinkName());
    // // }
    // }
}
