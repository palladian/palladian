package ws.palladian.retrieval.feeds.meta;

import java.sql.Timestamp;
import java.util.Date;

/**
 * <p>Represents MetaInformation about a Feed. Used for evaluation and statistical purposes.</p>
 * 
 * @author Philipp Katz
 * @author Sandro Reichert
 * 
 */
public class FeedMetaInformation {

    // ////// General meta data \\\\\\\\

    /** The URL of the website this feed has been found at. */
    private String siteUrl = null;
    
    /** <code>true</code> if feed is accessible, <code>false</code> if not, <code>null</code> if unknown. */
    private Boolean isAccessible = null;

    /** <code>true</code> if feed supports PubSubHubbub, <code>false</code> if not, <code>null</code> if unknown. */
    private Boolean supportsPubSubHubBub = null;

    /** The feed's format such as rss 2.0, <code>null</code> if unknown. */
    private String feedFormat = null;

    /**
     * <code>true</code> if the feed's items have ids (id or guid), <code>false</code> if not, <code>null</code> if
     * unknown.
     */
    private Boolean hasItemIds = null;

    /** The date the feed has been added. */
    private Date added = null;

    /** The feed's title */
    private String title = null;

    /** The feed's language. */
    private String language = null;

    /** The size of the feed in bytes. */
    private long byteSize = 0;

    /**
     * The size of a conditional GET response. <code>null</code> if unknown.
     */
    private Integer cgHeaderSize;


    // ////// RSS specific meta data. \\\\\\\\

    /**
     * <code>true</code> if the feed's items contain the pubDate attribute (RSS2 specific), <code>false</code> if not,
     * <code>null</code> if unknown.
     */
    private Boolean hasPubDate = null;

    /**
     * <code>true</code> if the feed supports the cloud attribute (RSS2 specific) , <code>false</code> if not,
     * <code>null</code> if unknown.
     */
    private Boolean hasCloud = null;

    /**
     * The value of the feed's time to live attribute (RSS2 specific), <code>null</code> if unknown or not
     * present.
     */
    private Integer ttl = null;

    /**
     * <code>true</code> if the feed supports the skipHours attribute (RSS2 specific), <code>false</code> if
     * not, <code>null</code> if unknown.
     */
    private Boolean hasSkipHours = null;

    /**
     * <code>true</code> if the feed supports the skipDays attribute (RSS2 specific), <code>false</code> if
     * not, <code>null</code> if unknown.
     */
    private Boolean hasSkipDays = null;
    

    // ////// Atom specific meta data. \\\\\\\\

    /**
     * <code>true</code> if the feed's items contain the updated attribute (Atom specific), <code>false</code> if not,
     * <code>null</code> if unknown.
     */
    private Boolean hasUpdated = null;

    /**
     * <code>true</code> if the feed's items contain the published attribute (Atom specific), <code>false</code> if not,
     * <code>null</code> if unknown.
     */
    private Boolean hasPublished = null;
    

    
    /**
     * @return <code>true</code> if feed is accessible, <code>false</code> if not, <code>null</code> if unknown.
     */
    public Boolean isAccessible() {
        return isAccessible;
    }

    /**
     * Set to <code>true</code> if feed is accessible, <code>false</code> if not, <code>null</code> if unknown.
     * 
     * @param isAccessible <code>true</code> if feed is accessible, <code>false</code> if not, <code>null</code> if
     *            unknown.
     */
    public void setAccessible(Boolean isAccessible) {
        this.isAccessible = isAccessible;
    }


    /**
     * @return <code>true</code> if feed supports PubSubHubbub, <code>false</code> if not, <code>null</code> if unknown.
     */
    public Boolean isSupportsPubSubHubBub() {
        return supportsPubSubHubBub;
    }

    /**
     * <code>true</code> if feed supports PubSubHubbub, <code>false</code> if not, <code>null</code> if unknown.
     * 
     * @param supportsPubSubHubBub <code>true</code> if feed supports PubSubHubbub, <code>false</code> if not,
     *            <code>null</code> if unknown.
     */
    public void setSupportsPubSubHubBub(Boolean supportsPubSubHubBub) {
        this.supportsPubSubHubBub = supportsPubSubHubBub;
    }

