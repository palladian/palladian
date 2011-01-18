/**
 * The LinkAnalyzer checks if some of the Links of MIOPageCandidates have targets with relevant MIOs (simulates an
 * indirect
 * search)
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import tud.iir.helper.HTMLHelper;
import tud.iir.knowledge.Concept;
import tud.iir.web.Crawler;
import tud.iir.web.URLDownloader;
import tud.iir.web.resources.WebLink;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

public class LinkAnalyzer {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(LinkAnalyzer.class);

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
    public List<MIOPage> getLinkedMioPages(String parentPageContent, String parentPageURL) {
        List<String> linkTags = new ArrayList<String>();

        // find all <a>-tags
        // Pattern p = Pattern.compile("<a[^>]*href=\\\"?[^(>|)]*\\\"?[^>]*>[^<]*</a>", Pattern.CASE_INSENSITIVE);
        Pattern pattern = Pattern.compile("<a[^>]*>.*</a>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(parentPageContent);
        while (matcher.find()) {
            String completeLinkTag = matcher.group(0);

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
    private List<MIOPage> analyzeLinkTags(List<String> linkTags, String parentPageURL) {

        List<MIOPage> mioPages = new ArrayList<MIOPage>();
        FastMIODetector mioDetector = new FastMIODetector();
        // Crawler craw = new Crawler(4000, 5000, 5000);

        // linkURLs save the URLs that were already found
        // List<WebLink> linkURLs = new ArrayList<WebLink>();
        Map<String, WebLink> webLinks = new HashMap<String, WebLink>();
        JaroWinkler jaroWinkler = new JaroWinkler();

        LOGGER.info("found " + linkTags.size() + " links to pages");

        for (String linkTag : linkTags) {
            boolean isExisting = false;

            String linkURL = getLinkURL(linkTag, parentPageURL);

            if (linkURL.length() < 5) {
                continue;
            }

            if (!webLinks.isEmpty()) {

                for (WebLink webLink : webLinks.values()) {
                    double similarity = jaroWinkler.getSimilarity(webLink.getUrl(), linkURL);
                    if (similarity > 0.99) {
                        isExisting = true;
                        break;
                    }
                }
            }

            if (!isExisting) {

                // extract the Text of a Link
                String linkName = getLinkName(linkTag);

                // extract the value of the title-attribute of a link
                String linkTitle = getLinkTitle(linkTag);

                // check if the linkURL or linkInfo or linkTitle contains entity
                // relevant words
                if (isRelevantLinkCheck(linkURL, linkName, linkTitle)
                        && hasConceptRelevance(linkURL, linkName, linkTitle)) {

                    WebLink webLink = new WebLink();
                    webLink.setTitle(linkTitle);
                    webLink.setText(linkName);
                    webLink.setUrl(linkURL);

                    webLinks.put(linkURL, webLink);
                }
            }
        }

        URLDownloader urlDownloader = new URLDownloader();
        // limit the linked pages to 25
        int c = 0;
        for (WebLink webLink : webLinks.values()) {
            urlDownloader.add(webLink.getUrl());
            if (++c >= 25) {
                break;
            }
        }
        urlDownloader.setMaxThreads(10);

        Set<Document> linkedPages = urlDownloader.start();

        LOGGER.info("downloaded " + linkedPages.size() + " linked pages");

        for (Document webDocument : linkedPages) {
            String linkedPageContent = Crawler.documentToString(webDocument);

            if (linkedPageContent != null && linkedPageContent.length() > 5
                    && mioDetector.containsMIO(linkedPageContent)) {

                WebLink webLink = webLinks.get(webDocument.getDocumentURI());

                MIOPage mioPage = generateMIOPage(webLink.getUrl(), webDocument, parentPageURL, webLink.getText(),
                        webLink.getTitle());
                mioPages.add(mioPage);

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
    private String getLinkName(String linkTag) {
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
    private MIOPage generateMIOPage(String linkURL, Document webDocument, String parentURL, String linkName,
            String linkTitle) {

        MIOPage mioPage = new MIOPage(linkURL, webDocument);
        mioPage.setLinkParentPage(parentURL);
        mioPage.setLinkName(linkName);
        mioPage.setLinkTitle(linkTitle);
        mioPage.setLinkedPage(true);

        return mioPage;
    }
}
