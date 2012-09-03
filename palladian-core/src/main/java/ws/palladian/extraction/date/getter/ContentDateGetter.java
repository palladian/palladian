package ws.palladian.extraction.date.getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
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
import ws.palladian.helper.math.MathHelper;
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

    private final MetaDateGetter metaDateGetter = new MetaDateGetter();

    private final UrlDateGetter urlDateGetter = new UrlDateGetter();

    /** All keywords with index, that are within document's text. */
    private final Map<Integer, String> keyContentMap = CollectionHelper.newHashMap();
    
    /** Nodes with their keywords. */
    private final Map<Node, String> keyAttrMap = CollectionHelper.newHashMap();
    /**
     * Stores the position after in a document found text. <br>
     * Used to restart search after an already found text.
     */
    private final Map<String, Integer> nodeIndexMap = CollectionHelper.newHashMap();
    private String doc;

    /** Caches looked up {@link StructureDate}s. */
    private final Map<Node, StructureDate> structDates = CollectionHelper.newHashMap();

    @Override
    public List<ContentDate> getDates(Document document) {
        List<ContentDate> result = getContentDates(document);
        setFeatures(result, document);
        reset();
        return result;
    }

    private void setFeatures(List<ContentDate> dates, Document document) {

        List<ContentDate> posOrder = CollectionHelper.newArrayList();
        List<ContentDate> ageOrder = CollectionHelper.newArrayList();
        for (ContentDate date : dates) {
            if (date.get(ContentDate.DATEPOS_IN_DOC) != -1) {
                posOrder.add(date);
            }
            ageOrder.add(date);
        }

        Collections.sort(posOrder, new ContentDateComparator());
        Collections.sort(ageOrder, new DateComparator());

        List<MetaDate> metaDates = metaDateGetter.getDates(document);
        List<UrlDate> urlDates = urlDateGetter.getDates(document.getDocumentURI());

        for (ContentDate date : dates) {

            date.setRelSize(1.0 / dates.size());

            date.setOrdDocPos(MathHelper.round((posOrder.indexOf(date) + 1.0) / posOrder.size(), 3));

            date.setOrdAgePos(MathHelper.round((ageOrder.indexOf(date) + 1.0) / dates.size(), 3));

            if (DateExtractionHelper.countDates(date, metaDates, DateExactness.DAY) > 0) {
                date.setInMetaDates(true);
            }
            if (DateExtractionHelper.countDates(date, urlDates, DateExactness.DAY) > 0) {
                date.setInUrl(true);
            }

            double relCntSame = MathHelper.round(
                    (double)(DateExtractionHelper.countDates(date, dates, DateExactness.DAY) + 1) / dates.size(), 3);
            date.setRelCntSame(relCntSame);

            int datePosOrderAbs = posOrder.indexOf(date);
            if (datePosOrderAbs > 0) {
                date.setDistPosBefore(date.get(ContentDate.DATEPOS_IN_DOC)
                        - posOrder.get(datePosOrderAbs - 1).get(ContentDate.DATEPOS_IN_DOC));
            }
            if (datePosOrderAbs < posOrder.size() - 1) {
                date.setDistPosAfter(posOrder.get(datePosOrderAbs + 1).get(ContentDate.DATEPOS_IN_DOC)
                        - date.get(ContentDate.DATEPOS_IN_DOC));
            }
            int dateAgeOrderAbs = ageOrder.indexOf(date);
            if (dateAgeOrderAbs > 0) {
                date.setDistAgeBefore(Math.round(date.getDifference(ageOrder.get(dateAgeOrderAbs - 1), TimeUnit.HOURS)));
            }
            if (dateAgeOrderAbs < ageOrder.size() - 1) {
                date.setDistAgeAfter(Math.round(date.getDifference(ageOrder.get(dateAgeOrderAbs + 1), TimeUnit.HOURS)));
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

        List<ContentDate> dates = CollectionHelper.newArrayList();
        List<Node> textNodes = XPathHelper.getNodes(document, "//text()");

        if (textNodes.isEmpty()) {
            return dates;
        }

        Node bodyNode = XPathHelper.getXhtmlNode(document, "//body");
        doc = StringHelper.removeDoubleWhitespaces(replaceHtmlSymbols(HtmlHelper.documentToReadableText(bodyNode)));
        // TODO: Check if an element is visible
        // checkVisiblityOfAllNodes(body.item(0));
        // Get webpage as text (for finding position).

        setDocKeywords();

        for (Node textNode : textNodes) {
            if (textNode.getNodeType() == Node.TEXT_NODE) {
                Node parent = textNode.getParentNode();
                String parentName = parent.getNodeName().toLowerCase();
                if (parent.getNodeType() != Node.COMMENT_NODE && !Arrays.asList("script", "style").contains(parentName)) {
                    dates.addAll(checkTextnode((Text)textNode));
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

        String text = replaceHtmlSymbols(node.getNodeValue());

        int index = -1;
        Node parent = node.getParentNode();
        Node tag = parent;

        while (HtmlHelper.isSimpleElement(parent)) {
            parent = parent.getParentNode();
        }

        List<ContentDate> returnDates = CollectionHelper.newArrayList();
        List<String> textSplit = CollectionHelper.newArrayList();
        for (int i = 0, beginIndex; (beginIndex = i * 10000) < text.length(); i++) {
            int endIndex = Math.min(beginIndex + 10000, text.length());
            textSplit.add(text.substring(beginIndex, endIndex));
        }

        List<ContentDate> dateList = CollectionHelper.newArrayList();
        for (String textPart : textSplit) {
            dateList.addAll(findAllDates(textPart));
        }

        if (dateList.size() > 0) {
            Integer beginIndex = nodeIndexMap.get(text);
            if (beginIndex == null) {
                beginIndex = -1;
            }
            index = doc.indexOf(text, beginIndex);
            if (index != -1) {
                nodeIndexMap.put(text, index + text.length());
            }
        }

        for (ContentDate date : dateList) {

            date.setStructureDate(getStructureDate(tag));

            if (date.getStructureDate() == null && tag != parent) {
                date.setStructureDate(getStructureDate(parent));
            }

            boolean keyword3Class = true;

            date.setTag(tag.getNodeName());

            date.setSimpleTag(HtmlHelper.isSimpleElement(tag) ? "1" : "0");
            date.setHTag(HtmlHelper.isHeadlineTag(tag) ? "1" : "0");

            if (index != -1) {
                int absDocPos = index + date.get(ContentDate.DATEPOS_IN_TAGTEXT);
                date.set(ContentDate.DATEPOS_IN_DOC, absDocPos);
                date.setRelDocPos(MathHelper.round((double)absDocPos / doc.length(), 3));
            }

            String keyword = getNodeKeyword(tag);

            if (keyword.isEmpty() && tag != parent) {
                keyword = getNodeKeyword(parent);
            }

            if (!keyword.isEmpty()) {

                keyword3Class = KeyWords.getKeywordPriority(keyword) == KeyWords.OTHER_KEYWORD;
                date.set(ContentDate.KEYWORDLOCATION, ContentDate.KEY_LOC_ATTR);
                date.setKeyLoc("1");
                date.setKeyLoc201("1");
            }

            if (keyword.isEmpty() || keyword3Class) {
                setClosestKeyword(date);
                if (date.getKeyword() != null) {
                    date.set(ContentDate.KEYWORDLOCATION, ContentDate.KEY_LOC_CONTENT);
                    date.setKeyLoc("2");
                    date.setKeyLoc202("1");
                    keyword = date.getKeyword();
                }
            }

            if (!keyword.isEmpty()) {
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

        if (doc == null) {
            return;
        }

        String text = doc.toLowerCase();
        for (int i = 0; i < KeyWords.BODY_CONTENT_KEYWORDS_ALL.length; i++) {
            String key = KeyWords.BODY_CONTENT_KEYWORDS_ALL[i];
            int index = text.indexOf(key);
            if (index != -1) {
                keyContentMap.put(index, key);
                text = text.replaceFirst(key, StringUtils.repeat('x', key.length()));
                i--;
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

        if (datePos < 0) {
            return;
        }

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

            String keywordAfter = keyContentMap.get(indexAfter);
            if (keywordAfter != null) {
                keyword = keywordAfter;
                subStart = datePos + date.getDateString().length();
                subEnd = indexAfter;
                break;
            }
        }
        if (keyword != null) {
            date.setKeyword(keyword);
            int diff = StringHelper.countWhitespaces(doc.substring(subStart, subEnd));
            date.set(ContentDate.DISTANCE_DATE_KEYWORD, diff);
            if (diff >= 30 || diff == -1) {
                date.setKeyDiff(0.0);
            } else {
                date.setKeyDiff(1 - MathHelper.round(diff / 30.0, 3));
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
        String keyword = keyAttrMap.get(node);
        if (keyword == null) {
            keyword = findNodeKeyword(node);
            if (keyword == null) {
                keyword = "";
            }
            keyAttrMap.put(node, keyword);
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
        String nodeText = HtmlHelper.xmlToString(node, false);
        for (String keyword : KeyWords.BODY_CONTENT_KEYWORDS_ALL) {
            if (nodeText.indexOf(keyword) != -1) {
                return keyword;
            }
        }
        return null;
    }

    /**
     * Clears all maps for a new use, if the ContentDateGetter will not be initialized for another turn. <br>
     * Use this to avoid OutOfMemeryErrors!
     */
    private void reset() {
        this.doc = null;
        this.keyAttrMap.clear();
        this.keyContentMap.clear();
        this.nodeIndexMap.clear();
        this.structDates.clear();
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
        
        StructureDate date = structDates.get(node);
        
        if (date == null) {
            date = findStructureDate(node);
            structDates.put(node, date);
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

        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            if (attribute.getNodeName().equalsIgnoreCase("href")) {
                continue;
            }
            ExtractedDate date = DateParser.findDate(attribute.getNodeValue());
            if (date != null) {
                String keyword = getNodeKeyword(node);
                structDate = new StructureDate(date, keyword, null);
            }
        }
        return structDate;
    }

    static List<ContentDate> findAllDates(String text) {
        List<ContentDate> dates = CollectionHelper.newArrayList();
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

        String result = StringEscapeUtils.unescapeHtml4(text);
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
