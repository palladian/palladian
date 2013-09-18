package ws.palladian.retrieval.search.web;

import java.util.Date;

import ws.palladian.retrieval.search.WebContent;

/**
 * <p>
 * {@link BasicWebContent}s represent search results from web search engines.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class BasicWebContent implements WebContent {
	
	/** The URL of the web result. */
	private final String url;
	
    private final String title;
    
    private final String summary;
    
    private final Date date;

    /**
     * <p>
     * Create a new {@link BasicWebContent}.
     * </p>
     * 
     * @param url The URL to the result. This should usually point to an HTML page.
     * @param title
     */
    public BasicWebContent(String url, String title) {
    	this(url, title, null, null);
    }

    /**
     * <p>
     * Create a new {@link BasicWebContent}.
     * </p>
     * 
     * @param url The URL to the result. This should usually point to an HTML page.
     * @param title
     * @param summary
     * @param date
     */
    public BasicWebContent(String url, String title, String summary, Date date) {
        this.url = url;
        this.title = title;
        this.summary = summary;
        this.date = date;
    }

    /**
     * <p>
     * Create a new {@link BasicWebContent}.
     * </p>
     * 
     * @param url The URL to the result. This should usually point to an HTML page.
     * @param title
     * @param summary
     */
    public BasicWebContent(String url, String title, String summary) {
    	this(url, title, summary, null);
    }

    protected BasicWebContent(WebContent webResult) {
        this.url = webResult.getUrl();
        this.title = webResult.getTitle();
        this.summary = webResult.getSummary();
        this.date = webResult.getDate();
    }

    @Override
    public String getUrl() {
        return url;
    }
    
	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getSummary() {
		return summary;
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("WebResult [url=");
		builder.append(url);
		builder.append(", title=");
		builder.append(title);
		builder.append(", summary=");
		builder.append(summary);
		builder.append(", date=");
		builder.append(date);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((summary == null) ? 0 : summary.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BasicWebContent other = (BasicWebContent) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (summary == null) {
			if (other.summary != null)
				return false;
		} else if (!summary.equals(other.summary))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

}