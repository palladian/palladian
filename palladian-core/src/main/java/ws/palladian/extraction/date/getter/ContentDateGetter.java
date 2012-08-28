package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.comparators.ContentDateComparator;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.dates.StructureDate;
import ws.palladian.extraction.date.dates.UrlDate;
import ws.palladian.extraction.date.helper.DateExtractionHelper;
import ws.palladian.helper.DateFormat;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * This class extracts all dates out of the content of webpages.
 * </p>
 * 
 * @author Martin Gregor
 * 
 */
public class ContentDateGetter extends TechniqueDateGetter<ContentDate> {

    /** All keywords with index, that are within document's text. */
    private Map<Integer, String> keyContentMap = new HashMap<Integer, String>();
    /** Nodes with their keywords. */
    private Map<Node, String> keyAttrMap = new HashMap<Node, String>();
    /**
     * Stores the position after in a document found text. <br>
     * Used to restart search after an already found text.
     */
    private Map<String, Integer> nodeIndexMap = new HashMap<String, Integer>();
    private String doc;

    private Map<Node, StructureDate> structDateMap = new HashMap<Node, StructureDate>();
    private Map<Node, Boolean> lookedUpNodeMap = new HashMap<Node, Boolean>();

    // private HashMap<Node, Boolean> visibleNodeMap = new HashMap<Node, Boolean>();

    @Override
    public List<ContentDate> getDates(Document document) {
        List<ContentDate> result = new ArrayList<ContentDate>();
        if (document != null) {
            result = getContentDates(document);
            setFeatures(result, document);
        }
        reset();
        return result;
    }

    private void setFeatures(List<ContentDate> dates, Document document) {

        List<ContentDate> posOrder = new LinkedList<ContentDate>();
        List<ContentDate> ageOrder = new LinkedList<ContentDate>();
        for (int i = 0; i < dates.size(); i++) {
            if (dates.get(i).get(ContentDate.DATEPOS_IN_DOC) != -1) {
                posOrder.add(dates.get(i));
            }
            ageOrder.add(dates.get(i));
        }

        Collections.sort(posOrder, new ContentDateComparator());
        Collections.sort(ageOrder, new DateComparator());

        MetaDateGetter mdg = new MetaDateGetter();
        UrlDateGetter udg = new UrlDateGetter();
        List<MetaDate> metaDates = mdg.getDates(document);
        CollectionHelper.removeNulls(metaDates);
        List<UrlDate> urlDates = udg.getDates(document.getDocumentURI());
        CollectionHelper.removeNulls(urlDates);

        for (ContentDate date : dates) {

            date.setRelSize(1.0 / dates.size());

            double ordDocPos = Math.round((posOrder.indexOf(date) + 1.0) / posOrder.size() * 1000.0) / 1000.0;
            date.setOrdDocPos(ordDocPos);

            double ordAgePos = Math.round((ageOrder.indexOf(date) + 1.0) / dates.size() * 1000.0) / 1000.0;
            date.setOrdAgePos(ordAgePos);

            if (metaDates.size() > 0 && DateExtractionHelper.countDates(date, metaDates, DateExactness.DAY) > 0) {
                date.setInMetaDates(true);
            }
            if (urlDates.size() > 0 && DateExtractionHelper.countDates(date, urlDates, DateExactness.DAY) > 0) {
                date.setInUrl(true);
            }

            double relCntSame = Math.round((double)(DateExtractionHelper.countDates(date, dates, DateExactness.DAY) + 1)
                    / (double)dates.size() * 1000.0) / 1000.0;
            date.setRelCntSame(relCntSame);

            int datePosOrderAbsl = posOrder.indexOf(date);
            if (datePosOrderAbsl > 0) {
                date.setDistPosBefore(date.get(ContentDate.DATEPOS_IN_DOC)
                        - posOrder.get(datePosOrderAbsl - 1).get(ContentDate.DATEPOS_IN_DOC));
            }
            if (datePosOrderAbsl < posOrder.size() - 1) {
                date.setDistPosAfter(posOrder.get(datePosOrderAbsl + 1).get(ContentDate.DATEPOS_IN_DOC)
                        - date.get(ContentDate.DATEPOS_IN_DOC));
            }
            int dateAgeOrdAbsl = ageOrder.indexOf(date);
            if (dateAgeOrdAbsl > 0) {
                date.setDistAgeBefore(Math.round(date.getDifference(ageOrder.get(dateAgeOrdAbsl - 1), TimeUnit.HOURS)));
            }
            if (dateAgeOrdAbsl < ageOrder.size() - 1) {
                date.setDistAgeAfter(Math.round(date.getDifference(ageOrder.get(dateAgeOrdAbsl + 1), TimeUnit.HOURS)));
            }
        }
    }

