package ws.palladian.retrieval.feeds.meta;

/**
 * Represents MetaInformation about a Feed. Used for evaluation purposes.
 * 
 * @author Philipp Katz
 *
 */
public class FeedMetaInformation {
    
    private boolean isAccessible = false;
    private boolean supports304 = false;
    private boolean supportsETag = false;
    private int responseSize = -1;
    private boolean supportsPubSubHubBub = false;
    private String feedVersion = null;
    private boolean hasItemIds = false;
    
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
    public String getFeedVersion() {
        return feedVersion;
    }
    public void setFeedVersion(String feedVersion) {
        this.feedVersion = feedVersion;
    }
    public boolean hasItemIds() {
        return hasItemIds;
    }
    public void setHasItemIds(boolean hasItemIds) {
        this.hasItemIds = hasItemIds;
    }
    
    

}
