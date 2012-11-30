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
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.PageAnalyzer;
import ws.palladian.retrieval.XPathSet;
import ws.palladian.retrieval.resources.WebImage;

/**
 * <p>
 * The PalladianContentExtractor extracts clean sentences from (English) texts. That is, short phrases are not included
 * in the output. Consider the {@link ReadabilityContentExtractor} for general content. The main difference is that this
 * class also finds sentences in comment sections of web pages.
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

    private static final List<String> MAIN_NODE_HINTS = new ArrayList<String>();

    private Document document;
    private Node resultNode;

    private List<String> sentences = new ArrayList<String>();
    private String mainContentHtml = "";
    private String mainContentText = "";

    /**
     * <p>
     * Extracted images will have a width and height. If the webmaster decides to specify these values in percentages we
     * take the following value as a guess of the container size in which the image is located in. Finding the real
     * width and height of the container would require too much effort and possibly CSS parsing.
     * </p>
     */
    private static final int DEFAULT_IMAGE_CONTAINER_SIZE = 500;

    private List<WebImage> imageURLs;

    static {
        MAIN_NODE_HINTS.add("articleText");
        MAIN_NODE_HINTS.add("article_body");
        MAIN_NODE_HINTS.add("article-body");
        MAIN_NODE_HINTS.add("articleBody");
        // MAIN_NODE_HINTS.add("story_body");
        // MAIN_NODE_HINTS.add("single_post_content");
        // MAIN_NODE_HINTS.add("entry-single");
        // MAIN_NODE_HINTS.add("entry-content");
        // MAIN_NODE_HINTS.add("section-entry");
        // MAIN_NODE_HINTS.add("storyText");
        // MAIN_NODE_HINTS.add("post-content");
        // MAIN_NODE_HINTS.add("post-body");
        // MAIN_NODE_HINTS.add("articleContent");
        // MAIN_NODE_HINTS.add("articleBody");
        // MAIN_NODE_HINTS.add("article-content");
        // MAIN_NODE_HINTS.add("main-content");
        // MAIN_NODE_HINTS.add("contentBody");
        // MAIN_NODE_HINTS.add("article");
        // MAIN_NODE_HINTS.add("content");
        // MAIN_NODE_HINTS.add("post");
    }

    @Override
    public PalladianContentExtractor setDocument(Document document) throws PageContentExtractorException {
        this.document = document;
        imageURLs = null;

        resultNode = null;
        sentences = new ArrayList<String>();
        mainContentHtml = "";
        mainContentText = "";

        parseDocument();
        return this;
    }

    public Document getDocument() {
        return document;
    }

    public List<String> getSentences() {
        return sentences;
    }

    private void parseDocument() throws PageContentExtractorException {

        String content = "";

        // try to find the article using html 5 article tag
        Node articleNode = XPathHelper.getXhtmlNode(document, "//article");
        if (false && articleNode != null) {
            content = HtmlHelper.documentToText(articleNode);
            sentences = Tokenizer.getSentences(content, true);

            resultNode = articleNode;

        } else {

            // if true, we didn't find valid elements within the main content block and take the whole node text
            boolean useMainNodeText = false;

            String parentXpath = "";
            String resultNodeXPath = "";
            resultNode = getMainContentNodeWithHints();
            int textNodeCount = 0;

            if (resultNode != null) {
                resultNodeXPath = PageAnalyzer.constructXPath(resultNode);
                resultNodeXPath = XPathHelper.addXhtmlNsToXPath(getDocument(), resultNodeXPath);
                // resultNode = XPathHelper.getXhtmlNode(getDocument(), resultNodeXPath);
                parentXpath = resultNodeXPath;
                List<Node> breakNodes = XPathHelper.getXhtmlNodes(resultNode, "./text()");
                for (Node node : breakNodes) {
                    String tc = node.getTextContent().trim();
                    if (tc.length() > 20 && !tc.startsWith("<!--")) {
                        textNodeCount++;
                    }
                }
                System.out.println("direct text nodes: " + textNodeCount);

            }

            cleanDom();

            content = HtmlHelper.documentToText(document);
            sentences = Tokenizer.getSentences(content, true);

            // try to find the main content in absence of HTML5 article node
            XPathSet xpathset = new XPathSet();

            Set<String> uniqueSentences = new HashSet<String>(sentences);
            for (String sentence : uniqueSentences) {
                // if (sentence.contains("As news of the bag find broke, however, they altered their tune.")) {
                // System.out.println("stop");
                // }
                Set<String> xPaths = PageAnalyzer.constructAllXPaths(getDocument(), sentence);
                for (String xPath : xPaths) {
                    xPath = PageAnalyzer.removeXPathIndicesFromLastCountNode(xPath);
                    if (!xPath.contains("/xhtml:li") && !xPath.contains("/li")) {
                        xpathset.add(xPath);
                    }
                }
            }

            // take the shortest xPath that has a count of at least 50% of the xPath with the highest count
            LinkedHashMap<String, Integer> xpmap = xpathset.getXPathMap();
            String highestCountXPath = xpathset.getHighestCountXPath();
            int highestCount = xpathset.getCountOfXPath(highestCountXPath);

            // if we know the main content block, remove all xPath which are not in that block
            Set<String> outOfMainContent = CollectionHelper.newHashSet();
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
                        // if (highestCountXPath.endsWith("div")) {
                        useMainNodeText = true;
                    }

                } else {
                    useMainNodeText = true;
                }
                // CollectionHelper.print(outOfMainContent);
            }

            String shortestMatchingXPath = highestCountXPath;
            if (!useMainNodeText) {
                for (Entry<String, Integer> mapEntry : xpmap.entrySet()) {
                    if (mapEntry.getKey().length() < shortestMatchingXPath.length()
                            && mapEntry.getValue() == highestCount) {
                        shortestMatchingXPath = mapEntry.getKey();
                    }
                }
            } else {
                parentXpath = resultNodeXPath;
            }
            // for (Entry<String, Integer> mapEntry : xpmap.entrySet()) {
            // if (mapEntry.getKey().length() < shortestMatchingXPath.length()
            // && mapEntry.getValue() / (double)highestCount >= 0.5) {
            // // shortestMatchingXPath = mapEntry.getKey();
            // }
            // }

            // in case we did not find anything, we take the body content
            if (!useMainNodeText) {
                parentXpath = XPathHelper.getParentXPath(shortestMatchingXPath);
            }
            parentXpath = parentXpath.replace("html/body", "");
            parentXpath = parentXpath.replace("xhtml:html/xhtml:body", "");

            // in case we did not find anything, we take the body content
            if (parentXpath.isEmpty()) {
                parentXpath = "//body";
            }

            resultNode = XPathHelper.getXhtmlNode(getDocument(), parentXpath);
            if (resultNode == null) {
                parentXpath = parentXpath.replaceAll("\\/[^x].*?\\:.*?\\/", "//");
                resultNode = XPathHelper.getXhtmlNode(getDocument(), parentXpath);
                if (resultNode == null) {
                    // System.out.println(content);
                    throw new PageContentExtractorException("could not get main content node for URL: "
                            + getDocument().getDocumentURI() + ", using xpath" + shortestMatchingXPath);
                }
            }

            if (!useMainNodeText) {
                // add possible headlines that are on the same level as the content nodes to the target text nodes
                shortestMatchingXPath = addHeadlineSiblings(shortestMatchingXPath);

                // get the clean text only
                StringBuilder cleanText = new StringBuilder();
                List<Node> contentNodes = XPathHelper.getXhtmlNodes(getDocument(), shortestMatchingXPath);
                for (Node node : contentNodes) {
                    String textContent = node.getTextContent();
                    if (!textContent.isEmpty()) {
                        cleanText.append(textContent).append("\n\n");
                    }
                }

                mainContentText = cleanText.toString();
            }
        }

        mainContentHtml = HtmlHelper.xmlToString(resultNode, true);

        if (mainContentText.trim().length() < 100) {
            mainContentText = mainNodeToText();
        }
    }

    private void cleanDom() {

        // remove comments
        removeCommentNodes();

        // remove scripts / style / iframes etc.
        List<Node> divs = XPathHelper.getXhtmlNodes(document,
                "//*[(self::xhtml:style) or (self::xhtml:script) or (self::xhtml:iframe)]");
        for (Node node : divs) {
            node.getParentNode().removeChild(node);
        }

        // remove scripts / style / iframes etc.
        // removeFooterNodes();

    }

    private void removeCommentNodes() {

        List<Node> divs = XPathHelper
                .getXhtmlNodes(
                        document,
                        "//*[(self::xhtml:div) or (self::xhtml:p)][@class='comment' or contains(@class,'comments ') or contains(@class,' comments') or contains(@id,'comments')]");
        for (Node node : divs) {
            node.getParentNode().removeChild(node);
        }

    }

    private void removeFooterNodes() {

        List<Node> divs = XPathHelper
                .getXhtmlNodes(
                        document,
                        "//*[(self::xhtml:div) or (self::xhtml:p)][@class='comment' or contains(@class,'comments ') or contains(@class,' comments') or contains(@id,'comments')]");
        for (Node node : divs) {
            node.getParentNode().removeChild(node);
        }

    }

    private String mainNodeToText() {

        // List<Node> divs = XPathHelper.getXhtmlNodes(resultNode, "./div");
        // for (Node node : divs) {
        // resultNode.removeChild(node);
        // }

        // StringBuilder stringBuilder = new StringBuilder();
        // List<Node> divs = XPathHelper.getXhtmlNodes(resultNode, "./text()");
        // Set<String> stringsToRemove = CollectionHelper.newHashSet();
        // for (Node divNode : divs) {
        // stringBuilder.append(divNode.getTextContent()).append("\n\n");
        // }
        //
        // // return HtmlHelper.documentToReadableText(resultNode);
        // return stringBuilder.toString();

        return HtmlHelper.documentToReadableText(resultNode);
    }

    // private String getMainContentNodeWithHints() {
    //
    // String mainNodeXPath = "";
    //
    // for (String hint : MAIN_NODE_HINTS) {
    // List<Node> mainNodes = XPathHelper.getXhtmlNodes(getDocument(),
    // "//*[(self::xhtml:div) or (self::xhtml:p) or (self::xhtml:span)][@class='" + hint
    // + "' or contains(@class,'" + hint + " ') or contains(@class,' " + hint
    // + "') or @itemprop='" + hint + "' or @id='" + hint + "']");
    //
    // if (!mainNodes.isEmpty()) {
    // mainNodeXPath = PageAnalyzer.constructXPath(mainNodes.get(0));
    // }
    //
    // if (mainNodes.size() > 1) {
    // mainNodeXPath = XPathHelper.getParentXPath(mainNodeXPath);
    // }
    //
    // if (!mainNodeXPath.isEmpty()) {
    // System.out.println("found main node with hint: " + hint);
    // break;
    // }
    // }
    //
    // return mainNodeXPath;
    // }

    private Node getMainContentNodeWithHints() {

        Node mainNode = null;

        for (String hint : MAIN_NODE_HINTS) {
            List<Node> mainNodes = XPathHelper.getXhtmlNodes(getDocument(),
                    "//*[(self::xhtml:div) or (self::xhtml:p) or (self::xhtml:span)][@class='" + hint
                            + "' or contains(@class,'" + hint + " ') or contains(@class,' " + hint
                            + "') or @itemprop='" + hint + "' or @id='" + hint + "']");

            if (!mainNodes.isEmpty()) {
                mainNode = mainNodes.get(0);
                if (mainNodes.size() > 1) {
                    mainNode = mainNode.getParentNode();
                }
            }

            // mainNode = XPathHelper.getXhtmlNode(getDocument(),
            // "//*[(self::xhtml:div or self::xhtml:p or self:xhtml:span)][@class='"
            // + hint + "' or @id='" + hint + "']");
            if (mainNode != null) {
                System.out.println("found main node with hint: " + hint);
                // System.out.println(HtmlHelper.getInnerXml(mainNode));
                break;
            }
        }

        return mainNode;
    }

    private String addHeadlineSiblings(String xPath) {
        try {
            String[] parts = xPath.split("/");
            String lastPart = parts[parts.length - 1];
            String xhtmlNs = "";
            if (lastPart.contains("xhtml")) {
                xhtmlNs = "xhtml:";
            }
            String newLastPart = "*[(self::" + lastPart + ") or (self::" + xhtmlNs + "h1) or (self::" + xhtmlNs
                    + "h2) or (self::" + xhtmlNs + "h3) or (self::" + xhtmlNs + "h4) or (self::" + xhtmlNs
                    + "h5) or (self::" + xhtmlNs + "h6) or (self::" + xhtmlNs + "span) or (self::" + xhtmlNs
                    + "ul) or (self::" + xhtmlNs + "ol) or (self::" + xhtmlNs + "blockquote)]";
            xPath = xPath.replaceAll(lastPart + "$", newLastPart);
        } catch (Exception e) {
        }

        return xPath;
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
        String imgXPath = ".//img";
        // if (XPathHelper.hasXhtmlNs(document)) {
        // imgXPath = XPathHelper.addXhtmlNsToXPath(imgXPath);
        // }

        List<Node> imageNodes = XPathHelper.getXhtmlNodes(imageParentNode, imgXPath);
        for (Node node : imageNodes) {
            try {

                WebImage webImage = new WebImage();

                NamedNodeMap nnm = node.getAttributes();
                String imageUrl = nnm.getNamedItem("src").getTextContent();

                if (!imageUrl.startsWith("http")) {
                    imageUrl = UrlHelper.makeFullUrl(getDocument().getDocumentURI(), null, imageUrl);
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
            size = (int)(0.01 * Integer.parseInt(attributeText) * DEFAULT_IMAGE_CONTAINER_SIZE);
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
        // System.out.println(pe.getResultText());
        // CollectionHelper.print(pe.getImages());

        PalladianContentExtractor pe = new PalladianContentExtractor();
        // WebPageContentExtractor pe2 = new ReadabilityContentExtractor();
        // pe.setDocument("http://jezebel.com/5733078/can-you-wear-six-items-or-less");
        // pe.setDocument("http://www.seobook.com/shopping-search");
        // pe.setDocument("http://www.fourhourworkweek.com/blog/2012/11/24/the-4-hour-chef-launch-summary-of-week-one/");
        // pe.setDocument("http://www.latimes.com/news/nationworld/world/la-fg-israel-gaza-20121120,0,4042611.story");

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
        // article-body
        // pe.setDocument("http://www.dailykos.com/story/2012/11/28/1165435/-Pat-Robertson-pulls-the-rug-out-from-under-Marco-Rubio");
        // itemprop="articleBody"
        // pe.setDocument("http://www.digitalspy.co.uk/movies/news/a441646/django-unchained-trailer-quentin-tarantino-movie-debuts-final-teaser.html");
        // pe.setDocument("http://www.dailyfinance.com/2012/07/20/stockbroker-corrupt-wall-street-cheats/");
        pe.setDocument("page073.html");
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

        // CollectionHelper.print(pe.setDocument("http://www.bbc.co.uk/news/science-environment-12209801").getImages());
        System.out.println("Title:" + pe.getResultTitle());
        System.out.println("Result Text: " + pe.getResultText());
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