    /**
     * Get dates of text-nodes of body part of document.
     * 
     * @param document
     *            Document to be searched.
     * @return List of dates.
     */
    private List<ContentDate> getContentDates(Document document) {
        List<ContentDate> dates = new ArrayList<ContentDate>();
        List<Node> nodeList = XPathHelper.getNodes(document, "//text()");

        if (!nodeList.isEmpty()) {
            NodeList body = document.getElementsByTagName("body");
            // TODO: Check if an element is visible
            // checkVisiblityOfAllNodes(body.item(0));
            // Get webpage as text (for finding position).
            this.doc = StringHelper.removeDoubleWhitespaces(replaceHtmlSymbols(HtmlHelper.documentToReadableText(body
                    .item(0))));

            setDocKeywords();

            for (int i = 0; i < nodeList.size(); i++) {

                if (nodeList.get(i).getNodeType() == Node.TEXT_NODE) {
                    Node node = nodeList.get(i);
                    Node parent = node.getParentNode();
                    if (parent.getNodeType() != Node.COMMENT_NODE && !parent.getNodeName().equalsIgnoreCase("script")
                            && !parent.getNodeName().equalsIgnoreCase("style")) {
                        dates.addAll(checkTextnode((Text)node));
                    }
                }
            }
        }

        return dates;
    }

    /**
     * Find a date in text of node.<br>
     * Node as to be a {@link Text}.
     * 
     * @param node
     *            Text-node to be searched.
     * @param doc
     *            Whole human readable document (displayed content) as string to
     *            get position of found dates.
     * @param depth
     *            Depth of node in document structure.
     * @return
     */
    private List<ContentDate> checkTextnode(Text node) {

        // String text = StringHelper.removeDoubleWhitespaces(HtmlHelper.replaceHtmlSymbols(node.getNodeValue()));

        String text = replaceHtmlSymbols(node.getNodeValue());

        int index = -1;
        Node parent = node.getParentNode();
        Node tag = parent;

        while (HtmlHelper.isSimpleElement(parent)) {
            parent = parent.getParentNode();
        }

        List<ContentDate> returnDates = new ArrayList<ContentDate>();
        List<String> textSplit = new ArrayList<String>();
        List<ContentDate> dateList = new ArrayList<ContentDate>();
        for (int i = 0, beginIndex; (beginIndex = i * 10000) < text.length(); i++) {
            int endIndex = Math.min(beginIndex + 10000, text.length());
            textSplit.add(text.substring(beginIndex, endIndex));

        }
        for (String textPart : textSplit) {
            dateList.addAll(findAllDates(textPart));
        }
        if (dateList.size() > 0) {
            Integer beginIndex = nodeIndexMap.get(text);
            if (beginIndex == null) {
                beginIndex = -1;
            }
            index = this.doc.indexOf(text, beginIndex);
            if (index != -1) {
                nodeIndexMap.put(text, index + text.length());
            }
        }

        CollectionHelper.removeNulls(dateList);

        for (ContentDate date : dateList) {

            date.setStructureDate(getStructureDate(tag));

            if (date.getStructureDate() == null && tag != parent) {
                date.setStructureDate(getStructureDate(parent));
            }

            boolean keyword3Class = true;

            date.setTagNode(parent.toString());
            date.setTag(tag.getNodeName());
            // date.setNode(tag);

            date.setSimpleTag(HtmlHelper.isSimpleElement(tag) ? "1" : "0");
            date.sethTag(HtmlHelper.isHeadlineTag(tag) ? "1" : "0");

            if (index != -1) {
                int ablsDocPos = index + date.get(ContentDate.DATEPOS_IN_TAGTEXT);
                date.set(ContentDate.DATEPOS_IN_DOC, ablsDocPos);
                date.setRelDocPos(Math.round((double)ablsDocPos / (double)doc.length() * 1000.0) / 1000.0);
            }

            String keyword = getNodeKeyword(tag);

            if (keyword.equals("") && tag != parent) {
                keyword = getNodeKeyword(parent);
            }

            if (!keyword.equals("")) {

                keyword3Class = KeyWords.getKeywordPriority(keyword) == KeyWords.OTHER_KEYWORD;
                date.set(ContentDate.KEYWORDLOCATION, ContentDate.KEY_LOC_ATTR);
                date.setKeyLoc("1");
                date.setKeyLoc201("1");
            }

            if (keyword.equals("") || keyword3Class) {
                setClosestKeyword(date);
                if (date.getKeyword() != null) {
                    date.set(ContentDate.KEYWORDLOCATION, ContentDate.KEY_LOC_CONTENT);
                    date.setKeyLoc("2");
                    date.setKeyLoc202("1");
                    keyword = date.getKeyword();
                }
            }

            if (!keyword.equals("")) {
                date.setKeyword(keyword);
                switch (KeyWords.getKeywordPriority(keyword)) {
                    case 1:
                        date.setIsKeyClass1("1");
                        break;
                    case 2:
                        date.setIsKeyClass2("1");
                        break;
                    case 3:
                        date.setIsKeyClass3("1");
                        break;

                }
            }

            returnDates.add(date);
        }
        return returnDates;
    }

