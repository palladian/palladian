package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateGetterHelper;
import ws.palladian.helper.date.ExtractedDateHelper;
import ws.palladian.helper.date.dates.DateParser;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.date.dates.MetaDate;
import ws.palladian.helper.html.XPathHelper;

/**
 * <p>
 * This class extracts all dates from an HTML {@link Document}'s <code>meta</code> tags found in the <code>head</code>
 * section.
 * </p>
 * 
 * @author Martin Gregor
 * @author David Urbansky
 * @author Philipp Katz
 */
public class HeadDateGetter extends TechniqueDateGetter<MetaDate> {

    @Override
    public List<MetaDate> getDates() {
        List<MetaDate> result = new ArrayList<MetaDate>();
        if (document != null) {
            result = getHeadDates(document);
        }
        return result;
    }

    /**
     * <p>
     * Find dates in <code>meta</code> tags of a web {@link Document}'s <code>head</code> section.
     * </p>
     * 
     * @param document The {@link Document} from which to extract head dates, not <code>null</code>.
     * @return List with extracted {@link MetaDate}s, or empty list if no dates were found, never <code>null</code>.
     */
    private static List<MetaDate> getHeadDates(final Document document) {
        List<MetaDate> dates = new ArrayList<MetaDate>();

        List<Node> metaNodes = XPathHelper.getXhtmlNodes(document, "//head/meta");
        for (Node metaNode : metaNodes) {
            NamedNodeMap nodeAttributes = metaNode.getAttributes();
            Node nameAttribute = nodeAttributes.getNamedItem("name");
            if (nameAttribute == null) {
                nameAttribute = nodeAttributes.getNamedItem("http-equiv");
            }
            Node contentAttribute = nodeAttributes.getNamedItem("content");
            if (nameAttribute == null || contentAttribute == null) {
                continue;
            }
            String keyword = ExtractedDateHelper.hasKeyword(nameAttribute.getNodeValue(), KeyWords.HEAD_KEYWORDS);
            if (keyword == null) {
                continue;
            }
            ExtractedDate date = DateParser.findDate(contentAttribute.getNodeValue(), RegExp.getHeadRegExp());
            if (date == null) {
                continue;
            }
            String tagName = nameAttribute.getNodeName();
            dates.add(new MetaDate(date, keyword, tagName));
        }

        return dates;
    }

}
