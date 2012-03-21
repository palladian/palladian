package ws.palladian.retrieval.wiki.data;

import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import ws.palladian.helper.html.HtmlHelper;

/**
 * Representation of a Wiki page with title, pageID, namespaceID, content, revisions, etc.
 * 
 * @author Sandro Reichert
 */
public class WikiPage {

    /** The global logger */
    private static final Logger LOGGER = Logger.getLogger(WikiPage.class);

    /** Unique identifier of the Wiki this page is in, created by data base. */
    private Integer wikiID = null;

    /** The page's title. */
    private String title = null;

    /** The page's URL */
    private URL pageURL = null;

    /** The Wiki's internal id of this page. */
    private Integer pageID = null;

    /** The Wiki's namespace id the page is in. */
    private Integer namespaceID = null;

    /** Additional meta data, calculated from the page's revision history to express the page's update frequency. */
    private Float sourceDynamics = null;

    /** The page's content as HTML representation, rendered by the Wiki. */
    private String pageContentHTML = null;

    /** Flag, if <code>true</code>, the page has been deleted from the Wiki. */
    private boolean pageDeleted = false;

    /** The page's revision history. */
    private TreeMap<Long, Revision> revisions = null;

    /** All Wiki-internal hyperlinks to other pages that do exist */
    private Set<Integer> hyperLinks = new HashSet<Integer>();

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
     * @return Unique identifier of the Wiki this page is in, created by data base. <code>null</code> if the wikiID has
     *         not been set yet.
     */
    public final Integer getWikiID() {
        return wikiID;
    }

    /**
     * @param wikiID Unique identifier of the Wiki this page is in, created by data base.
     */
    public final void setWikiID(int wikiID) {
        this.wikiID = wikiID;
    }

    /**
     * @return The page's title. <code>null</code> if the title has not been set yet.
     */
    public final String getTitle() {
        return title;
    }

    /**
     * @param pageTitle The page's title. Value may not be an empty {@link String}.
     */
    public final void setTitle(String pageTitle) {
        if (pageTitle.length() == 0) {
            throw new IllegalArgumentException("Value for pageTitle may not be an empty string!");
        }
        this.title = pageTitle;
    }

    /**
     * @return The {@link URL} this page can be found at or <code>null</code> if it has not been set.
     */
    public final URL getPageURL() {
        return pageURL;
    }

    /**
     * @param pageURL The {@link URL} this page can be found at.
     */
    public final void setPageURL(URL pageURL) {
        this.pageURL = pageURL;
    }

    /**
     * @return The Wiki's internal id of this page or <code>null</code> if it has not been set.
     */
    public final Integer getPageID() {
        return pageID;
    }

    /**
     * @param pageID The Wiki's internal id of this page.
     */
    public final void setPageID(int pageID) {
        this.pageID = pageID;
    }

