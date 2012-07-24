package ws.palladian.helper.date.dates;

import org.w3c.dom.Node;

/**
 * Template for dates found in HTML-body.
 * 
 * @author Martin Gregor
 * 
 */
public abstract class AbstractBodyDate extends KeywordDate {

    public static final int STRUCTURE_DEPTH = 101;

    /**
     * The surrounding tag of the datestring.
     */
    private String tag = null;

    /**
     * In witch depth of html-structure the date was found. For rating of date.
     */
    private int structuralDepth = -1;

    /**
     * Tag.
     */
    private String tagNode = null;

    /**
     * Node.
     */
    private Node node = null;
    
//    /**
//     * @param dateString
//     * @param format
//     */
//    public AbstractBodyDate(String dateString, String format) {
//        super(dateString, format);
//    }

    public AbstractBodyDate(ExtractedDate date) {
        super(date);
    }

    /**
     * 
     * @param tag
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Should be name of tag, where this date was found.
     * 
     * @return
     */
    public String getTag() {
        return tag;
    }

    @Override
    public int get(int field) {
        int value;
        switch (field) {
            case STRUCTURE_DEPTH:
                value = this.structuralDepth;
                break;
            default:
                value = super.get(field);
        }
        return value;
    }

    @Override
    public void set(int field, int value) {
        switch (field) {
            case STRUCTURE_DEPTH:
                this.structuralDepth = value;
                break;
            default:
                super.set(field, value);
        }

    }

    @Override
    public String toString() {
        return super.toString() + "\n" + "Keyword: " + super.getKeyword() + " Tagname: " + tag + "\n"
                + "Stucturedepth: " + structuralDepth;
    }

    /**
     * Tag-node is the string representation of a HTML-node, the date is found.
     * 
     * @param tagNode
     */
    public void setTagNode(String tagNode) {
        this.tagNode = tagNode;
    }

    /**
     * Tag-node is the string representation of a HTML-node, the date is found.
     * 
     * @return
     */
    public String getTagNode() {
        return tagNode;
    }
    
    public Node getNode(){
    	return node;
    }
    
    public void setNode(Node node){
    	this.node= node;
    }
    
}
