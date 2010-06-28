/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The LinkAnalyzer checks if some of the Links of MIOPageCandidates have targets with MIOs (simulates an indirect
 * search)
 * 
 * @author Martin Werner
 */
public class LinkAnalyzer extends GeneralAnalyzer {

    private SearchWordMatcher swMatcher;

    /**
     * Instantiates a new link analyzer.
     * 
     * @param swMatcher the sw matcher
     */
    public LinkAnalyzer(SearchWordMatcher swMatcher) {
        super();
        this.swMatcher = swMatcher;
    }

    /**
     * Gets the linked mio pages.
     * 
     * @param parentPageContent the parent page content
     * @param parentPageURL the parent page url
     * @return the linked mio pages
     */
    public List<MIOPage> getLinkedMioPages(String parentPageContent, String parentPageURL) {
        List<MIOPage> mioPages = new ArrayList<MIOPage>();

        // find all <a>-tags
        // Pattern p = Pattern.compile("<a[^>]*href=\\\"?[^(>|)]*\\\"?[^>]*>[^<]*</a>", Pattern.CASE_INSENSITIVE);
        Pattern p = Pattern.compile("<a[^>]*>.*</a>", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(parentPageContent);
        while (m.find()) {
            String completeLinkTag = m.group(0);

            String linkURL = getLinkURL(completeLinkTag, parentPageURL);
            if (!("").equals(linkURL)) {

                // extract the Text of a Link
                String linkName = getLinkName(completeLinkTag);

                // extract the value of the title-attribute of a link
                String linkTitle = getLinkTitle(completeLinkTag);

                // check if the linkURL or linkInfo or linkTitle contains entity
                // relevant words
                if (isRelevantLinkCheck(linkURL, linkName, linkTitle)) {
                    String linkedPageContent = getPage(linkURL, false);
                    if (!("").equals(linkedPageContent)) {
                        FastMIODetector mioDetector = new FastMIODetector();
                        if (mioDetector.containsMIO(linkedPageContent)) {
                            MIOPage mioPage = generateMIOPage(linkURL, parentPageURL, linkName, linkTitle,
                                    linkedPageContent);
                            mioPages.add(mioPage);
                        }
                    }

                }
            }

        }

        return mioPages;
    }

    /**
     * Gets the link url.
     * 
     * @param linkTag the link tag
     * @param pageURL the page url
     * @return the link url
     */
    private String getLinkURL(String linkTag, String pageURL) {
        String extractedLink = extractElement("href=\"[^>#\"]*\"", linkTag, "href=");
        if (extractedLink.length() <= 3) {
            extractedLink = extractElement("=\\\"?'?http[^>#\\\"']*\\\"?'?", linkTag, "=");
        }
        return verifyURL(extractedLink, pageURL);

    }

    /**
     * Gets the link name.
     * 
     * @param linkTag the link tag
     * @return the link name
     */
    private String getLinkName(String linkTag) {
        String result = extractElement(">[^<]*<", linkTag, "");
        result = result.replaceAll("[<>]", "");
        return result;
    }

    /**
     * Gets the link title.
     * 
     * @param linkTag the link tag
     * @return the link title
     */
    private String getLinkTitle(String linkTag) {
        return extractElement("title=\"[^>\"]*\"", linkTag, "title=");
    }

    /**
     * Check if the linkURL or linkInfo or linkTitle contains entity relevant words.
     * 
     * @param linkURL the link url
     * @param linkName the link name
     * @param linkTitle the link title
     * @return true, if is relevant link check
     */
    private boolean isRelevantLinkCheck(String linkURL, String linkName, String linkTitle) {

        if (swMatcher.containsSearchWordOrMorphs(linkURL) || swMatcher.containsSearchWordOrMorphs(linkName)
                || swMatcher.containsSearchWordOrMorphs(linkTitle)) {
            return true;
        }

        // a string is relevant if a minimum of 2words (>1) of the given
        // SearchWord or morpheme is contained
        // if (swMatcher.getNumberOfSearchWordMatches(linkURL) > 1
        // || swMatcher.getNumberOfSearchWordMatches(linkName) > 1
        // || swMatcher.getNumberOfSearchWordMatches(linkTitle) > 1) {
        // return true;
        // }
        return false;
    }

    /**
     * Create a MIOPage.
     * 
     * @param linkURL the link url
     * @param parentURL the parent url
     * @param linkName the link name
     * @param linkTitle the link title
     * @param pageContent the page content
     * @return the mIO page
     */
    private MIOPage generateMIOPage(String linkURL, String parentURL, String linkName, String linkTitle,
            String pageContent) {

        MIOPage mioPage = new MIOPage(linkURL, pageContent);
        mioPage.setLinkParentPage(parentURL);
        mioPage.setLinkName(linkName);
        mioPage.setLinkTitle(linkTitle);
        mioPage.setLinkedPage(true);

        return mioPage;
    }

}
