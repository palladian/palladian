package tud.iir.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;

import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.log4j.Logger;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLDocument;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tud.iir.knowledge.HTMLSymbols;

/**
 * Some HTML specific helper methods.
 * 
 * @author David Urbansky
 * @author Martin Werner
 * @author Philipp Katz
 */
public class HTMLHelper {

    private static final Logger LOGGER = Logger.getLogger(HTMLHelper.class);

    /** HTML block level elements. */
    private static final List<String> BLOCK_ELEMENTS = Arrays.asList("address", "blockquote", "div", "dl", "fieldset",
            "form", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "noscript", "ol", "p", "pre", "table", "ul", "dd", "dt",
            "li", "tbody", "td", "tfoot", "th", "thead", "tr", "button", "del", "ins", "map", "object", "script");

    /** "Junk" elements which do not contain relevant content. */
    private static final List<String> IGNORE_INSIDE = Arrays.asList("script", "style");

    private static Pattern normalizeLines = Pattern.compile("^\\s+$|^[ \t]+|[ \t]+$", Pattern.MULTILINE);

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

        return tagList;
    }

    /**
     * Converts HTML markup to a more or less human readable string. For example we insert line breaks for HTML block
     * level elements, filter out comments, scripts and stylesheets, remove unnecessary white space and so on.
     * 
     * In contrast to @link{@link #removeHTMLTags(String, boolean, boolean, boolean, boolean)}, which works on Strings
     * and just strips out all tags via RegExes, this approach tries to keep some structure for displaying HTML content
     * in text mode in a readable form.
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
        result = normalizeLines.matcher(result).replaceAll("");
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
            if (!("").equals(removeTerm)) {
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

    public static void main(String[] args) throws Exception {

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
