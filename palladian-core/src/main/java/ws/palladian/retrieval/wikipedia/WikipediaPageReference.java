package ws.palladian.retrieval.wikipedia;

import java.util.Date;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.retrieval.resources.WebContent;

/**
 * <p>
 * A reference to a MediaWiki page. Not the fully retrieved page, this is represented by {@link WikipediaPage} and
 * additionally contains the text.
 * </p>
 * 
 * @author katz
 */
public class WikipediaPageReference implements WebContent {

    /** The id of the main namespace with articles. Other namespaces contain meta pages, like discussions etc. */
    public static final int MAIN_NAMESPACE = 0;

    private final int pageId;
    private final int namespaceId;
    private final String title;

    public WikipediaPageReference(int pageId, int namespaceId, String title) {
        this.pageId = pageId;
        this.namespaceId = namespaceId;
        this.title = title;
    }
    
    @Override
    public String getIdentifier() {
        return String.valueOf(pageId);
    }

//    /**
//     * @return The unique page ID in the wiki.
//     */
//    public int getPageId() {
//        return pageId;
//    }

    /**
     * @return The namespace ID, in which this page resides.
     */
    public int getNamespaceId() {
        return namespaceId;
    }

    @Override
    public String getTitle() {
        return title;
    }

    /**
     * @return The title of the page, but with text in parenthesis and after comma removed.
     */
    public String getCleanTitle() {
        String clean = title.replaceAll("\\s\\([^)]*\\)", "");
        clean = clean.replaceAll(",.*", ""); // XXX comma should not be here! (makes sense for locations only)
        return clean;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WikipediaPageReference [pageId=");
        builder.append(pageId);
        builder.append(", namespaceId=");
        builder.append(namespaceId);
        builder.append(", title=");
        builder.append(title);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public String getSummary() {
        return null;
    }

    @Override
    public Date getPublished() {
        return null;
    }

    @Override
    public GeoCoordinate getCoordinate() {
        return null;
    }

}