    /**
     * @return The feed's format such as rss 2.0, <code>null</code> if unknown.
     */
    public String getFeedFormat() {
        return feedFormat;
    }

    /**
     * The feed's format such as rss 2.0, <code>null</code> if unknown.
     * 
     * @param feedVersion The feed's format such as rss 2.0, <code>null</code> if unknown.
     */
    public void setFeedFormat(String feedVersion) {
        this.feedFormat = feedVersion;
    }

    /**
     * @return <code>true</code> if the feed's items have ids (id or guid), <code>false</code> if not, <code>null</code>
     *         if unknown.
     */
    public Boolean hasItemIds() {
        return hasItemIds;
    }

    /**
     * Set to <code>true</code> if the feed's items have ids (id or guid), <code>false</code> if not, <code>null</code>
     * if unknown.
     * 
     * @param hasItemIds <code>true</code> if the feed's items have ids (id or guid), <code>false</code> if not,
     *            <code>null</code> if unknown.
     */
    public void setHasItemIds(Boolean hasItemIds) {
        this.hasItemIds = hasItemIds;
    }

    /**
     * @return <code>true</code> if the feed's items contain the pubDate attribute (RSS2 specific), <code>false</code>
     *         if not, <code>null</code> if unknown.
     */
    public Boolean hasPubDate() {
        return hasPubDate;
    }

    /**
     * <code>true</code> if the feed's items contain the pubDate attribute (RSS2 specific), <code>false</code> if not,
     * <code>null</code> if unknown.
     * 
     * @param hasPubDate <code>true</code> if the feed's items contain the pubDate attribute (RSS2 specific),
     *            <code>false</code> if not, <code>null</code> if unknown.
     */
    public void setHasPubDate(Boolean hasPubDate) {
        this.hasPubDate = hasPubDate;
    }

    /**
     * @return <code>true</code> if the feed supports the cloud attribute (RSS2 specific) , <code>false</code> if not,
     *         <code>null</code> if unknown.
     */
    public Boolean hasCloud() {
        return hasCloud;
    }

    /**
     * <code>true</code> if the feed supports the cloud attribute (RSS2 specific) , <code>false</code> if not,
     * <code>null</code> if unknown.
     * 
     * @param hasCloud <code>true</code> if the feed supports the cloud attribute (RSS2 specific) , <code>false</code>
     *            if not, <code>null</code> if unknown.
     */
    public void setHasCloud(Boolean hasCloud) {
        this.hasCloud = hasCloud;
    }

    /**
     * @return The value of the feed's time to live attribute (RSS2 specific), <code>null</code> if unknown or not
     *         present.
     */
    public Integer getRssTtl() {
        return ttl;
    }

    /**
     * The value of the feed's time to live attribute (RSS2 specific), <code>null</code> if unknown or not
     * present.
     * 
     * @param ttl The value of the feed's time to live attribute (RSS2 specific), <code>null</code> if unknown or not
     *            present.
     */
    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    /**
     * @return <code>true</code> if the feed supports the skipHours attribute (RSS2 specific), <code>false</code> if
     *         not, <code>null</code> if unknown.
     */
    public Boolean hasSkipHours() {
        return hasSkipHours;
    }

    /**
     * <code>true</code> if the feed supports the skipHours attribute (RSS2 specific), <code>false</code> if
     * not, <code>null</code> if unknown.
     * 
     * @param hasSkipHours <code>true</code> if the feed supports the skipHours attribute (RSS2 specific),
     *            <code>false</code> if not, <code>null</code> if unknown.
     */
    public void setHasSkipHours(Boolean hasSkipHours) {
        this.hasSkipHours = hasSkipHours;
    }

    /**
     * @return <code>true</code> if the feed supports the skipDays attribute (RSS2 specific), <code>false</code> if
     *         not, <code>null</code> if unknown.
     */
    public Boolean hasSkipDays() {
        return hasSkipDays;
    }

    /**
     * <code>true</code> if the feed supports the skipDays attribute (RSS2 specific), <code>false</code> if
     * not, <code>null</code> if unknown.
     * 
     * @param hasSkipDays <code>true</code> if the feed supports the skipDays attribute (RSS2 specific),
     *            <code>false</code> if not, <code>null</code> if unknown.
     */
    public void setHasSkipDays(Boolean hasSkipDays) {
        this.hasSkipDays = hasSkipDays;
    }

