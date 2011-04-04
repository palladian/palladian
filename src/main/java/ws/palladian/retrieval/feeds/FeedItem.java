package ws.palladian.retrieval.feeds;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.helper.html.XPathHelper;

/**
 * Represents a news item within a feed ({@link Feed}).
 * 
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class FeedItem {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedItem.class);
    
    private static final Map<String, String> namespaceMap = new HashMap<String, String>();
    
    static {
        namespaceMap.put("atom", "http://www.w3.org/2005/Atom");
    }

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

    /** Text which we downloaded from the corresponding web page. */
    private String pageText;

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

    public String getPageText() {
        return pageText;
    }

    public void setPageText(String pageText) {
        this.pageText = pageText;
    }

    /**
     * Get entry's text, either (preferably) from the page or from the feed. Never return <code>null</code>.
     * 
     * @return
     */
    public String getText() {

        String text = getPageText();

        if (text == null || text.isEmpty()) {
            text = getItemText();
        }

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

    /**
     * @return The raw XML markup for this feed entry.
     */
    public String getRawMarkup() {
        String rawMarkup = "";

        rawMarkup = HTMLHelper.documentToHTMLString(getNode());

        return rawMarkup;
    }

    /**
     * <p>
     * Extracts the DOM node of the provided feed entry from the feed currently processed by the aggregator.
     * </p>
     * 
     * @return The extracted DOM node representing the provided feed entry.
     */
    Node getNode() {

        Node node = null;

        // the feed's document representation
        Document document = getFeed().getDocument();

        try {

            // for RSS
            node = XPathHelper.getNode(document, "//item[link=\"" + getLink() + "\"]");

            if (node == null) {
                node = XPathHelper.getNode(document, "//item[title=\"" + getTitle().replaceAll("\"", "&quot;") + "\"]");

                // for Atom
                if (node == null) {
                    node = XPathHelper.getNode(document, "//atom:entry[atom:id=\"" + getRawId() + "\"]", namespaceMap);
                }
            }

        } catch (Exception e) {
            LOGGER.error("synd entry was not complete (" + getFeedUrl() + "), " + e.getMessage());
        }

        if (node == null) {
            LOGGER.error("feed: " + getFeedUrl() + ", node = null");
        }

        return node;
    }

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

}