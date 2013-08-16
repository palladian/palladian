package ws.palladian.retrieval.feeds.discovery;

/**
 * <p>
 * Represents a feed which was found on a Web page.
 * </p>
 * 
 * @author Philipp Katz
 */
public class DiscoveredFeed {

    /** The field separator when outputting CSV. */
    private static final String CSV_SEPARATOR = "$$$";

    public static enum Type {
        RSS, ATOM
    }

    private final Type feedType;
    private final String feedLink;
    private final String feedTitle;
    private final String pageLink;

    DiscoveredFeed(Type feedType, String feedLink, String feedTitle, String pageLink) {
        this.feedType = feedType;
        this.feedLink = feedLink;
        this.feedTitle = feedTitle;
        this.pageLink = pageLink;
    }

    public Type getFeedType() {
        return feedType;
    }

    public String getFeedLink() {
        return feedLink;
    }

    public String getFeedTitle() {
        return feedTitle;
    }

    public String getPageLink() {
        return pageLink;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DiscoveredFeed [feedType=");
        builder.append(feedType);
        builder.append(", feedLink=");
        builder.append(feedLink);
        builder.append(", feedTitle=");
        builder.append(feedTitle);
        builder.append(", pageLink=");
        builder.append(pageLink);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((feedLink == null) ? 0 : feedLink.hashCode());
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
        DiscoveredFeed other = (DiscoveredFeed)obj;
        if (feedLink == null) {
            if (other.feedLink != null)
                return false;
        } else if (!feedLink.equals(other.feedLink))
            return false;
        return true;
    }

    /**
     * Return a CSV representation of the discovered feed, containing feed link, feed type and page link.
     * 
     * @return
     */
    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append(feedLink).append(CSV_SEPARATOR);
        sb.append(feedType).append(CSV_SEPARATOR);
        sb.append(pageLink);
        return sb.toString();
    }
}
