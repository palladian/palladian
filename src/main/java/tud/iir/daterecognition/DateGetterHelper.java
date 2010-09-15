package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HTTPDate;
import tud.iir.daterecognition.dates.HeadDate;
import tud.iir.daterecognition.dates.ReferenceDate;
import tud.iir.daterecognition.dates.StructureDate;
import tud.iir.daterecognition.dates.URLDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StringHelper;
import tud.iir.knowledge.KeyWords;
import tud.iir.knowledge.RegExp;
import tud.iir.web.Crawler;

/**
 * DateGetterHelper provides the techniques to find dates out of webpages. Also provides different helper methods.
 * 
 * @author Martin Gregor
 * 
 */
public final class DateGetterHelper {

    /**
     * Private Constructor.
     */
    private DateGetterHelper() {
        super();
    }

    /**
     * looks up for a date in the URL
     * 
     * @param url
     * @return a extracted Date
     */
    public static URLDate getURLDate(final String url) {
        ExtractedDate date = null;
        URLDate temp = null;
        Object[] regExpArray = RegExp.getURLRegExp();
        int index = 0;
        while (date == null && index < regExpArray.length) {
            date = getDateFromString(url, (String[]) regExpArray[index]);
            index++;
        }
        if (date != null) {
            temp = DateConverter.convert(date, DateConverter.TECH_URL);
            temp.setUrl(url);
        }
        return temp;
    }

    /**
     * Extracts date form HTTP-header, that is written in "Last-Modified"-tag.
     * 
     * @param url
     * @return The extracted Date.
     */
    public static ArrayList<HTTPDate> getHTTPHeaderDate(final String url) {
        ArrayList<HTTPDate> result = new ArrayList<HTTPDate>();
        Crawler crawler = new Crawler();
        Map<String, List<String>> headers = crawler.getHeaders(url);
        String[] keywords = KeyWords.HTPP_KEYWORDS;
        for (int i = 0; i < keywords.length; i++) {
            ArrayList<HTTPDate> temp = checkHttpTags(keywords[i], headers);
            if (temp != null) {
                result.addAll(temp);
            }
        }
        return result;
    }

    private static ArrayList<HTTPDate> checkHttpTags(String keyword, Map<String, List<String>> headers) {
        ArrayList<HTTPDate> result = new ArrayList<HTTPDate>();
        Object[] regExpArray = RegExp.getHTTPRegExp();
        ExtractedDate date = null;
        if (headers.containsKey(keyword)) {
            List<String> dateList = headers.get(keyword);
            Iterator<String> dateListIterator = dateList.iterator();
            while (dateListIterator.hasNext()) {
                String dateString = dateListIterator.next().toString();
                int index = 0;
                while (date == null && index < regExpArray.length) {
                    date = getDateFromString(dateString, (String[]) regExpArray[index]);
                    index++;
                }
                if (date != null) {
                    HTTPDate httpDate = DateConverter.convert(date, DateConverter.TECH_HTTP_HEADER);
                    // HTTPDate httpDate = DateConverter.convertToHTTPDate(date);
                    httpDate.setKeyword(keyword);
                    result.add(httpDate);
                }
            }
        }
        return result;
    }

    public static ArrayList<StructureDate> getStructureDate(Document document) {

        ArrayList<StructureDate> dates = new ArrayList<StructureDate>();

        if (document != null) {
            ArrayList<StructureDate> structureDates = getBodyStructureDates(document);
            if (structureDates != null) {
                dates.addAll(structureDates);
            }
        }
        return dates;

    }

    public static ArrayList<StructureDate> getBodyStructureDates(Document document) {
        final ArrayList<StructureDate> dates = new ArrayList<StructureDate>();
        final NodeList bodyNodeList = document.getElementsByTagName("body");
        if (bodyNodeList != null) {
            for (int i = 0; i < bodyNodeList.getLength(); i++) {
                Node node = bodyNodeList.item(i);
                ArrayList<StructureDate> childrernDates = getChildrenDates(node, 0);
                if (childrernDates != null) {
                    dates.addAll(childrernDates);
                }
            }
        }
        return dates;
    }

