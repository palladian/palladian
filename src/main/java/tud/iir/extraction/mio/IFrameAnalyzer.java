package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tud.iir.web.Crawler;

/**
 * The IFrameAnalyzer analyzes a webPage for existing IFrames and checks if their targets contains MIOs.
 * 
 * @author Martin Werner
 */
public class IFrameAnalyzer extends GeneralAnalyzer {

    private SearchWordMatcher swMatcher;

    /**
     * Instantiates a new i frame analyzer.
     *
     * @param swMatcher the sw matcher
     */
    public IFrameAnalyzer(SearchWordMatcher swMatcher) {
        this.swMatcher = swMatcher;
    }

    /**
     * Gets the iframe mio pages.
     *
     * @param parentPageContent the parent page content
     * @param parentPageURL the parent page url
     * @return the iframe mio pages
     */
    public List<MIOPage> getIframeMioPages(String parentPageContent, String parentPageURL) {
        List<MIOPage> mioPages = new ArrayList<MIOPage>();

        List<String> iframeSources = analyzeForIframe(parentPageContent, parentPageURL);

        if (!iframeSources.isEmpty()) {
            for (String iframeSourceURL : iframeSources) {
                // only analyze if the url contains hints for the entity
                if (swMatcher.containsSearchWordOrMorphs(iframeSourceURL)) {
                    String iframePageContent = getPage(iframeSourceURL, false);
                    // check for MIOs
                    if (!iframePageContent.equals("")) {
                        FastMIODetector mioDetector = new FastMIODetector();
                        if (mioDetector.containsMIO(iframePageContent)) {

                            MIOPage iframeMIOPage = generateMIOPage(iframeSourceURL, iframePageContent, parentPageURL);
                            mioPages.add(iframeMIOPage);
                        }
                    }
                }
            }
        }

        return mioPages;
    }

    /**
     * analyze a page for existing IFrames.
     *
     * @param pageContent the page content
     * @param pageURL the page url
     * @return the list
     */
    private List<String> analyzeForIframe(String pageContent, String pageURL) {
        List<String> iframeURLs;
        List<String> iframeURLCandidates = new ArrayList<String>();
        if (pageContent.contains("<iframe") || pageContent.contains("<IFRAME")) {

            Pattern p = Pattern.compile("<iframe[^>]*src=[^>]*>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(pageContent);
            while (m.find()) {
                // first get the complete iframe-tag Result:
                // <iframe src="http://..."
                String iframeTag = m.group(0);
                // extract the source; keep attention to ' vs " Result:
                // http://...
                iframeURLCandidates.addAll(getSrcFromIframe(iframeTag, "\""));
                iframeURLCandidates.addAll(getSrcFromIframe(iframeTag, "'"));

            }
        }
        iframeURLs = checkURLs(iframeURLCandidates, pageURL);
        return iframeURLs;
    }

    /**
     * extract the src-url out of an iframe-tag.
     *
     * @param iframeTag the iframe tag
     * @param quotMark the quot mark
     * @return the src from iframe
     */
    private List<String> getSrcFromIframe(String iframeTag, String quotMark) {
        List<String> iframeURLs = new ArrayList<String>();
        String pattern = "src=" + quotMark + "[^>" + quotMark + "]*" + quotMark + "";
        String iframeSrc = extractElement(pattern, iframeTag, "src=");
        iframeURLs.add(iframeSrc);

        return iframeURLs;
    }

    /**
     * analyze the URLs for validness and eventually modify them e.g. relative Paths
     *
     * @param urlCandidates the url candidates
     * @param parentPageURL the parent page url
     * @return the list
     */
    private List<String> checkURLs(List<String> urlCandidates, String parentPageURL) {
        List<String> validURLs = new ArrayList<String>();

        for (String urlCandidate : urlCandidates) {
            String validURL = verifyURL(urlCandidate, parentPageURL);
            if (!validURL.equals("")) {
                validURLs.add(validURL);
            }
        }

        return validURLs;
    }

    /**
     * Create a new MIOPage.
     *
     * @param iframeSourceURL the iframe source url
     * @param iframePageContent the iframe page content
     * @param parentPageURL the parent page url
     * @return the mIO page
     */
    private MIOPage generateMIOPage(String iframeSourceURL, String iframePageContent, String parentPageURL) {
        MIOPage mioPage = new MIOPage(iframeSourceURL, iframePageContent);
        mioPage.setIFrameSource(true);
        mioPage.setIframeParentPage(parentPageURL);

        Crawler crawler = new Crawler(5000, 5000, 10000);
        crawler.setDocument(parentPageURL);
        mioPage.setIframeParentPageTitle(Crawler.extractTitle(crawler.getDocument()));
        return mioPage;

    }

    /**
     * The main method.
     *
     * @param abc the arguments
     */
    public static void main(String[] abc) {

        SearchWordMatcher swMatcher = new SearchWordMatcher("Sennheiser HD800");
        IFrameAnalyzer iframeAnalyzer = new IFrameAnalyzer(swMatcher);

        String pageContent = iframeAnalyzer
                .getPage("http://www.sennheiser.com/sennheiser/home_de.nsf/root/private_headphones_audiophile-headphones_500319");
        List<MIOPage> mioPages = iframeAnalyzer
                .getIframeMioPages(pageContent,
                        "http://www.sennheiser.com/sennheiser/home_de.nsf/root/private_headphones_audiophile-headphones_500319");
        for (MIOPage mioPage : mioPages) {
            System.out.println(mioPage.getUrl() + " is IFRAMESOURCE: " + mioPage.isIFrameSource());
        }
    }

}
