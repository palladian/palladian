/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import tud.iir.helper.StringHelper;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;

/**
 * The GeneralAnalyzer provides often used analyzing-methods.
 * 
 * @author Martin Werner
 */
public class GeneralAnalyzer {

    // TODO Think over to change the methods to final for override-protection or
    // to protected for only package usage

    /**
     * Get WebPage as String.
     * 
     * @param urlString the uRL string
     * @param removeJS the remove js
     * @return the page
     */
    public String getPage(final String urlString) {
        String pageString = "";
        // // Crawler craw = new Crawler(5000, 5000, 10000);
        final Crawler craw = new Crawler();

        // page = craw.download(URLString, false, false, false, false);
        pageString = craw.downloadNotBlacklisted(urlString);

        return pageString;
    }

    /**
     * Extract values e.g for: src=, href= or title=
     * 
     * @param pattern the pattern
     * @param content the content
     * @param removeTerm the remove term
     * @return the string
     */
    public String extractElement(final String pattern, final String content, final String removeTerm) {
        String element = "";
        final Pattern elementPattern = Pattern.compile(pattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        final Matcher elementMatcher = elementPattern.matcher(content);
        while (elementMatcher.find()) {
            String result = elementMatcher.group(0);
            if (!removeTerm.equals("")) {
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
     * Check URL for validness and eventually modify e.g. relative path
     * 
     * @param urlCandidate the url candidate
     * @param pageURL the page url
     * @return the string
     */
    public String verifyURL(final String urlCandidate, final String pageURL) {

        String returnValue = "";
        // Crawler crawler = new Crawler(5000, 5000, 10000);

        final String modUrlCandidate = urlCandidate.trim();
        if (modUrlCandidate.startsWith("http://")) {
            if (Crawler.isValidURL(modUrlCandidate, false)) {
                return modUrlCandidate;
            }
        } else {

            if (modUrlCandidate.length() > 2) {
                final String modifiedURL = Crawler.makeFullURL(pageURL, modUrlCandidate);
                if (Crawler.isValidURL(modifiedURL, false)) {
                    return modifiedURL;
                }
            }

        }
        return returnValue;
    }

    /**
     * Extract alt text from tag.
     * 
     * @param relevantTag the relevant tag
     * @return the string
     */
    public String extractALTTextFromTag(final String relevantTag) {
        String altText = "";
        final int beginIndex = relevantTag.indexOf(">") + 1;

        final int endIndex = relevantTag.lastIndexOf("<");
        if (beginIndex < endIndex) {
            final String modRelevantTag = relevantTag.substring(beginIndex, endIndex);
            // StringHelper stringHelper = new StringHelper();
            altText = StringHelper.removeHTMLTags(modRelevantTag, true, true, false, true);
        }

        return altText;
    }

    public MIO extractSurroundingInfos(final String relevantTag, final MIOPage mioPage, final MIO mio) {
        // XPathHelper xpathHelper = new XPathHelper();

        List<String> previousHeadlines = new ArrayList<String>();
        final String lowRelevantTag = relevantTag.toLowerCase(Locale.ENGLISH);
        final Crawler crawler = new Crawler();
        final Document document = crawler.getWebDocument(mioPage.getUrl());
        mioPage.setTitle(Crawler.extractTitle(document));
        String xPath = "";

        if (XPathHelper.hasXMLNS(document)) {

            // if (lowRelevantTag.startsWith("<script")) {
            // xPath = "//BODY//SCRIPT";
            // } else {
            if (lowRelevantTag.startsWith("<object")) {
                xPath = "//BODY//OBJECT";
            } else if (lowRelevantTag.startsWith("<embed")) {
                xPath = "//BODY//EMBED";
            } else {
                if (lowRelevantTag.startsWith("<applet")) {
                    xPath = "//BODY//APPLET";
                }
                // }
            }
            // nodes = new ArrayList<Node>();
            final List<Node> nodes = XPathHelper.getNodes(document, xPath);
            // System.out.println("Anzahl nodes: " + nodes.size());

            for (Node node : nodes) {

                final String nodeString = XPathHelper.convertNodeToString(node);

                if (nodeString.toLowerCase(Locale.ENGLISH).contains(mio.getFileName())) {
                    Node tempNode = node;

                    while (previousHeadlines.isEmpty() && !tempNode.getLocalName().equals("BODY")) {

                        previousHeadlines = extractPreviousHeadlines(tempNode);
                        tempNode = tempNode.getParentNode();
                    }

                    if (!previousHeadlines.isEmpty()) {

                        mio.addInfos("previousHeadlines", previousHeadlines);
                    }

                    // extract surrounding TextContent
                    String surroundingText = "";
                    tempNode = node;
                    while (surroundingText.length() < 2 && !tempNode.getLocalName().equals("BODY")) {
                        surroundingText = extractNearTextContent(tempNode);
                        tempNode = tempNode.getParentNode();

                    }
                    if (surroundingText.length() > 2) {
                        final List<String> textList = new ArrayList<String>();
                        textList.add(surroundingText);
                        mio.addInfos("surroundingText", textList);

                    }

                    break;
                }
            }
        }
        // else {
        // System.out.println("no XHTML NAMESPACE");
        // }

        return mio;
    }

    private List<String> extractPreviousHeadlines(final Node node) {
        final List<String> headlines = new ArrayList<String>();
        // StringBuffer hrContent = new StringBuffer();
        final List<Node> siblingNodes = XPathHelper.getPreviousSiblings(node);
        // System.out.println(siblingNodes.size() + " previousSiblings");
        for (Node siblingNode : siblingNodes) {
            // System.out.println(siblingNode.getLocalName());
            if (siblingNode.getLocalName().matches("H[0-6]")) {
                // hrContent.append();
                headlines.add(siblingNode.getTextContent());
            }
        }

        return headlines;
    }

    private String extractNearTextContent(final Node node) {
        final Node parentNode = node.getParentNode();
        String text = parentNode.getTextContent();
        // remove Comments
        text = StringHelper.removeConcreteHTMLTag(text, "<!--", "-->");
        // trim
        text = StringHelper.trim(text);
        return text;

    }

    public static void main(String[] args) {

        // GeneralAnalyzer geAn = new GeneralAnalyzer();
        // String relevantTag =
        // "<object type=\"text/javascript\">showSpin(\"http://pic.gsmarena.com/vv/spin/samsung-wave.swf\");</script>";
        // MIOPage mioPage = new MIOPage("data/test/webPages/headlineTest.html", "nix");
        // Concept concept = new Concept("mobilePhone");
        // Entity entity = new Entity("Samsung S8500 Wave", concept);
        // MIO mio = new MIO("FLASH", "", "", entity);
        // mio.setFileName("samsung-wave.swf");
        // mio = geAn.extractSurroundingInfos(relevantTag, mioPage, mio);
        // System.out.println(mio.getInfos().toString());
    }

}
