package ws.palladian.extraction.date.getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.dates.StructureDate;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.html.XPathHelper;

/**
 * <p>
 * This {@link TechniqueDateGetter} extracts {@link StructureDate}s out of the structure of an HTML document. Dates
 * inside the <code>body</code> section of the document are considered. Example for such a {@link StructureDate} within
 * a HTML {@link Document}:
 * </p>
 * 
 * <pre>
 * &lt;div id="spShortDate" itemprop="datePublished"  content="2010-07-18T11:32:01+0200"&gt; […] &lt/div&gt;
 * </pre>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class StructureDateGetter extends TechniqueDateGetter<StructureDate> {

    @Override
    public List<StructureDate> getDates(Document document) {
        Node bodyElement = XPathHelper.getXhtmlNode(document, "//body");
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
            StructureDate date = getDate(node);
            if (date != null) {
                date.setStructureDepth(depth);
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
     * <p>
     * Look for a {@link StructureDate} in a {@link Node}'s attributes. If a date was found try to retrieve date
     * keywords from other attributes of the Node. If such a date keyword can be found, it is set as context for the
     * date, else wise the attribute name is considered as context. The <code>href</code> attribute is not checked, as
     * this task is carried out by {@link ReferenceDateGetter}.
     * </p>
     * 
     * @param node The {@link Node} to check, not <code>null</code>.
     * @return A {@link StructureDate} if one could be extracted, <code>null</code> otherwise.
     */
    public static StructureDate getDate(Node node) {

        // node has no attributes, return
        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null) {
            return null;
        }

        int highestPriority = -1;
        ExtractedDate date = null;
        String dateKeyword = null;
        String dateAttribute = null; // name of the attribute, in which the date was found

        for (int i = 0; i < attributes.getLength(); i++) {
            Node attributeNode = attributes.item(i);
            String currentKeyword = KeyWords.searchKeyword(attributeNode.getNodeValue(), KeyWords.DATE_BODY_STRUC);
            String currentAttributeName = attributeNode.getNodeName().toLowerCase();
            if (currentAttributeName.equals("href")) {
                continue;
            }
            ExtractedDate currentDate = DateParser.findDate(attributeNode.getNodeValue());
            if (currentDate != null) {
                dateAttribute = currentAttributeName;
                date = currentDate;
            } else if (dateKeyword == null) {
                dateKeyword = currentKeyword;
            } else {
                int currentPriority = KeyWords.getKeywordPriority(currentKeyword);
                if (currentPriority > highestPriority) {
                    dateKeyword = currentKeyword;
                    highestPriority = currentPriority;
                }
            }
        }

        // no dates could be extracted, return
        if (date == null) {
            return null;
        }

        if (dateKeyword == null) {
            dateKeyword = dateAttribute;
        }

        return new StructureDate(date, dateKeyword, node.getNodeName());
    }

}
