package tud.iir.helper;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.log4j.Logger;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLDocument;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tud.iir.extraction.PageAnalyzer;
import tud.iir.knowledge.HTMLSymbols;

/**
 * Some HTML and XML/DOM specific helper methods.
 * 
 * @author David Urbansky
 * @author Martin Werner
 * @author Philipp Katz
 * @author Martin Gregor
 */
public class HTMLHelper {

    private static final Logger LOGGER = Logger.getLogger(HTMLHelper.class);

    /** HTML block level elements. */
    private static final List<String> BLOCK_ELEMENTS = Arrays.asList("address", "blockquote", "div", "dl", "fieldset",
            "form", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "noscript", "ol", "p", "pre", "table", "ul", "dd", "dt",
            "li", "tbody", "td", "tfoot", "th", "thead", "tr", "button", "del", "ins", "map", "object", "script");

    /** "Junk" elements which do not contain relevant content. */
    private static final List<String> IGNORE_INSIDE = Arrays.asList("script", "style");

    private static final Pattern NORMALIZE_LINES = Pattern.compile("^\\s+$|^[ \t]+|[ \t]+$", Pattern.MULTILINE);

    /** prevent instantiation. */
    private HTMLHelper() {
    }

    /**
     * Count the tags.
     * 
     * @param htmlText The html text.
     * @return The number of tags.
     */
    public static int countTags(String htmlText) {
        return countTags(htmlText, false);
    }

