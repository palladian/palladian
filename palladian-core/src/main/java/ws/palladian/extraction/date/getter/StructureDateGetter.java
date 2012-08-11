package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.dates.AbstractBodyDate;
import ws.palladian.extraction.date.dates.StructureDate;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;

/**
 * This class extracts dates out of the structure of a HTML-document.
 * 
 * @author Martin Gregor
 * 
 */
public class StructureDateGetter extends TechniqueDateGetter<StructureDate> {

    @Override
    public List<StructureDate> getDates() {
        List<StructureDate> result = new ArrayList<StructureDate>();
        if (document != null) {
            result = getBodyStructureDates(document);
        }
        return result;
    }

    /**
     * Finds dates in structure of a document.
     * 
     * @param document Document to be searched.
     * @return List of dates.
     */
    List<StructureDate> getBodyStructureDates(Document document) {
        List<StructureDate> dates = new ArrayList<StructureDate>();
        NodeList bodyNodeList = document.getElementsByTagName("body");
        if (bodyNodeList != null) {
            for (int i = 0; i < bodyNodeList.getLength(); i++) {
                Node node = bodyNodeList.item(i);
                List<StructureDate> childrenDates = getChildrenDates(node, 0);
                dates.addAll(childrenDates);
            }
        }
        return dates;
    }

    /**
     * Searches in a node and it's children for structure dates.<br>
     * Used recursively.
     * 
     * @param node Node to be searched.
     * @param depth Depth of hierarchy the node is in.
     * @return
     */
    private List<StructureDate> getChildrenDates(Node node, int depth) {
        List<StructureDate> dates = new ArrayList<StructureDate>();
        StructureDate date = null;

        if (!node.getNodeName().equalsIgnoreCase("script") && !node.getNodeName().equalsIgnoreCase("img")) {
            date = checkForDate(node);
        }
        if (date != null) {
            date.set(AbstractBodyDate.STRUCTURE_DEPTH, depth);
            dates.add(date);
        }
        NodeList nodeList = node.getChildNodes();
        if (nodeList != null) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                if (node.getNodeName().equalsIgnoreCase("script")) {
                    continue;
                }
                Node childNode = nodeList.item(i);
                List<StructureDate> childDates = getChildrenDates(childNode, depth + 1);
                dates.addAll(childDates);
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
    private StructureDate checkForDate(Node node) {

        StructureDate date = null;
        NamedNodeMap tag = node.getAttributes();
        if (tag != null) {
            String keyword = null;
            String dateTagName = null;
            for (int i = 0; i < tag.getLength(); i++) {
                String tempKeyword = null;
                Node attributeNode = tag.item(i);
                String nodeName = attributeNode.getNodeName();
                if (nodeName.equalsIgnoreCase("href")) {
                    continue;
                }
                ExtractedDate t = DateParser.findDate(attributeNode.getNodeValue());
                if (t == null) {
                    if (keyword == null) {
                        keyword = KeyWords.searchKeyword(attributeNode.getNodeValue(), KeyWords.DATE_BODY_STRUC);
                    } else {
                        tempKeyword = KeyWords.searchKeyword(attributeNode.getNodeValue(), KeyWords.DATE_BODY_STRUC);
                        if (KeyWords.getKeywordPriority(keyword) > KeyWords.getKeywordPriority(tempKeyword)) {
                            keyword = tempKeyword;
                        }
                    }
                } else {
                    date = new StructureDate(t);
                    dateTagName = nodeName;
                }

            }
            if (date != null) {
                if (keyword == null) {
                    date.setKeyword(dateTagName);
                } else {
                    date.setKeyword(keyword);
                }
                date.setTag(node.getNodeName());
                date.setTagNode(node.toString());
            }
        }
        return date;
    }

}
