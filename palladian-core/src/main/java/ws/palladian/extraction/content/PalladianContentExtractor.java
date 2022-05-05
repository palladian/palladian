package ws.palladian.extraction.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.WebPageDateEvaluator;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.ImageSizeComparator;
import ws.palladian.retrieval.PageAnalyzer;
import ws.palladian.retrieval.XPathSet;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * <p>
 * The PalladianContentExtractor extracts clean sentences from (English) texts. That is, short phrases are not included in the output. Consider the
 * {@link ReadabilityContentExtractor} for general content. The main difference is that this class also finds sentences in comment sections of web pages.
 * </p>
 * <p/>
 * <p>
 * Score on boilerplate dataset: 0.76088387 (r1505);
 * </p>
 *
 * @author David Urbansky
 */
public class PalladianContentExtractor extends WebPageContentExtractor {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianContentExtractor.class);

    private static final List<String> MAIN_NODE_HINTS = new ArrayList<>();

    /**
     * The entire document.
     */
    private Document document;

    private JsonObject schemaJson;

    /**
     * The detected main content node.
     */
    private Node resultNode;

    /**
     * The main content node but less strict, it might contain some clutter but also more images (used to find the main
     * image).
     */
    private Node outerResultNode;

    /**
     * All sentences in the main content.
     */
    private List<String> sentences = new ArrayList<>(1);

    /**
     * Detected comments on the page.
     */
    private List<String> comments = new ArrayList<>(0);

    /**
     * The html text of the main content node.
     */
    private String mainContentHtml = "";

    /**
     * The readable text of the main content node.
     */
    private String mainContentText = "";

    /**
     * The cleansed entire text content of the page.
     */
    private String fullTextContent = "";

    private ExtractedDate publishDate = null;

    /**
     * <p>
     * Extracted images will have a width and height. If the webmaster decides to specify these values in percentages we take the following value as a guess of the container size
     * in which the image is located in. Finding the real width and height of the container would require too much effort and possibly CSS parsing.
     * </p>
     */
    private static final int DEFAULT_IMAGE_CONTAINER_SIZE = 500;

    private List<WebImage> imageUrls;

    static {
        MAIN_NODE_HINTS.add("articleText");
        MAIN_NODE_HINTS.add("article_body");
        MAIN_NODE_HINTS.add("article-body");
        MAIN_NODE_HINTS.add("articleBody");
        // TODO next hint "hfeed" not tested properly with evaluation dataset!
        MAIN_NODE_HINTS.add("hfeed");
        // TODO more fine tuning possible here:
        // MAIN_NODE_HINTS.add("story_body");
        // MAIN_NODE_HINTS.add("single_post_content");
        // MAIN_NODE_HINTS.add("entry-single");
        // MAIN_NODE_HINTS.add("entry-content");
        // MAIN_NODE_HINTS.add("section-entry");
        // MAIN_NODE_HINTS.add("storyText");
        // MAIN_NODE_HINTS.add("post-content");
        // MAIN_NODE_HINTS.add("post-body");
        // MAIN_NODE_HINTS.add("articleContent");
        // MAIN_NODE_HINTS.add("article-content");
        // MAIN_NODE_HINTS.add("main-content");
        // MAIN_NODE_HINTS.add("contentBody");
        // MAIN_NODE_HINTS.add("article");
        // MAIN_NODE_HINTS.add("content");
        // MAIN_NODE_HINTS.add("post");
        MAIN_NODE_HINTS.add("st_text_c");
    }

    @Override
    public PalladianContentExtractor setDocument(Document document) throws PageContentExtractorException {
        setDocumentOnly(document);
        parseDocument();
        return this;
    }

    @Override
    public PalladianContentExtractor setDocument(Document document, boolean parse) throws PageContentExtractorException {
        setDocumentOnly(document);
        if (parse) {
            parseDocument();
        }
        return this;
    }

    public PalladianContentExtractor setDocumentOnly(Document document) throws PageContentExtractorException {
        this.document = document;
        imageUrls = null;

        resultNode = null;
        outerResultNode = null;
        sentences = new ArrayList<>();
        comments = new ArrayList<>();
        mainContentHtml = "";
        mainContentText = "";
        fullTextContent = "";

        return this;
    }

    public Document getDocument() {
        return document;
    }

    public List<String> getSentences() {
        return sentences;
    }

    public List<String> getComments() {
        return comments;
    }

    private String cleanXPath(String xPath) {
        // System.out.println("before clean xpath: " + xPath);
        xPath = xPath.replaceAll("/text(\\[.*?\\])?", "/");
        xPath = xPath.replace("html/body", "");
        xPath = xPath.replace("xhtml:html/xhtml:body", "");
        // xPath = xPath.replaceAll("/font(\\[.*?\\])?", "/");
        // xPath = xPath.replaceAll("/xhtml:font(\\[.*?\\])?", "/");

        xPath = xPath.replace("///", "//");

        // in case we did not find anything, we take the body content
        if (xPath.isEmpty() || xPath.equals("//")) {
            xPath = "//body";
        }

        if (xPath.endsWith("//")) {
            xPath = xPath.substring(0, xPath.length() - 2);
        }

        // System.out.println("clean xpath: " + xPath);

        // xPath = XPathHelper.addXhtmlNsToXPath(xPath);

        return xPath;
    }

    /**
     * <p>
     * This does not only contain the main content but also comments etc.
     * </p>
     */
    public String getEntireTextContent() {
        fullTextContent = fullTextContent.replaceAll("(\t)+", "");
        fullTextContent = Pattern.compile("^.{0,40}$", Pattern.MULTILINE).matcher(fullTextContent).replaceAll("\n");
        fullTextContent = fullTextContent.replaceAll("\n(\\s)+\n", "\n\n");
        fullTextContent = fullTextContent.replaceAll("(\n){2,}", "\n\n");

        return fullTextContent;
    }

    private void parseDocument() {
        String content = "";

        // if true, we didn't find valid elements within the main content block and take the whole node text
        boolean useMainNodeText = false;

        String parentXpath = "";
        String resultNodeXPath = "";
        resultNode = getMainContentNodeWithHints();
        int textNodeCount = 0;

        if (resultNode != null) {
            resultNodeXPath = PageAnalyzer.constructXPath(resultNode);
            resultNodeXPath = XPathHelper.addXhtmlNsToXPath(getDocument(), resultNodeXPath);
            parentXpath = resultNodeXPath;

            textNodeCount = countDirectTextNodes();
            LOGGER.debug("direct text nodes: " + textNodeCount);

            outerResultNode = resultNode;
        }

        fullTextContent = HtmlHelper.documentToText(document);

        // try getting the articleBody via schema (do that before cleaning the DOM)
        schemaJson = getSchemaJson(document);
        publishDate = extractPublishDate();
        cleanDom();
        if (schemaJson != null) {
            String articleBody = schemaJson.tryGetString("articleBody");
            if (articleBody == null) {
                articleBody = schemaJson.tryGetString("text");
            }
            if (articleBody != null && !articleBody.trim().isEmpty()) {
                content = articleBody;
                mainContentText = content;
            }
        }
        if (content.isEmpty()) {
            content = HtmlHelper.documentToText(document);
        }

        sentences = Tokenizer.getSentences(content, true);

        XPathSet xpathset = new XPathSet();

        // build xpaths to the sentences in the text, the more sentences we find in one area, the more likely it is the
        // main content
        Set<String> uniqueSentences = new HashSet<>(sentences);
        for (String sentence : uniqueSentences) {
            Set<String> xPaths = PageAnalyzer.constructAllXPaths(getDocument(), sentence);
            for (String xPath : xPaths) {
                xPath = PageAnalyzer.removeXPathIndicesFromLastCountNode(xPath);
                // XXX? not really since it is better without this if (!xPath.contains("/xhtml:li") &&
                // !xPath.contains("/li")) {
                xpathset.add(xPath);
                // }
            }
        }

        Map<String, Integer> xpmap = xpathset.getXPathMap();
        String highestCountXPath = xpathset.getHighestCountXPath();
        int highestCount = xpathset.getCountOfXPath(highestCountXPath);

        // if we know the main content block, remove all xPath which are not in that block
        Set<String> outOfMainContent = new HashSet<>();
        if (!resultNodeXPath.isEmpty()) {
            for (Entry<String, Integer> mapEntry : xpmap.entrySet()) {
                if (!mapEntry.getKey().startsWith(resultNodeXPath)) {
                    outOfMainContent.add(mapEntry.getKey());
                }
            }
            for (String string : outOfMainContent) {
                xpathset.remove(string);
            }

            if (!xpathset.isEmpty()) {
                highestCountXPath = xpathset.getHighestCountXPath();
                highestCount = xpathset.getCountOfXPath(highestCountXPath);

                if (textNodeCount > 3) {
                    useMainNodeText = true;
                }

            } else {
                useMainNodeText = true;
            }
        }

        String shortestMatchingXPath = highestCountXPath;
        if (!useMainNodeText) {
            // shorter paths with the same counts should be favored to not miss any content
            for (Entry<String, Integer> mapEntry : xpmap.entrySet()) {
                if (mapEntry.getKey().length() < shortestMatchingXPath.length() && mapEntry.getValue() == highestCount) {
                    shortestMatchingXPath = mapEntry.getKey();
                }
            }
        } else {
            parentXpath = resultNodeXPath;
        }

        if (shortestMatchingXPath.isEmpty()) {
            useMainNodeText = true;
        }

        shortestMatchingXPath = PageAnalyzer.findLastBoxSection(shortestMatchingXPath);

        // in case we did not find anything, we take the body content
        if (!useMainNodeText) {
            // parentXpath = PageAnalyzer.findLastBoxSection(shortestMatchingXPath);
            parentXpath = XPathHelper.getParentXPath(shortestMatchingXPath);
        }

        parentXpath = cleanXPath(parentXpath);

        resultNode = XPathHelper.getXhtmlNode(getDocument(), parentXpath);
        if (resultNode == null) {
            parentXpath = parentXpath.replaceAll("/[^x].*?:.*?/", "//");
            resultNode = XPathHelper.getXhtmlNode(getDocument(), parentXpath);

            if (resultNode == null) {
                parentXpath = XPathHelper.addXhtmlNsToXPath(parentXpath);
                resultNode = XPathHelper.getXhtmlNode(getDocument(), parentXpath);

                if (resultNode == null && mainContentText.isEmpty()) {
                    // XXX
                    mainContentText = fullTextContent;
                    return;
                }
            }
        }

        if (!useMainNodeText) {
            // shortestMatchingXPath = cleanXPath(shortestMatchingXPath);

            // add possible headlines that are on the same level as the content nodes to the target text nodes
            shortestMatchingXPath = addHeadlineSiblings(shortestMatchingXPath);

            // get the clean text only
            StringBuilder cleanText = new StringBuilder();
            List<Node> contentNodes = XPathHelper.getXhtmlNodes(getDocument(), shortestMatchingXPath);

            // if (contentNodes.isEmpty()) {
            // shortestMatchingXPath = XPathHelper.addXhtmlNsToXPath(shortestMatchingXPath);
            // if (!shortestMatchingXPath.contains("::xhtml:")) {
            // shortestMatchingXPath = shortestMatchingXPath.replace("::", "::xhtml:");
            // }
            // contentNodes = XPathHelper.getXhtmlNodes(getDocument(), shortestMatchingXPath);
            // }

            for (Node node : contentNodes) {
                String textContent = node.getTextContent();
                if (!textContent.isEmpty()) {
                    cleanText.append(textContent).append("\n\n");
                }
            }

            if (mainContentText.isEmpty()) {
                mainContentText = cleanText.toString();
            }
        }

        mainContentHtml = HtmlHelper.xmlToString(resultNode, true);

        // if we didn't get clean text, let's take the content of the main node
        if (mainContentText.trim().length() < 100) {
            mainContentText = HtmlHelper.documentToReadableText(resultNode);
        }
        if (mainContentText.trim().length() < 100) {
            mainContentText = fullTextContent;
        }

        mainContentText = StringHelper.cleanKeepFormat(mainContentText);
    }

    private int countDirectTextNodes() {
        int textNodeCount = 0;

        List<Node> breakNodes = XPathHelper.getXhtmlNodes(resultNode, "./text()");
        for (Node node : breakNodes) {
            String tc = node.getTextContent().trim();
            if (tc.length() > 20 && !tc.startsWith("<!--")) {
                textNodeCount++;
            }
        }

        return textNodeCount;
    }

    /**
     * <p>
     * Remove comment nodes, scripts, and iframes etc.
     * </p>
     */
    private void cleanDom() {
        // remove comments
        removeCommentNodes();

        // remove header, footer, and sidebars
        List<Node> removeNodes = new ArrayList<>();
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//header//*"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//nav//*"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//div[translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')= 'head']//*"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//div[translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')= 'pageheader']//*"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//div[translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')= 'header']//*"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//footer//*"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//div[translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')= 'foot']//*"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//div[translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')= 'footer']//*"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//div[translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')= 'pagefooter']//*"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//div[translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')= 'sidebar']//*"));
        // remove scripts / style / iframes etc.
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//*[(self::xhtml:style) or (self::xhtml:script) or (self::xhtml:iframe)]"));

        // registration / login
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//*[contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'register')]//*"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//label"));

        // related content
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//*[contains(translate(@class,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'taboola')]//*"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//aside//*"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//amp-iframe//*"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//amp-sidebar//*"));

        // paragraphs that only consist of a single link (usually references to other stories)
        // NOTE: xhtml namespace added here because XPathHelper doesn't do that for node names inside of string-length
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//xhtml:p[string-length(xhtml:a)=string-length()]"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(document, "//xhtml:p/xhtml:em[string-length(xhtml:strong)=string-length()]"));

        for (Node node : removeNodes) {
            if (node == null) {
                continue;
            }
            Node parentNode = node.getParentNode();
            if (parentNode == null) {
                continue;
            }
            parentNode.removeChild(node);
        }
    }

    private void removeCommentNodes() {
        List<Node> divs = XPathHelper.getXhtmlNodes(document,
                "//*[(self::xhtml:div) or (self::xhtml:p) or (self::xhtml:section) or (self::xhtml:ol) or (self::xhtml:ul) or (self::xhtml:li)][@class='comment' or contains(@class,'comment ') or contains(@class,' comment') or contains(@class,'comments ') or contains(@class,' comments') or contains(@id,'comments') or @id='disqus_thread']");

        for (Node node : divs) {
            comments.add(HtmlHelper.documentToReadableText(node));
            node.getParentNode().removeChild(node);
        }
    }

    private Node getMainContentNodeWithHints() {
        Node mainNode = null;

        for (String hint : MAIN_NODE_HINTS) {
            List<Node> mainNodes = new ArrayList<>();

            try {
                mainNodes = XPathHelper.getXhtmlNodes(getDocument(),
                        "//*[(self::xhtml:div) or (self::xhtml:p) or (self::xhtml:span)][@class='" + hint + "' or contains(@class,'" + hint + " ') or contains(@class,' " + hint
                                + "') or @itemprop='" + hint + "' or @id='" + hint + "' or @data-testid='" + hint + "']");
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!mainNodes.isEmpty()) {
                mainNode = mainNodes.get(0);
                if (mainNodes.size() > 1) {
                    mainNode = mainNode.getParentNode();
                }
            }

            if (mainNode != null) {
                LOGGER.debug("found main node with hint: " + hint);
                // System.out.println(HtmlHelper.getInnerXml(mainNode));
                break;
            }
        }

        return mainNode;
    }

    /**
     * <p>
     * Several elements are allowed to be siblings to the main text nodes (such as lists etc.)
     * </p>
     *
     * @param xPath The xPath that points to the main content nodes.
     * @return An xpath that also targets the siblings of the main text nodes.
     */
    private String addHeadlineSiblings(String xPath) {
        try {
            String[] parts = xPath.split("/");
            String lastPart = parts[parts.length - 1];
            String xhtmlNs = "";
            if (lastPart.contains("xhtml")) {
                xhtmlNs = "xhtml:";
            }
            String newLastPart =
                    "*[(self::" + lastPart + ") or (self::" + xhtmlNs + "h1) or (self::" + xhtmlNs + "h2) or (self::" + xhtmlNs + "h3) or (self::" + xhtmlNs + "h4) or (self::"
                            + xhtmlNs + "h5) or (self::" + xhtmlNs + "h6) or (self::" + xhtmlNs + "span) or (self::" + xhtmlNs + "ul) or (self::" + xhtmlNs + "ol) or (self::"
                            + xhtmlNs + "blockquote)]";
            xPath = xPath.replaceAll(lastPart + "$", newLastPart);
        } catch (Exception e) {
        }

        return xPath;
    }

    public List<WebImage> getImages(String fileType) {
        List<WebImage> filteredImages = new ArrayList<>();
        String ftSmall = fileType.toLowerCase();
        for (WebImage webImage : getImages()) {
            if (webImage.getFileType().toLowerCase().equalsIgnoreCase(ftSmall)) {
                filteredImages.add(webImage);
            }
        }

        return filteredImages;
    }

    public void filterBySize(List<WebImage> images, int minWidth, int minHeight) {
        List<WebImage> filteredImages = new ArrayList<>();

        for (WebImage webImage : images) {
            // if no dimensions known we allow the image or if the dimensions match our criteria
            if (webImage.getWidth() < 0 || webImage.getHeight() < 0 || (webImage.getWidth() > 0 && webImage.getWidth() > minWidth && webImage.getHeight() > 0
                    && webImage.getHeight() > minHeight)) {
                filteredImages.add(webImage);
            }
        }

        images.clear();
        images.addAll(filteredImages);
    }

    public void filterByFileType(List<WebImage> images, String... imageFormats) {
        List<WebImage> filteredImages = new ArrayList<>();

        for (WebImage webImage : images) {
            for (String imageFormat : imageFormats) {
                if (webImage.getFileType().equalsIgnoreCase(imageFormat)) {
                    filteredImages.add(webImage);
                }
            }
        }

        images.clear();
        images.addAll(filteredImages);
    }

    public void filterByName(List<WebImage> images, String mustNotContain) {
        List<WebImage> filteredImages = new ArrayList<>();

        for (WebImage webImage : images) {
            if (mustNotContain != null && !mustNotContain.isEmpty() && webImage.getImageUrl().contains(mustNotContain)) {
                continue;
            }
            filteredImages.add(webImage);
        }

        images.clear();
        images.addAll(filteredImages);
    }

    public List<WebImage> getImages() {
        if (outerResultNode != null) {
            return getImages(outerResultNode, getDocument(), new HashSet<>());
        }
        return getImages(resultNode, getDocument(), new HashSet<>());
    }

    public List<WebImage> getImages(Node imageParentNode) {
        return getImages(imageParentNode, document, new HashSet<>());
    }

    public List<WebImage> getImages(Node imageParentNode, Document webDocument, Collection<Node> excludeNodes) {
        // we need to query the result document with an xpath but the name space check has to be done on the original
        // document
        String imgXPath = ".//xhtml:img";
        return getImages(imageParentNode, webDocument, imgXPath, excludeNodes);
    }

    public List<WebImage> getImages(Node imageParentNode, Document webDocument, String imgXPath, Collection<Node> excludeNodes) {
        if (imageUrls != null) {
            return imageUrls;
        }

        if (imageParentNode == null) {
            return new ArrayList<>();
        }
        imageUrls = new ArrayList<>();

        // is there a base href?"
        String base = XPathHelper.getXhtmlNodeTextContent(webDocument, "//head/base/@href");

        List<Node> imageNodes = new ArrayList<>();

        while (imageNodes.isEmpty() && imageParentNode != null) {
            imageNodes = XPathHelper.getXhtmlNodes(imageParentNode, imgXPath);
            imageParentNode = imageParentNode.getParentNode();
        }

        // remove images from header and footer
        List<Node> removeNodes = XPathHelper.getXhtmlNodes(webDocument, "//header//img");
        removeNodes.addAll(XPathHelper.getXhtmlNodes(webDocument, "//div[@id='header']//img"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(webDocument, "//footer//img"));
        removeNodes.addAll(XPathHelper.getXhtmlNodes(webDocument, "//div[@id='footer']//img"));
        imageNodes.removeAll(removeNodes);

        for (Node node : imageNodes) {
            try {
                if (excludeNodes.contains(node)) {
                    continue;
                }

                NamedNodeMap nnm = node.getAttributes();
                BasicWebImage.Builder builder = new BasicWebImage.Builder();
                String imageUrl = nnm.getNamedItem("src").getTextContent();

                if (!imageUrl.startsWith("http")) {
                    if (base.isEmpty()) {
                        imageUrl = UrlHelper.makeFullUrl(webDocument.getDocumentURI(), null, imageUrl);
                    } else {
                        imageUrl = UrlHelper.makeFullUrl(base, null, imageUrl);
                    }
                }
                builder.setImageUrl(imageUrl);
                builder.setFileType(FileHelper.getFileType(imageUrl));

                if (nnm.getNamedItem("alt") != null) {
                    builder.setSummary(nnm.getNamedItem("alt").getTextContent());
                }
                if (nnm.getNamedItem("title") != null) {
                    builder.setTitle(nnm.getNamedItem("title").getTextContent());
                }

                boolean widthOrHeightFound = false;
                if (nnm.getNamedItem("width") != null) {
                    String w = nnm.getNamedItem("width").getTextContent();
                    builder.setWidth(getImageSize(w));
                    widthOrHeightFound = true;
                }
                if (nnm.getNamedItem("height") != null) {
                    String h = nnm.getNamedItem("height").getTextContent();
                    builder.setHeight(getImageSize(h));
                    widthOrHeightFound = true;
                }

                // maybe there is some inline css about width and height?
                if (!widthOrHeightFound) {
                    Node style = nnm.getNamedItem("style");
                    if (style != null) {
                        String styleText = style.getTextContent();
                        String widthText = StringHelper.getSubstringBetween(styleText, "width:", "px").trim();
                        String heightText = StringHelper.getSubstringBetween(styleText, "height:", "px").trim();
                        if (!widthText.isEmpty()) {
                            builder.setWidth(MathHelper.parseStringNumber(widthText, 0.0).intValue());
                        }
                        if (!heightText.isEmpty()) {
                            builder.setHeight(MathHelper.parseStringNumber(heightText, 0.0).intValue());
                        }
                    }
                }

                imageUrls.add(builder.create());
            } catch (NumberFormatException e) {
                LOGGER.debug(e.getMessage());
            } catch (NullPointerException e) {
                LOGGER.debug("an image has not all necessary attributes");
            }
        }

        return imageUrls;
    }

    private int getImageSize(String attributeText) throws NumberFormatException {
        int size;
        attributeText = attributeText.replace(",*", "");

        if (attributeText.contains("%")) {
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
        return mainContentHtml;
    }

    @Override
    public String getResultText() {
        return mainContentText;
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
        return getResultTitle(new HashSet<>());
    }

    public String getResultTitle(Collection<String> excludeNodes) {
        String resultTitle = "";

        // try schema first
        if (schemaJson != null) {
            resultTitle = schemaJson.tryGetString("headline");
            if (resultTitle != null && !resultTitle.isEmpty()) {
                return cleanTitle(resultTitle);
            }
        }

        // try to get it from the biggest headline, take last one as we assume this to be the most specific
        List<Node> xhtmlNodes = XPathHelper.getXhtmlNodes(getDocument(), "//h1[not(ancestor::header) and not(ancestor::footer)]");

        for (String excludeNodeXPath : excludeNodes) {
            xhtmlNodes.removeAll(XPathHelper.getXhtmlNodes(getDocument(), excludeNodeXPath + "//h1"));
        }

        Node h1Node;
        if (excludeNodes.isEmpty()) {
            h1Node = CollectionHelper.getLast(xhtmlNodes);
        } else {
            h1Node = CollectionHelper.getFirst(xhtmlNodes);
        }

        if (h1Node != null) {
            resultTitle = StringHelper.clean(HtmlHelper.documentToReadableText(h1Node).replaceAll("\n+", " - "));
        }

        if (resultTitle == null || resultTitle.isEmpty()) {
            Node titleNode = XPathHelper.getXhtmlNode(getDocument(), "//title");

            if (titleNode != null) {
                resultTitle = titleNode.getTextContent();

                // remove everything after | sign
                resultTitle = resultTitle.replaceAll("\\|.*", "").trim();
            } else {
                resultTitle = StringHelper.getFirstWords(mainContentText, 20);
            }
        }

        return cleanTitle(resultTitle);
    }

    private String cleanTitle(String title) {
        //        return title.replaceAll(" - [^ ]+$", "").trim();
        return title.trim();
    }

    @Override
    public String getExtractorName() {
        return "Palladian";
    }

    /**
     * <p>
     * Try to find the correct image dimensions of all extracted images. Do that only for images that had no "width" and "height" attributes in the image tag. Note that other
     * images might have different real dimensions and might have been scaled using the HTML attributes.
     * </p>
     */
    public void analyzeImages() {
        List<WebImage> temp = new ArrayList<>();

        for (WebImage webImage : getImages()) {
            if (webImage.getWidth() == 0 || webImage.getHeight() == 0) {
                BufferedImage image = ImageHandler.load(webImage.getUrl());
                if (image != null) {
                    BasicWebImage.Builder builder = new BasicWebImage.Builder();
                    builder.setWebImage(webImage);
                    builder.setWidth(image.getWidth());
                    builder.setHeight(image.getHeight());
                    temp.add(builder.create());
                } else {
                    temp.add(webImage);
                }
            } else {
                temp.add(webImage);
            }
        }

        imageUrls = temp;

    }

    /**
     * <p>
     * Get the author of the article using the WebKnox API.
     * </p>
     *
     * @param apiKey The WebKnox API key.
     * @return The detected author name.
     */
    public String getAuthorName(String apiKey) {
        String author = "";
        String url = "http://webknox.com/api/webpage/author?url=" + getDocument().getDocumentURI() + "&language=en&apiKey=" + apiKey;
        DocumentRetriever retriever = new DocumentRetriever();
        // changed to palladian JSON, but untested. Philipp, 2013-09-22
        String authorsJson = retriever.getText(url);
        if (authorsJson != null && authorsJson.length() > 0) {
            try {
                return new JsonArray(authorsJson).getJsonObject(0).getString("name");
            } catch (JsonException e) {
            }
        }
        return author;
    }

    /**
     * <p>
     * Get the publish date of the Web page.
     * </p>
     *
     * @return The extracted date.
     */
    public ExtractedDate getPublishDate() {
        return publishDate;
    }

    private ExtractedDate extractPublishDate() {
        if (schemaJson != null) {
            String datePublished = schemaJson.tryGetString("datePublished");
            if (datePublished != null) {
                ExtractedDate dateFromSchema = DateParser.findDate(datePublished);
                if (dateFromSchema != null) {
                    return dateFromSchema;
                }
            }
        }
        return WebPageDateEvaluator.getBestDate(document, PageDateType.PUBLISH);
    }

    /**
     * <p>
     * Use several indicators in the site's HTML to detect its language.
     * </p>
     */
    public Language detectLanguage() {
        // look in HTML lang attribute <html lang="de">
        String innerXml = HtmlHelper.getInnerXml(getDocument());
        innerXml = innerXml.toLowerCase();
        String substringBetween = StringHelper.getSubstringBetween(innerXml, " lang=\"", "\"");
        if (substringBetween.isEmpty()) {
            substringBetween = StringHelper.getSubstringBetween(innerXml, " xml:lang=\"", "\"");
        }
        if (substringBetween.isEmpty()) {
            substringBetween = StringHelper.getSubstringBetween(innerXml, " xmlu00003alang=\"", "\"");
        }
        if (substringBetween.isEmpty()) {
            substringBetween = StringHelper.getSubstringBetween(innerXml, "<meta name=\"content-language\" content=\"", "\"");
        }
        if (substringBetween.isEmpty()) {
            substringBetween = StringHelper.getSubstringBetween(innerXml, "<meta name=\"language\" content=\"", "\"");
        }
        if (substringBetween != null && !substringBetween.isEmpty() && substringBetween.length() < 6) {
            // remove country, e.g. en-US
            String[] parts = substringBetween.split("[-:]");
            return Language.getByIso6391(parts[0]);
        }

        // use TLDs
        String uri = getDocument().getDocumentURI();

        String domain = UrlHelper.getDomain(uri);
        if (domain.endsWith(".de") || domain.endsWith(".at")) {
            return Language.GERMAN;
        } else if (domain.endsWith(".fr")) {
            return Language.FRENCH;
        } else if (domain.endsWith(".es")) {
            return Language.SPANISH;
        } else if (domain.endsWith(".it")) {
            return Language.ITALIAN;
        } else if (domain.endsWith(".co.uk") || domain.endsWith(".ac.uk") || domain.endsWith(".ac.za") || domain.endsWith(".ie") || domain.endsWith(".co.nz") || domain.endsWith(
                ".co.za") || domain.endsWith(".au") || domain.endsWith(".ca") || domain.endsWith(".us")) {
            return Language.ENGLISH;
        } else if (domain.endsWith(".pl")) {
            return Language.POLISH;
        } else if (domain.endsWith(".dk")) {
            return Language.DANISH;
        } else if (domain.endsWith(".co.jp")) {
            return Language.JAPANESE;
        } else if (domain.endsWith(".pt")) {
            return Language.PORTUGUESE;
        } else if (domain.endsWith(".nl")) {
            return Language.DUTCH;
        } else if (domain.endsWith(".ru")) {
            return Language.RUSSIAN;
        } else if (domain.endsWith(".no")) {
            return Language.NORWEGIAN;
        } else if (domain.endsWith(".cz")) {
            return Language.CZECH;
        } else if (domain.endsWith(".sk")) {
            return Language.SLOVAK;
        } else if (domain.endsWith(".fi")) {
            return Language.FINNISH;
        } else if (domain.endsWith(".gr")) {
            return Language.GREEK;
        } else if (domain.endsWith(".co.il")) {
            return Language.HEBREW;
        } else if (domain.endsWith(".vn")) {
            return Language.VIETNAMESE;
        } else if (domain.endsWith(".se")) {
            return Language.SWEDISH;
        }

        return null;
    }

    /**
     * <p>
     * Try to find the dominant image of the site.
     * </p>
     *
     * @param contentIncludeXPath A collection xPath that hint to where the image must be found.
     * @param contentExcludeXPath A collection xPath that hint to where the image must NOT be found.
     * @return The dominant image.
     */
    public WebImage getDominantImage(Collection<String> contentIncludeXPath, Collection<String> contentExcludeXPath) {
        // check schema first
        if (schemaJson != null) {
            // image as object, array, or simple string, all are possible
            // array
            JsonArray imagesArray = schemaJson.tryGetJsonArray("image");
            if (imagesArray != null && !imagesArray.isEmpty() && imagesArray.tryGetString(0) != null) {
                return new BasicWebImage.Builder().setImageUrl(imagesArray.tryGetString(0).trim()).create();
            }

            // obj
            JsonObject imageObj = schemaJson.tryGetJsonObject("image");
            if (imageObj != null) {
                String imageUrl = imageObj.tryQueryString("url");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    BasicWebImage.Builder builder = new BasicWebImage.Builder().setImageUrl(imageUrl.trim());
                    int width = Optional.ofNullable(imageObj.tryGetInt("width")).orElse(0);
                    int height = Optional.ofNullable(imageObj.tryGetInt("height")).orElse(0);
                    if (width > 0 && height > 0) {
                        builder.setWidth(width).setHeight(height);
                    } else {
                        Double width1 = Optional.ofNullable(MathHelper.parseStringNumber(Optional.ofNullable(imageObj.tryGetString("width")).orElse(""))).orElse(0.);
                        Double height1 = Optional.ofNullable(MathHelper.parseStringNumber(Optional.ofNullable(imageObj.tryGetString("height")).orElse(""))).orElse(0.);
                        if (width1 > 0 && height1 > 0) {
                            builder.setWidth(width1.intValue()).setHeight(height1.intValue());
                        }
                    }
                    return builder.create();
                }
            }

            // string
            String image = schemaJson.tryGetString("image");
            if (image != null && !image.isEmpty()) {
                return new BasicWebImage.Builder().setImageUrl(image.trim()).create();
            }
        }

        // check meta property first
        Node xhtmlNode = XPathHelper.getXhtmlNode(getDocument(), "//meta[@property='og:image']//@content");
        if (xhtmlNode != null) {
            return new BasicWebImage.Builder().setImageUrl(xhtmlNode.getTextContent().trim()).create();
        }

        List<Node> excludeImageNodes = new ArrayList<>();
        if (contentExcludeXPath != null && !contentExcludeXPath.isEmpty()) {
            for (String xpath : contentExcludeXPath) {
                List<Node> excludeImages = XPathHelper.getXhtmlNodes(getDocument(), xpath + "//img");
                excludeImageNodes.addAll(excludeImages);
            }
        }

        // exclude image nodes that are links to other websites (e.g. partner logos)
        List<Node> linkedImageNodes = XPathHelper.getXhtmlNodes(getDocument(), "//a/img");
        String documentDomain = UrlHelper.getDomain(getDocument().getDocumentURI(), false, false);
        for (Node linkedImageNode : linkedImageNodes) {
            // check that link is really going to a different domain
            try {
                String link = linkedImageNode.getParentNode().getAttributes().getNamedItem("href").getTextContent();
                link = UrlHelper.makeFullUrl(getDocument().getDocumentURI(), link);
                if (!UrlHelper.getDomain(link, false, false).equals(documentDomain)) {
                    excludeImageNodes.add(linkedImageNode);
                }
            } catch (Exception e) {
                // ccl
            }
        }

        // look for itemprop image
        xhtmlNode = XPathHelper.getXhtmlNode(getDocument(),
                "//*[(translate(@itemprop,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')= 'image' or translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')= 'photo') and not(ancestor::header) and not(ancestor::footer)]");
        if (xhtmlNode != null && !excludeImageNodes.contains(xhtmlNode)) {
            Node xhtmlNode1 = XPathHelper.getXhtmlNode(xhtmlNode, ".//@src");
            if (xhtmlNode1 != null) {
                String url = UrlHelper.makeFullUrl(getDocument().getDocumentURI(), null, xhtmlNode1.getTextContent().trim());
                return new BasicWebImage.Builder().setImageUrl(url).create();
            }
        }

        // look for "main image"
        xhtmlNode = XPathHelper.getXhtmlNode(getDocument(),
                "//img[(contains(@class,'main-photo') or contains(@class,'main-image')) and not(ancestor::header) and not(ancestor::footer)]");
        if (xhtmlNode != null && !excludeImageNodes.contains(xhtmlNode)) {
            Node xhtmlNode1 = XPathHelper.getXhtmlNode(xhtmlNode, ".//@src");
            if (xhtmlNode1 != null) {
                String url = UrlHelper.makeFullUrl(getDocument().getDocumentURI(), null, xhtmlNode1.getTextContent().trim());
                return new BasicWebImage.Builder().setImageUrl(url).create();
            }
        }

        // try something else
        WebImage image = null;
        List<WebImage> images = new ArrayList<>();
        Node mainContentNode = getDocument();

        if (contentIncludeXPath != null && !contentIncludeXPath.isEmpty()) {
            for (String includeXPath : contentIncludeXPath) {
                mainContentNode = XPathHelper.getXhtmlNode(getDocument(), includeXPath);
                images.addAll(getImages(mainContentNode, getDocument(),
                        ".//img[not(ancestor::header) and not(ancestor::footer) and not(ancestor::a[contains(@href,'index') or @href=''])]", excludeImageNodes));
            }
        } else {
            // get images that are not in header or footer or that link to the index (which are usually logos and
            // banners)
            images.addAll(
                    getImages(mainContentNode, getDocument(), ".//img[not(ancestor::header) and not(ancestor::footer) and not(ancestor::a[contains(@href,'index') or @href=''])]",
                            excludeImageNodes));
        }

        filterByFileType(images, "jpeg", "png", "jpg");
        if (!images.isEmpty()) {
            // remove duplicate images (likely to be icons)
            Map<String, WebImage> deduplicationMap = new HashMap<>();
            for (WebImage webImage : images) {
                deduplicationMap.put(webImage.getImageUrl(), webImage);
            }
            images = new ArrayList<>(deduplicationMap.values());

            // only sort by size if the first one is below a certain size
            image = CollectionHelper.getFirst(images);
            if (image != null && image.getSize() < 10000) {
                // filter out icons
                // if (images.size() > 1) {
                filterByName(images, "icon");
                // }
                // filter out images that are too small (that we know of)
                filterBySize(images, 50, 50);
                images.sort(new ImageSizeComparator());
                image = CollectionHelper.getFirst(images);
            }
        }

        return image;
    }

    public WebImage getDominantImage() {
        return getDominantImage(null, null);
    }

    private JsonObject getSchemaJson(Document webPage) {
        List<Node> scriptNodes = XPathHelper.getXhtmlNodes(webPage, "//script[@type=\"application/ld+json\"]");
        for (Node scriptNode : scriptNodes) {
            String jsonString = scriptNode.getTextContent();
            try {
                jsonString = jsonString.trim();
                JsonObject jsonObject = null;
                if (jsonString.startsWith("[")) {
                    JsonArray schemas = new JsonArray(jsonString);
                    // find the one with @type = article
                    for (int i = 0; i < schemas.size(); i++) {
                        JsonObject schemaJso = schemas.tryGetJsonObject(i);
                        if (Optional.ofNullable(schemaJso.tryGetString("@type")).orElse("").contains("article")) {
                            jsonObject = schemaJso;
                            break;
                        }
                    }
                    if (jsonObject == null) {
                        jsonObject = schemas.tryGetJsonObject(0);
                    }
                } else {
                    // some cleansing
                    //                    jsonString = jsonString.replaceAll("\\$[^{}()\" ]+", "");
                    jsonString = jsonString.replaceAll("}\\s*\"", "},\"");
                    jsonString = jsonString.replace("/*<![CDATA[*/", "");
                    jsonString = jsonString.replace("/*]]>*/", "");
                    jsonObject = Optional.ofNullable(JsonObject.tryParse(jsonString)).orElse(new JsonObject());
                }

                // some websites wrap it in a "@graph": [] node
                if (jsonObject.containsKey("@graph")) {
                    JsonArray graphArray = jsonObject.tryGetJsonArray("@graph");
                    if (graphArray != null) {
                        for (int i = 0; i < graphArray.size(); i++) {
                            JsonObject jsonObject1 = graphArray.tryGetJsonObject(i);
                            String type = Optional.ofNullable(jsonObject1.tryGetString("@type")).orElse("").toLowerCase();
                            if (type.contains("article")) {
                                return jsonObject1;
                            }
                        }
                    }
                }
                if (jsonObject.containsKey("headline")) {
                    return jsonObject;
                }
            } catch (Exception e) {
                LOGGER.error("could not get schema json from " + webPage.getDocumentURI(), e);
            }
        }
        return null;
    }

    public static void main(String[] args) throws PageContentExtractorException {
        PalladianContentExtractor palladianContentExtractor = new PalladianContentExtractor();
        palladianContentExtractor.setDocument(new DocumentRetriever().getWebDocument("http://janeshealthykitchen.com/instant-red-sauce/"));
        Language language = palladianContentExtractor.detectLanguage();
        List<WebImage> images = palladianContentExtractor.getImages();
        CollectionHelper.print(images);

        System.out.println(language);
        System.exit(0);

        // ////////////////////////////////////
        // Document webDocument = new DocumentRetriever().getWebDocument("C:\\Workspace\\data\\GoldStandard\\98.html");
        // String xPath =
        // "//table[4]/tbody/tr[1]/td[1]/table[1]/tbody/tr[1]/td/table[1]/tbody/tr[1]/td[1]/table[1]/tbody/tr[2]/td[1]";
        // // xPath = "//text//table[1]/tbody/tr[2]/td[1]";
        // xPath = XPathHelper.addXhtmlNsToXPath(xPath);
        // List<Node> xhtmlNodes = XPathHelper.getXhtmlNodes(webDocument, xPath);
        // CollectionHelper.print(xhtmlNodes);
        // System.exit(0);
        // ////////////////////////////////////

        PalladianContentExtractor pe = new PalladianContentExtractor();
        // pe.setDocument("http://jezebel.com/5733078/can-you-wear-six-items-or-less");
        // pe.setDocument("http://www.seobook.com/shopping-search");
        // pe.setDocument("http://www.fourhourworkweek.com/blog/2012/11/24/the-4-hour-chef-launch-summary-of-week-one/");

        // pe.setDocument("http://www.dailyfinance.com/2012/07/20/stockbroker-corrupt-wall-street-cheats/");
        // pe.setDocument("http://www.nationalmemo.com/white-house-tax-rates-on-the-rich-will-go-up/");
        // pe.setDocument("http://news.discovery.com/human/women-prefer-thin-men-121128.html");
        // pe.setDocument("http://www.extremetech.com/extreme/141643-mits-sun-funnel-could-slit-solar-powers-efficiency-bottleneck-2");
        // pe.setDocument("http://www.cbsnews.com/8301-201_162-57556049/3-bp-employees-plead-not-guilty-on-gulf-oil-spill-charges/");
        // pe.setDocument("http://www.jta.org/news/article/2012/11/28/3113081/petitions-do-not-find-wonder-lovely");
        // pe.setDocument("http://www.walesonline.co.uk/news/wales-news/2012/11/29/leveson-report-fears-its-recommendations-could-throw-governnment-into-turmoil-91466-32329849/");
        // pe.setDocument("http://www.sfgate.com/business/energy/article/Workers-raise-1st-section-of-new-Chernobyl-shelter-4069524.php");
        // pe.setDocument("http://news.yahoo.com/germany-not-back-palestinian-u-n-bid-government-130743009.html");
        // pe.setDocument("http://www.thelocal.se/44726/20121129/");
        // pe.setDocument("http://www.politicususa.com/walmart-earns-record-profits-supporting-republicans-plan-slash-employees-food-stamps.html");

        // to solve:
        // ol/li
        // pe.setDocument("http://www.africanews.com/site/Rebels_begin_withdrawal_in_eastern_DR_Congo/list_messages/42682");
        // pe.setDocument("http://www.dailyfinance.com/2012/07/20/stockbroker-corrupt-wall-street-cheats/");
        // pe.setDocument("http://jezebel.com/5733078/can-you-wear-six-items-or-less");
        // pe.setDocument("http://slotmachinebasics.com/");
        // -> formatting
        // pe.setDocument("http://www.absoluteastronomy.com/topics/Jet_Li");
        // pe.setDocument("http://www.reuters.com/article/2012/11/15/us-usa-obama-petraeus-idUSBRE8AD1FB20121115");
        // pe.setDocument("http://gulfnews.com/opinions/editorials/israel-has-to-change-its-policy-of-violence-1.1108785");

        // pe.setDocument("http://www.cinefreaks.com/news/692/Neun-interessante-Fakten%2C-die-du-nicht-%C3%BCber-die-Oscars-2012-wusstest");
        // pe.setDocument("http://www.komonews.com/news/local/Will-feds-let-new-marijuana-law-stand-without-a-fight-177666311.html");
        // pe.setDocument("http://www.raptitude.com/2012/11/what-love-is-not/");
        // pe.setDocument("http://blogs.windows.com/windows_phone/b/windowsphone/archive/2012/11/28/an-update-on-windows-phone-7-8.aspx");
        // pe.setDocument("http://blog.mashape.com/170078722");
        // pe.setDocument("http://www.killerstartups.com/startups-tools-and-guides/launch-your-own-crazy-idea-jason-sadler/");
        // pe.setDocument("http://uncommonbusiness.blogspot.de/2012/11/savingscom-story.html");
        // pe.setDocument("http://www.daemonology.net/blog/2012-11-28-broken-EC2-firewall.html");
        // pe.setDocument("http://www.bizjournals.com/washington/blog/techflash/2012/11/major-layoffs-set-for-livingsocial.html");
        // pe.setDocument("http://blog.stephenwolfram.com/2012/11/mathematica-9-is-released-today/");
        // pe.setDocument("http://www.allaboutbirds.org/guide/Peregrine_Falcon/lifehistory");
        // pe.setDocument("http://www.hollyscoop.com/cameron-diaz/52.aspx");
        // pe.setDocument("http://www.absoluteastronomy.com/topics/Jet_Li");
        // pe.setDocument("http://www.cinefreaks.com/news/696/Die-Hard-5");
        // pe.setDocument("http://edition.cnn.com/2012/11/23/world/meast/egypt-protests/index.html?hpt=hp_t1");
        // pe.setDocument("http://www.bbc.co.uk/news/world-middle-east-20458148");
        // pe.setDocument("http://lifehacker.com/5862004/heres-your-black-friday-survival-toolkit");
        // pe.setDocument("http://www.reuters.com/article/2012/11/23/us-egypt-president-idUSBRE8AM0DO20121123");
        // pe.setDocument("http://www.foxnews.com/us/2012/11/23/walmart-calls-black-friday-success-despite-protests-about-worker-conditions/");
        // pe.setDocument("http://www.seobythesea.com/2012/11/not-all-anchor-text-is-equal-other-co-citation-observations/");
        // pe.setDocument("http://arstechnica.com/tech-policy/2012/11/ca-measure-would-ban-anonymous-online-speech-for-sex-offenders/");
        // pe.setDocument("http://www.usatoday.com/story/opinion/2012/10/31/mitt-romney-jeep-chrysler-uaw/1672501/");
        // pe.setDocument("http://www.washingtonpost.com/politics/decision2012/after-grueling-campaign-polls-open-for-election-day-2012/2012/11/06/d1c24c98-2802-11e2-b4e0-346287b7e56c_story.html");
        // pe.setDocument("http://mobile.smashingmagazine.com/2012/11/07/succeed-with-your-app/");
        // pe.setDocument("http://www.bbc.com/travel/feature/20121108-irelands-outlying-islands");
        // pe.setDocument("http://www.huffingtonpost.com/2012/11/22/black-friday-creep-retail-workers_n_2167066.html");
        // pe.setDocument("http://webknox.com/p/best-proxy-services");
        // pe.setDocument("http://www.politicususa.com/walmart-earns-record-profits-supporting-republicans-plan-slash-employees-food-stamps.html");
        // pe.setDocument("http://greatist.com/fitness/perfect-squat/");
        // pe.setDocument("http://www.latimes.com/news/nationworld/world/la-fg-israel-gaza-20121120,0,4042611.story");
        // pe.setDocument("http://www.labour.org.uk/govt-decision-on-palestine-vote-is-worse-than-a-blunder,2012-11-30");
        // pe.setDocument("C:\\Workspace\\data\\ContentExtraction\\TUD\\page203.html");
        // pe.setDocument("http://www.thedailybeast.com/articles/2012/11/29/where-ban-ki-moon-meets-pink-floyd.html");
        // pe.setDocument("http://www.nationalmemo.com/white-house-tax-rates-on-the-rich-will-go-up/");
        // pe.setDocument("http://www.ynetnews.com/articles/0,7340,L-4314175,00.html");
        // pe.setDocument("http://tech.slashdot.org/story/12/11/16/207227/german-city-says-openoffice-shortcomings-are-forcing-it-back-to-microsoft");
        // pe.setDocument("http://news.mongabay.com/2012/1204-hance-lions-population.html");
        // pe.setDocument("C:\\Workspace\\data\\GoldStandard\\82.html");
        // pe.setDocument("C:\\Workspace\\data\\GoldStandard\\105.html");
        // pe.setDocument("C:\\Workspace\\data\\GoldStandard\\771.html"); // ???
        // pe.setDocument("C:\\Workspace\\data\\GoldStandard\\652.html");
        // pe.setDocument("C:\\Workspace\\data\\GoldStandard\\640.html");
        // pe.setDocument("http://www.upi.com/Top_News/US/2013/12/31/Man-faces-kidnapping-other-charges-in-trip-to-Las-Vegas-to-marry/UPI-67931388527587/");
        pe.setDocument("http://www.voanews.com/content/russia-urges-nations-to-take-active-role-in-the-middle-east-93610219/169955.html", true);

        // CollectionHelper.print(pe.setDocument("http://www.bbc.co.uk/news/science-environment-12209801").getImages());
        System.out.println("Title: " + pe.getResultTitle());
        // System.out.println("Author: "
        // + pe.getAuthorName(ConfigHolder.getInstance().getConfig().getString("api.webknox.apiKey")));
        System.out.println("Result Text: " + pe.getResultText());
        System.out.println("Comments: ");
        CollectionHelper.print(pe.getComments());

        System.out.println("Full Text: " + pe.getEntireTextContent());
        // CollectionHelper.print(pe.getSentences());
    }

}