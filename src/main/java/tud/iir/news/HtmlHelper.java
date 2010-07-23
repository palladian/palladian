package tud.iir.news;

import java.util.Arrays;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tud.iir.helper.StringHelper;
import tud.iir.web.Crawler;

/**
 * Some HTML specific helper methods.
 * XXX merge with helper.HTMLHelper
 * 
 * @author Philipp Katz
 * 
 */
public class HtmlHelper {

    private static final Logger LOGGER = Logger.getLogger(HtmlHelper.class);

    // block level elements for which we want to create a line break
    private static final String[] BLOCK_ELEMENTS = new String[] { "address", "blockquote", "div", "dl", "fieldset", "form", "h1", "h2", "h3", "h4", "h5", "h6",
            "hr", "noscript", "ol", "p", "pre", "table", "ul", "dd", "dt", "li", "tbody", "td", "tfoot", "th", "thead", "tr", "button", "del", "ins", "map",
            "object", "script" };
    
    // "junk" elements, which we want to filter out
    private static final String[] IGNORE_INSIDE = new String[] { "script", "style" };

    private static Pattern normalizeLines = Pattern.compile("^\\s+$|^[ \t]+|[ \t]+$", Pattern.MULTILINE);

    // prevent instantiation
    private HtmlHelper() {
    }

    /**
     * Converts HTML markup to a more or less human readable string. For example we insert line breaks for HTML block
     * level elements, filter out comments, scripts and stylesheets, remove unnecessary white space and so on.
     * 
     * In contrast to {@link StringHelper#removeHTMLTags(String, boolean, boolean, boolean, boolean)}, which just strips
     * out all tags, this approach tries to keep some structure for displaying HTML content in text mode in a readable
     * form.
     * 
     * @param doc
     * @return
     */
    public static String htmlDocToString(Document doc) {
        final StringBuilder builder = new StringBuilder();
        try {
            TransformerFactory transFac = TransformerFactory.newInstance();
            Transformer trans = transFac.newTransformer();
            trans.transform(new DOMSource(doc), new SAXResult(new DefaultHandler() {
                boolean ignoreCharacters = false;
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if (isInIgnore(localName)) {
                        ignoreCharacters = true;
                        return;
                    }
                    if (isBlockElement(localName) || localName.equalsIgnoreCase("br")) {
                        builder.append("\n");
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if (isInIgnore(localName)) {
                        ignoreCharacters = false;
                        return;
                    }
                    if (isBlockElement(localName)) {
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
        //result = result.replaceAll("<!--(.|\n)*?-->", "");
        
        result = result.trim();
        return result;
    }

    private static boolean isBlockElement(String elementName) {
        return Arrays.asList(BLOCK_ELEMENTS).contains(elementName.toLowerCase());
    }
    
    private static boolean isInIgnore(String elementName) {
        return Arrays.asList(IGNORE_INSIDE).contains(elementName.toLowerCase());
    }

    public static void main(String[] args) throws Exception {

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
        
        Crawler c = new Crawler();
        Document doc = c.getWebDocument("data/test/pageContentExtractor/test001.html");
        System.out.println(HtmlHelper.htmlDocToString(doc));

    }
}
