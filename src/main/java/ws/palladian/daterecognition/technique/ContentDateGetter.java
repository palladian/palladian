package ws.palladian.daterecognition.technique;

import java.util.ArrayList;
import java.util.Iterator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.KeyWords;
import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.helper.HTMLHelper;
import ws.palladian.helper.StringHelper;

/**
 * This class extracts all dates out of the content of webpages.
 * 
 * @author Martin Gregor
 * 
 */
public class ContentDateGetter extends TechniqueDateGetter<ContentDate> {

    @Override
    public ArrayList<ContentDate> getDates() {
        ArrayList<ContentDate> result = new ArrayList<ContentDate>();
        if (document != null) {
            result = getContentDates(this.document);
        }
        return result;
    }

    /**
     * Get dates of text-nodes of body part of document.
     * 
     * @param document Document to be searched.
     * @return List of dates.
     */
    private ArrayList<ContentDate> getContentDates(Document document) {
        ArrayList<ContentDate> dates = new ArrayList<ContentDate>();
        NodeList body = document.getElementsByTagName("body");
        String doc = StringHelper.removeDoubleWhitespaces(HTMLHelper.replaceHTMLSymbols(HTMLHelper.documentToReadableText(body
                .item(0))));
        if (body.getLength() > 0) {
            dates.addAll(enterTextnodes(body.item(0), doc, 0));
        }
        return dates;
    }

    /**
     * Tries to find dates in node, if node is a text-node.<br>
     * Otherwise node will be checked for children, which will be searched.<br>
     * Is only the recursive part of finding dates in document structure.
     * 
     * @param node No to be searched.
     * @param doc Whole human readable document (displayed content) as string to get position of found dates.
     * @param depth Depth of node in document structure.
     * @return
     */
    private ArrayList<ContentDate> enterTextnodes(Node node, String doc, int depth) {
        ArrayList<ContentDate> dates = new ArrayList<ContentDate>();
        if (node.getNodeType() == Node.TEXT_NODE) {
        	
            dates.addAll(checkTextnode((Text) node, doc, depth));
            
        } else {
        	
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (!children.item(i).getNodeName().equalsIgnoreCase("script")
                        && !children.item(i).getNodeName().equalsIgnoreCase("style"))
                    dates.addAll(enterTextnodes(children.item(i), doc, depth + 1));
            }
            
        }

        return dates;
    }

    /**
     * Find a date in text of node.<br>
     * Node as to be a {@link Text}.
     * 
     * @param node Text-node to be searched.
     * @param doc Whole human readable document (displayed content) as string to get position of found dates.
     * @param depth Depth of node in document structure.
     * @return
     */
    private ArrayList<ContentDate> checkTextnode(Text node, String doc, int depth) {

        String text = node.getNodeValue();
        int index = -1;
        Node parent = node.getParentNode();
        Node tag = parent;

        
        while (HTMLHelper.isSimpleElement(parent)) {
            parent = parent.getParentNode();
        }
        
        ArrayList<ContentDate> dates = new ArrayList<ContentDate>();
        Iterator<ContentDate> iterator = DateGetterHelper.findALLDates(text).iterator();
        if (iterator.hasNext()) {
            index = doc.indexOf(StringHelper.removeDoubleWhitespaces(HTMLHelper.replaceHTMLSymbols(text)));
            // System.out.println(HTMLHelper.replaceHTMLSymbols(text));
        }
        
        while (iterator.hasNext()) {
        	
            ContentDate date = iterator.next();
            date.set(ContentDate.STRUCTURE_DEPTH, depth);
            date.setTagNode(parent.toString());
            if (index != -1) {
                date.set(ContentDate.DATEPOS_IN_DOC, index + date.get(ContentDate.DATEPOS_IN_TAGTEXT));
            }
            date.setTag(parent.getNodeName());
            String keyword = DateGetterHelper.findNodeKeywordPart(tag, KeyWords.BODY_CONTENT_KEYWORDS_FIRST);
            if (keyword == null) {
                keyword = DateGetterHelper.findNodeKeywordPart(parent, KeyWords.BODY_CONTENT_KEYWORDS_FIRST);
            }
            if (keyword != null) {
                date.setKeyword(keyword);
                date.set(ContentDate.KEYWORDLOCATION, ContentDate.KEY_LOC_ATTR);
            } else {
                DateGetterHelper.setNearestTextkeyword(text, date, KeyWords.BODY_CONTENT_KEYWORDS_FIRST);
            }

            if (date.getKeyword() == null) {
                keyword = DateGetterHelper.findNodeKeywordPart(tag, KeyWords.DATE_BODY_STRUC);
                if (keyword == null) {
                    keyword = DateGetterHelper.findNodeKeywordPart(parent, KeyWords.DATE_BODY_STRUC);
                }
                if (keyword != null) {
                    date.setKeyword(keyword);
                    date.set(ContentDate.KEYWORDLOCATION, ContentDate.KEY_LOC_ATTR);
                }
            }
            
            if (date.getKeyword() == null) {
                text = HTMLHelper.documentToReadableText(parent.getParentNode());
                
                DateGetterHelper.setNearestTextkeyword(text, date, KeyWords.BODY_CONTENT_KEYWORDS_ALL);
            }
            
            dates.add(date);
            
        }
        
        return dates;
    }
}
