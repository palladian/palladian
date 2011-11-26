package ws.palladian.retrieval.search.web;

import java.util.Date;

/**
 * <p>
 * {@link WebImageResult}s represent search results from image searches on web search engines.
 * </p>
 * 
 * @author Philipp Katz
 */
public class WebImageResult extends WebResult {

    private final int width;
    private final int height;

    public WebImageResult(String url, String title, String summary, int width, int height, Date date) {
        super(url, title, summary, date);
        this.width = width;
        this.height = height;
    }

    public WebImageResult(String url, String title, String summary, int width, int height) {
        this(url, title, summary, width, height, null);
    }

    public WebImageResult(String url, String title, int width, int height) {
        this(url, title, null, width, height, null);
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WebImageResult [width=");
        builder.append(width);
        builder.append(", height=");
        builder.append(height);
        builder.append(", getUrl()=");
        builder.append(getUrl());
        builder.append(", getTitle()=");
        builder.append(getTitle());
        builder.append(", getSummary()=");
        builder.append(getSummary());
        builder.append(", getDate()=");
        builder.append(getDate());
        builder.append("]");
        return builder.toString();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + height;
        result = prime * result + width;
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        WebImageResult other = (WebImageResult) obj;
        if (height != other.height)
            return false;
        if (width != other.width)
            return false;
        return true;
    }

}