    /**
     * Finds all content-keywords in a text. <br>
     * Returns a hashmap with keyword-index as key an keyword as value.
     * 
     * @param doc
     *            Text to be searched.
     * @return Hashmap with indexes and keywords.
     */
    private void setDocKeywords() {
        if (doc != null) {
            keyContentMap = new HashMap<Integer, String>();
            String text = doc.toLowerCase();
            String[] keywords = KeyWords.BODY_CONTENT_KEYWORDS_ALL;
            int index;
            for (int i = 0; i < keywords.length; i++) {
                String key = keywords[i];
                index = text.indexOf(key);
                if (index != -1) {
                    keyContentMap.put(index, key);
                    text = text.replaceFirst(key, StringUtils.repeat('x', key.length()));
                    i--;
                }
            }
        }
    }

    /**
     * Finds the keyword closest to the date.
     * 
     * @param date
     */
    private void setClosestKeyword(ContentDate date) {
        int datePos = date.get(ContentDate.DATEPOS_IN_DOC);

        if (datePos >= 0) {
            String keyword = null;
            int subStart = 0;
            int subEnd = 0;

            for (int i = 1; i < 151; i++) {
                int indexBefore = datePos - i;
                int indexAfter = datePos + i;

                String keywordBefore = keyContentMap.get(indexBefore);
                if (keywordBefore != null) {
                    keyword = keywordBefore;
                    subStart = indexBefore + keywordBefore.length();
                    subEnd = datePos;
                    break;
                }

                String keywordAfter = this.keyContentMap.get(indexAfter);
                if (keywordAfter != null) {
                    keyword = keywordAfter;
                    subStart = datePos + date.getDateString().length();
                    subEnd = indexAfter;
                    break;
                }
            }
            if (keyword != null) {
                date.setKeyword(keyword);
                int diff = StringHelper.countWhitespaces(this.doc.substring(subStart, subEnd));
                date.set(ContentDate.DISTANCE_DATE_KEYWORD, diff);
                if (diff >= 30 || diff == -1) {
                    date.setKeyDiff(0.0);
                } else {
                    date.setKeyDiff(1 - Math.round(diff / 30.0 * 1000.0) / 1000.0);
                }
            }
        }
    }

