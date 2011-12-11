package ws.palladian.helper.html;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import org.apache.commons.lang.StringUtils;
import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.log4j.Logger;
import org.apache.xerces.dom.DocumentImpl;
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

import ws.palladian.extraction.PageAnalyzer;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StringInputStream;
import ws.palladian.helper.StringOutputStream;
import ws.palladian.helper.nlp.StringHelper;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 * Some HTML and XML/DOM specific helper methods.
 * 
 * @author David Urbansky
 * @author Martin Werner
 * @author Philipp Katz
 * @author Martin Gregor
 */
public class HtmlHelper {

    private static final Logger LOGGER = Logger.getLogger(HtmlHelper.class);

    /** HTML block level elements. */
    private static final List<String> BLOCK_ELEMENTS = Arrays.asList("address", "blockquote", "div", "dl", "fieldset",
            "form", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "noscript", "ol", "p", "pre", "table", "ul", "dd", "dt",
            "li", "tbody", "td", "tfoot", "th", "thead", "tr", "button", "del", "ins", "map", "object", "script");

    /** "Junk" elements which do not contain relevant content. */
    private static final List<String> IGNORE_INSIDE = Arrays.asList("script", "style");

    private static final Pattern NORMALIZE_LINES = Pattern.compile("^\\s+$|^[ \t]+|[ \t]+$", Pattern.MULTILINE);

