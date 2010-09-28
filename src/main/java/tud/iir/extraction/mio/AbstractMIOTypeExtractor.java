/**
 * This is an abstract class that is inherit by all MIOExtractors (e.g. FlashExtractor, SilverlightExtractor).
 * It contains needed methods and often used functionalities for extracting MIO-Information. 
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

import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StringHelper;
import tud.iir.helper.XPathHelper;
import tud.iir.knowledge.Entity;
import tud.iir.web.Crawler;

public abstract class AbstractMIOTypeExtractor {

    /**
     * Extract MIOs by type.
     * 
     * @param mioPage the mioPage
     * @param entity the entity
     * @return the list of extracted MIOs
     */
   abstract List<MIO> extractMIOsByType(final MIOPage mioPage, final Entity entity);

    /**
     * Extract relevant HTML-tags.
     * 
     * @param mioPageContent the mioPageContent
     * @return the list of relevant tags
     */
   abstract List<String> extractRelevantTags(final String mioPageContent);

    /**
     * Analyze relevant tags.
     * 
     * @param relevantTags the relevant tags
     * @return the list with extracted MIOs
     */
    abstract List<MIO> analyzeRelevantTags(final List<String> relevantTags);

    /**
     * Extract the MIO-URLs from a relevant Tag and generate MIO-Objects.
     * 
     * @param content the content
     * @param mioPage the MIOPage
     * @param urlPattern the URL-RegEx-Pattern
     * @param entity the entity
     * @param mioType the MIO-Type
     * @return the list
     */
    protected List<MIO> extractMioURL(final String content, final MIOPage mioPage, final String urlPattern,
            final Entity entity, final String mioType) {

        final List<MIO> resultList = new ArrayList<MIO>();

        final String regExp = urlPattern;

        final Pattern pattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        // remove wrong-leading-attributes like name="140.swf"
        final String modConcreteTag = content.replaceAll("name=\".[^\"]*\"", "");

        final Matcher matcher = pattern.matcher(modConcreteTag);
        while (matcher.find()) {
            final String mioAdr = matcher.group(0).replaceAll("\"", "");
            // System.out.println("URL: "+ mioAdr);
            final String mioURL = Crawler.verifyURL(mioAdr, mioPage.getUrl());
            final String testString = mioURL.toLowerCase(Locale.ENGLISH);

            if (!(testString.contains("expressinstall") || testString.contains("banner") || testString
                    .contains("header"))) {

                if (mioURL.length() > 4 && mioURL.contains(".")) {
                    final MIO mio = new MIO(mioType, mioURL, mioPage.getUrl(), entity);
                    resultList.add(mio);
                }
            }
        }
        return resultList;
    }

    /**
     * Extract surrounding information of a relevant tag(previous headlines, surrounding text).
     * 
     * @param relevantTag the relevant tag
     * @param mioPage the mioPage
     * @param mio the MIO
     */
    protected void extractSurroundingInfo(final String relevantTag, final MIOPage mioPage, final MIO mio) {

        final List<String> previousHeadlines = new ArrayList<String>();
        final String lowRelevantTag = relevantTag.toLowerCase(Locale.ENGLISH);
        final Document document = mioPage.getWebDocument();

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
                } else {
                    if (lowRelevantTag.startsWith("<html5")) {
                        xPath = "//BODY//CANVAS";
                    }
                }
            }
        }

        final List<Node> nodes = XPathHelper.getNodes(document, xPath);

        for (Node node : nodes) {

            final String nodeString = XPathHelper.convertNodeToString(node);

            if (nodeString.contains(mio.getFileName())) {
                Node tempNode = node;
                
                //try to find previous headlines
                while (previousHeadlines.isEmpty() && !tempNode.getNodeName().equals("BODY")) {
                    previousHeadlines.addAll(extractPreviousHeadlines(tempNode));
                    tempNode = tempNode.getParentNode();
                }

                if (!previousHeadlines.isEmpty()) {
                    mio.setPreviousHeadlines(previousHeadlines.toString());
                }

                // extract surrounding TextContent
                String surroundingText = "";
                tempNode = node;

                while (surroundingText.length() < 2 && !tempNode.getNodeName().equals("BODY")) {
                    surroundingText = extractNearTextContent(tempNode);
                    tempNode = tempNode.getParentNode();
                }

                if (surroundingText.length() > 2) {
                    mio.setSurroundingText(surroundingText);
                }
                break;
            }
        }
    }

    /**
     * Extract previous headlines.
     * 
     * @param node the node
     * @return the list
     */
    private List<String> extractPreviousHeadlines(final Node node) {
        final List<String> headlines = new ArrayList<String>();

        final List<Node> siblingNodes = XPathHelper.getPreviousSiblings(node);
        for (Node siblingNode : siblingNodes) {

            if (siblingNode.getNodeName().matches("H[0-6]")) {
                headlines.add(siblingNode.getTextContent());
            }
        }
        return headlines;
    }

    /**
     * Extract surrounding text content.
     * 
     * @param node the node
     * @return the string
     */
    private String extractNearTextContent(final Node node) {
        final Node parentNode = node.getParentNode();
        String text = parentNode.getTextContent();

        // remove Comments
        text = HTMLHelper.removeConcreteHTMLTag(text, "<!--", "-->");
        // trim
        text = StringHelper.trim(text);
        return text;

    }

    /**
     * Extract xml info.
     * 
     * @param relevantTag the relevant tag
     * @param mio the mio
     */
