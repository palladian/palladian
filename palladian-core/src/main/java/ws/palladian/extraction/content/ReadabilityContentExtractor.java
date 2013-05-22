package ws.palladian.extraction.content;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.nlp.StringHelper;

// possible improvements:
// TODO add frame handling? -> low priority
// TODO paging detection + appending of all following pages ... I just discovered, that newer versions of the script
// implement this functionality. I will try to port this when a have time. -- Philipp.
// TODO weight negative indicators more than positives, found two examples, where this would improve results
// - http://crimewatch.gaeatimes.com/2010/08/29/two-police-officers-among-12-killed-in-chechnya-46005/
// - http://www.codinghorror.com/blog/2010/05/on-working-remotely.html
// need to test this extensively though
/**
 * <p>
 * A quick <s>and dirty</s> port of the JavaScript browser bookmarklet "Readability" by Arc90 -- a great tool for
 * extracting content from HTML pages. <i>"Readability [...] takes a crack at wiping out all that junk so you can have a
 * more enjoyable reading experience. [...] its success rate is pretty respectable (we'd guess over 90% of web sites are
 * handled properly)"</i>.
 * </p>
 * 
 * <p>
 * Note, that this is not designed for front pages like <a href="http://cnn.com">http://cnn.com</a>, but for articles
 * and blog entries with one topic. The result should be just the actual content, without irrelevant elements like
 * navigation menus, headers, footers, ads, etc.
 * </p>
 * 
 * <p>
 * How it works, in a nutshell: Readability operates on the document's DOM tree. Basically, it assigns all elements a
 * score for their contents. Metrics for the scoring are length of their text content, number of commas and link
 * density. Also, "class" and "id" names are taken into consideration; for example, elements with class name "sidebar"
 * contain unlikely actual content in contrast to elements with class "article". After the top element has been
 * determined, the algorithm also checks its siblings whether they contain content, too.
 * </p>
 * 
 * <p>
 * Score on boilerplate dataset: 0.9090505 (r1505);
 * </p>
 * 
 * @version Based on: SVN r152, Jun 28, 2010
 * 
 * @see <a href="http://lab.arc90.com/experiments/readability">Website</a>
 * @see <a href="http://code.google.com/p/arc90labs-readability">JavaScript Source</a>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * 
 */
