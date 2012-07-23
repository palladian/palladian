package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.helper.date.DateGetterHelper;
import ws.palladian.helper.date.dates.AbstractBodyDate;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.date.dates.StructureDate;

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
            result = getStructureDate(document);
        }
        return result;
    }

    /**
     * Finds dates in structure of a document.
     * 
     * @param document Document to be searched.
     * @return List of dates.
     */
    public List<StructureDate> getStructureDate(Document document) {

        List<StructureDate> dates = new ArrayList<StructureDate>();

        if (document != null) {
            List<StructureDate> structureDates = getBodyStructureDates(document);
            if (structureDates != null) {
                dates.addAll(structureDates);
            }
        }
        return dates;

    }

    /**
     * Finds dates in structure of a document.
     * 
     * @param document Document to be searched.
     * @return List of dates.
     */
    private List<StructureDate> getBodyStructureDates(Document document) {
        List<StructureDate> dates = new ArrayList<StructureDate>();
        NodeList bodyNodeList = document.getElementsByTagName("body");
        if (bodyNodeList != null) {
            for (int i = 0; i < bodyNodeList.getLength(); i++) {
                Node node = bodyNodeList.item(i);
                ArrayList<StructureDate> childrernDates = getChildrenDates(node, 0);
                if (childrernDates != null) {
                    dates.addAll(childrernDates);
                }
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
    private ArrayList<StructureDate> getChildrenDates(final Node node, int depth) {
        ArrayList<StructureDate> dates = new ArrayList<StructureDate>();
        StructureDate date = null;

        if (!node.getNodeName().equalsIgnoreCase("script") && !node.getNodeName().equalsIgnoreCase("img")) {

            date = checkForDate(node);
        }
        if (date != null) {
            date.set(AbstractBodyDate.STRUCTURE_DEPTH, depth);
            dates.add(date);
        }
        final NodeList nodeList = node.getChildNodes();
        if (nodeList != null) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node childNode = null;
                ArrayList<StructureDate> childDates = null;
                if (!node.getNodeName().equalsIgnoreCase("script")) {
                    childNode = nodeList.item(i);
                    childDates = getChildrenDates(childNode, depth + 1);
                }
                if (childDates != null) {
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
     * @param node to check
     * @return A ExtractedDate with Context.
     */
    private StructureDate checkForDate(final Node node) {

        StructureDate date = null;
        /*
    	if(this.lookedUpNodeMap.get(node) == null){

    	}else{
    		date = this.structDateMap.get(node);
    	}
         */
        NamedNodeMap tag = node.getAttributes();
        if (tag != null) {
            String keyword = null;
            String tempKeyword = null;
            String dateTagName = null;
            for (int i = 0; i < tag.getLength(); i++) {
                Node attributeNode = tag.item(i);
                String nodeName = attributeNode.getNodeName();
                if (!nodeName.equalsIgnoreCase("href")) {
                    ExtractedDate t = DateGetterHelper.findDate(attributeNode.getNodeValue());
                    if (t == null) {
                        if (keyword == null) {
                            keyword = DateGetterHelper.hasKeyword(attributeNode.getNodeValue(),
                                    KeyWords.DATE_BODY_STRUC);
                        } else {
                            tempKeyword = DateGetterHelper.hasKeyword(attributeNode.getNodeValue(),
                                    KeyWords.DATE_BODY_STRUC);
                            if (KeyWords.getKeywordPriority(keyword) > KeyWords.getKeywordPriority(tempKeyword)) {
                                keyword = tempKeyword;
                            }

                        }
                    } else {
                        date = new StructureDate(t);
                        dateTagName = nodeName;
                    }
                    
                    
                    //StructureDate tempDate = DateConverter.convert(t, DateType.StructureDate);
//                    StructureDate tempDate = new StructureDate(t);
//                    if (tempDate == null) {
//                        if (keyword == null) {
//                            keyword = DateGetterHelper.hasKeyword(attributeNode.getNodeValue(),
//                                    KeyWords.DATE_BODY_STRUC);
//                        } else {
//                            tempKeyword = DateGetterHelper.hasKeyword(attributeNode.getNodeValue(),
//                                    KeyWords.DATE_BODY_STRUC);
//                            if (KeyWords.getKeywordPriority(keyword) > KeyWords.getKeywordPriority(tempKeyword)) {
//                                keyword = tempKeyword;
//                            }
//
//                        }
//                    } else {
//                        date = tempDate;
//                        dateTagName = nodeName;
//                    }
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
