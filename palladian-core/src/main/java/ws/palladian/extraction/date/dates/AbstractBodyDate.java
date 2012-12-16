package ws.palladian.extraction.date.dates;

import ws.palladian.helper.date.ExtractedDate;

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
     * In which depth of html-structure the date was found. For rating of date.
     */
    private int structuralDepth = -1;

    public AbstractBodyDate(ExtractedDate date) {
        super(date);
    }

    public AbstractBodyDate(ExtractedDate date, String keyword) {
        super(date, keyword);
    }
    
    public AbstractBodyDate(ExtractedDate date, String keyword, String tag) {
        super(date, keyword);
        this.tag = tag;
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



//    @Override
//    public void set(int field, int value) {
//        switch (field) {
//            case STRUCTURE_DEPTH:
//                this.structuralDepth = value;
//                break;
//            default:
//                super.set(field, value);
//        }
//
//    }
    
    public void setStructureDepth(int depth) {
        this.structuralDepth = depth;
    }

    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());
        builder.append(" tag=");
        builder.append(tag);
        builder.append(", structuralDepth=");
        builder.append(structuralDepth);
        return builder.toString();
    }
    
}
