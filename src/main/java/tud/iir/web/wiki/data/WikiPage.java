package tud.iir.web.wiki.data;

import java.util.Date;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * Representation of a Wiki page with title, pageID, namespaceID, content, revisions, etc.
 * 
 * @author Sandro Reichert
 */
public class WikiPage {

    /** The global logger */
    private static final Logger LOGGER = Logger.getLogger(WikiPage.class);

    /** Unique identifier of the Wiki this page is in, created by data base. */
    private int wikiID = -1;

    /** The page's title. */
    private String title = null;

    /** The Wiki's internal id of this page. */
    private int pageID = -1;

    /** The Wiki's namespace id the page is in. */
    private int namespaceID = -1;

    /** Additional meta data, calculated from the page's revision history to express the page's update frequency. */
    private Float sourceDynamics = null;

    /** The page's content as HTML representation, rendered by the Wiki. */
    private String pageContentHTML = null;

    /** The page's revision history. */
    private TreeMap<Long, Revision> revisions = new TreeMap<Long, Revision>();

    /**
     * The revisionID of the newest revision. Ugly hack since one have to be careful to synchronize it with
     * {@link #revisions}.
     * 
     * @see {@link #setNewestRevisionID(Long)}
     */
    private Long newestRevisionID = null;

    /** The date this page should be checked for new revisions the next time */
    private Date nextCheck = null;

    /**
     * @return Unique identifier of the Wiki this page is in, created by data base.
     */
    public final int getWikiID() {
        return wikiID;
    }

    /**
     * @param wikiID Unique identifier of the Wiki this page is in, created by data base.
     */
    public final void setWikiID(int wikiID) {
        this.wikiID = wikiID;
    }

    /**
     * @return The page's title.
     */
    public final String getTitle() {
        return title;
    }

    /**
     * @param pageTitle The page's title.
     */
    public final void setTitle(String pageTitle) {
        this.title = pageTitle;
    }

    /**
     * @return The Wiki's internal id of this page.
     */
    public final int getPageID() {
        return pageID;
    }

    /**
     * @param pageID The Wiki's internal id of this page.
     */
    public final void setPageID(int pageID) {
        this.pageID = pageID;
    }

    /**
     * @return The Wiki's namespace id the page is in.
     */
    public final int getNamespaceID() {
        return namespaceID;
    }

    /**
     * @param namespaceID The Wiki's namespace id the page is in.
     */
    public final void setNamespaceID(int namespaceID) {
        this.namespaceID = namespaceID;
    }

    /**
     * @return Additional meta data, calculated from the page's revision history to express the page's update frequency.
     */
    public final Float getSourceDynamics() {
        return sourceDynamics;
    }

    /**
     * @param sourceDynamics Additional meta data, calculated from the page's revision history to express the page's
     *            update frequency.
     */
    public final void setSourceDynamics(float sourceDynamics) {
        this.sourceDynamics = sourceDynamics;
    }

    /**
     * @return The page's content as HTML representation, rendered by the Wiki.
     */
    public final String getPageContent() {
        return pageContentHTML;
    }

    /**
     * @param pageContent The page's content as HTML representation, rendered by the Wiki.
     */
    public final void setPageContent(String pageContent) {
        this.pageContentHTML = pageContent;
    }

    /**
     * @return All authors that contributed to at least one revision of the page.
     */
    public final TreeSet<String> getAuthors() {
        TreeSet<String> authors = new TreeSet<String>();
        for (Revision revision : revisions.values()) {
            authors.add(revision.getAuthor());
        }
        return authors;
    }

    /**
     * @return The page's revision history as pairs of (revisionID, {@link Revision}).
     */
    public final TreeMap<Long, Revision> getRevisions() {
        return revisions;
    }

    /**
     * @return All revision time stamps.
     */
    public final TreeSet<Date> getRevisionTimeStamps() {
        TreeSet<Date> timeStamps = new TreeSet<Date>();
        for (Revision revision : revisions.values()) {
            timeStamps.add(revision.getTimestamp());
        }
        return timeStamps;
    }

    /**
     * Add a single revision to this page.
     * 
     * @param revision The revision from Wiki API.
     */
    public final void addRevision(final Revision revision) {
        if (revisions.containsKey(revision.getRevisionID())) {
            LOGGER.warn("Revision " + revision.getRevisionID() + " could not be added, it is already contained!");
        } else {
            revisions.put(revision.getRevisionID(), revision);
            newestRevisionID = revisions.lastKey();
        }
    }

    /**
     * Ugly hack to set and store the newest revisionID additional to {@link #revisions} to get this information without
     * asking database table revisions (speeds up crawler).
     * 
     * @param newesRevisionID the revisionId read from table pages, might be null if page content has never been
     *            crawled.
     */
    public final void setNewestRevisionID(Long newesRevisionID) {
        this.newestRevisionID = newesRevisionID;
    }

    /**
     * Get the highest revisionID of this page.
     * 
     * @return The highest revisionID of this page or null if no revision is known.
     */
    public final Long getNewestRevisionID() {
        return newestRevisionID;
    }

    /**
     * The date this page should be checked for new revisions the next time. This value is predicted by a predictor
     * component.
     * 
     * @return The date this page should be checked for new revisions the next time.
     */
    public final Date getNextCheck() {
        return nextCheck;
    }

    /**
     * The date this page should be checked for new revisions the next time. This value is predicted by a predictor
     * component.
     * 
     * @param nextCheck The date this page should be checked for new revisions the next time.
     */
    public final void setNextCheck(Date nextCheck) {
        this.nextCheck = nextCheck;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "WikiPage [wikiID=" + wikiID + ", title=" + title + ", pageID=" + pageID + ", namespaceID="
                + namespaceID + ", sourceDynamics=" + sourceDynamics + ", pageContentHTML=" + pageContentHTML
                + ", newestRevisionID=" + newestRevisionID + ", nextCheck=" + nextCheck + ", revisions=" + revisions
                + "]";
    }

}
