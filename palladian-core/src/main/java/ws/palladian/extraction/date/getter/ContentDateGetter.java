package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.comparators.ContentDateComparator;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateGetterHelper;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.helper.date.dates.DateExactness;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.date.dates.MetaDate;
import ws.palladian.helper.date.dates.StructureDate;
import ws.palladian.helper.date.dates.UrlDate;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * This class extracts all dates out of the content of webpages. For more than one use, use
 * {@link ContentDateGetter#reset()} to avoid errors and mistakes. Otherwise list will not be cleared and you get
 * {@link OutOfMemoryError}.
 * </p>
 * 
 * @author Martin Gregor
 * 
 */
public class ContentDateGetter extends TechniqueDateGetter<ContentDate> {

    /**
     * Stores all keywords with index, that are within document's text.
     */
    private Map<Integer, String> keyContentMap = new HashMap<Integer, String>();
    /**
     * Stores a node and its keyword.
     */
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
    public List<ContentDate> getDates() {
        List<ContentDate> result = new ArrayList<ContentDate>();
        if (document != null) {
            result = getContentDates(this.document);
            // DataSetHandler.writeDateFactors(result, url, doc);
            setFeatures(result);
        }
        return result;
    }

    private void setFeatures(List<ContentDate> dates) {

        LinkedList<ContentDate> posOrder = new LinkedList<ContentDate>();
        LinkedList<ContentDate> ageOrder = new LinkedList<ContentDate>();
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
        mdg.setDocument(document);
        mdg.setUrl(url);
        udg.setUrl(url);
        List<MetaDate> metaDates = DateArrayHelper.removeNull(mdg.getDates());
        List<UrlDate> urlDates = DateArrayHelper.removeNull(udg.getDates());

        for (ContentDate date : dates) {

            date.setRelSize(1.0 / dates.size());

            double ordDocPos = Math.round((posOrder.indexOf(date) + 1.0) / posOrder.size() * 1000.0) / 1000.0;
            date.setOrdDocPos(ordDocPos);

            double ordAgePos = Math.round((ageOrder.indexOf(date) + 1.0) / dates.size() * 1000.0) / 1000.0;
            date.setOrdAgePos(ordAgePos);

            if (metaDates.size() > 0 && DateArrayHelper.countDates(date, metaDates, DateExactness.DAY.getValue()) > 0) {
                date.setInMetaDates(true);
            }
            if (urlDates.size() > 0 && DateArrayHelper.countDates(date, urlDates, DateExactness.DAY.getValue()) > 0) {
                date.setInUrl(true);
            }

            double relCntSame = Math
                    .round((double) (DateArrayHelper.countDates(date, dates, DateExactness.DAY.getValue()) + 1)
                            / (double) dates.size() * 1000.0) / 1000.0;
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
                date.setDistAgeBefore(Math.round(date.getDifference(ageOrder.get(dateAgeOrdAbsl - 1),
                        TimeUnit.HOURS)));
            }
            if (dateAgeOrdAbsl < ageOrder.size() - 1) {
                date.setDistAgeAfter(Math.round(date.getDifference(ageOrder.get(dateAgeOrdAbsl + 1),
                        TimeUnit.HOURS)));
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
        List<Node> nodeList;
        ArrayList<ContentDate> dates = new ArrayList<ContentDate>();

        nodeList = XPathHelper.getNodes(document, "//text()");

        if (!nodeList.isEmpty()) {
            NodeList body = document.getElementsByTagName("body");
            // TODO: Check if an element is visible
            // checkVisiblityOfAllNodes(body.item(0));
            // Get webpage as text (for finding position).
            this.doc = StringHelper.removeDoubleWhitespaces(DateGetterHelper.replaceHtmlSymbols(HtmlHelper
                    .documentToReadableText(body.item(0))));

            /*
             * Prepare Pattern for faster matching. Only regExps.length. Not
             * (regExps.length)*(nodeList.size) [n < n*m]
             */
            Object[] regExps = RegExp.getAllRegExp();
            Pattern[] pattern = new Pattern[regExps.length];
            Matcher[] matcher = new Matcher[regExps.length];
            for (int i = 0; i < regExps.length; i++) {
                pattern[i] = Pattern.compile(((String[]) regExps[i])[0]);
                matcher[i] = pattern[i].matcher("");
            }

            setDocKeywords();

            for (int i = 0; i < nodeList.size(); i++) {

                if (nodeList.get(i).getNodeType() == Node.TEXT_NODE) {
                    Node node = nodeList.get(i);
                    Node parent = node.getParentNode();
                    if (parent.getNodeType() != Node.COMMENT_NODE && !parent.getNodeName().equalsIgnoreCase("script")
                            && !parent.getNodeName().equalsIgnoreCase("style")) {
                        dates.addAll(checkTextnode((Text) node, matcher, regExps));
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
    private List<ContentDate> checkTextnode(Text node, Matcher[] matcher, Object[] regExps) {

        // String text = StringHelper.removeDoubleWhitespaces(HtmlHelper.replaceHtmlSymbols(node.getNodeValue()));

        String text = DateGetterHelper.replaceHtmlSymbols(node.getNodeValue());

        int index = -1;
        Node parent = node.getParentNode();
        Node tag = parent;

        while (HtmlHelper.isSimpleElement(parent)) {
            parent = parent.getParentNode();
        }

        ArrayList<ContentDate> returnDates = new ArrayList<ContentDate>();
        ArrayList<String> textSplitt = new ArrayList<String>();
        ArrayList<ContentDate> dateList = new ArrayList<ContentDate>();
        for (int i = 0, beginIndex; (beginIndex = i * 10000) < text.length(); i++) {
            int endIndex = Math.min(beginIndex + 10000, text.length());
            textSplitt.add(text.substring(beginIndex, endIndex));

        }
        for (String textPart : textSplitt) {
            dateList.addAll(DateGetterHelper.findAllDates(textPart, matcher, regExps));
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
            // System.out.println(index);
        }

        DateArrayHelper.removeNull(dateList);

        for (ContentDate date : dateList) {

            date.setStructureDate(getStructureDate(tag, matcher, regExps));

            if (date.getStructureDate() == null && tag != parent) {
                date.setStructureDate(getStructureDate(parent, matcher, regExps));
            }

            boolean keyword3Class = true;

            date.setTagNode(parent.toString());
            date.setTag(tag.getNodeName());
            date.setNode(tag);

            date.setSimpleTag(HtmlHelper.isSimpleElement(tag) ? "1" : "0");
            date.sethTag(HtmlHelper.isHeadlineTag(tag) ? "1" : "0");

            if (index != -1) {
                int ablsDocPos = index + date.get(ContentDate.DATEPOS_IN_TAGTEXT);
                date.set(ContentDate.DATEPOS_IN_DOC, ablsDocPos);
                date.setRelDocPos(Math.round((double) ablsDocPos / (double) doc.length() * 1000.0) / 1000.0);
            }

            // String keyword = DateGetterHelper.findNodeKeywordPart(tag,
            // KeyWords.BODY_CONTENT_KEYWORDS_FIRST);
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
        if (this.doc != null) {
            this.keyContentMap = new HashMap<Integer, String>();
            String text = this.doc.toLowerCase();
            String[] keywords = KeyWords.BODY_CONTENT_KEYWORDS_ALL;
            int index;
            for (int i = 0; i < keywords.length; i++) {
                String key = keywords[i];
                index = text.indexOf(key);
                if (index != -1) {
                    this.keyContentMap.put(index, key);
                    text = text.replaceFirst(key, DateGetterHelper.getXs(key));
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
        String keyword = null;
        String keywordBefore;
        String keywordAfter;
        int indexBefore;
        int indexAfter;
        int subStart = 0;
        int subEnd = 0;
        int datePos = date.get(ContentDate.DATEPOS_IN_DOC);

        if (datePos >= 0) {

            for (int i = 1; i < 151; i++) {
                indexBefore = datePos - i;
                indexAfter = datePos + i;

                keywordBefore = this.keyContentMap.get(indexBefore);
                if (keywordBefore != null) {
                    keyword = keywordBefore;
                    subStart = indexBefore + keywordBefore.length();
                    subEnd = datePos;
                    break;
                }

                keywordAfter = this.keyContentMap.get(indexAfter);
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

    @Override
    /**
     * Clears all maps for a new use, if the ContentDateGetter will not be initialized for another turn. <br>
     * Use this to avoid OutOfMemeryErrors!
     */
    public void reset() {
        this.doc = null;
        this.document = null;
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
     * @param matcher
     * @param regExps
     * @return
     */
    private StructureDate getStructureDate(Node node, Matcher[] matcher, Object[] regExps) {
        Boolean hasDate = lookedUpNodeMap.get(node);
        StructureDate date;

        if (hasDate == null) {
            date = findStructureDate(node, matcher, regExps);
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
     * @param matcher
     * @param regExps
     * @return
     */
    private StructureDate findStructureDate(Node node, Matcher[] matcher, Object[] regExps) {
        StructureDate structDate = null;
        ExtractedDate date = null;

        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            if (!attr.getNodeName().equalsIgnoreCase("href")) {
                date = DateGetterHelper.findDate(attr.getNodeValue(), matcher, regExps);
                if (date != null) {

                    break;
                }
            }
        }

        if (date != null) {
            String keyword = getNodeKeyword(node);
            //structDate = DateConverter.convert(date, DateType.StructureDate);
            structDate = new StructureDate(date);
            structDate.setKeyword(keyword);
            structDate.setNode(node);
        }
        return structDate;
    }

    public String getDoc() {
        return this.doc;
    }
}
