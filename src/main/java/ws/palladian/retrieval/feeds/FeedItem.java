package ws.palladian.retrieval.feeds;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import ws.palladian.helper.nlp.StringHelper;

/**
 * Represents a news item within a feed ({@link Feed}).
 * 
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class FeedItem {

    // /** The logger for this class. */
    // private static final Logger LOGGER = Logger.getLogger(FeedItem.class);

    // private static final Map<String, String> NAMESPACE_MAP = new HashMap<String, String>();
    //
    // static {
    // NAMESPACE_MAP.put("atom", "http://www.w3.org/2005/Atom");
    // }

    private int id = -1;

    /** The feed to which this item belongs to. */
    private Feed feed;

    /**
     * For performance reasons, we need to get feed items from the database and in that case we don't have the feed
     * object.
     */
    private int feedId = -1;

    private String title;
    private String link;

    /** Original ID from the feed. */
    private String rawId;

    /** Publish date from the feed */
    private Date published;

    /** When the entry was aggregated */
    private Date added;

    /** Author information. */
    private String authors;

    /** Description text of feed entry */
    private String itemDescription;

    /** Text directly from the feed entry */
    private String itemText;

    /** Allows to keep arbitrary, additional information. */
    private Map<String, Object> additionalData;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFeedId() {
        if (getFeed() != null) {
            return getFeed().getId();
        }
        return feedId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getRawId() {
        return rawId;
    }

    public void setRawId(String rawId) {
        this.rawId = rawId;
    }

    public Date getPublished() {
        return published;
    }

    public void setPublished(Date published) {
        this.published = published;
    }

    public Timestamp getPublishedSQLTimestamp() {
        if (published != null) {
            return new Timestamp(published.getTime());
        }
        return null;
    }

    public Date getAdded() {
        return added;
    }

    public void setAdded(Date added) {
        this.added = added;
    }

    public Timestamp getAddedSQLTimestamp() {
        if (added != null) {
            return new Timestamp(added.getTime());
        }
        return null;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getItemText() {
        return itemText;
    }

    public void setItemText(String entryText) {
        this.itemText = entryText;
    }

    /**
     * Get entry's text, either (preferably) from the page or from the feed. Never return <code>null</code>.
     * 
     * @return
     */
    public String getText() {

        String text = getItemText();

        if (text == null || text.isEmpty()) {
            text = getItemDescription();
        }

        if (text == null) {
            text = "";
        }

        return text;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Entry");
        // sb.append(" id:").append(id);
        sb.append(" title:").append(title);
        sb.append(" link:").append(link);
        // sb.append(" rawId:").append(rawId);
        sb.append(" published:").append(published);
        // sb.append(" entryText:").append(entryText);
        return sb.toString();
    }

    // /**
    // * @return The raw XML markup for this feed entry.
    // */
    // public String getRawMarkup() {
    // String rawMarkup = "";
    // Node node = getNode();
    //
    // if (node != null) {
    // rawMarkup = HTMLHelper.documentToHTMLString(node);
    // }
    //
    // return rawMarkup;
    // }
    //
    // /**
    // * <p>
    // * Extracts the DOM node of the provided feed entry from the feed currently processed by the aggregator.
    // * </p>
    // *
    // * @return The extracted DOM node representing the provided feed entry.
    // */
    // Node getNode() {
    //
    // Node node = null;
    //
    // // the feed's document representation
    // Document document = getFeed().getDocument();
    //
    // try {
    //
    // // for RSS
    // node = XPathHelper.getNode(document, "//item[link=\"" + getLink() + "\"]");
    //
    // if (node == null) {
    // node = XPathHelper.getNode(document, "//item[title=\"" + getTitle().replaceAll("\"", "&quot;") + "\"]");
    //
    // // for Atom
    // if (node == null) {
    // node = XPathHelper.getNode(document, "//atom:entry[atom:id=\"" + getRawId() + "\"]", NAMESPACE_MAP);
    // }
    // }
    //
    // } catch (Exception e) {
    // LOGGER.error("synd entry was not complete (" + getFeedUrl() + "), " + e.getMessage());
    // }
    //
    // if (node == null) {
    // LOGGER.error("feed: " + getFeedUrl() + ", node = null");
    // }
    //
    // return node;
    // }

    public String getFeedUrl() {
        if (getFeed() != null) {
            return getFeed().getFeedUrl();
        }
        return "";
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
    }

    public Feed getFeed() {
        return feed;
    }

    public void setFeedId(int feedId) {
        this.feedId = feedId;
    }

    /**
     * Returns a custom hash representation calculated by the item's title, link and raw id.
     * 
     * @return
     */
    public String getHash() {
        StringBuilder hash = new StringBuilder();
        hash.append(getTitle());
        hash.append(getLink());
        hash.append(getRawId());
        return StringHelper.sha1(hash.toString());
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public Object getAdditionalData(String key) {
        return additionalData.get(key);
    }

}