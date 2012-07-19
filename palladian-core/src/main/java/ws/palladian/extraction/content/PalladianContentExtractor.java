package ws.palladian.extraction.content;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.PageAnalyzer;
import ws.palladian.retrieval.XPathSet;
import ws.palladian.retrieval.resources.WebImage;

/**
 * <p>
 * The PalladianContentExtractor extracts clean sentences from (English) texts. That is, short phrases are not included in
 * the output. Consider the {@link ReadabilityContentExtractor} for general content. The main difference is that this class
 * also finds sentences in comment sections of web pages.
 * </p>
 * 
 * <p>
 * Score on boilerplate dataset: 0.76088387 (r1505);
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class PalladianContentExtractor extends WebPageContentExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ReadabilityContentExtractor.class);

    private Document document;
    private Node resultNode;

    private List<String> sentences = null;
    private String mainContentHtml = null;
    private String mainContentText = null;

    /**
     * Extracted images will have a width and height. If the webmaster decides to specify these values in percentages we
     * take the following value as a guess of the container size in which the image is located in. Finding the real
     * width and height of the container would require too much effort and possibly CSS parsing.
     */
    private static final int DEFAULT_IMAGE_CONTAINER_SIZE = 500;

    private List<WebImage> imageURLs;


    @Override
    public PalladianContentExtractor setDocument(Document document) throws PageContentExtractorException {
        this.document = document;
        imageURLs = null;
        parseDocument();
        return this;
    }

    public Document getDocument() {
        return document;
    }

    public List<String> getSentences() {
        if (sentences == null) {
            sentences = Tokenizer.getSentences(getResultText(), true);
        }
        return sentences;
    }

    private void parseDocument() throws PageContentExtractorException {

        // String content = "";

        // try to find the article using html 5 article tag
        Node articleNode = XPathHelper.getXhtmlNode(document, "//article");
        if (articleNode != null) {
            // content = HtmlHelper.documentToText(articleNode);

            resultNode = articleNode;

        } else {
            // content = HtmlHelper.documentToText(document);

            // try to find the main content in absence of HTML5 article node
            PageAnalyzer pa = new PageAnalyzer();
            pa.setDocument(getDocument());
            XPathSet xpathset = new XPathSet();

            Set<String> uniqueSentences = new HashSet<String>(sentences);
            for (String sentence : uniqueSentences) {
                Set<String> xPaths = pa.constructAllXPaths(sentence);
                for (String xPath : xPaths) {
                    xPath = PageAnalyzer.removeXPathIndicesFromLastCountNode(xPath);
                    xpathset.add(xPath);
                }
            }

            // take the shortest xPath that has a count of at least 50% of the xPath with the highest count
            LinkedHashMap<String, Integer> xpmap = xpathset.getXPathMap();
            String highestCountXPath = xpathset.getHighestCountXPath();
            int highestCount = xpathset.getCountOfXPath(highestCountXPath);

            String shortestMatchingXPath = highestCountXPath;
            for (Entry<String, Integer> mapEntry : xpmap.entrySet()) {
                if (mapEntry.getKey().length() < shortestMatchingXPath.length()
                        && mapEntry.getValue() / (double)highestCount >= 0.5) {
                    shortestMatchingXPath = mapEntry.getKey();
                }
            }

            // String highestCountXPath = xpathset.getHighestCountXPath();

            // in case we did not find anything, we take the body content
            if (shortestMatchingXPath.isEmpty()) {
                shortestMatchingXPath = "//body";
            } else {
                shortestMatchingXPath = XPathHelper.getParentXPath(shortestMatchingXPath);
            }
            shortestMatchingXPath = shortestMatchingXPath.replace("html/body", "");
            shortestMatchingXPath = shortestMatchingXPath.replace("xhtml:html/xhtml:body", "");

            // in case we did not find anything, we take the body content
            if (shortestMatchingXPath.isEmpty()) {
                shortestMatchingXPath = "//body";
            }

            // /xhtml:HTML/xhtml:BODY/xhtml:DIV[3]/xhtml:TABLE[1]/xhtml:TR[1]/xhtml:TD[1]/xhtml:TABLE[1]/xhtml:TR[2]/xhtml:TD[1]/xhtml:P/xhtml:FONT
            // shortestMatchingXPath =
            // "//xhtml:DIV[3]/xhtml:TABLE[1]/xhtml:TR[1]/xhtml:TD[1]/xhtml:TABLE[1]/xhtml:TR[2]/xhtml:TD[1]/xhtml:P/xhtml:FONT";
            // resultNode = XPathHelper.getNode(getDocument(), shortestMatchingXPath);
            // shortestMatchingXPath = "//xhtml:div[1]/xhtml:table[3]/xhtml:tr[1]/xhtml:td[2]/xhtml:blockquote[2]";
            // shortestMatchingXPath = "//xhtml:div[1]/xhtml:table[3]//tr//xhtml:td[2]//xhtml:blockquote[2]";
            // shortestMatchingXPath = "//xhtml:tr";
            // HtmlHelper.printDom(document);
            // System.out.println(HtmlHelper.documentToString(document));
            resultNode = XPathHelper.getXhtmlNode(getDocument(), shortestMatchingXPath);
            if (resultNode == null) {
                // System.out.println(content);
                throw new PageContentExtractorException("could not get main content node for URL: "
                        + getDocument().getDocumentURI() + ", using xpath" + shortestMatchingXPath);
            }

        }

    }

    public List<WebImage> getImages(String fileType) {

        List<WebImage> filteredImages = new ArrayList<WebImage>();
        String ftSmall = fileType.toLowerCase();
        for (WebImage webImage : getImages()) {
            if (webImage.getType().toLowerCase().equalsIgnoreCase(ftSmall)) {
                filteredImages.add(webImage);
            }
        }

        return filteredImages;
    }


    public List<WebImage> getImages() {
        return getImages(resultNode);
    }

    public List<WebImage> getImages(Node imageParentNode) {

        if (imageURLs != null) {
            return imageURLs;
        }

        imageURLs = new ArrayList<WebImage>();

        if (resultNode == null) {
            return imageURLs;
        }

        // we need to query the result document with an xpath but the name space check has to be done on the original
        // document
        String imgXPath = "//img";
        // if (XPathHelper.hasXhtmlNs(document)) {
        // imgXPath = XPathHelper.addXhtmlNsToXPath(imgXPath);
        // }

        List<Node> imageNodes = XPathHelper.getXhtmlChildNodes(imageParentNode, imgXPath);
        for (Node node : imageNodes) {
            try {

                WebImage webImage = new WebImage();

                NamedNodeMap nnm = node.getAttributes();
                String imageUrl = nnm.getNamedItem("src").getTextContent();

                if (!imageUrl.startsWith("http")) {

                    imageUrl = UrlHelper.makeFullUrl(getDocument().getDocumentURI(), null, imageUrl);
                    // if (imageURL.startsWith("/")) {
                    // imageURL = UrlHelper.getDomain(getDocument().getDocumentURI()) + imageURL;
                    // } else {
                    // imageURL = getDocument().getDocumentURI() + imageURL;
                    // }
                }

                webImage.setUrl(imageUrl);

                if (nnm.getNamedItem("alt") != null) {
                    webImage.setAlt(nnm.getNamedItem("alt").getTextContent());
                }
                if (nnm.getNamedItem("title") != null) {
                    webImage.setTitle(nnm.getNamedItem("title").getTextContent());
                }
                if (nnm.getNamedItem("width") != null) {
                    String w = nnm.getNamedItem("width").getTextContent();
                    webImage.setWidth(getImageSize(w));
                }
                if (nnm.getNamedItem("height") != null) {
                    String h = nnm.getNamedItem("height").getTextContent();
                    webImage.setHeight(getImageSize(h));
                }

                imageURLs.add(webImage);

            } catch (NumberFormatException e) {
                LOGGER.debug(e.getMessage());
            } catch (NullPointerException e) {
                LOGGER.debug("an image has not all necessary attributes");
            }
        }

        return imageURLs;
    }

    private int getImageSize(String attributeText) throws NumberFormatException {

        int size = -1;
        attributeText = attributeText.replace(",*", "");

        if (attributeText.indexOf("%") > -1) {
            attributeText = attributeText.replace("%", "");
            attributeText = StringHelper.trim(attributeText);
            size = (int) (0.01 * Integer.parseInt(attributeText) * DEFAULT_IMAGE_CONTAINER_SIZE);
        } else {
            attributeText = attributeText.replace("px", "");
            attributeText = StringHelper.trim(attributeText);
            size = Integer.parseInt(attributeText);
        }

        return size;
    }

    @Override
    public Node getResultNode() {
        return resultNode;
    }

    public String getMainContentHtml() {
        if (mainContentHtml == null) {
            mainContentHtml = HtmlHelper.xmlToString(getResultNode(), true);
        }

        return mainContentHtml;
    }


    @Override
    public String getResultText(){
        if (mainContentText == null) {
            mainContentText = HtmlHelper.documentToReadableText(getResultNode());
        }
        return mainContentText;
    }

    @Deprecated
    public static String getText(String url) {

        StringBuilder text = new StringBuilder();

        try {
            PalladianContentExtractor pse = new PalladianContentExtractor();
            pse.setDocument(url);

            List<String> sentences = pse.getSentences();

            for (String string : sentences) {
                text.append(string).append(" ");
            }
        } catch (PageContentExtractorException e) {
            LOGGER.error(e);
        }

        return text.toString();
    }

    public String getSentencesString() {
        StringBuilder text = new StringBuilder();
        List<String> sentences = getSentences();

        for (String string : sentences) {
            text.append(string).append(" ");
        }

        return text.toString();
    }



    @Override
    public String getResultTitle() {
        // try to get it from the biggest headline
        Node h1Node = XPathHelper.getXhtmlNode(getDocument(), "//h1");

        String resultTitle = "";
        if (h1Node != null) {
            resultTitle = h1Node.getTextContent();
        } else {
            Node titleNode = XPathHelper.getXhtmlNode(getDocument(), "//title");

            if (titleNode != null) {
                resultTitle = titleNode.getTextContent();

                // remove everything after | sign
                resultTitle = resultTitle.replaceAll("\\|.*", "").trim();
            } else {
                resultTitle = StringHelper.getFirstWords(mainContentText, 20);
            }
        }

        return resultTitle;
    }

    @Override
    public String getExtractorName() {
        return "Palladian";
    }

    /**
     * <p>
     * Try to find the correct image dimensions of all extracted images. Do that only for images that had no "width" and
     * "height" attributes in the image tag. Note that other images might have different real dimensions and might have
     * been scaled using the HTML attributes.
     * </p>
     */
    public void analyzeImages() {

        for (WebImage webImage : getImages()) {
            if (webImage.getWidth() == 0 || webImage.getHeight() == 0) {
                BufferedImage image = ImageHandler.load(webImage.getUrl());
                if (image != null) {
                    webImage.setWidth(image.getWidth());
                    webImage.setHeight(image.getHeight());
                }
            }
        }

    }

    
    /**
     * @param args
     * @throws PageContentExtractorException
     */
    public static void main(String[] args) throws PageContentExtractorException {

        // String url = "http://lifehacker.com/5690722/why-you-shouldnt-switch-your-email-to-facebook";
        // String url = "http://stackoverflow.com/questions/2670082/web-crawler-that-can-interpret-javascript";
        // System.out.println("SentenceExtractor: " + PageSentenceExtractor.getText(url));
        // System.out.println("ContentExtractor:  " + new PageContentExtractor().getResultText(url));

        // PageContentExtractor pe = new PageContentExtractor();
        // pe.setDocument("http://jezebel.com/5733078/can-you-wear-six-items-or-less");
        // System.out.println(pe.getResultText());
        // CollectionHelper.print(pe.getImages());

        PalladianContentExtractor pe = new PalladianContentExtractor();
        //WebPageContentExtractor pe2 = new ReadabilityContentExtractor();
        // pe.setDocument("http://www.allaboutbirds.org/guide/Peregrine_Falcon/lifehistory");
        // pe.setDocument("http://www.hollyscoop.com/cameron-diaz/52.aspx");
//        pe.setDocument("http://www.absoluteastronomy.com/topics/Jet_Li");
//        pe.setDocument("http://www.cinefreaks.com/news/692/Neun-interessante-Fakten%2C-die-du-nicht-%C3%BCber-die-Oscars-2012-wusstest");
        // pe.setDocument("http://slotmachinebasics.com/");
        pe.setDocument("http://www.cinefreaks.com/news/696/Die-Hard-5");

        // CollectionHelper.print(pe.setDocument("http://www.bbc.co.uk/news/science-environment-12209801").getImages());
        System.out.println("Title:"+pe.getResultTitle());
        System.out.println("Result Text: "+pe.getResultText());
        // CollectionHelper.print(pe.getSentences());

        // CollectionHelper.print(pe.setDocument(
        // "data/datasets/L3S-GN1-20100130203947-00001/original/2281f3c1-7a86-4c4c-874c-b19964e588f1.html")
        // .getImages());
        // System.out.println(pe.getMainContentText());
        //
        // CollectionHelper.print(pe.setDocument("http://jezebel.com/5733078/can-you-wear-six-items-or-less").getImages());
        // System.out.println(pe.getMainContentText());
        //
        // CollectionHelper.print(pe.setDocument("http://www.bbc.co.uk/news/science-environment-12190895")
        // .getImages("jpg"));
        // System.out.println(pe.getMainContentText());
        //
        // CollectionHelper.print(pe.setDocument("http://lifehacker.com/5715912/how-to-plant-ideas-in-someones-mind")
        // .getImages("jpg"));
        // System.out.println(pe.getMainContentText());
        //
        // CollectionHelper.print(pe.setDocument(
        // "http://edition.cnn.com/2011/WORLD/europe/01/14/italy.berlusconi/index.html").getImages("jpg"));
        // System.out.println(pe.getMainContentText());

    }
}