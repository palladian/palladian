package tud.iir.news;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
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
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import tud.iir.helper.FileHelper;

/**
 * Various more or less feed specific helper functions.
 * TODO most of these methods can be moved to the global Helper classes.
 * 
 * @author Philipp Katz
 * 
 */
public class Helper {

    // /**
    // * Namespace context for parsing result documents using xhtml namespace with XPath. See:
    // http://www.ibm.com/developerworks/library/x-javaxpathapi.html
    // * http://www.ibm.com/developerworks/xml/library/x-nmspccontext/index.html
    // *
    // * @author pk
    // *
    // */
    // public static class XhtmlNamespaceContext implements NamespaceContext {
    // @SuppressWarnings("unchecked")
    // @Override
    // public Iterator getPrefixes(String namespaceURI) {
    // // not used
    // throw new UnsupportedOperationException();
    // }
    //
    // @Override
    // public String getPrefix(String namespaceURI) {
    // // not used
    // throw new UnsupportedOperationException();
    // }
    //
    // @Override
    // public String getNamespaceURI(String prefix) {
    // if (prefix.equals("xhtml")) {
    // return "http://www.w3.org/1999/xhtml";
    // }
    // return XMLConstants.NULL_NS_URI;
    // }
    // }

    /** class logger */
    private static final Logger LOGGER = Logger.getLogger(Helper.class);

    /** prevent instantiation */
    private Helper() {
    }

    // //////////////////////
    // helper methods
    public static String xmlToString(Node node) {
        // try {
        // Source source = new DOMSource(node);
        // StringWriter stringWriter = new StringWriter();
        // Result result = new StreamResult(stringWriter);
        // TransformerFactory factory = TransformerFactory.newInstance();
        // Transformer transformer = factory.newTransformer();
        // transformer.transform(source, result);
        // return stringWriter.getBuffer().toString();
        // } catch (TransformerConfigurationException e) {
        // e.printStackTrace();
        // } catch (TransformerException e) {
        // e.printStackTrace();
        // }
        // return null;
        return xmlToString(node, false, false);
    }

