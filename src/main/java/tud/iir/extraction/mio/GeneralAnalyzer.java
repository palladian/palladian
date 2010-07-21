/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
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

        final Crawler craw = new Crawler();
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
     * Extract alternative text (ALT-Text) from tag.
     * 
     * @param relevantTag the relevant XHTML-Tag
     * @return the string
     */
    public String extractALTTextFromTag(final String relevantTag) {
        String altText = "";
        final int beginIndex = relevantTag.indexOf(">") + 1;

        final int endIndex = relevantTag.lastIndexOf("<");
        if (beginIndex < endIndex) {
            final String modRelevantTag = relevantTag.substring(beginIndex, endIndex);
            // StringHelper stringHelper = new StringHelper();
            altText = HTMLHelper.removeHTMLTags(modRelevantTag, true, true, false, true);
        }

        return altText;
    }

    public MIO extractSurroundingInfos(final String relevantTag, final MIOPage mioPage, final MIO mio) {

        List<String> previousHeadlines = new ArrayList<String>();
        final String lowRelevantTag = relevantTag.toLowerCase(Locale.ENGLISH);
        final Crawler crawler = new Crawler();
        final Document document = crawler.getWebDocument(mioPage.getUrl());
        mioPage.setTitle(Crawler.extractTitle(document));
        String xPath = "";

        if (lowRelevantTag.startsWith("<script")) {
            xPath = "//BODY//SCRIPT";
        } else {
            if (lowRelevantTag.startsWith("<object")) {
                xPath = "//BODY//OBJECT";
            } else if (lowRelevantTag.startsWith("<embed")) {
                xPath = "//BODY//EMBED";
            } else {
                if (lowRelevantTag.startsWith("<applet")) {
                    xPath = "//BODY//APPLET";
                }
            }
        }

        final List<Node> nodes = XPathHelper.getNodes(document, xPath);
        // System.out.println("Anzahl nodes: " + nodes.size());

        for (Node node : nodes) {

            final String nodeString = XPathHelper.convertNodeToString(node);

            if (nodeString.contains(mio.getFileName())) {
                Node tempNode = node;

                while (previousHeadlines.isEmpty() && !tempNode.getNodeName().equals("BODY")) {

                    previousHeadlines.addAll(extractPreviousHeadlines(tempNode));
                    tempNode = tempNode.getParentNode();
                }

                if (!previousHeadlines.isEmpty()) {
                    mio.addInfos("previousHeadlines", previousHeadlines);
                }

                // extract surrounding TextContent
                String surroundingText = "";
                tempNode = node;
                while (surroundingText.length() < 2 && !tempNode.getNodeName().equals("BODY")) {
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

        return mio;
    }

    private List<String> extractPreviousHeadlines(final Node node) {
        final List<String> headlines = new ArrayList<String>();
        // StringBuffer hrContent = new StringBuffer();

        final List<Node> siblingNodes = XPathHelper.getPreviousSiblings(node);
        // System.out.println(siblingNodes.size() + " previousSiblings");
        for (Node siblingNode : siblingNodes) {

            if (siblingNode.getNodeName().matches("H[0-6]")) {
                // hrContent.append();
                // System.out.println(siblingNode.getTextContent());
                headlines.add(siblingNode.getTextContent());
            }
        }

        return headlines;
    }

    private String extractNearTextContent(final Node node) {
        final Node parentNode = node.getParentNode();
        String text = parentNode.getTextContent();

        // remove Comments
        text = HTMLHelper.removeConcreteHTMLTag(text, "<!--", "-->");
        // trim
        text = StringHelper.trim(text);
        return text;

    }

    public MIO extractXMLInfos(final String relevantTag, final MIO mio) {

        if (relevantTag.toLowerCase(Locale.ENGLISH).contains(".xml")) {

            try {
                FileHelper.appendFile("f:/xmlcontent.html", mio.getEntity().getName() + " " + mio.getDirectURL()
                        + " <br>");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // String regExp = "\".[^\"]*\\.xml\"";
            // final Pattern pattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            // final Matcher matcher = pattern.matcher(relevantTag);
            // while (matcher.find()) {
            // final String xmlName = matcher.group(0).replaceAll("\"", "");
            // }
        }

        return mio;

    }

    public static void main(String[] args) {
        //
        // GeneralAnalyzer geAn = new GeneralAnalyzer();
        // String relevantTag =
        // "<script type=\"text/javascript\">showSpin(\"http://pic.gsmarena.com/vv/spin/samsung-wave-final.swf\");</script>";
        // MIOPage mioPage = new MIOPage("data/test/webPages/headlineTest.html", "nix");
        // Concept concept = new Concept("mobilePhone");
        // Entity entity = new Entity("Samsung S8500 Wave", concept);
        // MIO mio = new MIO("FLASH", "", "", entity);
        // mio.setFileName("samsung-wave-final.swf");
        // mio = geAn.extractSurroundingInfos(relevantTag, mioPage, mio);
        // System.out.println(mio.getInfos().toString());
    }

}
