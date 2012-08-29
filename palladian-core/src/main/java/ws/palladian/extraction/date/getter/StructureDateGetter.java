package ws.palladian.extraction.date.getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.dates.AbstractBodyDate;
import ws.palladian.extraction.date.dates.StructureDate;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.html.XPathHelper;

/**
 * <p>
 * This {@link TechniqueDateGetter} extracts {@link StructureDate}s out of the structure of an HTML document. Dates
 * inside the <code>body</code> section of the document are considered.
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class StructureDateGetter extends TechniqueDateGetter<StructureDate> {
    
    @Override
    public List<StructureDate> getDates(Document document) {
        Node bodyElement = XPathHelper.getXhtmlChildNode(document, "//body");
        if (bodyElement != null) {
            return getChildrenDates(bodyElement, 0);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * <p>
     * Recursively searches for {@link StructureDate}s in a {@link Node} and its children.
     * </p>
     * 
     * @param node The Node to be searched.
     * @param depth The depth of the Node in the hierarchy.
     * @return A {@link List} of StructureDates extracted from the Node, or an empty List if no dates could be
     *         extracted, never <code>null</code>.
     */
    private List<StructureDate> getChildrenDates(Node node, int depth) {
        List<StructureDate> dates = CollectionHelper.newArrayList();

        String nodeName = node.getNodeName().toLowerCase();
        if (!Arrays.asList("script", "img").contains(nodeName)) {
            StructureDate date = checkForDate(node);
            if (date != null) {
                date.set(AbstractBodyDate.STRUCTURE_DEPTH, depth);
                dates.add(date);
            }
        }

        NodeList childNodes = node.getChildNodes();
        if (childNodes != null) {
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (!nodeName.equals("script")) {
                    Node childNode = childNodes.item(i);
                    List<StructureDate> childDates = getChildrenDates(childNode, depth + 1);
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
     * @param node The {@link Node} to check, not <code>null</code>.
     * @return A {@link StructureDate} if one could be extracted, <code>null</code> otherwise.
     */
    private StructureDate checkForDate(Node node) {

        StructureDate date = null;
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            String keyword = null;
            String dateTagName = null;
            for (int i = 0; i < attributes.getLength(); i++) {
                String tempKeyword = null;
                Node attributeNode = attributes.item(i);
                String nodeName = attributeNode.getNodeName().toLowerCase();
                if (nodeName.equals("href")) {
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
