/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;

import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.knowledge.Concept;
import tud.iir.web.Crawler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

/**
 * The LinkAnalyzer checks if some of the Links of MIOPageCandidates have targets with MIOs (simulates an indirect
 * search)
 * 
 * @author Martin Werner
 */
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
            // System.out.println(completeLinkTag);
            // String linkURL = getLinkURL(completeLinkTag, parentPageURL);
            if (!("").equals(completeLinkTag) && completeLinkTag != null) {
                linkTags.add(completeLinkTag);
            }
        }
        return analyzeLinkTags(linkTags, parentPageURL);
    }

    /**
     * Analyze link tags.
     * 
     * @param linkTags the link tags
     * @param parentPageURL the parent page url
     * @return the list
     */
    private List<MIOPage> analyzeLinkTags(final List<String> linkTags, final String parentPageURL) {
        // final long timeStamp4 = System.currentTimeMillis();

        final List<MIOPage> mioPages = new ArrayList<MIOPage>();
        final FastMIODetector mioDetector = new FastMIODetector();
        // final Crawler craw = new Crawler();
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
                    final double similarity = (double) jaroWinkler.getSimilarity(checkURL, linkURL);
                    if (similarity > 0.99) {
                        // isSimilare = true;
                        isExisting = true;
                        break;
                    }
                    // if (checkURL.equalsIgnoreCase(linkURL)) {
                    // // System.out.println(linkURL + " EQUALS " + checkURL);
                    // isExisting = true;
                    // break;
                    // }
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

                    // boolean isSimilare = false;
                    // for (String checkURL : linkURLs) {
                    //
                    // final double similarity = (double) jaroWinkler.getSimilarity(checkURL, linkURL);
                    // if (similarity > 0.99) {
                    // isSimilare = true;
                    // // System.out.println(linkURL + " aussortiert! da " + checkURL);
                    // break;
                    // // int checkStartIndex = checkURL.lastIndexOf("/");
                    // // int checkEndIndex = checkURL.length();
                    // // String lastCharacters = checkURL.substring(checkStartIndex, checkEndIndex);
                    // //
                    // // int linkStartIndex = linkURL.lastIndexOf("/");
                    // // int linkEndIndex = linkURL.length();
                    // // String lastCharacters2 = linkURL.substring(linkStartIndex, linkEndIndex);
                    // // if (lastCharacters2.length() > 1 && lastCharacters.length() > 1) {
                    // // double endingSimilarity = jaroWinkler.getSimilarity(lastCharacters, lastCharacters2);
                    // //
                    // // if (endingSimilarity > 0.93) {
                    // // // linkURLs.add(linkURL);
                    // // isExisting = true;
                    // // System.out.println(linkURL + " aussortiert! da " + checkURL);
                    // // break;
                    // // }
                    // // }
                    //
                    // }
                    // }
                    // if (!isSimilare) {

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
                    // }

                }
            }
        }
        // System.out.println("======> Analyzing LinkTags dauerte: " + DateHelper.getRuntime(timeStamp4));

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
            // System.out.println(extractedLink);
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
                // System.out.println(keyword);

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
            // if (keyword.contains(" ")) {
            // // also add 360 and view of "360 view" for example
            // String splittedKeyword[] = keyword.split(" ");
            // for (String sKeyWord : splittedKeyword) {
            // relevantVoc.add(sKeyWord);
            // }
            // } else {
            relevantVoc.add(keyword);
            // }
        }

        // remove duplicates
        HashSet<String> hashSet = new HashSet<String>(relevantVoc);
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

    /**
     * The main method.
     * 
     * @param args the arguments
     */
    // public static void main(String[] args) {
    // String url = "http://www.jr.com/canon/pe/CAN_MP990/";
    //
    // final long timeStamp2 = System.currentTimeMillis();
    // MIOPage mioPage = new MIOPage(url);
    // String content2 = mioPage.getContentAsString();
    // System.out.println("content2 dauerte: " + DateHelper.getRuntime(timeStamp2));

    // final long timeStamp1 = System.currentTimeMillis();
    // Crawler craw = new Crawler();
    // Document webDocument = craw.getWebDocument(url);
    // String content1 = Crawler.documentToString(webDocument);
    // System.out.println("content1 dauerte: "+ DateHelper.getRuntime(timeStamp1));

    // System.out.println(Crawler.isValidURL(" ", false));
    // System.exit(1);
    //
    // SearchWordMatcher svm = new SearchWordMatcher("canon mp990");
    // LinkAnalyzer linkAn = new LinkAnalyzer(svm);
    // Crawler c = new Crawler();
    // // String parentPageURL =
    // "http://www.canon-europe.com/For_Home/Product_Finder/Multifunctionals/Inkjet/PIXMA_MP990/";
    // String parentPageURL =
    // "http://www.canon.co.uk/for_home/product_finder/multifunctionals/inkjet/pixma_mp990/index.aspx?specs=1";
    // String content = c.download(parentPageURL);
    // if(svm.containsSearchWordOrMorphs(parentPageURL)){
    // System.out.println("YEAHHHHHHHHHHHHHH");
    // }else{
    // System.out.println("MIST");
    // }
    // if(linkAn.isRelevantLinkCheck("http://www.canon-europe.com/z/pixma_tour/en/mp990/swf/main.html?WT.acCCI_PixmaTour_MP990_Europe",
    // "", "")){
    // System.out.println(true);
    // }else{
    // System.out.println(false);
    // }
    // List<MIOPage> mioPages = linkAn.getLinkedMioPages(content, parentPageURL);
    // System.out.println(mioPages.size());
    // for (MIOPage mioPage : mioPages){
    // System.out.println(mioPage.getUrl());
    // }
    // }

}