    /**
     * @return <code>true</code> if the feed's items contain the updated attribute (Atom specific), <code>false</code>
     *         if not, <code>null</code> if unknown.
     */
    public Boolean hasUpdated() {
        return hasUpdated;
    }

    /**
     * <code>true</code> if the feed's items contain the updated attribute (Atom specific), <code>false</code> if not,
     * <code>null</code> if unknown.
     * 
     * @param hasUpdated <code>true</code> if the feed's items contain the updated attribute (Atom specific),
     *            <code>false</code> if not, <code>null</code> if unknown.
     */
    public void setHasUpdated(Boolean hasUpdated) {
        this.hasUpdated = hasUpdated;
    }

    /**
     * @return <code>true</code> if the feed's items contain the published attribute (Atom specific), <code>false</code>
     *         if not, <code>null</code> if unknown.
     */
    public Boolean hasPublished() {
        return hasPublished;
    }

    /**
     * <code>true</code> if the feed's items contain the published attribute (Atom specific), <code>false</code> if not,
     * <code>null</code> if unknown.
     * 
     * @param hasPublished <code>true</code> if the feed's items contain the published attribute (Atom specific),
     *            <code>false</code> if not, <code>null</code> if unknown.
     */
    public void setHasPublished(Boolean hasPublished) {
        this.hasPublished = hasPublished;
    }

    /**
     * @return The URL of the website this feed has been found at.
     */
    public String getSiteUrl() {
        return siteUrl;
    }

    /**
     * @param pageUrl The URL of the website this feed has been found at.
     */
    public void setSiteUrl(String pageUrl) {
        // do not override an existing value with null.
        if (getSiteUrl() != null && pageUrl == null) {
            return;
        }
        this.siteUrl = pageUrl;
    }

    /**
     * @return The feed's title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title The feed's title.
     */
    public void setTitle(String title) {
        if (title != null) {
            this.title = title;
        }
    }

    /**
     * @return The date the feed has been added.
     */
    public Date getAdded() {
        return added;
    }

    /**
     * @return The date the feed has been added.
     */
    public Timestamp getAddedSQLTimestamp() {
        if (added != null) {
            return new Timestamp(added.getTime());
        }
        return null;
    }

    /**
     * @param added The date the feed has been added.
     */
    public void setAdded(Date added) {
        this.added = added;
    }

    /**
     * @return The feed's language.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language The feed's language.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @param byteSize The size in bytes of the raw feed.
     */
    public void setByteSize(long byteSize) {
        this.byteSize = byteSize;
    }

    /**
     * @return byteSize The size in bytes of the raw feed.
     */
    public long getByteSize() {
        return byteSize;
    }

    /**
     * @param cgHeaderSize The size of a conditional GET response. <code>null</code> if unknown.
     */
    public void setCgHeaderSize(Integer cgHeaderSize) {
        this.cgHeaderSize = cgHeaderSize;
    }

    /**
     * @return The size of a conditional GET response. <code>null</code> if unknown.
     */
    public Integer getCgHeaderSize() {
        if (cgHeaderSize != null && cgHeaderSize <= 0) {
            cgHeaderSize = null;
        }
        return cgHeaderSize;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FeedMetaInformation [isAccessible=");
        builder.append(isAccessible);
        builder.append(", supportsPubSubHubBub=");
        builder.append(supportsPubSubHubBub);
        builder.append(", feedFormat=");
        builder.append(feedFormat);
        builder.append(", hasItemIds=");
        builder.append(hasItemIds);
        builder.append(", hasPubDate=");
        builder.append(hasPubDate);
        builder.append(", hasCloud=");
        builder.append(hasCloud);
        builder.append(", ttl=");
        builder.append(ttl);
        builder.append(", hasSkipHours=");
        builder.append(hasSkipHours);
        builder.append(", hasSkipDays=");
        builder.append(hasSkipDays);
        builder.append(", hasUpdated=");
        builder.append(hasUpdated);
        builder.append(", hasPublished=");
        builder.append(hasPublished);
        builder.append("]");
        return builder.toString();
    }

}