    /** prevent instantiation. */
    private HtmlHelper() {
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
        String currentTag = "";

        Pattern pattern = Pattern.compile("(\\<.*?>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(htmlText);

        while (matcher.find()) {
            currentTag = matcher.group();

            // Delete arguments within the tags
            if (currentTag.contains(" ")) {
                currentTag = currentTag.substring(0, currentTag.indexOf(" ")) + ">";

                // System.out.print("+++++++++++++++++++"+currentTag);

                if (currentTag.contains("<!") || currentTag.contains("<html") || currentTag.contains("<head")
                        || currentTag.contains("<title") || currentTag.contains("<body") /*
                         * ||
                         * currentTag.contains("meta_name"
                         * )
                         */) {
                    continue;
                }

                // if (currentTag.contains("http") || currentTag.contains("span") || currentTag.contains("href")) {
                // currentTag=currentTag.substring(0, currentTag.indexOf(" "))+">";
                // }
                //
                // if (currentTag.contains("id=")) {
                // currentTag=currentTag.substring(0, currentTag.indexOf("id=")-1).concat(
                // currentTag.substring(currentTag.indexOf("\"",currentTag.indexOf("id=")+4)+1,
                // currentTag.indexOf(">")+1));
                // }
                //
                // if (currentTag.contains("name=")) {
                // currentTag=currentTag.substring(0, currentTag.indexOf("name=")-1).concat(
                // currentTag.substring(currentTag.indexOf("\"",currentTag.indexOf("name=")+6)+1,
                // currentTag.indexOf(">")+1));
                // }
                //
                //
                //
                // if (currentTag.substring(0,2).equals("<i")) currentTag="<img>";
                // if (currentTag.substring(0,2).equals("<a")) currentTag="<a>";
                // if (currentTag.contains("<div class")) currentTag="<div>";
                // if (currentTag.contains("<meta ")) currentTag="<meta>";
                //
                //
                //
                // //System.out.println(" ersetzt zu "+currentTag);
                //
                //
                // currentTag=currentTag.replaceAll(" ", "_");
            }

            /*
             * Versuch die aktuelle Ebene einzubeziehen - fehlgeschlagen, nicht brauchbar
             * if (!currentTag.contains("/")) level++;
             * tags.add(level+currentTag);
             * if (currentTag.contains("/")) level--;
             * tags.add(level+currentTag);
             */

            if (!lev.contains(currentTag)) {
                // System.out.println(currentTag+"..."+lev);

                lev.add(currentTag);
                // lev2.add("1"+"o"+currentTag);
                // currentTag="1"+"o"+currentTag;

            }

            tags.add(currentTag);
        }

        return tags;
    }

    /**
     * Remove all style and script tags including their content (css, javascript). Remove all other tags as well. Close
     * gaps. The text might not be readable since all format hints are discarded. Consider using
     * {@link HtmlHelper.htmlToReableText} in case you need formatting.
     * 
     * @param htmlContent the html content
     * @param stripTags the strip tags
     * @param stripComments the strip comments
     * @param stripJSAndCSS the strip js and css
     * @param joinTagsAndRemoveNewlines the join tags and remove newlines
     * @return The text of the web page.
     */
    public static String stripHtmlTags(String htmlContent, boolean stripTags, boolean stripComments,
            boolean stripJSAndCSS, boolean joinTagsAndRemoveNewlines) {

        String htmlText = htmlContent;

        String regExp = "";

        if (joinTagsAndRemoveNewlines) {
            htmlText = htmlText.replaceAll(">\\s*?<", "><");
            htmlText = htmlText.replaceAll("\n", "");
        }

        if (stripComments) {
            regExp += "(\\<!--.*?-->)|";
            // htmlText = htmlText.replaceAll("<!--.*?-->", "");
        }

        if (stripJSAndCSS) {
            regExp += "(<style.*?>.*?</style>)|(<script.*?>.*?</script>)|";
            // htmlText = removeConcreteHTMLTag(htmlText, "style");
            // htmlText = removeConcreteHTMLTag(htmlText, "script");
        }

        if (stripTags) {
            regExp += "(<.*?>)";
            // htmlText = removeConcreteHTMLTag(htmlText, "\\<", ">");
            // htmlText = htmlText.replaceAll("<.*?>", "");
        }

        if (regExp.length() == 0) {
            return htmlText;
        }

        if (regExp.endsWith("|")) {
            regExp = regExp.substring(0, regExp.length() - 1);
        }

        Pattern pattern = Pattern.compile("(" + regExp + ")", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        try {
            Matcher matcher = pattern.matcher(htmlText);

            while (matcher.find()) {
                htmlText = htmlText.replace(matcher.group(), "");
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        // close gaps
        // htmlText = htmlText.replaceAll("[ ]{2,}", " ");

        // return htmlText.trim();
        return htmlText;
    }

    /**
     * <p>
     * Remove all style and script tags including their content (css, javascript). Remove all other tags as well. Close
     * gaps. The text might not be readable since all format hints are discarded. Consider using
     * {@link HtmlHelper.htmlToReableText} in case you need formatting.
     * </p>
     * <p>
     * All tags, including css and javascript, will be removed. Lines will be joined.
     * </p>
     * 
     * @param htmlContent the html content
     */
    public static String stripHtmlTags(String htmlContent) {
        return stripHtmlTags(htmlContent, true, true, true, false);
    }

    /**
     * Removes the concrete html tag.
     * 
     * @param pageContent The html text.
     * @param tag The tag that should be removed.
     * @return The html text without the tag.
     */
    public static String removeConcreteHtmlTag(String pageString, String tag) {
        return removeConcreteHtmlTag(pageString, tag, tag);
    }

    /**
     * <p>
     * Remove concrete HTMLTags from a string; this version is for special-tags like <!-- -->.
     * </p>
     * 
     * @param pageContent The html text.
     * @param beginTag The begin tag.
     * @param endTag The end tag.
     * @return The string without the specified html tag.
     */
    public static String removeConcreteHtmlTag(String pageContent, String beginTag, String endTag) {
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

        // LOGGER.info("get concrete tags took " + sw.getElapsedTimeString() + " for a string of length "+
        // pageString.length());

        return tagList;
    }

    /**
     * <p>
     * Converts HTML markup to a more or less human readable string. For example we insert line breaks for HTML block
     * level elements, filter out comments, scripts and stylesheets, remove unnecessary white space and so on.
     * </p>
     * 
     * <p>
     * In contrast to {@link #stripHtmlTags(String, boolean, boolean, boolean, boolean)}, which works on Strings and
     * just strips out all tags via RegExes, this approach tries to keep some structure for displaying HTML content in
     * text mode in a readable form.
     * </p>
     * 
     * FIXME: "namespace not declared" errors pop up too often
     * 
     * @param node
     * @return
     * @author Philipp Katz
     */
    public static String documentToReadableText(Node node) {
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
     * whitespace is handled more correctly than in {@link #stripHtmlTags(String, boolean, boolean, boolean, boolean)}
     * which never worked well for me.
     * TODO: "namespace not declared errors"
     * 
     * @param html
     * @param oneLine
     * @return
     * @author Philipp Katz
     */
    public static String documentToReadableText(String html, boolean oneLine) {

        String result;

        try {

            DOMFragmentParser parser = new DOMFragmentParser();
            HTMLDocument document = new HTMLDocumentImpl();

            // see http://nekohtml.sourceforge.net/usage.html
            DocumentFragment fragment = document.createDocumentFragment();
            parser.parse(new InputSource(new StringInputStream(html)), fragment);
            result = documentToReadableText(fragment);

        } catch (Exception e) {

            // parser failed -> fall back, remove tags directly from the string without parsing
            LOGGER.debug("encountered error while parsing, will just strip tags : " + e.getMessage());
            result = stripHtmlTags(html, true, true, true, false);

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
                node = HtmlHelper.removeWhitespace(node);
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

    // /**
    // * Convert a node and his children to string.
    // *
    // * duplicate of {@link HTMLHelper#documentToHTMLString(Node)}, {@link HTMLHelper#getXmlDump(Node)}?
    // *
    // * @param node the node
    // * @return the node as string
    // */
    // public static String convertNodeToString(Node node) {
    // Transformer trans = null;
    // try {
    // trans = TransformerFactory.newInstance().newTransformer();
    // } catch (TransformerConfigurationException e1) {
    // Logger.getRootLogger().error(e1.getMessage());
    // } catch (TransformerFactoryConfigurationError e1) {
    // Logger.getRootLogger().error(e1.getMessage());
    // }
    //
    // final StringWriter sWriter = new StringWriter();
    // try {
    // trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    // trans.transform(new DOMSource(node), new StreamResult(sWriter));
    // } catch (TransformerException e) {
    // Logger.getRootLogger().error(e.getMessage());
    // }
    // String result = sWriter.toString();
    // result = result.replace(" xmlns=\"http://www.w3.org/1999/xhtml\"", "");
    // // result = result.replace("xmlns=\"http://www.w3.org/1999/xhtml\"", "");
    //
    // return result;
    // }

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
        HtmlHelper.removeAll(node, nodeType, null);
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
        // System.out.println(input.hashCode());

        input = StringEscapeUtils.unescapeHtml(input);

        System.out.println(input);

        HtmlHelper.stringToXml(input);

        System.exit(0);

        System.out.println(stripHtmlTags("<p>One <b>sentence</b>.</p><p>Another sentence.", true, true, true, true));
        System.out.println(documentToReadableText("<p>One <b>sentence</b>.</p><p>Another sentence.", true));

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

    public static String xmlToString(Node node) {
        try {
            Source source = new DOMSource(node);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
            return stringWriter.getBuffer().toString();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the raw HTML code of the document. The code will not be reformatted.
     * 
     * @param document The web document to transform to the HTML string.
     * @return The unformatted HTML code of the document.
     */
    public static String documentToHtmlString(Document document) {

        String htmlString = "";

        OutputStream os = new StringOutputStream();

        try {
            OutputFormat format = new OutputFormat(document);
            XMLSerializer serializer = new XMLSerializer(os, format);
            serializer.serialize(document);

            // for some reason the following line is added to the document even if it doesn't exist
            htmlString = os.toString().replaceFirst("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>", "").trim();

        } catch (IOException e) {
            LOGGER.error("could not serialize document, " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("could not serialize document, " + e.getMessage());
        }

        return htmlString;
    }

    /**
     * Get the raw HTML code of the node. The code will not be reformatted.
     * 
     * @param node An HTML node that should be transformed to an HTML string.
     * @return The unformatted HTML code of the node.
     */
    public static String documentToHtmlString(Node node) {
        Document doc = new DocumentImpl();

        String ret = "";

        try {
            Node clonedNode = node.cloneNode(true);
            Node adoptedNode = doc.adoptNode(clonedNode);
            doc.appendChild(adoptedNode);
            String rawMarkupString = documentToHtmlString(doc);
            ret = rawMarkupString.replaceFirst("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>", "").trim();
        } catch (Exception e) {
            LOGGER.error("couldn't get raw markup from node " + e.getMessage());
        }

        return ret;
    }

    /**
     * <p>Print DOM tree for diagnostic purposes. The output includes all Nodes and their Attributes (prefixed with @).</p>
     * @param node
     */
    public static void printDom(Node node) {
        printDom(node, 0);
    }

    private static void printDom(Node node, int indent) {
        String indentString = StringUtils.repeat(" ", indent);

        String nodeName = node.getNodeName();
        String prefix = node.getPrefix();
        String namespaceURI = node.getNamespaceURI();
        System.out.println(indentString + nodeName + "(" + prefix + " : " + namespaceURI + ")");

        if (node.getAttributes() != null) {
            for (int i = 0; i < node.getAttributes().getLength(); i++) {
                System.out.println(indentString + "@" + node.getAttributes().item(i));
            }
        }

        Node child = node.getFirstChild();
        while (child != null) {
            printDom(child, indent + 1);
            child = child.getNextSibling();
        }
    }

    /**
     * Get the sub tree of the document or node as text without tags.
     * You could also use {@link documentToHTMLString} and {@link htmlToReadableText} to achieve similar results.
     * 
     * @param node The node from where to start.
     * @return A text representation of the node and its sub nodes without tags.
     */
    public static String documentToText(Node node) {

        // ignore css and script nodes
        if (node == null || node.getNodeName().equalsIgnoreCase("script")
                || node.getNodeName().equalsIgnoreCase("style") || node.getNodeName().equalsIgnoreCase("#comment")
                || node.getNodeName().equalsIgnoreCase("option") || node.getNodeName().equalsIgnoreCase("meta")
                || node.getNodeName().equalsIgnoreCase("head")) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // System.out.println(node.getNodeName()+node.getTextContent());
        if (node.getTextContent() != null) {

            if (node.getNodeName().equalsIgnoreCase("#text")) {
                sb.append(node.getTextContent().trim()).append(" ");
            }

        }
        if (isWrappingNode(node)) {
            sb.append("\n");
        }

        Node child = node.getFirstChild();
        while (child != null) {
            sb.append(documentToText(child));
            child = child.getNextSibling();
        }

        return sb.toString().replaceAll("[ ]{2,}", "");
    }

    public static String getDocumentTextDump(Document document) {
        if (document != null && document.getLastChild() != null) {
            return document.getLastChild().getTextContent();
        }
        return "";
    }

    private static boolean isWrappingNode(Node node) {

        String nodeName = node.getNodeName().toLowerCase();

        Set<String> wrappingNodes = new HashSet<String>();
        wrappingNodes.add("p");
        wrappingNodes.add("div");
        wrappingNodes.add("td");
        wrappingNodes.add("h1");
        wrappingNodes.add("h2");
        wrappingNodes.add("h3");
        wrappingNodes.add("h4");
        wrappingNodes.add("h5");
        wrappingNodes.add("h6");
        wrappingNodes.add("li");

        if (wrappingNodes.contains(nodeName)) {
            return true;
        }

        return false;
    }

    /**
     * Get the string representation of a document.
     * TODO duplicate of {@link #documentToHtmlString(Document)}?
     * 
     * @param document The document.
     * @return The string representation of the document.
     */
    public static String documentToString(Document document) {
        String documentString = "";

        try {
            DOMSource domSource = new DOMSource(document);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            documentString = writer.toString();
        } catch (TransformerException e) {
            LOGGER.error("could not get string representation of document " + e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error("could not get string representation of document " + e.getMessage());
        }

        return documentString;
    }

    /**
     * Sometimes texts in webpages have special code for character.<br>
     * E.g. <i>&ampuuml;</i> or whitespace. <br>
     * To evaluate this text reasonably you need to convert this code.<br>
     * 
     * @param text
     * @return
     */
    // TODO very specific and only used by date recognition
    public static String replaceHtmlSymbols(String text) {

        String result = StringEscapeUtils.unescapeHtml(text);
        result = StringHelper.replaceProtectedSpace(result);

        // remove undesired characters
        result = result.replace("&#8203;", " "); // empty whitespace
        result = result.replace("\n", " ");
        result = result.replace("&#09;", " "); // html tabulator
        result = result.replace("\t", " ");
        result = result.replace(" ,", " ");

        return result;
    }

}
