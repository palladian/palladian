package ws.palladian.retrieval.resources;

import java.util.Date;

import ws.palladian.extraction.location.GeoCoordinate;

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
    
    private final Date published;
    
    private final GeoCoordinate coordinate;

    /**
     * <p>
     * Create a new {@link BasicWebContent}.
     * </p>
     * 
     * @param url The URL to the result. This should usually point to an HTML page.
     * @param title
     */
    public BasicWebContent(String url, String title) {
    	this(url, title, null, null, null);
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
    	this(url, title, summary, null, null);
    }

    public BasicWebContent(String url, String title, String summary, Date published) {
    	this(url, title, summary, published, null);
    }
    
    /**
     * <p>
     * Create a new {@link BasicWebContent}.
     * </p>
     * 
     * @param url The URL to the result. This should usually point to an HTML page.
     * @param title
     * @param summary
     * @param published
     */
    public BasicWebContent(String url, String title, String summary, Date published, GeoCoordinate coordinate) {
        this.url = url;
        this.title = title;
        this.summary = summary;
        this.published = published;
        this.coordinate = coordinate;
    }

    protected BasicWebContent(WebContent webResult) {
        this.url = webResult.getUrl();
        this.title = webResult.getTitle();
        this.summary = webResult.getSummary();
        this.published = webResult.getPublished();
        this.coordinate = webResult.getCoordinate();
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
	public Date getPublished() {
		return published;
	}
	
	@Override
	public GeoCoordinate getCoordinate() {
		return coordinate;
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
		builder.append(published);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((published == null) ? 0 : published.hashCode());
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
		if (published == null) {
			if (other.published != null)
				return false;
		} else if (!published.equals(other.published))
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