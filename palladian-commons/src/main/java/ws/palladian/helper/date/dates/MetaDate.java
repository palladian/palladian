package ws.palladian.helper.date.dates;

public class MetaDate extends KeywordDate {
	
	private String tag = null;
	
	public MetaDate() {
	}

	public MetaDate(String dateString) {
		super(dateString);
	}

	public MetaDate(String dateString, String format) {
		super(dateString, format);
	}
	
	public MetaDate(ExtractedDate date) {
	    super(date, DateType.MetaDate);
	}
	
	 /**
     * Should be name of tag, where this date was found.
     * 
     * @return
     */
    public String getTag() {
        return this.tag;
    }

    /**
     * 
     * @param tag
     */
    public void setTag(String tag) {
        this.tag = tag;
    }
	
}
