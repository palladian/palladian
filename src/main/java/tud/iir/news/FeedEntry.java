package tud.iir.news;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a news entry within a feed ({@link Feed}).
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class FeedEntry {

    private int id = -1;
    private String title;
    private String link;

    /** original ID from the feed */
    private String rawId;

    /** publish date from the feed */
    private Date published;

    /** when the entry was aggregated */
    private Date added;

    /** contains text directly from the feed entry */
    private String text;

    /** contains text which we scraped from the corresponding web page */
    private String pageText;

    // assigned tags
    private List<String> tags = new ArrayList<String>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPageText() {
        return pageText;
    }

    public void setPageText(String pageText) {
        this.pageText = pageText;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Entry");
        // sb.append(" id:").append(id);
        sb.append(" title:").append(title);
        sb.append(" link:").append(link);
        // sb.append(" rawId:").append(rawId);
        // sb.append(" published:").append(published);
        // sb.append(" text:").append(text);
        return sb.toString();
    }

}