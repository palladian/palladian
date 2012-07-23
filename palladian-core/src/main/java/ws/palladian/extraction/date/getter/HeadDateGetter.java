package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateGetterHelper;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.date.dates.MetaDate;

/**
 * This class finds all dates in a HTML-head.<br>
 * Therefore it needs a document.
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
     * Finds dates in head-part of a webpage.<br>
     * Look up only in <i>"meta"</i> tags.
     * 
     * @param document
     * @return a array-list with dates.
     */
    private static List<MetaDate> getHeadDates(final Document document) {
        List<MetaDate> dates = new ArrayList<MetaDate>();
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
                    String keyword = DateGetterHelper.hasKeyword(nameAttr.getNodeValue(), KeyWords.HEAD_KEYWORDS);
                    if (keyword == null) {
                        continue;
                    }
                    ExtractedDate temp = DateGetterHelper.findDate(contentAttr.getNodeValue(), RegExp.getHEADRegExp());
                    if (temp == null) {
                        continue;
                    }
                    //MetaDate headDate = DateConverter.convert(temp, DateType.MetaDate);
                    MetaDate headDate = new MetaDate(temp, keyword, nameAttr.getNodeName());
                    //headDate.setKeyword(keyword);
                    //headDate.setTag(nameAttr.getNodeName());
                    dates.add(headDate);
                }
            }
        }

        return dates;
    }

}
