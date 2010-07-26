package tud.iir.news;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.cyberneko.html.HTMLConfiguration;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StringInputStream;

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

    /** content directly from the feed entry */
    private String content;

    /** content which we downloaded from the corresponding web page. */
    // private String pageContent;
    private Document pageContent;

    /** assigned tags from feed entry. */
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Document getPageContent() {
        return pageContent;
    }

    public void setPageContent(Document pageContent) {
        this.pageContent = pageContent;
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
        sb.append(" published:").append(published);
        // sb.append(" text:").append(text);
        return sb.toString();
    }

    /**
     * Get text representation of FeedEntry, without HTML tags. Either from the page content or directly from the feed
     * entry.
     * 
     * @return
     */
    public String getText() {
        String result = getPageText();
        if (result == null) {
            result = getEntryText();
        }
        return result;
    }
    
    public Document getEntryContent() {
        Document result = null;
        try {
            
            DOMParser parser = new DOMParser(new HTMLConfiguration());
            parser.setFeature("http://cyberneko.org/html/features/insert-namespaces", true);
            parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
            parser.parse(new InputSource(new StringInputStream(getContent())));
            result = parser.getDocument();
            
        } catch (SAXNotRecognizedException e) {
            Logger.getRootLogger().error(e);
        } catch (SAXNotSupportedException e) {
            Logger.getRootLogger().error(e);
        } catch (SAXException e) {
            Logger.getRootLogger().error(e);
        } catch (IOException e) {
            Logger.getRootLogger().error(e);
        }
        return result;        
    }

    String getPageText() {
        String result = null;
        if (getPageContent() != null) {
            result = HTMLHelper.htmlDocToString(getPageContent());
            result = result.replaceAll("\n", " ");
            result = result.replaceAll(" {2,}", " ");
        }
        return result;
    }

    String getEntryText() {
        String result = null;
        if (getContent() != null) {
            result = HTMLHelper.htmlFragmentsToString(getContent(), true);
        }
        return result;
    }

}