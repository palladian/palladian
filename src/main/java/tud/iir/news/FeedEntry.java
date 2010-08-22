package tud.iir.news;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Represents a news entry within a feed ({@link Feed}).
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class FeedEntry {

    private int id = -1;
    
    private int feedId = -1;
    
    private String title;
    private String link;

    /** original ID from the feed */
    private String rawId;

    /** publish date from the feed */
    private Date published;

    /** when the entry was aggregated */
    private Date added;

    /** entryText directly from the feed entry */
    private String entryText;

    /** entryText which we downloaded from the corresponding web page. */
    private String pageText;

    // /** assigned tags from feed entry. */
    // private List<String> tags = new ArrayList<String>();
    
    /** arbitrary, numeric features, used for feature extraction and classification. */
    // TODO change from Float to Number
    // private Map<String, Float> features = new HashMap<String, Float>();
    private SortedMap<String, Float> features = new TreeMap<String, Float>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public int getFeedId() {
        return feedId;
    }
    
    public void setFeedId(int feedId) {
        this.feedId = feedId;
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

    public String getEntryText() {
        return entryText;
    }

    public void setEntryText(String entryText) {
        this.entryText = entryText;
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
            text = getEntryText();
        }

        if (text == null) {
            text = "";
        }

        return text;
    }

    // public List<String> getTags() {
    // return tags;
    // }
    //
    // public void setTags(List<String> tags) {
    // this.tags = tags;
    // }
    //
    // public void addTag(String tag) {
    // tags.add(tag);
    // }
    
    public SortedMap<String, Float> getFeatures() {
        return features;
    }
    
    public void setFeatures(SortedMap<String, Float> features) {
        this.features = features;
    }

    public Float getFeature(String key) {
        return features.get(key);
    }

    public Float putFeature(String key, Float value) {
        return features.put(key, value);
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

}