    public static ArrayList<StructureDate> getChildrenDates(final Node node, int depth) {
        ArrayList<StructureDate> dates = new ArrayList<StructureDate>();
        StructureDate date = null;
        if (!node.getNodeName().equalsIgnoreCase("script") && !node.getNodeName().equalsIgnoreCase("img")) {
            date = checkForDate(node);
        }
        if (date != null) {
            date.set(StructureDate.STRUCTURE_DEPTH, depth);
            dates.add(date);
        }
        final NodeList nodeList = node.getChildNodes();
        if (nodeList != null) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node childNode = null;
                ArrayList<StructureDate> childDates = null;
                if (!node.getNodeName().equalsIgnoreCase("script")) {
                    childNode = nodeList.item(i);
                    childDates = getChildrenDates(childNode, depth + 1);
                }
                if (childDates != null) {
                    dates.addAll(childDates);
                }
            }
        }

        return dates;
    }

    /**
     * Looks up in a <a title=" E.g.: <a content=''date'' property=''2010-07-14''>"> <u>TAG</u> </a> for <a
     * title=" E.g.: property=''2010-07-14''"> <u>ATTRIBUTES</u> </a>. <br>
     * Trays to find dates in the attributes. <br>
     * If a date is found, looks for a date-keywords in the other attributes. <br>
     * If one is found, we got the context for the date, otherwise we use attribute-name for context.<br>
     * <br>
     * The "href"-attribute will not be checked, because we will do this in "links-out-technique" with getURLDate().
     * 
     * @param node to check
     * @return A ExtractedDate with Context.
     */
    public static StructureDate checkForDate(final Node node) {

        StructureDate date = null;
        NamedNodeMap tag = node.getAttributes();
        if (tag != null) {
            String keyword = null;
            String dateTagName = null;
            for (int i = 0; i < tag.getLength(); i++) {
                Node attributeNode = tag.item(i);
                String nodeName = attributeNode.getNodeName();
                if (!nodeName.equalsIgnoreCase("href")) {
                    ExtractedDate t = findDate(attributeNode.getNodeValue());
                    StructureDate tempDate = DateConverter.convert(t, DateConverter.TECH_HTML_STRUC);
                    if (tempDate == null) {
                        keyword = hasKeyword(attributeNode.getNodeValue(), KeyWords.DATE_BODY_STRUC);
                    } else {
                        date = tempDate;
                        dateTagName = nodeName;
                    }
                }

            }
            if (date != null) {
                if (keyword == null) {
                    date.setKeyword(dateTagName);
                } else {
                    date.setKeyword(keyword);
                }
                date.setTag(node.getNodeName());
            }
        }
        return date;
    }

    /**
     * Finds dates in head-part of a webpage.
     * 
     * @param document
     * @return a array-list with dates.
     */
    public static ArrayList<HeadDate> getHeadDates(final Document document) {
        ArrayList<HeadDate> dates = new ArrayList<HeadDate>();
        NodeList headNodeList = document.getElementsByTagName("head");
        Node head = null;
        if (headNodeList != null) {

            for (int i = 0; i < headNodeList.getLength(); i++) {
                head = headNodeList.item(i);
                if (head.getNodeName().equalsIgnoreCase("head")) {
                    break;
                }
            }
            if (head != null) {
                NodeList headList = head.getChildNodes();
                for (int i = 0; i < headList.getLength(); i++) {
                    Node metaNode = headList.item(i);
                    if (!metaNode.getNodeName().equalsIgnoreCase("meta")) {
                        continue;
                    }

                    Node nameAttr = metaNode.getAttributes().getNamedItem("name");
                    if (nameAttr == null) {
                        nameAttr = metaNode.getAttributes().getNamedItem("http-equiv");
                    }
                    Node contentAttr = metaNode.getAttributes().getNamedItem("content");
                    if (nameAttr == null || contentAttr == null) {
                        continue;
                    }
                    String keyword = hasKeyword(nameAttr.getNodeValue(), KeyWords.HEAD_KEYWORDS);
                    if (keyword == null) {
                        continue;
                    }
                    ExtractedDate temp = findDate(contentAttr.getNodeValue(), RegExp.getHEADRegExp());
                    if (temp == null) {
                        continue;
                    }
                    HeadDate headDate = DateConverter.convert(temp, DateConverter.TECH_HTML_HEAD);
                    headDate.setKeyword(keyword);
                    headDate.setTag(nameAttr.getNodeName());
                    dates.add(headDate);
                }
            }
        }

        return dates;
    }

    /**
     * Tries to match a date in a dateformat. The format is given by the regular expressions of RegExp.
     * 
     * @param dateString a date to match.
     * @return The found format, defined in RegExp constants. <br>
     *         If no match is found return <b>null</b>.
     */
    public static ExtractedDate findDate(final String dateString) {

        return findDate(dateString, null);
    }

    /**
     * Tries to match a date in a dateformat. The format is given by the regular expressions of RegExp.
     * 
     * @param dateString a date to match.
     * @param regExpArray regular expressions of dates to match. If this is null {@link RegExp}.getAllRegExp will be
     *            called.
     * @return The found format, defined in RegExp constants. <br>
     *         If no match is found return <b>null</b>.
     */
    public static ExtractedDate findDate(final String dateString, Object[] regExpArray) {
        Object[] regExps = regExpArray;
        if (regExps == null) {
            regExps = RegExp.getAllRegExp();
        }
        ExtractedDate date = null;

        for (int i = 0; i < regExps.length; i++) {
            date = getDateFromString(dateString, (String[]) regExps[i]);
            if (date != null) {
                break;
            }
        }
        return date;
    }

    /**
     * 
     * @param dateString a date to match.
     * @return The found format, defined in RegExp constants. <br>
     *         If no match is found return <b>null</b>.
     */
    public static ArrayList<ContentDate> findALLDates(String text) {
        String dateString = text;
        ArrayList<ContentDate> contentDates = new ArrayList<ContentDate>();
        ExtractedDate date = null;
        Object[] regExps = RegExp.getAllRegExp();

        for (int i = 0; i < regExps.length; i++) {
            date = getDateFromString(dateString, (String[]) regExps[i]);
            if (date != null) {
                ContentDate cDate = DateConverter.convert(date, DateConverter.TECH_HTML_CONT);
                int index = dateString.indexOf(date.getDateString());
                cDate.set(ContentDate.DATEPOS_IN_TAGTEXT, index);
                contentDates.add(cDate);
                dateString = dateString.replace(date.getDateString(), getWhitespaces(date.getDateString()));
            }
        }
        return contentDates;
    }

    public static String getWhitespaces(String text) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Check a string for keywords. Used to look in tag-values for date-keys.
     * 
     * @param text string with possible keywords.
     * @param keys a array of keywords.
     * @return the found keyword.
     */
    public static String hasKeyword(final String text, final String[] keys) {
        String keyword = null;
        Pattern pattern;
        Matcher matcher;

        for (int i = 0; i < keys.length; i++) {
            pattern = Pattern.compile(keys[i].toLowerCase());
            matcher = pattern.matcher(text.toLowerCase());
            if (matcher.find()) {
                keyword = keys[i];
                break;
            }
        }
        return keyword;
    }

    /**
     * Finds out the separating symbol of date-string
     * 
     * @param date
     * @return
     */
    public static String getSeparator(final ExtractedDate date) {
        final String dateString = date.getDateString();
        return ExtractedDateHelper.getSeparator(dateString);
    }

    /**
     * 
     * @param string string, which is to be searched
     * @param regExp regular expression for search
     * @param offsetStart is slider for beginning substring (no negative values) - e.g. substring: "abcd" offsetStart=0:
     *            "abcd" offsetStart=1: "bcd" offsetStart=-1: "abcd"
     * @return found substring or null
     */
    public static ExtractedDate getDateFromString(final String dateString, final String[] regExp) {
        String text = HTMLHelper.replaceHTMLSymbols(StringHelper.removeDoubleWhitespaces(dateString));

        boolean hasPrePostNum = false;
        ExtractedDate date = null;
        Pattern pattern;
        Matcher matcher;
        pattern = Pattern.compile(regExp[0]);
        matcher = pattern.matcher(text);
        if (matcher.find()) {
            final int start = matcher.start();
            final int end = matcher.end();
            if (start > 0) {
                String temp = text.substring(start - 1, start);
                try {
                    Integer.parseInt(temp);
                    hasPrePostNum = true;

                } catch (NumberFormatException e) {
                }
            }
            if (end < text.length()) {
                String temp = text.substring(end, end + 1);
                try {
                    Integer.parseInt(temp);
                    hasPrePostNum = true;

                } catch (NumberFormatException e) {

                }
            }

            if (!hasPrePostNum) {
                date = new ExtractedDate(text.substring(start, end), regExp[1]);
            }

        }
        return date;
    }

    public static ArrayList<ContentDate> getContentDates(Document document) {
        ArrayList<ContentDate> dates = new ArrayList<ContentDate>();
        NodeList body = document.getElementsByTagName("body");
        String doc = HTMLHelper.htmlToString(body.item(0));

        if (body.getLength() > 0) {
            dates.addAll(enterTextnodes(body.item(0), doc, 0));
        }
        return dates;
    }

    public static ArrayList<ContentDate> enterTextnodes(Node node, String doc, int depth) {
        ArrayList<ContentDate> dates = new ArrayList<ContentDate>();

        if (node.getNodeType() == Node.TEXT_NODE) {
            dates.addAll(checkTextnode((Text) node, doc, depth));
        } else {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (!children.item(i).getNodeName().equalsIgnoreCase("script"))
                    dates.addAll(enterTextnodes(children.item(i), doc, depth + 1));
            }

        }

        return dates;
    }

    public static ArrayList<ContentDate> checkTextnode(Text node, String doc, int depth) {
        String text = node.getNodeValue();
        int index = doc.indexOf(text);

        Node parent = node.getParentNode();
        while (HTMLHelper.isSimpleElement(parent)) {
            parent = parent.getParentNode();
        }
        ArrayList<ContentDate> dates = new ArrayList<ContentDate>();
        Iterator<ContentDate> iterator = findALLDates(text).iterator();
        while (iterator.hasNext()) {
            ContentDate date = iterator.next();
            date.set(ContentDate.STRUCTURE_DEPTH, depth);
            if (index != -1) {
                date.set(ContentDate.DATEPOS_IN_DOC, index + date.get(ContentDate.DATEPOS_IN_TAGTEXT));
            }
            date.setTag(parent.getNodeName());
            String keyword = findNodeKeyword(parent, KeyWords.BODY_CONTENT_KEYWORDS);
            if (keyword != null) {
                date.setKeyword(keyword);
                date.set(ContentDate.KEYWORDLOCATION, ContentDate.KEY_LOC_ATTR);
            } else {
                date = setNearestTextkeyword(text, date);
            }

            if (date.getKeyword() == null) {

                keyword = findNodeKeyword(parent, KeyWords.DATE_BODY_STRUC);

                if (keyword != null) {
                    date.setKeyword(keyword);
                    date.set(ContentDate.KEYWORDLOCATION, ContentDate.KEY_LOC_ATTR);
                }
            }
            if (date.getKeyword() == null) {
                text = HTMLHelper.htmlToString(parent.getParentNode());
                date = setNearestTextkeyword(text, date);
            }
            dates.add(date);

        }
        return dates;
    }

    public static String findNodeKeywordPart(Node node, String[] keyWords) {
        String keyword = null;
        NamedNodeMap attrMap = node.getAttributes();
        if (attrMap != null) {
            for (int j = 0; j < keyWords.length; j++) {
                String lookUp = keyWords[j];
                for (int i = 0; i < attrMap.getLength(); i++) {
                    Node attr = attrMap.item(i);
                    if (attr.getNodeValue().indexOf(lookUp) != -1) {
                        keyword = lookUp;
                        break;
                    }
                }
                if (keyword != null) {
                    break;
                }
            }
        }
        return keyword;
    }

    public static String findNodeKeyword(Node node, String[] keyWords) {
        String keyword = null;
        NamedNodeMap attrMap = node.getAttributes();
        if (attrMap != null) {
            for (int j = 0; j < keyWords.length; j++) {
                String lookUp = keyWords[j];
                for (int i = 0; i < attrMap.getLength(); i++) {
                    Node attr = attrMap.item(i);
                    if (lookUp.equalsIgnoreCase(attr.getNodeValue())) {
                        keyword = lookUp;
                        break;
                    }
                }
                if (keyword != null) {
                    break;
                }
            }
        }
        return keyword;
    }

    public static ContentDate setNearestTextkeyword(String textString, ContentDate date) {
        String text = HTMLHelper.replaceHTMLSymbols(StringHelper.removeDoubleWhitespaces(textString));
        ContentDate returnDate = date;
        String keyword = null;
        String dateString = date.getDateString();
        String[] keys = KeyWords.BODY_CONTENT_KEYWORDS;
        int dateBegin = text.indexOf(dateString);
        int dateEnd = dateBegin + dateString.length();
        int distance = 9999;
        int keyBegin;
        int keyEnd;
        int temp;

        for (int i = 0; i < keys.length; i++) {
            keyBegin = text.toLowerCase(Locale.ENGLISH).indexOf(keys[i].toLowerCase(Locale.ENGLISH));
            if (keyBegin != -1) {
                keyEnd = keyBegin + keys[i].length();
                int subBegin = Math.min(dateBegin, keyBegin);
                int subende = Math.max(dateEnd, keyEnd);
                String subText = text.substring(subBegin, subende);
                temp = StringHelper.countWhitespaces(subText);
                if (temp < distance) {
                    distance = temp;
                    keyword = keys[i];
                }
            }
        }

        if (keyword != null) {
            returnDate.setKeyword(keyword);
            returnDate.set(ContentDate.DISTANCE_DATE_KEYWORD, distance);
            returnDate.set(ContentDate.KEYWORDLOCATION, ContentDate.KEY_LOC_CONTENT);
        }
        return returnDate;
    }

    public static ArrayList<ReferenceDate> getReferenceDates(Document document) {
        return getReferenceDates(document, -1);
    }

    public static ArrayList<ReferenceDate> getReferenceDates(Document document, int maxLinks) {
        ArrayList<ReferenceDate> dates = new ArrayList<ReferenceDate>();
        if (document != null) {
            Crawler c = new Crawler();
            Iterator<String> linksTo = c.getLinks(document, true, true).iterator();
            DateGetter dateGetter = new DateGetter();
            dateGetter.setTechReference(false);
            dateGetter.setTechArchive(false);

            DateComparator dc = new DateComparator();
            DateEvaluator de = new DateEvaluator();
            int i = 0;
            while (linksTo.hasNext()) {
                String link = linksTo.next();
                dateGetter.setURL(link);
                ArrayList<ExtractedDate> referenceDates = dateGetter.getDate();
                HashMap<ExtractedDate, Double> evaluatedDates = de.evaluate(referenceDates);
                double rate = DateArrayHelper.getHighestRate(evaluatedDates);
                referenceDates = DateArrayHelper.getRatedDates(evaluatedDates, rate);
                ReferenceDate refDate = DateConverter.convert((ExtractedDate) dc.getOldestDate(referenceDates),
                        DateConverter.TECH_REFERENCE);
                refDate.set(ReferenceDate.RATE, (int) (rate * 100));
                dates.add(refDate);
                if (i == maxLinks) {
                    break;
                }
                i++;
            }
        }
        return dates;

    }
}