public class ReadabilityContentExtractor extends WebPageContentExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadabilityContentExtractor.class);

    /** name of attribute for storing readability values in DOM elements */
    private static final String READABILITY_ATTR = "readability";

    /**
     * All of the regular expressions in use within readability. Defined up here so we don't instantiate them repeatedly
     * in loops.
     **/
    private static final Pattern UNLIKELY_CANDIDATES_RE = Pattern.compile("combx|comment|community|disqus|extra|foot|header|legal|menu|remark|rss|shoutbox|sidebar|sponsor|ad-break|agegate|pagination|pager|popup", Pattern.CASE_INSENSITIVE);
    private static final Pattern OK_MAYBE_ITS_A_CANDIDATE_RE = Pattern.compile("and|article|body|column|main|shadow", Pattern.CASE_INSENSITIVE);
    private static final Pattern POSITIVE_RE = Pattern.compile("article|body|content|entry|hentry|main|page|pagination|post|text|blog|story", Pattern.CASE_INSENSITIVE);
    private static final Pattern NEGATIVE_RE = Pattern.compile("combx|comment|com-|contact|foot|footer|footnote|link|masthead|media|meta|outbrain|promo|related|scroll|shoutbox|sidebar|sponsor|shopping|tags|tool|widget", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIV_TO_P_ELEMENTS_RE = Pattern.compile("<(a|blockquote|dl|div|img|ol|p|pre|table|ul)", Pattern.CASE_INSENSITIVE);
    // private static final Pattern trimRe = Pattern.compile("^\\s+|\\s+$");
    private static final Pattern NORMALIZE_RE = Pattern.compile("\\s{2,}");
    private static final Pattern VIDEO_RE = Pattern.compile("http:\\/\\/(www\\.)?(youtube|vimeo)\\.com", Pattern.CASE_INSENSITIVE);

    /** The original document. */
    private Document document;

    /** The filtered and result document. */
    private Document resultNode;

    private boolean weightClasses;

    private boolean stripUnlikelyCandidates;

    private boolean cleanConditionally;

    private boolean writeDump = false;

    /* (non-Javadoc)
     * @see ws.palladian.preprocessing.scraping.ContentExtractorInterface#setDocument(org.w3c.dom.Document)
     */
    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {
        this.document = document;
        stripUnlikelyCandidates = true;
        weightClasses = true;
        cleanConditionally = true;
        this.resultNode = init(document);
        return this;
    }

    /* (non-Javadoc)
     * @see ws.palladian.preprocessing.scraping.ContentExtractorInterface#getResultDocument()
     */
    @Override
    public Node getResultNode() {
        return resultNode;
    }

    /* (non-Javadoc)
     * @see ws.palladian.preprocessing.scraping.ContentExtractorInterface#getResultText()
     */
    @Override
    public String getResultText() {
        String result = HtmlHelper.documentToReadableText(getResultNode());
        return result;
    }

    /* (non-Javadoc)
     * @see ws.palladian.preprocessing.scraping.ContentExtractorInterface#getImages()
     */
    //    @Override
    //	public List<String> getImages() {
    //        List<String> imageURLs = new ArrayList<String>();
    //
    //        String baseURL = document.getDocumentURI();
    //
    //        // PageAnalyzer.printDOM(resultDocument, "");
    //
    //        // we need to query the result document with an xpath but the name space check has to be done on the original
    //        // document
    //        String imgXPath = "//img";
    //        if (XPathHelper.hasXhtmlNs(document)) {
    //            imgXPath = XPathHelper.addXhtmlNsToXPath(imgXPath);
    //        }
    //
    //        List<Node> imageNodes = XPathHelper.getNodes(getResultDocument(), imgXPath);
    //        for (Node node : imageNodes) {
    //            try {
    //                String imageURL = node.getAttributes().getNamedItem("src").getTextContent();
    //
    //                if (!imageURL.startsWith("http")) {
    //                    imageURL = baseURL + imageURL;
    //                }
    //
    //                imageURLs.add(imageURL);
    //            } catch (NullPointerException e) {
    //                LOGGER.warn("an image has not all necessary attributes");
    //            }
    //        }
    //
    //        return imageURLs;
    //    }

    /* (non-Javadoc)
     * @see ws.palladian.preprocessing.scraping.ContentExtractorInterface#getResultText(java.lang.String)
     */

    /* (non-Javadoc)
     * @see ws.palladian.preprocessing.scraping.ContentExtractorInterface#getResultTitle()
     */
    @Override
    public String getResultTitle() {
        return getArticleTitle(document);
    }

    /**
     * Enable to write dumps of the DOM document with calculated weight.
     * 
     * @param writeDump
     */
    public void setWriteDump(boolean writeDump) {
        this.writeDump = writeDump;
    }

    public boolean isWriteDump() {
        return writeDump;
    }
    
    @Override
    public String getExtractorName() {
        return "Readability";
    }

    // ///////////////////////////////////////////////////////////////////////////////
    //
    // The following private methods are ported from the JavaScript code
    //
    //
    // As far as possible, the variable names, method signatures and code
    // comments are taken directly from the JavaScript. All in all I tried
    // to stick to the structure of the original JavaScript as closely as
    // possible, instead of producing highly efficient and optimized code.
    // I hope this will ease synchronizations with future updates of the script.
    //
    // ///////////////////////////////////////////////////////////////////////////////

    /**
     * Runs readability.
     * 
     * Workflow:<br>
     * 1. Prep the document by removing script tags, css, etc.<br>
     * 2. Build readability's DOM tree.<br>
     * 3. Grab the article content from the current dom tree.<br>
     * 4. Replace the current DOM tree with the new one.<br>
     * 5. Read peacefully.
     * 
     * @return void
     **/
    private Document init(Document document) throws PageContentExtractorException {

        // Cache original document by creating a copy. This is necessary, as we
        // operate destructively, directly on the Document and we might need
        // multiple runs with different parameters.
        // -- Philipp.
        Document cache = HtmlHelper.cloneDocument(document);
        if (cache == null) {
            throw new PageContentExtractorException("caching the original document failed.");
        }

        Document result = grabArticle(cache);

        // write Document's dump to disk, using time stamped file name
        if (isWriteDump()) {
            String filename = "dumps/pageContentExtractor" + System.currentTimeMillis() + ".xml";
            HtmlHelper.writeToFile(cache, new File(filename));
            LOGGER.info("wrote dump to {}", filename);
        }

        /**
         * If we attempted to strip unlikely candidates on the first run through, and we ended up with no content, that
         * may mean we stripped out the actual
         * content so we couldn't parse it. So re-run init while preserving unlikely candidates to have a better shot at
         * getting our content out properly.
         **/
        if (result == null || getInnerText(result.getDocumentElement(), false).length() < 250) {
            if (stripUnlikelyCandidates) {
                stripUnlikelyCandidates = false;
                LOGGER.debug("re-running without stripping unlikely candidates");
                result = init(document);
            } else if (weightClasses) {
                weightClasses = false;
                LOGGER.debug("re-running without class weigths");
                result = init(document);
            } else if (cleanConditionally) {
                cleanConditionally = false;
                LOGGER.debug("re-running without conditional cleaning");
                result = init(document);
            } else {
                LOGGER.debug("looks like I could not parse this page for content (result looks too short)");
                // do we really need to throw an exception? better return result, which *might* be too short.
                // throw new PageContentExtractorException("document parsing failed, result looks too short");
            }
        }

        // postprocessing, replace <p style="display:inline"> with normal Text nodes again -- Philipp.
        NodeList pElements = result.getElementsByTagName("p");
        for (int i = pElements.getLength() - 1; i >= 0; i--) {
            Element element = (Element) pElements.item(i);
            if (element.getAttribute("style").equals("display:inline")) {
                Text textNode = result.createTextNode(element.getTextContent());
                element.getParentNode().replaceChild(textNode, element);
            }
        }

        // strip out class+readability attributes, as we dont need them
        NodeList elements = result.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            element.removeAttribute("class");
            element.removeAttribute(READABILITY_ATTR);
        }
        
        return result;
    }

    /**
     * Get the article title as an H1.
     * 
     * @return void
     **/
    private String getArticleTitle(Document document) {

        String curTitle = "";
        String origTitle = "";

        NodeList titleElements = document.getElementsByTagName("title");
        if (titleElements.getLength() == 1) {
            curTitle = origTitle = getInnerText((Element) titleElements.item(0));
        }

        if (Pattern.compile(" [\\|\\-] ").matcher(curTitle).find()) {
            curTitle = origTitle.replaceAll("(.*)[\\|\\-] .*", "$1");
            if (curTitle.split(" ").length < 3) {
                curTitle = origTitle.replaceAll("[^\\|\\-]*[\\|\\-](.*)", "$1");
            }
        } else if (curTitle.indexOf(": ") != -1) {
            curTitle = origTitle.replaceAll(".*:/(.*)", "$1");
            if (curTitle.split(" ").length < 3) {
                curTitle = origTitle.replaceAll("[^:]*[:](.*)", "$1");
            }
        } else if (curTitle.length() > 150 || curTitle.length() < 15) {
            NodeList hOnes = document.getElementsByTagName("h1");
            if (hOnes.getLength() == 1) {
                curTitle = getInnerText((Element) hOnes.item(0));
            }
        }

        // curTitle = trimRe.matcher(curTitle).replaceAll("");
        curTitle = curTitle.trim();

        if (curTitle.split(" ").length <= 4) {
            curTitle = origTitle;
        }

        return curTitle;
    }

    /**
     * Prepare the HTML document for readability to scrape it. This includes things like stripping javascript, CSS, and
     * handling terrible markup.
     * 
     * @return void
     **/
    private void prepDocument(Document document) {

        // this method is pretty simplified in comparison to the JavaScript
        // TODO handling of frames is missing -- but do we really still need this in 2010? (:
        // TODO Turn all double br's into p's
        HtmlHelper.removeAll(document, Node.ELEMENT_NODE, "script");
        HtmlHelper.removeAll(document, Node.ELEMENT_NODE, "style");
        HtmlHelper.removeAll(document, Node.COMMENT_NODE);

        // I did not port the addFootnotes functionality from r138 which converts
        // links to footnotes, as I don't think this makes much sense for our
        // use cases.

        cleanStyles(document.getDocumentElement());
    }

    /**
     * Prepare the article node for display. Clean out any inline styles, iframes, forms, strip extraneous
     * <p>
     * tags, etc.
     * 
     * @param Element
     * @return void
     **/
    private void prepArticle(Element articleContent) {

        // cleanStyles(articleContent); moved to prepDocument();
        // TODO readability.killBreaks(articleContent);

        /* Clean out junk from the article content */
        cleanConditionally(articleContent, "form");
        clean(articleContent, "object");
        clean(articleContent, "h1");

        // TODO experimental, remove noscript tag; for test case see
        // Mail David, 2010-10-14 & http://www.bbc.co.uk/news/world-europe-11539758
        clean(articleContent, "noscript");

        /**
         * If there is only one h2, they are probably using it as a header and not a subheader, so remove it since we
         * already have a header.
         ***/
        if (articleContent.getElementsByTagName("h2").getLength() == 1) {
            clean(articleContent, "h2");
        }

        clean(articleContent, "iframe");

        cleanHeaders(articleContent);

        /* Do these last as the previous stuff may have removed junk that will affect these */
        cleanConditionally(articleContent, "table");
        cleanConditionally(articleContent, "ul");
        cleanConditionally(articleContent, "div");

        /* Remove extra paragraphs */
        NodeList articleParagraphs = articleContent.getElementsByTagName("p");
        for (int i = articleParagraphs.getLength() - 1; i >= 0; i--) {
            Element currentParagraph = (Element) articleParagraphs.item(i);
            int imgCount = currentParagraph.getElementsByTagName("img").getLength();
            int embedCount = currentParagraph.getElementsByTagName("embed").getLength();
            int objectCount = currentParagraph.getElementsByTagName("object").getLength();

            if (imgCount == 0 && embedCount == 0 && objectCount == 0
                    && getInnerText(currentParagraph, false).length() == 0) {
                currentParagraph.getParentNode().removeChild(currentParagraph);
            }
        }

        // TODO
        // try {
        // articleContent.innerHTML = articleContent.innerHTML.replace(/<br[^>]*>\s*<p/gi, '<p');
        // } catch (e) {
        // dbg("Cleaning innerHTML of breaks failed. This is an IE strict-block-elements bug. Ignoring.: " + e);
        // }
    }

    /**
     * Initialize a node with the readability object. Also checks the className/id for special names to add to its
     * score.
     * 
     * @param Element
     * @return void
     **/
    private void initializeNode(Element node) {

        String tagName = node.getTagName().toLowerCase();
        int contentScore = 0;

        if (tagName.equals("div")) {
            contentScore += 5;
        } else if (Arrays.asList("pre", "td", "blockquote").contains(tagName)) {
            contentScore += 3;
        } else if (Arrays.asList("address", "ol", "ul", "dl", "dd", "dt", "li", "form").contains(tagName)) {
            contentScore -= 3;
        } else if (Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6", "th").contains(tagName)) {
            contentScore -= 5;
        }

        contentScore += getClassIdWeight(node);

        setReadability(node, contentScore);

    }

    /***
     * grabArticle - Using a variety of metrics (content score, classname, element types), find the content that is most
     * likely to be the stuff a user wants to
     * read. Then return it wrapped up in a div.
     * 
     * @return Element
     **/
    private Document grabArticle(Document document) {

        // moved here -- Philipp.
        prepDocument(document);

        /**
         * First, node prepping. Trash nodes that look cruddy (like ones with the class name "comment", etc), and turn
         * divs into P tags where they have been
         * used inappropriately (as in, where they contain no other block level elements.)
         * 
         * Note: Assignment from index for performance. See
         * http://www.peachpit.com/articles/article.aspx?p=31567&seqNum=5
         * 
         * todo: Shouldn't this be a reverse traversal?
         **/
        List<Element> nodesToScore = new LinkedList<Element>();
        NodeList allElements = document.getElementsByTagName("*");
        for (int nodeIndex = 0; nodeIndex < allElements.getLength(); nodeIndex++) {
            Element node = (Element) allElements.item(nodeIndex);
            /* Remove unlikely candidates */
            if (stripUnlikelyCandidates) {
                String unlikelyMatchString = node.getAttribute("class") + node.getAttribute("id");
                if (UNLIKELY_CANDIDATES_RE.matcher(unlikelyMatchString).find()
                        && !OK_MAYBE_ITS_A_CANDIDATE_RE.matcher(unlikelyMatchString).find()
                        && !node.getTagName().equalsIgnoreCase("body")) {
                    LOGGER.debug("Removing unlikely candidate - {}", unlikelyMatchString);
                    node.getParentNode().removeChild(node);
                    nodeIndex--;
                    continue;
                }
            }

            if (node.getTagName().equalsIgnoreCase("p") || node.getTagName().equalsIgnoreCase("td")) {
                nodesToScore.add(node);
            }

            /* Turn all divs that don't have children block level elements into p's */
            if (node.getTagName().equalsIgnoreCase("div")) {
                if (!DIV_TO_P_ELEMENTS_RE.matcher(HtmlHelper.getInnerXml(node)).find()) {
                    LOGGER.debug("Altering div to p");
                    document.renameNode(node, node.getNamespaceURI(), "p");
                    nodeIndex--;
                    nodesToScore.add(node);
                } else {
                    // EXPERIMENTAL
                    for (int i = 0; i < node.getChildNodes().getLength(); i++) {
                        Node childNode = node.getChildNodes().item(i);
                        if (childNode.getNodeType() == Node.TEXT_NODE &&
                                // added by Philipp to prevent creation of empty p nodes
                                childNode.getTextContent().trim().length() > 0) {
                            LOGGER.debug("replacing text node with a p tag with the same content.");
                            Element p = document.createElement("p");
                            p.setAttribute("style", "display:inline");
                            p.setTextContent(childNode.getTextContent());
                            childNode.getParentNode().replaceChild(p, childNode);
                        }
                    }
                }
            }
        }

        /**
         * Loop through all paragraphs, and assign a score to them based on how content-y they look. Then add their
         * score to their parent node.
         * 
         * A score is determined by things like number of commas, class names, etc. Maybe eventually link density.
         **/
        List<Element> candidates = new LinkedList<Element>();
        for (Element nodeToScore : nodesToScore) {
            Node parentNode = nodeToScore.getParentNode();

            if (parentNode == null) {
                continue;
            }

            Node grandParentNode = parentNode.getParentNode();

            String innerText = getInnerText(nodeToScore);

            /* If this paragraph is less than 25 characters, don't even count it. */
            if (innerText.length() < 25) {
                continue;
            }

            int contentScore = 0;

            /* Add a point for the paragraph itself as a base. */
            contentScore++;

            /* Add points for any commas within this paragraph */
            contentScore += innerText.split(",").length;

            /* For every 100 characters in this paragraph, add another point. Up to 3 points. */
            contentScore += Math.min(Math.floor((float) innerText.length() / 100), 3);

            // if parent is ELEMENT_NODE, initialize readability and add score -- Philipp.
            if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element parentElement = (Element) parentNode;
                if (!hasReadability(parentElement)) {
                    initializeNode(parentElement);
                    candidates.add(parentElement);
                }
                setReadability(parentElement, getReadability(parentElement) + contentScore);
            }

            // if grandparent is ELEMENT_NODE, initialize readability, add half of the score -- Philipp.
            if (grandParentNode != null && grandParentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element grandParentElement = (Element) grandParentNode;
                if (!hasReadability(grandParentElement)) {
                    initializeNode(grandParentElement);
                    candidates.add(grandParentElement);
                }
                setReadability(grandParentElement, getReadability(grandParentElement) + contentScore / (float) 2);
            }

        }

        /**
         * After we've calculated scores, loop through all of the possible candidate nodes we found and find the one
         * with the highest score.
         **/
        Element topCandidate = null;
        for (Element candidate : candidates) {
            /**
             * Scale the final candidates score based on link density. Good content should have a relatively small link
             * density (5% or less) and be mostly
             * unaffected by this operation.
             **/
            float contentScore = getReadability(candidate);
            contentScore = contentScore * (1 - getLinkDensity(candidate));
            setReadability(candidate, contentScore);

            LOGGER.debug("Candidate: {} ({}:{}) with score {}",
                    new Object[] {candidate, candidate.getAttribute("class"), candidate.getAttribute("id"),
                            contentScore});

            if (topCandidate == null || contentScore > getReadability(topCandidate)) {
                topCandidate = candidate;
            }
        }

        /**
         * If we still have no top candidate, just use the body as a last resort. We also have to copy the body node so
         * it is something we can modify.
         **/
        if (topCandidate == null) {
            LOGGER.debug("No top candidate found, using the body");
            NodeList bodyNodes = document.getElementsByTagName("body");
            if (bodyNodes.getLength() > 0) {
                topCandidate = (Element)bodyNodes.item(0);
                document.renameNode(topCandidate, topCandidate.getNamespaceURI(), "div");
            } else {
                return null;
            }
        }

        /**
         * Now that we have the top candidate, look through its siblings for content that might also be related. Things
         * like preambles, content split by ads
         * that we removed, etc.
         **/
        // create result Document
        Document result = HtmlHelper.createDocument();
        Element html = result.createElementNS("http://www.w3.org/1999/xhtml", "html");
        result.appendChild(html);
        Element articleContent = result.createElement("body");
        html.appendChild(articleContent);

        float siblingScoreThreshold = Math.max(10, Float.valueOf(getReadability(topCandidate)) * (float) 0.2);
        NodeList siblingNodes = topCandidate.getParentNode().getChildNodes();
        for (int s = 0; s < siblingNodes.getLength(); s++) {
            if (!(siblingNodes.item(s).getNodeType() == Node.ELEMENT_NODE)) {
                continue;
            }
            Element siblingNode = (Element) siblingNodes.item(s);
            boolean append = false;

            LOGGER.debug("Looking at sibling node: {} ({}:{}) {}",
                    new Object[] {siblingNode, siblingNode.getAttribute("class"), siblingNode.getAttribute("id"),
                            (hasReadability(siblingNode) ? " with score " + getReadability(siblingNode) : "")});

            if (siblingNode == topCandidate) {
                append = true;
            }

            int contentBonus = 0;
            /* Give a bonus if sibling nodes and top candidates have the example same classname */
            if (topCandidate.getAttribute("class").length() > 0
                    && siblingNode.getAttribute("class").equals(topCandidate.getAttribute("class"))) {
                contentBonus += getReadability(topCandidate) * 0.2;
            }

            if (hasReadability(siblingNode) && getReadability(siblingNode) + contentBonus >= siblingScoreThreshold) {
                append = true;
            }

            if (siblingNode.getNodeName().equalsIgnoreCase("p")) {
                float linkDensity = getLinkDensity(siblingNode);
                String nodeContent = getInnerText(siblingNode);
                int nodeLength = nodeContent.length();

                if (nodeLength > 80 && linkDensity < 0.25) {
                    append = true;
                } else if (nodeLength < 80 && linkDensity == 0
                        && Pattern.compile("\\.( |$)").matcher(nodeContent).find()) {
                    append = true;
                }
            }

            if (append) {
                LOGGER.debug("Appending node: {}", siblingNode);

                Element nodeToAppend;
                if (!siblingNode.getNodeName().equalsIgnoreCase("div")
                        && !siblingNode.getNodeName().equalsIgnoreCase("p")) {
                    /*
                     * We have a node that isn't a common block level element, like a form or td tag. Turn it into a div
                     * so it doesn't get filtered out later by
                     * accident.
                     */
                    LOGGER.debug("Altering siblingNode of {} to div.", siblingNode.getNodeName());
                    nodeToAppend = (Element) document.renameNode(siblingNode, siblingNode.getNamespaceURI(), "div");
                } else {
                    nodeToAppend = siblingNode;
                }

                /* To ensure a node does not interfere with readability styles, remove its classnames */
                // nodeToAppend.removeAttribute("class");

                /* Append sibling and subtract from our list because it removes the node when you append to another node */
                // in our case we copy the sibling to the new document, so no need for any subtraction from counters --
                // Philipp.
                Node nodeToAppendToResult = result.importNode(nodeToAppend, true);
                articleContent.appendChild(nodeToAppendToResult);
            }
        }

        /**
         * So we have all of the content that we need. Now we clean it up for presentation.
         **/
        prepArticle(articleContent);

        return result;
    }

    /**
     * Get the inner text of a node - cross browser compatibly. This also strips out any excess whitespace to be found.
     * 
     * @param Element
     * @return string
     **/
    private String getInnerText(Element e) {
        return getInnerText(e, true);
    }

    private String getInnerText(Element e, boolean normalizeSpaces) {

        // String textContent = trimRe.matcher(e.getTextContent()).replaceAll("");
        String textContent = e.getTextContent().trim();

        if (normalizeSpaces) {
            textContent = NORMALIZE_RE.matcher(textContent).replaceAll(" ");
        }

        return textContent;
    }

    /**
     * Remove the style attribute on every e and under.
     * 
     * todo: Test if getElementsByTagName(*) is faster.
     * 
     * @param Element
     * @return void
     **/
    private void cleanStyles(Element e) {
        Node cur = e.getFirstChild();

        if (cur == null) {
            return;
        }

        // Remove any root styles, if we're able.
        e.removeAttribute("style");

        // Go until there are no more child nodes
        while (cur != null) {
            if (cur.getNodeType() == Node.ELEMENT_NODE) {
                // Remove style attribute(s) :
                Element curElement = (Element) cur;
                curElement.removeAttribute("style");
                cleanStyles(curElement);
            }
            cur = cur.getNextSibling();
        }

    }

    /**
     * Get the density of links as a percentage of the content This is the amount of text that is inside a link divided
     * by the total text in the node.
     * 
     * @param Element
     * @return number (float)
     **/
    private float getLinkDensity(Element e) {

        NodeList links = e.getElementsByTagName("a");
        int textLength = getInnerText(e).length();
        int linkLength = 0;
        for (int i = 0; i < links.getLength(); i++) {
            Element linkElement = (Element) links.item(i);
            linkLength += getInnerText(linkElement).length();
        }
        float linkDensity = textLength != 0 ? (float) linkLength / textLength : 0;

        return linkDensity;
    }

    /**
     * Get an elements class/id weight. Uses regular expressions to tell if this element looks good or bad.
     * 
     * @param Element
     * @return number (Integer)
     **/
    private int getClassIdWeight(Element e) {

        if (!weightClasses) {
            return 0;
        }

        int weight = 0;

        /* Look for a special classname */
        if (e.hasAttribute("class")) {
            if (NEGATIVE_RE.matcher(e.getAttribute("class")).find()) {
                weight -= 25;
                // weight -= 50; // TODO could improve accuracy, see TODOnote above.
            }

            if (POSITIVE_RE.matcher(e.getAttribute("class")).find()) {
                weight += 25;
            }
        }

        /* Look for a special ID */
        if (e.hasAttribute("id")) {
            if (NEGATIVE_RE.matcher(e.getAttribute("id")).find()) {
                weight -= 25;
            }

            if (POSITIVE_RE.matcher(e.getAttribute("id")).find()) {
                weight += 25;
            }
        }

        return weight;
    }

    /**
     * Clean a node of all elements of type "tag". (Unless it's a youtube/vimeo video. People love movies.)
     * 
     * @param Element
     * @param string tag to clean
     * @return void
     **/
    private void clean(Element e, String tag) {

        NodeList targetList = e.getElementsByTagName(tag);
        boolean isEmbed = tag.equalsIgnoreCase("object") || tag.equalsIgnoreCase("embed");

        for (int y = targetList.getLength() - 1; y >= 0; y--) {
            /* Allow youtube and vimeo videos through as people usually want to see those. */
            Node item = targetList.item(y);

            if (isEmbed) {
                StringBuilder attributeValues = new StringBuilder();

                for (int i = 0; i < item.getAttributes().getLength(); i++) {
                    attributeValues.append(item.getAttributes().item(i).getTextContent() + "|");
                }

                /* First, check the elements attributes to see if any of them contain youtube or vimeo */
                if (VIDEO_RE.matcher(attributeValues).find()) {
                    continue;
                }

                /* Then check the elements inside this element for the same. */
                if (VIDEO_RE.matcher(item.getTextContent()).find()) {
                    continue;
                }

            }
            item.getParentNode().removeChild(item);
        }
    }

    /**
     * Clean an element of all tags of type "tag" if they look fishy. "Fishy" is an algorithm based on content length,
     * classnames, link density, number of
     * images & embeds, etc.
     * 
     * @return void
     **/
    private void cleanConditionally(Element e, String tag) {

        if (!cleanConditionally) {
            return;
        }

        NodeList tagsList = e.getElementsByTagName(tag);
        int curTagsLength = tagsList.getLength();

        /**
         * Gather counts for other typical elements embedded within. Traverse backwards so we can remove nodes at the
         * same time without effecting the traversal.
         * 
         * todo: Consider taking into account original contentScore here.
         **/
        for (int i = curTagsLength - 1; i >= 0; i--) {
            Element element = (Element) tagsList.item(i);

            int weight = getClassIdWeight(element);
            float contentScore = getReadability(element);

            LOGGER.debug("Cleaning Conditionally {} ({}:{}) {}", new Object[] {element, element.getAttribute("class"),
                    element.getAttribute("id"),
                    (hasReadability(element) ? " with score " + getReadability(element) : "")});

            if (weight + contentScore < 0) {
                element.getParentNode().removeChild(element);
            } else if (StringHelper.countOccurrences(element.getTextContent(), ",") < 10) {
                /**
                 * If there are not very many commas, and the number of non-paragraph elements is more than paragraphs
                 * or other ominous signs, remove the
                 * element.
                 **/
                int p = element.getElementsByTagName("p").getLength();
                int img = element.getElementsByTagName("img").getLength();
                int li = element.getElementsByTagName("li").getLength() - 100;
                int input = element.getElementsByTagName("input").getLength();

                int embedCount = 0;
                NodeList embeds = element.getElementsByTagName("embed");
                for (int ei = 0; ei < embeds.getLength(); ei++) {
                    Element embedElement = (Element) embeds.item(ei);
                    if (VIDEO_RE.matcher(embedElement.getAttribute("src")).find()) {
                        embedCount++;
                    }
                }

                float linkDensity = getLinkDensity(element);
                int contentLength = getInnerText(element).length();
                boolean toRemove = false;

                if (img > p) {
                    toRemove = true;
                } else if (li > p && !tag.equalsIgnoreCase("ul") && !tag.equalsIgnoreCase("ol")) {
                    toRemove = true;
                } else if (input > Math.floor(p / (double) 3)) {
                    toRemove = true;
                } else if (contentLength < 25 && (img == 0 || img > 2)) {
                    toRemove = true;
                } else if (weight < 25 && linkDensity > 0.2) {
                    toRemove = true;
                } else if (weight >= 25 && linkDensity > 0.5) {
                    toRemove = true;
                } else if (embedCount == 1 && contentLength < 75 || embedCount > 1) {
                    toRemove = true;
                }

                if (toRemove) {
                    element.getParentNode().removeChild(element);
                }

            }
        }
    }

    /**
     * Clean out spurious headers from an Element. Checks things like classnames and link density.
     * 
     * @param Element
     * @return void
     **/
    private void cleanHeaders(Element e) {
        for (int headerIndex = 1; headerIndex < 7; headerIndex++) {
            NodeList headers = e.getElementsByTagName("h" + headerIndex);
            for (int i = headers.getLength() - 1; i >= 0; i--) {
                Element current = (Element) headers.item(i);
                if (getClassIdWeight(current) < 0 || getLinkDensity(current) > 0.33) {
                    current.getParentNode().removeChild(current);
                }
            }
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // additional internal convenience methods
    // ////////////////////////////////////////////////////////////////////////

    /**
     * setting and getting the readability attribute to the DOM Elements
     */
    private void setReadability(Element element, float readability) {
        element.setAttribute(READABILITY_ATTR, String.valueOf(readability));
    }

    private boolean hasReadability(Element element) {
        return element.hasAttribute(READABILITY_ATTR);
    }

    private float getReadability(Element element) {
        if (!hasReadability(element)) {
            return 0;
        }
        return Float.valueOf(element.getAttribute(READABILITY_ATTR));
    }

    /**
     * Usage example for the book.
     * @throws Exception
     */
    @SuppressWarnings("unused")
    public static void usageExample() throws Exception {

        WebPageContentExtractor extractor = new ReadabilityContentExtractor();

        // this method is heavily overloaded and accepts various types of input
        String url = "http://www.wired.com/gadgetlab/2010/05/iphone-4g-ads/";
        extractor.setDocument(url);

        // get the main content as text representation
        String contentText = extractor.getResultText();

        // get the main content as DOM representation

        // Node contentNode = extractor.getResultNode();

        // get the title
        String title = extractor.getResultTitle();

    }


    // private Document createResultDocument() {
    // Document result = Helper.createDocument();
    //
    // Element html = result.createElementNS("http://www.w3.org/1999/xhtml", "html");
    // result.appendChild(html);
    //
    // Element body = result.createElement("body");
    // html.appendChild(body);
    //
    // // Element content = result.createElement("div");
    // // content.setAttribute("id", "readability-content");
    // // content.setIdAttribute("id", true);
    // // body.appendChild(content);
    //
    // return result;
    // }

//    // //////////////////////////////////////////////////////////////////////////
//    // main method for command line usage
//    // //////////////////////////////////////////////////////////////////////////
//    @SuppressWarnings("static-access")
//    public static void main(String[] args) throws Exception {
//
//        ReadabilityContentExtractor pageContentExtractor = new ReadabilityContentExtractor();
//        String outputfile = null;
//
//        CommandLineParser parser = new BasicParser();
//
//        Options options = new Options();
//        options.addOption(OptionBuilder.withLongOpt("dump").withDescription("write dump of parsed page").create());
//        options.addOption(OptionBuilder.withLongOpt("output").withDescription("save result to xml file").hasArg().withArgName("fileName").create());
//
//        try {
//
//            if (args.length < 1) {
//                // no arguments given, print usage help in catch clause.
//                throw new ParseException(null);
//            }
//
//            CommandLine cmd = parser.parse(options, args);
//
//            if (cmd.hasOption("dump")) {
//                pageContentExtractor.setWriteDump(true);
//            }
//            if (cmd.hasOption("output")) {
//                outputfile = cmd.getOptionValue("output");
//            }
//
//            if (cmd.getArgs().length == 1) {
//
//                pageContentExtractor.setDocument(cmd.getArgs()[0]);
//
//                System.out.println(pageContentExtractor.getResultTitle());
//                System.out.println("================================");
//                System.out.println(pageContentExtractor.getResultText());
//
//                if (outputfile != null) {
//                    HtmlHelper.writeToFile(pageContentExtractor.getResultNode(), new File(outputfile));
//                }
//
//
//            } else {
//                throw new ParseException(null);
//            }
//
//        } catch (ParseException e) {
//            // print usage help
//            HelpFormatter formatter = new HelpFormatter();
//            formatter.printHelp("PageContentExtractor [options] inputUrlOrFilePath", options);
//
//            WebPageContentExtractor extractor = new ReadabilityContentExtractor();
//
//            // this method is heavily overloaded and accepts various types of input
//            String url = "http://www.ccc.govt.nz/cityleisure/recreationsport/sportsrecreationguide/orienteering.aspx";
//            url = "http://www.abc.net.au/news/tag/cricket/";
//            url = "http://www.lynchburg.edu/equestrian.xml";
//            url = "http://manteno.govoffice.com/index.asp?Type=B_BASIC&SEC={0D9936D0-B3A8-4614-9140-4EAAACCDE62B}&DE={5C4D155C-AC28-409D-9CE8-87735F1AC462}";
//
//            extractor.setDocument(new URL(url));
//
//            // get the main content as text representation
//            String contentText = extractor.getResultText();
//
//            // get the title
//            String title = extractor.getResultTitle();
//
//            System.out.println("title: " + title);
//            System.out.println("content: " + contentText);
//        }
//
//    }


}
