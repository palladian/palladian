package ws.palladian.daterecognition.dates;

public class MetaDate extends KeywordDate {

	private DateType dateType = DateType.MetaDate;
	
	private String tag = null;
	
	public MetaDate() {
		// TODO Auto-generated constructor stub
	}

	public MetaDate(String dateString) {
		super(dateString);
		// TODO Auto-generated constructor stub
	}

	public MetaDate(String dateString, String format) {
		super(dateString, format);
		// TODO Auto-generated constructor stub
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