    /**
     * Converts a DOM Node or Document into a String.
     * 
     * @param node
     * @param removeWhitespace whether to remove superfluous whitespace outside of tags.
     * @param prettyPrint wheter to nicely indent the result.
     * @return String representation of the supplied Node, empty String in case of errors.
     */
    public static String xmlToString(Node node, boolean removeWhitespace, boolean prettyPrint) {
        String strResult = "";
        try {

            if (removeWhitespace) {
                node = removeWhitespace(node);
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

    /**
     * Remove unneccessary whitespace from DOM nodes.
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
        String string = xmlToString(node);
        // try {
        // TODO use FileHelper.write... instead? (one fewer library to include)
        // FileUtils.writeStringToFile(new File(filename), string);
        FileHelper.writeToFile(filename, string);
        // } catch (IOException e) {
        // logger.error("writeXmlDump", e);
        // }
    }

    // /**
    // * Return the root URL -> http://www.domain.com TODO there is already a method in {@link Crawler#getDomain(String,
    // boolean)}
    // *
    // * @param urlString
    // * @return
    // *
    // * @deprecated use {@link Crawler#getDomain(String, true)}
    // */
    // @Deprecated
    // public static String getRootUrl(String urlString) {
    // String result = "";
    // try {
    // URL url = new URL(urlString);
    // result = url.getProtocol() + "://" + url.getHost();
    // LOGGER.trace("root url for " + urlString + " -> " + result);
    // } catch (MalformedURLException e) {
    // // e.printStackTrace();
    // LOGGER.error("could not convert url " + urlString);
    // }
    // return result;
    // }

    // /**
    // * Handling links in HTML documents can be tricky. If no absolute URL is specified there are two factors for which
    // we have to take care:<BR>
    // * <BR>
    // * 1) The document's URL<BR>
    // * 2) If provided, a base URL inside the document, which can be as well be absolute or relative to 1)
    // *
    // * See: http://www.mediaevent.de/xhtml/base.html
    // *
    // * @param pageUrl TODO
    // * @param baseUrl TODO
    // * @param linkUrl TODO
    // *
    // * @deprecated moved to {@link Crawler#makeFullURL(String, String, String)}
    // *
    // * @return the absolute URL
    // */
    // @Deprecated
    // public static String getFullUrl(String pageUrl, String baseUrl, String linkUrl) {
    // LOGGER.trace(">getFullUrl " + pageUrl + " " + baseUrl + " " + linkUrl);
    // String result = null;
    // try {
    // // let's java.net.URL do all the conversion work from relative to absolute
    // URL thePageUrl = new URL(pageUrl);
    // if (baseUrl == null) {
    // baseUrl = "";
    // } else if (!baseUrl.endsWith("/")) {
    // baseUrl = baseUrl.concat("/");
    // }
    // // baseUrl relative to pageUrl
    // URL theBaseUrl = new URL(thePageUrl, baseUrl);
    // // linkUrl relative to pageUrl+baseUrl
    // URL theLinkUrl = new URL(theBaseUrl, linkUrl);
    // result = theLinkUrl.toString();
    // } catch (MalformedURLException e) {
    // LOGGER.error("getFullUrl", e);
    // }
    // LOGGER.trace("<getFullUrl " + result);
    // return result;
    // }

    // /**
    // * @deprecated moved to {@link Crawler#makeFullURL(String, String)}
    // * @param pageUrl
    // * @param linkUrl
    // * @return
    // */
    // @Deprecated
    // public static String getFullUrl(String pageUrl, String linkUrl) {
    // return getFullUrl(pageUrl, "", linkUrl);
    // }

    // /**
    // * Simple approach for stripping out unwanted HTML tags.
    // * @param htmlString
    // * @return
    // * @deprecated use {@link StringHelper#removeHTMLTags(String, boolean, boolean, boolean, boolean)}
    // */
    // @Deprecated
    // public static String stripHtml(String htmlString) {
    // return htmlString.replaceAll("\\<.*?>", "").replaceAll("(\n\r?)+", "\n").trim();
    // }

    // // ///////////////
    // // from http://www.javadb.com/write-lines-of-text-to-file-using-a-printwriter
    // public static void writeLineToFile(String filename, String lineToWrite, boolean appendToFile) {
    // PrintWriter pw = null;
    // try {
    // if (appendToFile) {
    // pw = new PrintWriter(new FileWriter(filename, true));
    // } else {
    // pw = new PrintWriter(new FileWriter(filename));
    // }
    // pw.println(lineToWrite);
    // pw.flush();
    // } catch (IOException e) {
    // e.printStackTrace();
    // } finally {
    // if (pw != null) {
    // pw.close();
    // }
    // }
    // }

    //
    // see FileHelper
    //
    // public static void deleteFile(String filename) {
    // File file = new File(filename);
    // if (file.exists() && file.canWrite() && !file.isDirectory()) {
    // file.delete();
    // logger.trace("deleted " + filename);
    // }
    // }
    //	
    /**
     * @deprecated was already existing in DateHelper Get a human readable duration string from milliseconds. For
     *             example: 273823872 -> 3d 4h 3min 43s 872ms
     * 
     * @param msInput
     * @return
     */
    /*
     * public static String getDurationString(long msInput) { int ms = (int) (msInput % 1000); int s = (int) ((msInput /
     * 1000) % 60); int min = (int) ((msInput
     * / 1000 / 60) % 60); int h = (int) ((msInput / 1000 / 60 / 60) % 24); long d = (msInput / 1000 / 60 / 60 / 24);
     * StringBuilder sb = new StringBuilder(); if
     * (d > 0) { sb.append(d).append("d "); } if (d > 0 || h > 0) { sb.append(h).append("h "); } if (d > 0 || h > 0 ||
     * min > 0) { sb.append(min).append("min ");
     * } if (d > 0 || h > 0 || min > 0 || s > 0) { sb.append(s).append("s "); } sb.append(ms).append("ms"); return
     * sb.toString(); }
     */

    /**
     * Shorten a String; returns the first num words.
     * 
     * @param string
     * @param num
     * @return
     */
    public static String getFirstWords(String string, int num) {
        StringBuilder sb = new StringBuilder();
        if (string != null && num > 0) {
            String[] split = string.split("\\s");
            sb.append(split[0]);
            for (int i = 1; i < Math.min(num, split.length); i++) {
                sb.append(" ").append(split[i]);
            }
        }
        return sb.toString();
    }

    /**
     * Split an XPath expression into its components, separated by / We have to take care not to split inside quotes.
     * 
     * TODO works for single quotes now, does NOT work for "double" quotes.
     * 
     * @param xPath
     * @return
     */
    public static String[] splitXPath(String xPath) {
        return xPath.split("(/)(?=\\w(?:[^']|'[^']*')*$)");
    }

    /**
     * Determines the longest common XPath from the beginning, in other words: We will return an XPath to an elements
     * which contains both of the specified
     * elements.<BR>
     * <BR>
     * Example: //a/b/c/d and //a/b/e/f -> //a/b
     * 
     * @param xPath1
     * @param xPath2
     * @return
     */
    public static String getLargestCommonXPath(String xPath1, String xPath2) {
        StringBuilder sb = new StringBuilder();
        String[] split1 = splitXPath(xPath1);
        String[] split2 = splitXPath(xPath2);
        int length = Math.min(split1.length, split2.length);
        if (length > 0 && split1[0].equals(split1[0])) {
            sb.append(split1[0]);
            for (int i = 1; i < length; i++) {
                if (split1[i].equals(split2[i])) {
                    sb.append("/").append(split1[i]);
                } else {
                    break;
                }
            }
        }
        return sb.toString();
    }

    /**
     * Count number of occurences of pattern withing text. TODO this will fail if pattern contains RegEx metacharacters.
     * Need to escape.
     * 
     * @param text
     * @param pattern
     * @param ignoreCase
     * @return
     */
    public static int countOccurences(String text, String pattern, boolean ignoreCase) {
        if (ignoreCase) {
            text = text.toLowerCase();
            pattern = pattern.toLowerCase();
        }
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        int occurs = 0;
        while (m.find()) {
            occurs++;
        }
        return occurs;
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
        removeAll(node, nodeType, null);
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

    //
    // use DigestUtils.md5Hex
    //

    // /**
    // * MD5-Verschluesselung
    // * http://blog.databyte.at/index.php/14
    // *
    // * @param input
    // * @return
    // */
    // public static String md5(String input) {
    // StringBuffer result = new StringBuffer();
    // try {
    // MessageDigest md5 = MessageDigest.getInstance("MD5");
    // md5.update(input.getBytes());
    // Formatter f = new Formatter(result);
    // for (byte b : md5.digest()) {
    // f.format("%02x", b);
    // }
    // } catch (NoSuchAlgorithmException ex) {
    // throw new RuntimeException(ex);
    // }
    // return result.toString();
    // }

    // ///////////////////////////////////////////////////////////////////////////
    // helper methods
    // ///////////////////////////////////////////////////////////////////////////

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

    /**
     * Calculates Levenshtein similarity between the strings.
     * 
     * @param s1
     * @param s2
     * @return similarity between 0 and 1 (inclusive).
     */
    public static float getLevenshteinSim(String s1, String s2) {
        int distance = StringUtils.getLevenshteinDistance(s1, s2);
        float similarity = 1 - (float) distance / Math.max(s1.length(), s2.length());
        return similarity;
    }

    /**
     * Determine similarity based on String lengths. We can use this as threshold before even calculating Levenshtein
     * similarity which is computationally
     * expensive.
     * 
     * @param s1
     * @param s2
     * @return similarity between 0 and 1 (inclusive).
     */
    public static float getLengthSim(String s1, String s2) {
        int length1 = s1.length();
        int length2 = s2.length();
        if (length1 == 0 && length2 == 0) {
            return 1;
        }
        return (float) Math.min(length1, length2) / Math.max(length1, length2);
    }

    private static final String[] BINARY_PREFIXES = new String[] { "Ki", "Mi", "Gi", "Ti", "Pi", "Ei", "Zi", "Yi" };

    /**
     * Format number of bytes to human readable String using IEC binary unit prefixes, for example
     * getReadibleBytes(48956748) -> 46.69 MiB
     * 
     * @param bytes
     * @return
     */
    public static String getReadibleBytes(long bytes) {
        String result;
        if (bytes < 1024) {
            result = bytes + " B";
        } else {
            float value = bytes;
            int unitIndex = -1;
            while (value >= 1024) {
                value /= 1024;
                unitIndex++;
            }
            result = String.format(Locale.ENGLISH, "%.2f", value) + " " + BINARY_PREFIXES[unitIndex] + "B";
        }
        return result;
    }

    public static void main(String[] args) {

        // Crawler c = new Crawler();
        // Document doc = c.getXMLDocument("data/test2.xml");
        // System.out.println(Helper.xmlToString(doc, true, true));
        // System.out.println(Helper.xmlToString(doc, true, false));
        //        
    }

}
