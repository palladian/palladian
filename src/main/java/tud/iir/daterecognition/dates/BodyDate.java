package tud.iir.daterecognition.dates;

public abstract class BodyDate extends KeywordDate {

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
     * 
     */
    public BodyDate() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param dateString
     */
    public BodyDate(String dateString) {
        super(dateString);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param dateString
     * @param format
     */
    public BodyDate(String dateString, String format) {
        super(dateString, format);
        // TODO Auto-generated constructor stub
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

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
}
