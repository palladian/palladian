package ws.palladian.retrieval.feeds.meta;

import ws.palladian.retrieval.feeds.Feed;

/**
 * Represents MetaInformation about a Feed. Used for evaluation and statistical purposes.
 * 
 * @author Philipp Katz
 *
 */
public class FeedMetaInformation {
    
    /** The associated feed. */
    private Feed feed;
    
    private boolean isAccessible = false;
    private boolean supports304 = false;
    private boolean supportsETag = false;
    private int responseSize = -1;
    private boolean supportsPubSubHubBub = false;
    private String feedFormat = null;
    private boolean hasItemIds = false;
    
    /** RSS specific meta data. */
    private boolean hasPubDate = false;
    private boolean hasCloud = false;
    private int ttl = -1;
    private boolean hasSkipHours = false;
    private boolean hasSkipDays = false;
    
    /** Atom specific meta data. */
    private boolean hasUpdated = false;
    private boolean hasPublished = false;
    
    public FeedMetaInformation(Feed feed) {
        this.feed = feed;
    }
    
    public boolean isAccessible() {
        return isAccessible;
    }
    public void setAccessible(boolean isAccessible) {
        this.isAccessible = isAccessible;
    }
    public boolean isSupports304() {
        return supports304;
    }
    public void setSupports304(boolean supports304) {
        this.supports304 = supports304;
    }
    public boolean isSupportsETag() {
        return supportsETag;
    }
    public void setSupportsETag(boolean supportsETag) {
        this.supportsETag = supportsETag;
    }
    public int getResponseSize() {
        return responseSize;
    }
    public void setResponseSize(int responseSize) {
        this.responseSize = responseSize;
    }
    public boolean isSupportsPubSubHubBub() {
        return supportsPubSubHubBub;
    }
    public void setSupportsPubSubHubBub(boolean supportsPubSubHubBub) {
        this.supportsPubSubHubBub = supportsPubSubHubBub;
    }
    public String getFeedFormat() {
        return feedFormat;
    }
    public void setFeedFormat(String feedVersion) {
        this.feedFormat = feedVersion;
    }
    public boolean hasItemIds() {
        return hasItemIds;
    }
    public void setHasItemIds(boolean hasItemIds) {
        this.hasItemIds = hasItemIds;
    }
    public boolean hasPubDate() {
        return hasPubDate;
    }
    public void setHasPubDate(boolean hasPubDate) {
        this.hasPubDate = hasPubDate;
    }
    public boolean hasCloud() {
        return hasCloud;
    }
    public void setHasCloud(boolean hasCloud) {
        this.hasCloud = hasCloud;
    }
    public int getTtl() {
        return ttl;
    }
    public void setTtl(int ttl) {
        this.ttl = ttl;
    }
    public boolean hasSkipHours() {
        return hasSkipHours;
    }
    public void setHasSkipHours(boolean hasSkipHours) {
        this.hasSkipHours = hasSkipHours;
    }
    public boolean hasSkipDays() {
        return hasSkipDays;
    }
    public void setHasSkipDays(boolean hasSkipDays) {
        this.hasSkipDays = hasSkipDays;
    }
    public boolean hasUpdated() {
        return hasUpdated;
    }
    public void setHasUpdated(boolean hasUpdated) {
        this.hasUpdated = hasUpdated;
    }
    public boolean hasPublished() {
        return hasPublished;
    }
    public void setHasPublished(boolean hasPublished) {
        this.hasPublished = hasPublished;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FeedMetaInformation [feed=");
        builder.append(feed.getFeedUrl());
        builder.append(", isAccessible=");
        builder.append(isAccessible);
        builder.append(", supports304=");
        builder.append(supports304);
        builder.append(", supportsETag=");
        builder.append(supportsETag);
        builder.append(", responseSize=");
        builder.append(responseSize);
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
