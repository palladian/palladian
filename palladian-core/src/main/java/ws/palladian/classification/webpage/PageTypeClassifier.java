package ws.palladian.classification.webpage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.StringInputStream;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

public class PageTypeClassifier extends RuleBasedPageClassifier<PageType> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PageTypeClassifier.class);

    public PageType classify(Document document) {
        extractFeatures(document);

        LOGGER.info("starting to classify a new document");

        // check the "generator" meta tag of the page
        String generator = getMetaTags().get("generator");
        if (generator != null) {
            generator = generator.toLowerCase();
            if (generator.indexOf("wordpress") > -1 || generator.indexOf("blogger") > -1) {
                return PageType.BLOG;
            }
            if (generator.indexOf("vbulletin") > -1 || generator.indexOf("phpbb") > -1) {
                return PageType.FORUM;
            }
        }

        if (getMetaTags().get("copyright") != null
                && getMetaTags().get("copyright").toLowerCase().indexOf("phpbb") > -1) {
            return PageType.FORUM;
        }

        // check link elements
        List<Node> metaNodes = XPathHelper.getXhtmlNodes(document, "//LINK");
        for (Node metaNode : metaNodes) {
            if (metaNode.getAttributes().getNamedItem("rel") != null
                    && metaNode.getAttributes().getNamedItem("title") != null
                    && metaNode.getAttributes().getNamedItem("title").getTextContent().toLowerCase().indexOf("phpbb") > -1) {
                return PageType.FORUM;
            }
        }

        if (getPageTitle().toLowerCase().indexOf("google groups") > -1) {
            return PageType.FORUM;
        }

        return PageType.GENERIC;
    }

    @Override
    public PageType classify(String htmlContent) {
        StringInputStream sis = new StringInputStream(htmlContent);

        DocumentParser parser = ParserFactory.createHtmlParser();
        Document document = null;
        try {
            document = parser.parse(sis);

            // we need this otherwise the list discovery won't work
            document.setDocumentURI("http://net-clipping.de");

        } catch (ParserException e) {
            e.printStackTrace();
        }

        return classify(document);
    }

    public PageType classify(File file) {
        DocumentRetriever c = new DocumentRetriever();
        Document document = c.getWebDocument(file.getPath());

        return classify(document);
    }

    public PageType classify(URL url) {
        DocumentRetriever c = new DocumentRetriever();
        Document document = c.getWebDocument(url.toString());

        return classify(document);
    }

    public boolean isBlog(File file) {
        DocumentRetriever c = new DocumentRetriever();
        Document document = c.getWebDocument(file.getPath());

        return isBlog(document);
    }

    public boolean isBlog(String htmlContent) {

        StringInputStream sis = new StringInputStream(htmlContent);

        DocumentParser parser = ParserFactory.createHtmlParser();
        Document document = null;
        try {
            document = parser.parse(sis);

            // we need this otherwise the list discovery won't work
            document.setDocumentURI("http://net-clipping.de");

        } catch (ParserException e) {
            e.printStackTrace();
        }

        return isBlog(document);
    }

    public boolean isBlog(Document document) {

        PageType result = classify(document);

        if (!result.equals(PageType.BLOG)) {
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

        PageTypeClassifier ncp = new PageTypeClassifier();

        Map<String, PageType> classes = new HashMap<String, PageType>();

        // /////////// test a single page /////////////
        // String url = "data/test/pagetype/search/forum/http___groups_google_com_group_de_soc_politik_misc";
        // url =
        // "http://www.openpr.de/news/508966/Hando-stellt-auf-der-Paracelsus-Messe-vom-11-13-02-2011-in-Wiesbaden-aus.html";
        // String targetPath = "data/test/pagetype/content/generic/";
        // documentRetriever.downloadAndSave(url,
        // targetPath + StringHelper.makeSafeName(url.replace("http://www.", "").replace("www.", ""), 50));
        //
        // // System.out.println(ncp.classify(new File(url)));
        // System.out.println(ncp.classify(new URL(url)));
        //
        // System.exit(0);
        // /////////////////////////////////////

        Map<String, PageType> typeMap = new HashMap<String, PageType>();
        typeMap.put("data/test/pagetype/content/blog", PageType.BLOG);
        typeMap.put("data/test/pagetype/content/forum", PageType.FORUM);
        typeMap.put("data/test/pagetype/content/generic", PageType.GENERIC);
        typeMap.put("data/test/pagetype/overview/blog", PageType.BLOG);
        typeMap.put("data/test/pagetype/overview/forum", PageType.FORUM);
        typeMap.put("data/test/pagetype/overview/generic", PageType.GENERIC);
        typeMap.put("data/test/pagetype/search/blog", PageType.BLOG);
        typeMap.put("data/test/pagetype/search/forum", PageType.FORUM);
        typeMap.put("data/test/pagetype/search/generic", PageType.GENERIC);
        typeMap.put("data/test/pagetype/spam/blog", PageType.BLOG);
        typeMap.put("data/test/pagetype/spam/forum", PageType.FORUM);
        typeMap.put("data/test/pagetype/spam/generic", PageType.GENERIC);

        for (Entry<String, PageType> entry : typeMap.entrySet()) {
            File[] folderFiles = FileHelper.getFiles(entry.getKey());
            for (File file : folderFiles) {
                if (file.getAbsolutePath().indexOf(".svn") > -1 || file.isDirectory()) {
                    continue;
                }
                classes.put(file.getAbsolutePath(), entry.getValue());
            }
        }

        int correctlyClassified = 0;
        int justBlogCorrectlyClassified = 0;
        int falseNegativesBlog = 0;
        int falsePositivesBlog = 0;
        for (Entry<String, PageType> entry : classes.entrySet()) {
            if (entry.getKey().indexOf(".svn") > -1) {
                continue;
            }

            boolean isBlog = ncp.isBlog(new File(entry.getKey()));
            if (isBlog && entry.getValue().equals(PageType.BLOG)) {
                justBlogCorrectlyClassified++;
            } else if (entry.getValue().equals(PageType.BLOG)) {
                falseNegativesBlog++;
            } else if (isBlog) {
                falsePositivesBlog++;
            } else {
                justBlogCorrectlyClassified++;
            }

            PageType classified = ncp.classify(new File(entry.getKey()));
            if (classified.equals(entry.getValue())) {
                correctlyClassified++;
                LOGGER.info("CORRECT (as " + entry.getValue() + "): " + entry.getKey());
            } else {
                LOGGER.info("WRONG (as " + classified + ", should be " + entry.getValue() + "): " + entry.getKey());
            }
        }

        LOGGER.info("correctly classified: " + MathHelper.round(100 * correctlyClassified / (double) classes.size(), 2)
                + "%");
        LOGGER.info("correctly classified just blog: "
                + MathHelper.round(100 * justBlogCorrectlyClassified / (double) classes.size(), 2) + "%");
        LOGGER.info("false positive blog rate: "
                + MathHelper.round(100 * falsePositivesBlog / (double) classes.size(), 2) + "%");
        LOGGER.info("false negative blog rate: "
                + MathHelper.round(100 * falseNegativesBlog / (double) classes.size(), 2) + "%");

        LOGGER.info("classification took " + sw.getElapsedTimeString() + " on " + classes.size() + " documents");
    }
}