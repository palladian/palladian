package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
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
    public List<MetaDate> getDates(Document document) {
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
            String keyword = KeyWords.searchKeyword(nameAttribute.getNodeValue(), KeyWords.HEAD_KEYWORDS);
            if (keyword == null) {
                continue;
            }
            ExtractedDate date = DateParser.findDate(contentAttribute.getNodeValue(), RegExp.HTML_HEAD_DATES);
            if (date == null) {
                continue;
            }
            String tagName = nameAttribute.getNodeName();
            dates.add(new MetaDate(date, keyword, tagName));
        }

        return dates;
    }

}