    /**
     * <p>
     * Count the number of characters used for tags in the given string.
     * </p>
     * <p>
     * For example, &lt;PHONE&gt;iphone 4&lt;/PHONE&gt; => 15
     * </p>
     * 
     * @param taggedText The text with tags.
     * @return The cumulated number of characters used for tags in the given text.
     */
    public static int countTagLength(String taggedText) {
        int totalTagLength = 0;

        Pattern pattern = Pattern.compile("<(.*?)>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(taggedText);
        while (matcher.find()) {
            totalTagLength += matcher.group(1).length() + 2;
        }

        return totalTagLength;
    }

    /**
     * Count tags.
     * 
     * @param htmlText The html text.
     * @param distinct If true, count multiple occurrences of the same tag only once.
     * @return The number of tags.
     */
    public static int countTags(String htmlText, boolean distinct) {
        Set<String> tags = new HashSet<String>();

        int tagCount = 0;

        Pattern pattern = Pattern.compile("(\\<.*?>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(htmlText);

        while (matcher.find()) {
            tagCount++;
            tags.add(matcher.group());
        }

        if (distinct) {
            tagCount = tags.size();
        }

        return tagCount;
    }

    /**
     * Lists all tags. Deletes arguments within the tags, if there are any.
     * 
     * @param htmlText The html text.
     * @return A list of tags.
     */
    public static List<String> listTags(String htmlText) {
        List<String> tags = new ArrayList<String>();


        List<String> lev = new ArrayList<String>();
        String currentTag="";

        Pattern pattern = Pattern.compile("(\\<.*?>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(htmlText);

        while (matcher.find()) {
            currentTag=matcher.group();

            // Delete arguments within the tags
            if (currentTag.contains(" ")) {
                currentTag=currentTag.substring(0, currentTag.indexOf(" "))+">";

                //System.out.print("+++++++++++++++++++"+currentTag);

                if (currentTag.contains("<!") || currentTag.contains("<html") || currentTag.contains("<head")
                        || currentTag.contains("<title") || currentTag.contains("<body") /*|| currentTag.contains("meta_name")*/) {
                    continue;
                }



                //            	if (currentTag.contains("http") || currentTag.contains("span") || currentTag.contains("href")) {
                //            		currentTag=currentTag.substring(0, currentTag.indexOf(" "))+">";
                //            	}
                //
                //            	if (currentTag.contains("id=")) {
                //            		currentTag=currentTag.substring(0, currentTag.indexOf("id=")-1).concat(
                //            		currentTag.substring(currentTag.indexOf("\"",currentTag.indexOf("id=")+4)+1, currentTag.indexOf(">")+1));
                //            	}
                //
                //            	if (currentTag.contains("name=")) {
                //            		currentTag=currentTag.substring(0, currentTag.indexOf("name=")-1).concat(
                //            		currentTag.substring(currentTag.indexOf("\"",currentTag.indexOf("name=")+6)+1, currentTag.indexOf(">")+1));
                //            	}
                //
                //
                //
                //            	if (currentTag.substring(0,2).equals("<i")) currentTag="<img>";
                //            	if (currentTag.substring(0,2).equals("<a")) currentTag="<a>";
                //            	if (currentTag.contains("<div class")) currentTag="<div>";
                //            	if (currentTag.contains("<meta ")) currentTag="<meta>";
                //
                //
                //
                //        		//System.out.println(" ersetzt zu "+currentTag);
                //
                //
                //        		currentTag=currentTag.replaceAll(" ", "_");
            }

            /*Versuch die aktuelle Ebene einzubeziehen - fehlgeschlagen, nicht brauchbar
        	if (!currentTag.contains("/")) level++;
        	tags.add(level+currentTag);
        	if (currentTag.contains("/")) level--;
        	tags.add(level+currentTag);*/


            if (!lev.contains(currentTag)) {
                //System.out.println(currentTag+"..."+lev);

                lev.add(currentTag);
                //lev2.add("1"+"o"+currentTag);
                //currentTag="1"+"o"+currentTag;

            }

            tags.add(currentTag);
        }

        return tags;
    }

    /**
     * Remove all style and script tags including their content (css, javascript). Remove all other tags as well. Close
     * gaps.
     * 
     * @param htmlContent the html content
     * @param stripTags the strip tags
     * @param stripComments the strip comments
     * @param stripJSAndCSS the strip js and css
     * @param joinTagsAndRemoveNewlines the join tags and remove newlines
     * @return The text of the web page.
     */
    public static String removeHTMLTags(String htmlContent, boolean stripTags, boolean stripComments,
            boolean stripJSAndCSS, boolean joinTagsAndRemoveNewlines) {

        String htmlText = htmlContent;
        // modified by Martin Werner, 2010-06-02

        // String regExp = "";

        if (joinTagsAndRemoveNewlines) {
            htmlText = htmlText.replaceAll(">\\s*?<", "><");
            htmlText = htmlText.replaceAll("\n", "");
        }

        if (stripComments) {
            // regExp += "(\\<!--.*?-->)|";
            htmlText = htmlText.replaceAll("<!--.*?-->", "");
        }

        if (stripJSAndCSS) {
            // regExp += "(<style.*?>.*?</style>)|(<script.*?>.*?</script>)|";
            htmlText = removeConcreteHTMLTag(htmlText, "style");
            htmlText = removeConcreteHTMLTag(htmlText, "script");
        }

        if (stripTags) {
            // regExp += "(\\<.*?>)";
            htmlText = removeConcreteHTMLTag(htmlText, "\\<", ">");
            // htmlText = htmlText.replaceAll("<.*?>", "");
        }

        // if (regExp.length() == 0) {
        // return htmlText;
        // }

        // if (regExp.endsWith("|")) {
        // regExp = regExp.substring(0, regExp.length() - 1);
        // }
        //
        // // Pattern pattern =
        // //
        // Pattern.compile("((\\<!--.*?-->)|(\\<style.*?>.*?\\</style>)|(\\<script.*?>.*?\\</script>)|(\\<.*?>))",Pattern.DOTALL);
        // Pattern pattern = Pattern.compile("(" + regExp + ")", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        // Matcher matcher = pattern.matcher(htmlText);
        //
        // while (matcher.find()) {
        // htmlText = htmlText.replace(matcher.group(), " "); // TODO changed
        // // and untested
        // // 16/06/2009
        // // replace with
        // // whitespace
        // // instead of
        // // nothing
        // }

        // close gaps
        htmlText = htmlText.replaceAll("(\\s){2,}", " ");

        return htmlText.trim();
    }

    public static String removeHTMLTags(String htmlContent) {
        return removeHTMLTags(htmlContent, true, true, true, true);
    }

    /**
     * Removes the concrete html tag.
     * 
     * @param pageContent The html text.
     * @param tag The tag that should be removed.
     * @return The html text without the tag.
     */
    public static String removeConcreteHTMLTag(String pageString, String tag) {
        return removeConcreteHTMLTag(pageString, tag, tag);
    }

    /**
     * Remove concrete HTMLTags from a string; this version is for special-tags like <!-- -->.
     * 
     * @param pageContent The html text.
     * @param beginTag The begin tag.
     * @param endTag The end tag.
     * @return The string without the specified html tag.
     */
    public static String removeConcreteHTMLTag(String pageContent, String beginTag, String endTag) {
        String pageString = pageContent;
        List<String> removeList;
        removeList = getConcreteTags(pageString, beginTag, endTag);
        for (String removeTag : removeList) {
            pageString = pageString.replace(removeTag, "");
        }
        return pageString;
    }

    /**
     * Get a list of concrete HTMLTags; begin- and endtag are not different.
     * 
     * @param pageContent The html text.
     * @param tag The tag.
     * @return A list of concrete tags.
     */
    public static List<String> getConcreteTags(String pageString, String tag) {
        return getConcreteTags(pageString, tag, tag);
    }

    /**
     * Get a list of concrete HTMLTags; its possible that begin- and endtag are different like <!-- -->.
     * 
     * @param pageString The html text.
     * @param beginTag The begin tag.
     * @param endTag The end tag.
     * @return A list of concrete tag names.
     */
    public static List<String> getConcreteTags(String pageString, String beginTag, String endTag) {

        StopWatch sw = new StopWatch();

        List<String> tagList = new ArrayList<String>();
        String regExp = "";
        if (beginTag.equals(endTag)) {
            // regExp = "<"+beginTag+".*?>.*?</"+endTag+">";
            regExp = "<" + beginTag + ".*?>(.*?</" + endTag + ">)?";

        } else {
            regExp = beginTag + ".*?" + endTag;
        }

        Pattern pattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(pageString);
        while (matcher.find()) {
            tagList.add(matcher.group(0));
        }

        LOGGER.info("get concrete tags took " + sw.getElapsedTimeString() + " for a string of length "
                + pageString.length());

        return tagList;
    }

    /**
     * Converts HTML markup to a more or less human readable string. For example we insert line breaks for HTML block
     * level elements, filter out comments, scripts and stylesheets, remove unnecessary white space and so on.
     * 
     * In contrast to @link{@link #removeHTMLTags(String, boolean, boolean, boolean, boolean)}, which works on Strings
     * and just strips out all tags via RegExes, this approach tries to keep some structure for displaying HTML content
     * in text mode in a readable form.
     * FIXME: "namespace not declared" errors pop up too often
     * 
     * @param node
     * @return
     * @author Philipp Katz
     */
    public static String htmlToString(Node node) {
        final StringBuilder builder = new StringBuilder();
        try {
            TransformerFactory transFac = TransformerFactory.newInstance();
            Transformer trans = transFac.newTransformer();
            trans.transform(new DOMSource(node), new SAXResult(new DefaultHandler() {
                boolean ignoreCharacters = false;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
                    String tag = localName.toLowerCase();
                    if (IGNORE_INSIDE.contains(tag)) {
                        ignoreCharacters = true;
                        return;
                    }
                    if (BLOCK_ELEMENTS.contains(tag) || localName.equalsIgnoreCase("br")) {
                        builder.append("\n");
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    String tag = localName.toLowerCase();
                    if (IGNORE_INSIDE.contains(tag)) {
                        ignoreCharacters = false;
                        return;
                    }
                    if (BLOCK_ELEMENTS.contains(tag)) {
                        builder.append("\n");
                    }
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    if (ignoreCharacters) {
                        return;
                    }
                    builder.append(ch);
                }
            }));
        } catch (TransformerConfigurationException e) {
            LOGGER.error("htmlDocToString:TransformerConfigurationException", e);
        } catch (TransformerFactoryConfigurationError e) {
            LOGGER.error("htmlDocToString:TransformerFactoryConfigurationError", e);
        } catch (TransformerException e) {
            LOGGER.error("htmlDocToString:TransformerException", e);
        }
        String result = builder.toString();
        // result = result.replaceAll("[ \t]*?\n", "\n");
        result = NORMALIZE_LINES.matcher(result).replaceAll("");
        result = result.replaceAll("\n{3,}", "\n\n");
        result = result.replaceAll(" {2,}", " ");

        // experimental added 2010-06-30
        // remove multi line comments
        // result = result.replaceAll("<!--(.|\n)*?-->", "");

        result = result.trim();
        return result;
    }

    /**
     * Allows to strip HTML tags from HTML fragments. It will use the Neko parser to parse the String first and then
     * remove the tags, based on the document's structure. Advantage instead of using RegExes to strip the tags is, that
     * whitespace is handled more correctly than in {@link #removeHTMLTags(String, boolean, boolean, boolean, boolean)}
     * which never worked well for me.
     * TODO: "namespace not declared errors"
     * 
     * @param html
     * @param oneLine
     * @return
     * @author Philipp Katz
     */
    public static String htmlToString(String html, boolean oneLine) {

        String result;

        try {

            DOMFragmentParser parser = new DOMFragmentParser();
            HTMLDocument document = new HTMLDocumentImpl();

            // see http://nekohtml.sourceforge.net/usage.html
            DocumentFragment fragment = document.createDocumentFragment();
            parser.parse(new InputSource(new StringInputStream(html)), fragment);
            result = htmlToString(fragment);

        } catch (Exception e) {

            // parser failed -> fall back, remove tags directly from the string without parsing
            LOGGER.debug("encountered error while parsing, will just strip tags : " + e.getMessage());
            result = removeHTMLTags(html, true, true, true, false);

        }

        if (oneLine) {
            result = result.replaceAll("\n", " ");
            result = result.replaceAll(" {2,}", " ");
        }

        return result;
    }

    /**
     * Extract values e.g for: src=, href= or title=
     * 
     * @param pattern the pattern
     * @param content the content
     * @param removeTerm the term which should be removed e.g. " or '
     * @return the string
     */
    public static String extractTagElement(final String pattern, final String content, final String removeTerm) {
        String element = "";
        final Pattern elementPattern = Pattern.compile(pattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        final Matcher elementMatcher = elementPattern.matcher(content);
        while (elementMatcher.find()) {
            String result = elementMatcher.group(0);
            if (!"".equals(removeTerm)) {
                // remove the remove term
                result = result.replaceFirst(removeTerm, "");
                result = result.replaceFirst(removeTerm.toUpperCase(Locale.ENGLISH), "");
                result = result.replaceFirst(removeTerm.toLowerCase(Locale.ENGLISH), "");
            }
            // remove the quotation-marks
            result = result.replaceAll("\"", "");
            result = result.replaceAll("'", "");

            element = result;
        }
        return element;
    }

    /**
     * Checks, if a node is simple like &ltu&gt,&ltb&gt,&lti&gt,...
     * 
     * @param node
     * @return true if simple, else false.
     */
    public static boolean isSimpleElement(Node node) {
        boolean value = false;
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String name = node.getNodeName();
            if (name.equalsIgnoreCase("b") || name.equalsIgnoreCase("i") || name.equalsIgnoreCase("em")
                    || name.equalsIgnoreCase("ins") || name.equalsIgnoreCase("del") || name.equalsIgnoreCase("s")
                    || name.equalsIgnoreCase("small") || name.equalsIgnoreCase("big")
                    || name.equalsIgnoreCase("strong") || name.equalsIgnoreCase("u")) {
                value = true;
            }
        }
        return value;

    }

    /**
     * Checks, if tag is a headline.
     * 
     * @param tag
     * @return
     */
    public static boolean isHeadlineTag(String tag) {
        boolean result = false;
        if (tag.equalsIgnoreCase("h1") || tag.equalsIgnoreCase("h2") || tag.equalsIgnoreCase("h3")
                || tag.equalsIgnoreCase("h4") || tag.equalsIgnoreCase("h5") || tag.equalsIgnoreCase("h6")) {
            result = true;
        }
        return result;
    }

    public static boolean isHeadlineTag(Node tag) {
        return isHeadlineTag(tag.getNodeName());
    }

    /**
     * Sometimes texts in webpages have special code for character.<br>
     * E.g. <i>&ampuuml;</i> or whitespace. <br>
     * To evaluate this text reasonably you need to convert this code.<br>
     * This code and equivalent text is hold in {@link HTMLSymbols}.<br>
     * 
     * @param text
     * @return
     */

    // TODO rem: there is org.apache.commons.lang.StringEscapeUtils.unescapeHtml(text) which should do the same job.
    // -- Philipp.
    // Thanks, will do this after be sure nothing changes.

    public static String replaceHTMLSymbols(String text) {
        String result = text;
        if (result != null) {
            Iterator<String[]> htmlSymbols = HTMLSymbols.getHTMLSymboles().iterator();
            while (htmlSymbols.hasNext()) {
                String[] symbol = htmlSymbols.next();
                result = result.replaceAll(symbol[0], symbol[1]);
            }
        }
        return result;
    }

    /**
     * Converts a DOM Node or Document into a String. In contrast to {@link PageAnalyzer#getTextDump(Node)}, this method
     * will write out the full node, including tags.
     * 
     * TODO removing whitespace does not work with documents from the Crawler/Neko?
     * TODO duplicate of {@link XPathHelper#convertNodeToString(Node)}? Merge?
     * 
     * @param node
     * @param removeWhitespace whether to remove superfluous whitespace outside of tags.
     * @param prettyPrint whether to nicely indent the result.
     * @return String representation of the supplied Node, empty String in case of errors.
     */
    public static String getXmlDump(Node node, boolean removeWhitespace, boolean prettyPrint) {
        String strResult = "";
        try {

            if (removeWhitespace) {
                node = HTMLHelper.removeWhitespace(node);
            }

            Source source = new DOMSource(node);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();

            if (prettyPrint) {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            }

            transformer.transform(source, result);
            strResult = stringWriter.getBuffer().toString();
        } catch (TransformerConfigurationException e) {
            LOGGER.error(e);
        } catch (TransformerException e) {
            LOGGER.error(e);
        }
        return strResult;
    }

    public static String getXmlDump(Node node) {
        return getXmlDump(node, false, false);
    }

    /**
     * Remove unnecessary whitespace from DOM nodes.
     * http://stackoverflow.com/questions/978810/how-to-strip-whitespace-only-text-nodes-from-a-dom-before-serialization
     * 
     * @param node
     * @return
     */
    public static Node removeWhitespace(Node node) {

        Node result = node.cloneNode(true);

        try {
            XPathFactory xpathFactory = XPathFactory.newInstance();
            // XPath to find empty text nodes.
            XPathExpression xpathExp = xpathFactory.newXPath().compile("//text()[normalize-space(.) = '']");
            NodeList emptyTextNodes = (NodeList) xpathExp.evaluate(result, XPathConstants.NODESET);

            // Remove each empty text node from document.
            for (int i = 0; i < emptyTextNodes.getLength(); i++) {
                Node emptyTextNode = emptyTextNodes.item(i);
                emptyTextNode.getParentNode().removeChild(emptyTextNode);
            }
        } catch (XPathExpressionException e) {
            LOGGER.error(e);
        } catch (DOMException e) {
            LOGGER.error(e);
        }

        return result;
    }

    public static void writeXmlDump(Node node, String filename) {
        String string = getXmlDump(node);
        FileHelper.writeToFile(filename, string);
    }

    /**
     * Converts a String representation with XML markup to DOM Document. Returns an empty Document if parsing failed.
     * 
     * @param input
     * @return
     */
    public static Document stringToXml(String input) {
        DocumentBuilder builder = null;
        Document result = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
            result = builder.parse(new InputSource(new StringReader(input)));
        } catch (ParserConfigurationException e) {
            LOGGER.error("stringToXml:ParserConfigurationException " + e.getMessage());
        } catch (SAXException e) {
            LOGGER.error("stringToXml:SAXException " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error("stringToXml:IOException " + e.getMessage());
        }
        if (result == null && builder != null) {
            // return an empty Document
            result = builder.newDocument();
        }
        return result;
    }

    /**
     * Returns a String representation of the supplied Node, including the Node itself, like outerHTML in
     * JavaScript/DOM.
     * 
     * http://chicknet.blogspot.com/2007/05/outerxml-for-java.html
     * 
     * @param node
     * @return
     */
    public static String getOuterXml(Node node) {
        // logger.trace(">getOuterXml");
        String result = "";
        Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty("omit-xml-declaration", "yes");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            result = writer.toString();

        } catch (TransformerConfigurationException e) {
            LOGGER.error("getOuterXml:TransformerConfigurationException", e);
        } catch (TransformerFactoryConfigurationError e) {
            LOGGER.error("getOuterXml:TransformerFactoryConfigurationError", e);
        } catch (TransformerException e) {
            LOGGER.error("getOuterXml:TransformerException", e);
        }
        // logger.trace("<getOuterXml " + result);
        return result;
    }

    /**
     * Returns a String representation of the supplied Node, excluding the Node itself, like innerHTML in
     * JavaScript/DOM.
     * 
     * @param node
     * @return
     */
    public static String getInnerXml(Node node) {
        // logger.trace(">getInnerXml");
        StringBuilder sb = new StringBuilder();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            sb.append(getOuterXml(children.item(i)));
        }
        // logger.trace("<getInnerXml");
        return sb.toString();
    }

    /**
     * Creates a new, empty DOM Document.
     * 
     * @return
     */
    public static Document createDocument() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.newDocument();
        } catch (ParserConfigurationException e) {
            LOGGER.error("createDocument:ParserConfigurationException, throwing RuntimeException", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes all nodes with specified type from Node.
     * 
     * @param node
     * @param nodeType for example <code>Node.COMMENT_NODE</code>
     */
    public static void removeAll(Node node, short nodeType) {
        HTMLHelper.removeAll(node, nodeType, null);
    }

    /**
     * Removes all nodes with specified type and name from Node.
     * 
     * @param node
     * @param nodeType for example <code>Node.COMMENT_NODE</code>
     * @param name
     */
    public static void removeAll(Node node, short nodeType, String name) {
        if (node.getNodeType() == nodeType && (name == null || node.getNodeName().equals(name))) {
            node.getParentNode().removeChild(node);
        } else {
            NodeList list = node.getChildNodes();
            for (int i = list.getLength() - 1; i >= 0; i--) {
                removeAll(list.item(i), nodeType, name);
            }
        }
    }

    /**
     * Creates a copy of a DOM Document.
     * http://stackoverflow.com/questions/279154/how-can-i-clone-an-entire-document-using-the-java-dom
     * 
     * @param document
     * @return the cloned Document or <code>null</code> if cloning failed.
     */
    public static Document cloneDocument(Document document) {
        Document result = null;
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            DOMSource source = new DOMSource(document);
            DOMResult target = new DOMResult();
            transformer.transform(source, target);
            result = (Document) target.getNode();
        } catch (TransformerConfigurationException e) {
            LOGGER.error("cloneDocument:TransformerConfigurationException " + e.getMessage());
        } catch (TransformerFactoryConfigurationError e) {
            LOGGER.error("cloneDocument:TransformerFactoryConfigurationError " + e.getMessage());
        } catch (TransformerException e) {
            LOGGER.error("cloneDocument:TransformerException " + e.getMessage());
            // this happens when document is ill-formed, we could also throw
            // this exception. This way we have to check if this method returns
            // null;
        } catch (DOMException e) {
            LOGGER.error("cloneDocument:DOMException " + e.getMessage());
        }
        return result;
    }

    public static void main(String[] args) throws Exception {

        String input = FileHelper.readFileToString("NewFile2.xml");
        // input = StringEscapeUtils.unescapeXml(input);
        //        System.out.println(input.hashCode());

        input = StringEscapeUtils.unescapeHtml(input);

        System.out.println(input);

        HTMLHelper.stringToXml(input);

        System.exit(0);

        System.out.println(removeHTMLTags("<p>One <b>sentence</b>.</p><p>Another sentence.", true, true, true, true));
        System.out.println(htmlToString("<p>One <b>sentence</b>.</p><p>Another sentence.", true));

        // String html = readHtmlFile("testfiles/readability/test004.html");
        // html = htmlToString(html, true);
        // System.out.println(html);

        // DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
        // Document doc = df.newDocumentBuilder().parse(new File("dumps/readability1275037727850.xml"));
        // System.out.println(htmlDocToString(doc));
        //

        // System.out.println("1\n2\n3\n\n\n".trim());
        // System.out.println("-------------");

        // String s = null;
        // System.out.println(s.toLowerCase());

        // Crawler c = new Crawler();
        // Document doc = c.getWebDocument("data/test/pageContentExtractor/test001.html");
        // String result = htmlDocToString(doc);
        // System.out.println(DigestUtils.md5Hex(result)); // 489eb91cf94343d0b62e69c396bc6b6f
        // System.out.println(result);

    }

}
