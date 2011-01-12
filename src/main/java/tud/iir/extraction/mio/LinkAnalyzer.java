/**
 * The LinkAnalyzer checks if some of the Links of MIOPageCandidates have targets with relevant MIOs (simulates an
 * indirect
 * search)
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;

import tud.iir.helper.HTMLHelper;
import tud.iir.knowledge.Concept;
import tud.iir.web.Crawler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

public class LinkAnalyzer {

    /** The SearchWordMatcher. */
    private final transient SearchWordMatcher swMatcher;

    /** The relevant vocabulary. */
    private transient List<String> relevantVocabulary = null;

    private final transient boolean limitLinkAnalyzing;

    /**
     * Instantiates a new LinkAnalyzer.
     * 
     * @param swMatcher the SearchWordMatcher
     * @param concept the concept
     */
    public LinkAnalyzer(final SearchWordMatcher swMatcher, final Concept concept) {

        this.swMatcher = swMatcher;
        this.limitLinkAnalyzing = InCoFiConfiguration.getInstance().limitLinkAnalyzing;
        if (limitLinkAnalyzing) {
            this.relevantVocabulary = prepareRelevantVoc(concept.getName());
        }
    }

    /**
     * Gets the linked MIOpages.
     * 
     * @param parentPageContent the parent page content
     * @param parentPageURL the parent pageURL
     * @return the linked MIOPages
     */
    public List<MIOPage> getLinkedMioPages(final String parentPageContent, final String parentPageURL) {
        final List<String> linkTags = new ArrayList<String>();

        // find all <a>-tags
        // Pattern p = Pattern.compile("<a[^>]*href=\\\"?[^(>|)]*\\\"?[^>]*>[^<]*</a>", Pattern.CASE_INSENSITIVE);
        Pattern pattern = Pattern.compile("<a[^>]*>.*</a>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(parentPageContent);
        while (matcher.find()) {
            final String completeLinkTag = matcher.group(0);

            if (!"".equals(completeLinkTag) && completeLinkTag != null) {
                linkTags.add(completeLinkTag);
            }
        }
        return analyzeLinkTags(linkTags, parentPageURL);
    }

    /**
     * Analyze link tags, extract all relevant information.
     * 
     * @param linkTags the link tags
     * @param parentPageURL the parent page URL
     * @return the list
     */
    private List<MIOPage> analyzeLinkTags(final List<String> linkTags, final String parentPageURL) {

        final List<MIOPage> mioPages = new ArrayList<MIOPage>();
        final FastMIODetector mioDetector = new FastMIODetector();
        final Crawler craw = new Crawler(4000, 5000, 5000);

        // linkURLs save the URLs that were already found
        final List<String> linkURLs = new ArrayList<String>();
        final JaroWinkler jaroWinkler = new JaroWinkler();

        for (String linkTag : linkTags) {
            boolean isExisting = false;

            final String linkURL = getLinkURL(linkTag, parentPageURL);

            if (linkURL.length() < 5) {
                continue;
            }

            if (!linkURLs.isEmpty()) {

                for (String checkURL : linkURLs) {
                    final double similarity = jaroWinkler.getSimilarity(checkURL, linkURL);
                    if (similarity > 0.99) {
                        isExisting = true;
                        break;
                    }
                }
            }

            if (!isExisting) {

                // extract the Text of a Link
                final String linkName = getLinkName(linkTag);

                // extract the value of the title-attribute of a link
                final String linkTitle = getLinkTitle(linkTag);

                // check if the linkURL or linkInfo or linkTitle contains entity
                // relevant words
                if (isRelevantLinkCheck(linkURL, linkName, linkTitle)
                        && hasConceptRelevance(linkURL, linkName, linkTitle)) {

                    linkURLs.add(linkURL);

                    // System.out.println("download: " + linkURL);
                    final Document webDocument = craw.getWebDocument(linkURL);
                    final String linkedPageContent = Crawler.documentToString(webDocument);

                    if (linkedPageContent != null && linkedPageContent.length() > 5
                            && mioDetector.containsMIO(linkedPageContent)) {

                        final MIOPage mioPage = generateMIOPage(linkURL, webDocument, parentPageURL, linkName,
                                linkTitle);
                        mioPages.add(mioPage);

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
    private String getLinkURL(final String linkTag, final String pageURL) {
        String extractedLink = HTMLHelper.extractTagElement("href=\"[^>#\"]*\"", linkTag, "href=");
        if (extractedLink.length() <= 3) {
            extractedLink = HTMLHelper.extractTagElement("=\\\"?'?http[^>#\\\"']*\\\"?'?", linkTag, "=");
        }
        return Crawler.verifyURL(extractedLink, pageURL);
    }

    /**
     * Gets the link name.
     * 
     * @param linkTag the link tag
     * @return the link name
     */
    private String getLinkName(final String linkTag) {
        String result = HTMLHelper.extractTagElement(">[^<]*<", linkTag, "");
        result = result.replaceAll("[<>]", "");
        return result;
    }

    /**
     * Gets the link title.
     * 
     * @param linkTag the link tag
     * @return the link title
     */
    private String getLinkTitle(final String linkTag) {
        return HTMLHelper.extractTagElement("title=\"[^>\"]*\"", linkTag, "title=");
    }

    /**
     * Check if the linkURL or linkInfo or linkTitle contains entity relevant words.
     * 
     * @param linkURL the link url
     * @param linkName the link name
     * @param linkTitle the link title
     * @return true, if is relevant link check
     */
    private boolean isRelevantLinkCheck(final String linkURL, final String linkName, final String linkTitle) {
        boolean returnValue = false;
        if (swMatcher.containsSearchWordOrMorphs(linkURL) || swMatcher.containsSearchWordOrMorphs(linkName)
                || swMatcher.containsSearchWordOrMorphs(linkTitle)) {
            returnValue = true;
        }
        return returnValue;
    }

    /**
     * Checks for concept relevance.
     * 
     * @param linkURL the link url
     * @param linkName the link name
     * @param linkTitle the link title
     * @return true, if successful
     */
    private boolean hasConceptRelevance(final String linkURL, final String linkName, final String linkTitle) {
        boolean returnValue = false;
        if (limitLinkAnalyzing) {

            for (String keyword : relevantVocabulary) {
               
                if (linkURL.toLowerCase(Locale.ENGLISH).contains(keyword)
                        || linkName.toLowerCase(Locale.ENGLISH).contains(keyword)
                        || linkTitle.toLowerCase(Locale.ENGLISH).contains(keyword)) {
                    returnValue = true;
                    break;
                }
            }
        } else {
            returnValue = true;
        }
        return returnValue;
    }

    /**
     * Prepare a list of relevant vocabulary for Link-Relevance-Analyzing.
     * 
     * @param conceptName the concept name
     * @return the list of relevant vocabulary
     */
    private List<String> prepareRelevantVoc(String conceptName) {
        List<String> relevantVoc = new ArrayList<String>();
        List<String> conceptVoc = InCoFiConfiguration.getInstance().getVocByConceptName(conceptName);
        conceptVoc.addAll(InCoFiConfiguration.getInstance().getVocByConceptName("weakMIOs"));

        for (String keyword : conceptVoc) {
            relevantVoc.add(keyword);
        }

        // remove duplicates
        Set<String> hashSet = new HashSet<String>(relevantVoc);
        relevantVoc.clear();
        relevantVoc.addAll(hashSet);

        return relevantVoc;
    }

    /**
     * Create a MIOPage.
     * 
     * @param linkURL the link url
     * @param webDocument the web document
     * @param parentURL the parent url
     * @param linkName the link name
     * @param linkTitle the link title
     * @return the mIO page
     */
    private MIOPage generateMIOPage(final String linkURL, final Document webDocument, final String parentURL,
            final String linkName, final String linkTitle) {

        final MIOPage mioPage = new MIOPage(linkURL, webDocument);
        mioPage.setLinkParentPage(parentURL);
        mioPage.setLinkName(linkName);
        mioPage.setLinkTitle(linkTitle);
        mioPage.setLinkedPage(true);

        return mioPage;
    }
}
