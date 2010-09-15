package tud.iir.daterecognition.dates;

public abstract class KeywordDate extends ExtractedDate {
    /**
     * Context, in witch the date was found. <br>
     * E.g.: URL, tag-name, HTTP-tag, keyword...
     */
    private String keyword = null;

    /**
     * 
     */
    public KeywordDate() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param dateString
     */
    public KeywordDate(String dateString) {
        super(dateString);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param dateString
     * @param format
     */
    public KeywordDate(String dateString, String format) {
        super(dateString, format);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String toString() {
        return super.toString() + "\n" + "Keyword: " + keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

}
