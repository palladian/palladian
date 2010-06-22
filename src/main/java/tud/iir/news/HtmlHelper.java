package tud.iir.news;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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
import org.apache.xerces.parsers.DOMParser;
import org.cyberneko.html.HTMLConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;
import tud.iir.helper.StringInputStream;
import tud.iir.helper.XPathHelper;

/**
 * Some HTML specific helper methods.
 * 
 * @author Philipp Katz
 * 
 */
public class HtmlHelper {

    private static final Logger logger = Logger.getLogger(HtmlHelper.class);

    // the default encoding we assume in this class.
    private static final String DEFAULT_ENCODING = "UTF-8";

    // Pattern for opening and closing HTML block level elements
    // see: http://htmlhelp.com/reference/html40/block.html
    private static Pattern blockElements = Pattern
            .compile("</?(?=address|blockquote|div|dl|fieldset|form|h1|h2|h3|h4|h5|h6|hr|noscript|ol|p|pre|table|ul|dd|dt|li|tbody|td|tfoot|th|thead|tr|button|del|ins|map|object|script).*?>");

    private static final String[] BLOCK_ELEMENTS = new String[] { "address", "blockquote", "div", "dl", "fieldset", "form", "h1", "h2", "h3", "h4", "h5", "h6",
            "hr", "noscript", "ol", "p", "pre", "table", "ul", "dd", "dt", "li", "tbody", "td", "tfoot", "th", "thead", "tr", "button", "del", "ins", "map",
            "object", "script" };

    // "junk" elements, which we want to filter out
    private static Pattern junkElements = Pattern.compile("<script.*?>.*?</script>|<style.*?>.*?</style>|<!--.*?-->");

    // //
    // // Patterns for HTML, HTM5, XHTML. One Pattern for the whole tag
    // // containing the encoding, one Pattern to extract the actual encoding.
    // // See http://en.wikipedia.org/wiki/Character_encodings_in_HTML
    // //
    //
    // // HTML <meta http-equiv="content-type" content="text/html; charset=iso-8859-1" />
    // private static Pattern htmlMetaTag = Pattern.compile("<meta\\s*?content.*?http-equiv.*?/?>|<meta\\s*?http-equiv.*?content.*?/?>",
    // Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    // private static Pattern htmlMetaEncoding = Pattern.compile("content=([\"'])text/html;\\s*?charset=([A-Za-z0-9\\-_]+?)\\1.*?/?>", Pattern.CASE_INSENSITIVE
    // | Pattern.DOTALL);
    //	
    // // HTML5 <meta charset="iso-8859-1">
    // private static Pattern html5MetaTag = Pattern.compile("<meta.*?charset.*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    // private static Pattern html5MetaEncoding = Pattern.compile("charset=([\"']?)([A-Za-z0-9\\-_]+?)\\1\\s*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    //	
    // // XHTML <?xml version="1.0" encoding="ISO-8859-1"?>
    // private static Pattern xmlDeclaration = Pattern.compile("<\\?xml.*?encoding.*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    // private static Pattern xmlDeclarationEncoding = Pattern.compile("encoding=([\"'])([A-Za-z0-9\\-_]+?)\\1", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // Pattern for lines containing white space and whitespace at beginning or end of lines
    private static Pattern normalizeLines = Pattern.compile("^\\s+$|^[ \t]+|[ \t]+$", Pattern.MULTILINE);

    // prevent instantiation
    private HtmlHelper() {
    }

    /**
     * Converts HTML markup to a more or less human readable string. For example we insert line breaks for HTML block level elements, filter out comments,
     * scripts and stylesheets, remove unnecessary white space and so on.
     * 
     * Conclusion: The difference to {@link StringHelper#removeHTMLTags(String, boolean, boolean, boolean, boolean)} is, that this method tries to keep some
     * structure for displaying HTML content in text mode.
     * 
     * TODO this does not work well; better use {@link #htmlDocToString} if applicable, which will do the transformation based on the Document's DOM tree and
     * thus yield in better results.
     * 
     * @param html
     * @return
     */
    public static String htmlToString(String html, boolean skipTitle) {

        // html = html.replaceAll(">\\s*?<", "><");
        html = html.replaceAll(">\\s+?<", "> <");
        html = html.replaceAll("\n", " ");
        html = html.replaceAll("\\s{2,}", " ");
        html = junkElements.matcher(html).replaceAll("");
        html = blockElements.matcher(html).replaceAll("\n");
        if (skipTitle) {
            html = html.replaceAll("<title>.*?</title>", "");
        }
        html = html.replaceAll("<br />", "\n");
        html = html.replaceAll("<.*?>", "");
        html = html.replaceAll(" \n", "\n");
        html = html.replaceAll("\n{3,}", "\n\n");
        html = html.trim();

        return html;
    }

