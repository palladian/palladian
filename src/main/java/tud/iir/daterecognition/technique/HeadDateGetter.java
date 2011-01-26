package tud.iir.daterecognition.technique;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import tud.iir.daterecognition.DateConverter;
import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.daterecognition.KeyWords;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HeadDate;
import tud.iir.helper.RegExp;

/**
 * This class finds all dates in a HTML-head.<br>
 * Therefore it needs a document.
 * 
 * @author Martin Gregor
 * 
 */
public class HeadDateGetter extends TechniqueDateGetter<HeadDate> {

    @Override
    public ArrayList getDates() {
        ArrayList<HeadDate> result = new ArrayList<HeadDate>();
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
    private static ArrayList<HeadDate> getHeadDates(final Document document) {
        ArrayList<HeadDate> dates = new ArrayList<HeadDate>();
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
                    HeadDate headDate = DateConverter.convert(temp, DateConverter.TECH_HTML_HEAD);
                    headDate.setKeyword(keyword);
                    headDate.setTag(nameAttr.getNodeName());
                    dates.add(headDate);
                }
            }
        }

        return dates;
    }

}