//    protected void extractXMLInfo(final String relevantTag, final MIO mio) {
//        if (relevantTag.toLowerCase(Locale.ENGLISH).contains(".xml")) {
//            
//            try {
//                FileHelper.appendFile("f:/xmlinfos.txt", mio.getFindPageURL() + "\r\n");
//                FileHelper.appendFile("f:/xmlinfos.txt", relevantTag + "\r\n");
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//
//            extractXMLNameFromTag(relevantTag, mio);
//            extractXMLFileURLFromTag(relevantTag, mio);
//        }
//
//    }

    /**
     * Extract xml name from tag.
     * 
     * @param relevantTag the relevant tag
     * @param mio the mio
     */
//    private void extractXMLNameFromTag(final String relevantTag, final MIO mio) {
//
//        // TODO regExp überprüfen auf verschiedene formate
//        String regExp = "\\\\.[^\"\\\\]*\\.xml";
//        Pattern pattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
//        final Matcher matcher = pattern.matcher(relevantTag);
//        final List<String> xmlFileNames = new ArrayList<String>();
//        while (matcher.find()) {
//            final String xmlName = matcher.group(0).replaceAll("\"", "");
//            // FileHelper.appendFile("f:/xmlcontent.html",xmlName + " <br>");
//            xmlFileNames.add(xmlName);
//
//        }
//        if (!xmlFileNames.isEmpty()) {
//      //      mio.addInfos("xmlFileName", xmlFileNames);
//            try {
//                FileHelper.appendFile("f:/xmlinfos.txt", mio.getFindPageURL() + "\r\n");
//                FileHelper.appendFile("f:/xmlinfos.txt", "XMLName: "+xmlFileNames.toString() + "\r\n");
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//
//    }

    /**
     * Extract xml file url from tag.
     * 
     * @param relevantTag the relevant tag
     * @param mio the mio
     */
//    private void extractXMLFileURLFromTag(final String relevantTag, final MIO mio) {
//
//        // TODO regExp überprüfen auf verschiedene formate
//        final String regExp = "\".[^\"]*\\.xml\"";
//        final Pattern pattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
//        final Matcher matcher = pattern.matcher(relevantTag);
//        final List<String> xmlFileURLs = new ArrayList<String>();
//        while (matcher.find()) {
//            String xmlName = matcher.group(0).replaceAll("\"", "");
////            xmlName = Crawler.verifyURL(xmlName, mio.getFindPageURL());
//            if (!xmlName.equals("")) {
//                xmlFileURLs.add(xmlName);
//                try {
//                    FileHelper.appendFile("f:/xmlinfos.txt", mio.getFindPageURL() + "\r\n");
//                    FileHelper.appendFile("f:/xmlinfos.txt", xmlName + "\r\n");
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//
//        }
//
//        if (!xmlFileURLs.isEmpty()) {
//         //   mio.addInfos("xmlFileURL", xmlFileURLs);
//
//        }
//
//    }

    /**
     * Extract ALT-text from relevant tag.
     * 
     * @param relevantTag the relevant tag
     * @return the string
     */
    protected String extractALTTextFromTag(final String relevantTag) {
        String altText = "";
        final int beginIndex = relevantTag.indexOf(">") + 1;

        final int endIndex = relevantTag.lastIndexOf("<");
        if (beginIndex < endIndex) {
            final String modRelevantTag = relevantTag.substring(beginIndex, endIndex);
            altText = HTMLHelper.removeHTMLTags(modRelevantTag, true, true, false, true);
        }
        return altText;
    }

}