    /**
     * @return The Wiki's namespace id the page is in or <code>null</code> if it has not been set.
     */
    public final Integer getNamespaceID() {
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
     *         <code>null</code> if it has not been set.
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
     * @return The page content as HTML representation, rendered by the Wiki, or <code>null</code> if it has not been
     *         set.
     */
    public final String getPageContentHTML() {
        return pageContentHTML;
    }

    /**
     * @return The page content without HTML tags or <code>null</code> if it has not been set.
     */
    public final String getPageContentStriped() {
        String stripedText = null;
        if (pageContentHTML != null) {
            stripedText = HtmlHelper.documentToReadableText(pageContentHTML, false);
        }
        return stripedText;
    }

    /**
     * @param pageContent The page's content as HTML representation, rendered by the Wiki.
     */
    public final void setPageContentHTML(String pageContent) {
        this.pageContentHTML = pageContent;
    }

    /**
     * Check whether this page still exists on the Wiki.
     * 
     * @return <code>true</code> if the page has been deleted from the Wiki.
     */
    public final boolean isPageDeleted() {
        return pageDeleted;
    }

    /**
     * @param pageDeleted <code>true</code> if page has been deleted on the Wiki.
     */
    public final void setPageDeleted(boolean pageDeleted) {
        this.pageDeleted = pageDeleted;
    }

    /**
     * @return All authors that contributed to at least one revision of the page or <code>null</code> if revisions are
     *         not set and therefore authors are not known.
     */
    public final Set<String> getAuthors() {
        TreeSet<String> authors = null;

        if (revisions != null) {
            authors = new TreeSet<String>();
            for (Revision revision : revisions.values()) {
                authors.add(revision.getAuthor());
            }
        }
        return authors;
    }

    /**
     * @return The page's revision history as pairs of (revisionID, {@link Revision}) or <code>null</code> if revisions
     *         have not been set.
     */
    public final TreeMap<Long, Revision> getRevisions() {
        return revisions;
    }

    /**
     * @return All revision time stamps or <code>null</code> if revisions have not been set.
     */
    public final TreeSet<Date> getRevisionTimeStamps() {
        TreeSet<Date> timeStamps = null;
        if (revisions != null) {
            timeStamps = new TreeSet<Date>();
            for (Revision revision : revisions.values()) {
                timeStamps.add(revision.getTimestamp());
            }
        }
        return timeStamps;
    }

    /**
     * Get the {@link Date} this {@link WikiPage} has been created.
     * 
     * @return The {@link Date} this page has been created or <code>null</code> if revisions are not set.
     */
    public final Date getCreatedDate() {
        Date created = null;
        if ((revisions != null) && (!revisions.isEmpty())) {
            created = revisions.firstEntry().getValue().getTimestamp();
        }
        return created;
    }

    /**
     * Get the {@link Date} this {@link WikiPage} has been modified the last time.
     * 
     * @return The {@link Date} this page has been last modified or <code>null</code> if revisions are not set.
     */
    public final Date getLastModifiedDate() {
        Date lastModified = null;
        if ((revisions != null) && (!revisions.isEmpty())) {
            lastModified = revisions.lastEntry().getValue().getTimestamp();
        }
        return lastModified;
    }

    /**
     * Add a single revision to this page.
     * 
     * @param revision The revision from Wiki API.
     */
    public final void addRevision(final Revision revision) {
        if (revision == null) {
            throw new IllegalArgumentException("Revision may not be null!");
        }

        if (revisions == null){
            revisions = new TreeMap<Long, Revision>();
        }
        
        if (revisions.containsKey(revision.getRevisionID())) {
            LOGGER.warn("Revision " + revision.getRevisionID() + " could not be added, it is already contained!");
        } else {
            revisions.put(revision.getRevisionID(), revision);
            newestRevisionID = revisions.lastKey();
        }
    }

    /**
     * USE WITH CAUTION! Ugly hack to set and store the newest revisionID without setting complete revisions with
     * {@link #addRevision(Revision)}. This is required since the newest revisionID can be fetched from API without
     * fetching all revisions. (speeds up crawler).
     * 
     * @param newesRevisionID The revisionId read from DB table pages, might be null if page content has never been
     *            crawled.
     */
    public final void setNewestRevisionID(Long newesRevisionID) {
        this.newestRevisionID = newesRevisionID;
    }

    /**
     * Get the newest revisionID of this page.
     * 
     * @return The newest revisionID of this page or <code>null</code> if no revision is known.
     */
    public final Long getNewestRevisionID() {
        return newestRevisionID;
    }

    /**
     * The date this page should be checked for new revisions the next time. This value is normally predicted by a
     * predictor component.
     * 
     * @return The date this page should be checked for new revisions the next time or <code>null</code> if it is not
     *         set.
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

    /**
     * Get a {@link Set} of all pageIds this page has hyperlinks to. This set contains pages that:
     * <ul>
     * <li>are in the same Wiki. No external links.</li>
     * <li>already exist. Links to pages that do not exist yet are not included.</li>
     * </ul>
     * 
     * @return a {@link Set} of all pageIds this page has hyperlinks to or <code>null</code> if it is not
     *         set.
     */
    public final Set<Integer> getHyperLinks() {
        return hyperLinks;
    }

    /**
     * @param pageIDDestination The pageID this page has a hyperlink to.
     * @return <code>true</code> if this set did not already contain the specified element.
     */
    public final boolean addHyperLink(int pageIDDestination) {
        return hyperLinks.add(pageIDDestination);
    }

    /**
     * @param pageIDDestination The pageID this page has a hyperlink to.
     * @return <code>true</code> if this set does not already contains the specified element.
     */
    public final boolean addHyperLinks(Set<Integer> pageIDsDestination) {
        if (pageIDsDestination == null) {
            throw new IllegalArgumentException("Value for pageIDsDestination may not be null!");
        }
        return hyperLinks.addAll(pageIDsDestination);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "WikiPage [wikiID=" + wikiID + ", title=" + title + ", pageURL=" + pageURL + ", pageID=" + pageID
                + ", namespaceID=" + namespaceID + ", sourceDynamics=" + sourceDynamics + ", pageContentHTML="
                + pageContentHTML + ", pageDeleted=" + pageDeleted + ", revisions=" + revisions + ", hyperLinks="
                + hyperLinks + ", newestRevisionID=" + newestRevisionID + ", nextCheck=" + nextCheck + "]";
    }

}