    /*
     * public static String htmlToString(String string) { final StringBuilder builder = new StringBuilder(); ParserDelegator delegator = new ParserDelegator();
     * ParserCallback callback = new ParserCallback() {
     * @Override public void handleText(char[] data, int pos) { builder.append(data); }
     * @Override public void handleStartTag(Tag t, MutableAttributeSet a, int pos) { if (t.isBlock()) { builder.append("\n\n"); } }
     * @Override public void handleEndTag(Tag t, int pos) { if (t.isBlock()) { builder.append("\n\n"); } }
     * @Override public void handleSimpleTag(Tag t, MutableAttributeSet a, int pos) { if (t == Tag.BR) { builder.append("\n"); } } }; try { delegator.parse(new
     * StringReader(string), callback, true); } catch (IOException e) { e.printStackTrace(); } // post processing; remove more then three subsequent line
     * breaks, trim String result = builder.toString(); result = result.replaceAll("\n{3,}", "\n\n"); result = result.trim(); return result; }
     */

    /**
     * Reads a File with the specified encoding.
     * 
     * @param filePath
     * @param encoding
     * @return
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    private static String readFileWithEncoding(String filePath, String encoding) throws UnsupportedEncodingException, IOException {
        StringBuilder contents = new StringBuilder();
        FileInputStream fis = new FileInputStream(filePath);
        InputStreamReader in = new InputStreamReader(fis, encoding);
        BufferedReader input = new BufferedReader(in);
        String line;
        while ((line = input.readLine()) != null) {
            contents.append(line);
            // XXX contents.append(System.getProperty("line.separator"));
            contents.append("\n");
        }
        input.close();
        return contents.toString();
    }

    /**
     * Reads an (X)HTML file with the correct encoding, specified inside the file itself, using one of the following techniques. If no encoding is specified, we
     * assume UTF-8 as default.
     * 
     * HTML: <meta http-equiv="content-type" content="text/html; charset=iso-8859-1" />
     * 
     * HTML5: <meta charset="iso-8859-1">
     * 
     * XHTML: <?xml version="1.0" encoding="ISO-8859-1"?>
     * 
     * See: http://en.wikipedia.org/wiki/Character_encodings_in_HTML
     * 
     * TODO There is already such a method {@link FileHelper#readHTMLFileToString(String, boolean)} but this does not seem to honor specific encodings.
     * 
     * @param filePath
     * @return
     */
    public static String readHtmlFile(String filePath) throws IOException {
        String html = null;

        try {
            html = readFileWithEncoding(filePath, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            // this should not happen as we are using the default encoding UTF-8
            logger.error("unexpected UnsupportedEncodingException, throwing RuntimeException", e);
            throw new RuntimeException(e);
        }

        // if the encoding differs from default, we need to re-read the file with the specified encoding
        String encoding = determineEncoding(html);
        if (encoding != null) {
            if (!encoding.equalsIgnoreCase(DEFAULT_ENCODING)) {
                logger.debug(filePath + ": encoding " + encoding + " differs from default encoding, re-reading");
                try {
                    html = readFileWithEncoding(filePath, encoding);
                } catch (UnsupportedEncodingException e) {
                    logger.debug(filePath + ": unsupported encoding: " + encoding + " using " + DEFAULT_ENCODING + " " + e.getMessage());
                }
            } else {
                logger.debug(filePath + ": file has default encoding " + DEFAULT_ENCODING);
            }
        } else {
            logger.debug(filePath + ": could not determine encoding, assuming default " + DEFAULT_ENCODING);
        }

        System.out.println("used encoding =======> " + encoding);

        return html;
    }

    /**
     * Determine encoding of supplied HTML file.
     * 
     * @param filePath
     * @return
     * @throws IOException
     */
    public static String getEncodingForHtmlFile(String filePath) throws IOException {
        String html = readFileWithEncoding(filePath, DEFAULT_ENCODING);
        return determineEncoding(html);
    }

    /**
     * Try to get encoding from HTML text content. Returns <code>null</code> if encoding can not be determined, else the determined encoding as lowercase.
     * 
     * TODO this could be improved and extended to use "Mozilla's Automatic Charset Detection algorithm", see: http://jchardet.sourceforge.net/
     * 
     * @param html
     * @return
     */
    private static String determineEncoding(String html) {
        // String encoding = null;
        //		
        // Matcher htmlMatcher = htmlMetaTag.matcher(html);
        // Matcher html5Matcher = html5MetaTag.matcher(html);
        // Matcher xmlMatcher = xmlDeclaration.matcher(html);
        //		
        // Matcher encodingMatcher = null;
        // if (htmlMatcher.find()) {
        // logger.trace("determine encoding from html meta http-equiv " + htmlMatcher.group());
        // encodingMatcher = htmlMetaEncoding.matcher(htmlMatcher.group());
        // }
        // else if (html5Matcher.find()) {
        // logger.trace("determine encoding from html5 meta charset " + html5Matcher.group());
        // encodingMatcher = html5MetaEncoding.matcher(html5Matcher.group());
        // }
        // else if (xmlMatcher.find()) {
        // logger.trace("determine encoding from xml declaration " + xmlMatcher.group());
        // encodingMatcher = xmlDeclarationEncoding.matcher(xmlMatcher.group());
        // }
        // else {
        // logger.trace("could not determine encoding from header tags");
        // }
        //		
        // if (encodingMatcher != null && encodingMatcher.find() && encodingMatcher.groupCount() == 2) {
        // encoding = encodingMatcher.group(2).toLowerCase();
        // } // else { ... here we could use Mozilla's encoding detection as fallback ... }
        //
        // return encoding;

        String encoding = null;

        try {

            DOMParser parser = new DOMParser(new HTMLConfiguration());

            parser.setFeature("http://cyberneko.org/html/features/insert-namespaces", true);
            parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
            parser.setProperty("http://cyberneko.org/html/properties/names/attrs", "lower");

            parser.parse(new InputSource(new StringInputStream(html)));
            Document document = parser.getDocument();

            // HTML <meta http-equiv="content-type" content="text/html; charset=iso-8859-1" />
            // case insensitive match of attributes value ...:
            Node htmlMeta = XPathHelper.getNode(document,
                    "//meta[translate(@http-equiv,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='content-type']/@content");

            // HTML5 <meta charset="iso-8859-1">
            Node html5Meta = XPathHelper.getNode(document, "//meta[@charset]/@charset");

            // XHTML <?xml version="1.0" encoding="ISO-8859-1"?>
            String xmlEncoding = document.getXmlEncoding();

            if (htmlMeta != null) {
                String str = htmlMeta.getTextContent();
                encoding = str.substring(str.indexOf("=") + 1, str.length()).trim();
                logger.trace("determine encoding from html meta http-equiv " + encoding);
            } else if (html5Meta != null) {
                encoding = html5Meta.getTextContent();
                logger.trace("determine encoding from html5 meta charset " + encoding);
            } else if (xmlEncoding != null) {
                logger.trace("determine encoding from xml declaration " + xmlEncoding);
                encoding = xmlEncoding;
            }

            if (encoding != null) {
                encoding = encoding.toLowerCase();
            } else {
                logger.trace("could not determine encoding from header tags, returning null");
            }

        } catch (SAXNotRecognizedException e) {
            logger.error("determineEncoding", e);
        } catch (SAXNotSupportedException e) {
            logger.error("determineEncoding", e);
        } catch (SAXException e) {
            logger.error("determineEncoding", e);
        } catch (IOException e) {
            logger.error("determineEncoding", e);
        }

        return encoding;

    }

    public static String htmlDocToString(Document doc) {
        final StringBuilder builder = new StringBuilder();
        try {
            TransformerFactory transFac = TransformerFactory.newInstance();
            Transformer trans = transFac.newTransformer();
            trans.transform(new DOMSource(doc), new SAXResult(new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if (isBlockElement(localName) || localName.equals("br")) {
                        builder.append("\n");
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if (isBlockElement(localName)) {
                        builder.append("\n");
                    }
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    builder.append(ch);
                }
            }));
        } catch (TransformerConfigurationException e) {
            logger.error("htmlDocToString:TransformerConfigurationException", e);
        } catch (TransformerFactoryConfigurationError e) {
            logger.error("htmlDocToString:TransformerFactoryConfigurationError", e);
        } catch (TransformerException e) {
            logger.error("htmlDocToString:TransformerException", e);
        }
        String result = builder.toString();
        // result = result.replaceAll("[ \t]*?\n", "\n");
        result = normalizeLines.matcher(result).replaceAll("");
        result = result.replaceAll("\n{3,}", "\n\n");
        result = result.replaceAll(" {2,}", " ");
        result = result.trim();
        return result;
    }

    private static boolean isBlockElement(String elementName) {
        return Arrays.asList(BLOCK_ELEMENTS).contains(elementName.toLowerCase());
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

    }
}