    /**
     * Returns a keyword if there is one in the node, otherwise returns null. <br>
     * The node will be stored in a map connected to the keyword. <br>
     * (For a node without a keyword the stored string will be the empty string "".)
     * 
     * @param node
     * @return
     */
    private String getNodeKeyword(Node node) {
        String keyword = this.keyAttrMap.get(node);
        if (keyword == null) {
            keyword = findNodeKeyword(node);
            if (keyword == null) {
                keyword = "";
            }
            this.keyAttrMap.put(node, keyword);
        }
        return keyword;
    }

    /**
     * Checks a node for possible keywords.
     * 
     * @param node
     * @return null for no keyword found.
     */
    private String findNodeKeyword(Node node) {
        String returnValue = null;
        Node tempNode = node.cloneNode(false);
        String nodeText = HtmlHelper.xmlToString(tempNode, false);
        String[] keywords = KeyWords.BODY_CONTENT_KEYWORDS_ALL;
        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            if (nodeText.indexOf(keyword) != -1) {
                returnValue = keyword;
                break;
            }

        }
        return returnValue;
    }

    // @Override
    /**
     * Clears all maps for a new use, if the ContentDateGetter will not be initialized for another turn. <br>
     * Use this to avoid OutOfMemeryErrors!
     */
    private void reset() {
        this.doc = null;
        this.keyAttrMap = new HashMap<Node, String>();
        this.keyContentMap = new HashMap<Integer, String>();
        this.nodeIndexMap = new HashMap<String, Integer>();
        this.lookedUpNodeMap = new HashMap<Node, Boolean>();
        this.structDateMap = new HashMap<Node, StructureDate>();
        // this.visibleNodeMap = new HashMap<Node, Boolean>();
    }

    /**
     * Returns a StructureDate out of a node. <br>
     * Stores visited nodes and its dates in maps.
     * 
     * @param node
     * @param pattern
     * @param regExps
     * @return
     */
    private StructureDate getStructureDate(Node node) {
        Boolean hasDate = lookedUpNodeMap.get(node);
        StructureDate date;

        if (hasDate == null) {
            date = findStructureDate(node);
            lookedUpNodeMap.put(node, true);
            structDateMap.put(node, date);
        } else {
            date = structDateMap.get(node);
        }

        return date;
    }

    /**
     * Find StructureDates in nodes.
     * 
     * @param node
     * @param pattern
     * @param regExps
     * @return
     */
    private StructureDate findStructureDate(Node node) {
        StructureDate structDate = null;
        ExtractedDate date = null;

        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            if (!attr.getNodeName().equalsIgnoreCase("href")) {
                date = DateParser.findDate(attr.getNodeValue());
                if (date != null) {
                    break;
                }
            }
        }

        if (date != null) {
            String keyword = getNodeKeyword(node);
            structDate = new StructureDate(date);
            structDate.setKeyword(keyword);
            // structDate.setNode(node);
        }
        return structDate;
    }

    static List<ContentDate> findAllDates(String text) {
        List<ContentDate> dates = new ArrayList<ContentDate>();
        for (DateFormat format : RegExp.ALL_DATE_FORMATS) {
            Matcher matcher = format.getPattern().matcher(text);
            while (matcher.find()) {
                boolean digitNeighbor = false;
                int start = matcher.start();
                if (start > 0) {
                    digitNeighbor = Character.isDigit(text.charAt(start - 1));
                }
                int end = matcher.end();
                if (end < text.length()) {
                    digitNeighbor = Character.isDigit(text.charAt(end));
                }
                if (!digitNeighbor) {
                    String dateString = matcher.group();
                    ContentDate date = new ContentDate(DateParser.parseDate(dateString, format));
                    int datePosition = text.indexOf(date.getDateString());
                    date.set(ContentDate.DATEPOS_IN_TAGTEXT, datePosition);
                    text = text.replaceFirst(dateString, StringUtils.repeat('x', dateString.length()));
                    dates.add(date);
                }
            }
        }
        return dates;
    }

    /**
     * <p>
     * Sometimes texts in webpages have special code for character. E.g. <i>&ampuuml;</i> or whitespace. To evaluate
     * this text reasonably you need to convert this code.
     * </p>
     * 
     * @param text
     * @return
     */
    private static String replaceHtmlSymbols(String text) {

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
