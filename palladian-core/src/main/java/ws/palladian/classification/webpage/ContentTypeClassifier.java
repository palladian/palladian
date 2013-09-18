package ws.palladian.classification.webpage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.StringInputStream;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.resources.WebImage;

public class ContentTypeClassifier extends RuleBasedPageClassifier<ContentType> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentTypeClassifier.class);

    private static final String[] SEARCH_TRIGGERS = {"suchergebnis", "suchergebnisse", "search result",
            "search results"};

    public ContentType classify(Document document) {
        extractFeatures(document);

        LOGGER.info("starting to classify a new document");

        final int imagesThresholdCount = 10;

        // check whether many ingoing links contain "mehr >>" or similar phrases
        final int moreThresholdCount = 7;
        int moreCount = 0;
        final String[] readMore = {"mehr", "weiterlesen", "artikel lesen", "[...]"};

        java.util.List<WebContent> allLinks = new ArrayList<WebContent>();
        allLinks.addAll(getIngoingLinks());
        allLinks.addAll(getOutgoingLinks());

        if (getPageTitle().toLowerCase().indexOf("suche") > -1 || headlineContainsSearchTrigger()) {
            // headlineContents
            return ContentType.SEARCH_RESULTS;
        }

        if (getHighestNumberOfConsecutiveSentences() >= 4) {
            return ContentType.CONTENT;
        }

        for (WebContent ingoingLink : allLinks) {

            for (String readMoreWord : readMore) {
                if (ingoingLink.getTitle().toLowerCase().indexOf(readMoreWord) > -1) {
                    moreCount++;
                }
                if (moreCount >= moreThresholdCount) {
                    return ContentType.OVERVIEW;
                }
            }

        }

        int imageCount = 0;
        for (WebImage image : getImages()) {

            if (image.getSize() > 10000) {
                imageCount++;
            }

            if (imageCount >= imagesThresholdCount) {
                return ContentType.OVERVIEW;
            }
        }

        if (getPaginationLinks().size() > 3) {
            return ContentType.OVERVIEW;
        }

        if (getHighestNumberOfConsecutiveSentences() < 4 || getPageSentences().length() < 1000
                || getPageSentences().toLowerCase().indexOf("read the rest here:") > -1
                || getPageSentences().toLowerCase().indexOf("read the original post:") > -1
                || getPageSentences().toLowerCase().indexOf("continued here:") > -1
                || getPageSentences().toLowerCase().indexOf("see the rest here:") > -1) {
            return ContentType.SPAM;
        }

        return ContentType.CONTENT;
    }

    private boolean headlineContainsSearchTrigger() {

        for (String headline : getHeadlineContents()) {
            headline = headline.toLowerCase();
            for (String trigger : SEARCH_TRIGGERS) {
                if (headline.equalsIgnoreCase(trigger) || headline.indexOf(trigger + " ") > -1
                        || headline.indexOf(trigger + ":") > -1) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public ContentType classify(String htmlContent) {
        StringInputStream sis = new StringInputStream(htmlContent);

        DocumentParser parser = ParserFactory.createHtmlParser();
        Document document = null;
        try {
            document = parser.parse(sis);
        } catch (ParserException e) {
            LOGGER.error(e.getMessage());
            return ContentType.UNKNOWN;
        }

        // we need this otherwise the list discovery won't work
        document.setDocumentURI("http://net-clipping.de");

        return classify(document);
    }

    public ContentType classify(File file) {
        DocumentRetriever c = new DocumentRetriever();
        Document document = c.getWebDocument(file.getPath());

        return classify(document);
    }

    public ContentType classify(URL url) {
        DocumentRetriever c = new DocumentRetriever();
        Document document = c.getWebDocument(url.toString());

        return classify(document);
    }

    public boolean isUseful(File file) {
        DocumentRetriever c = new DocumentRetriever();
        Document document = c.getWebDocument(file.getPath());

        return isUseful(document);
    }

    public boolean isUseful(String htmlContent) {

        StringInputStream sis = new StringInputStream(htmlContent);

        DocumentParser parser = ParserFactory.createHtmlParser();
        Document document = null;
        try {
            document = parser.parse(sis);
        } catch (ParserException e) {
            LOGGER.error(e.getMessage());
            return false;
        }

        // we need this otherwise the list discovery won't work
        document.setDocumentURI("http://net-clipping.de");

        return isUseful(document);
    }

    public boolean isUseful(Document document) {

        ContentType result = classify(document);

        if (!result.equals(ContentType.CONTENT)) {
            return false;
        }

        return true;
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        StopWatch sw = new StopWatch();

        // DocumentRetriever documentRetriever = new DocumentRetriever();

        ContentTypeClassifier ncp = new ContentTypeClassifier();

        Map<String, ContentType> classes = new HashMap<String, ContentType>();

        // /////////// test a single page /////////////
        String url = "http://www.openpr.de/news/508966/Hando-stellt-auf-der-Paracelsus-Messe-vom-11-13-02-2011-in-Wiesbaden-aus.html";
        System.out.println(ncp.classify(new URL(url)));
        System.exit(0);
        // /////////////////////////////////////

        Map<String, ContentType> typeMap = new HashMap<String, ContentType>();
        typeMap.put("data/test/pagetype/overview/", ContentType.OVERVIEW);
        typeMap.put("data/test/pagetype/spam/", ContentType.SPAM);
        typeMap.put("data/test/pagetype/search/", ContentType.SEARCH_RESULTS);
        typeMap.put("data/test/pagetype/content/", ContentType.CONTENT);

        for (Entry<String, ContentType> entry : typeMap.entrySet()) {
            File[] folderFiles = FileHelper.getFilesRecursive(entry.getKey());
            for (File file : folderFiles) {
                if (file.getAbsolutePath().indexOf(".svn") > -1 || file.isDirectory()) {
                    continue;
                }
                classes.put(file.getAbsolutePath(), entry.getValue());
            }
        }

        int correctlyClassified = 0;
        int justUsefulCorrectlyClassified = 0;
        int falseNegatives = 0;
        int trueNegatives = 0;
        for (Entry<String, ContentType> entry : classes.entrySet()) {
            if (entry.getKey().indexOf(".svn") > -1) {
                continue;
            }

            boolean isUseful = ncp.isUseful(new File(entry.getKey()));
            if (isUseful && entry.getValue().equals(ContentType.CONTENT)) {
                justUsefulCorrectlyClassified++;
            } else if (entry.getValue().equals(ContentType.CONTENT)) {
                falseNegatives++;
            } else {
                // not useful and not classified as content (true negative)
                trueNegatives++;
                justUsefulCorrectlyClassified++;
            }

            ContentType classified = ncp.classify(new File(entry.getKey()));
            if (classified.equals(entry.getValue())) {
                correctlyClassified++;
                LOGGER.info("CORRECT (as " + entry.getValue() + "): " + entry.getKey());
            } else {
                LOGGER.info("WRONG (as " + classified + ", should be " + entry.getValue() + "): " + entry.getKey());
            }
        }

        LOGGER.info("correctly classified: " + MathHelper.round(100 * correctlyClassified / (double)classes.size(), 2)
                + "%");
        LOGGER.info("correctly classified just useful: "
                + MathHelper.round(100 * justUsefulCorrectlyClassified / (double)classes.size(), 2) + "%");
        LOGGER.info("false negative rate: " + MathHelper.round(100 * falseNegatives / (double)classes.size(), 2) + "%");
        LOGGER.info("true negative rate: " + MathHelper.round(100 * trueNegatives / (double)classes.size(), 2) + "%");

        LOGGER.info("classification took " + sw.getElapsedTimeString() + " on " + classes.size() + " documents");
    }
}