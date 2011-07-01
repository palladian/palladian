package ws.palladian.retrieval.feeds.meta;

import java.util.Date;

public class PollMetaInformation {

    private int feedID = -1;

    private Date pollTimestamp = null;

    private String httpETag = null;

    private Date httpDate = null;

    private Date httpLastModified = null;

    private Date httpExpires = null;

    private Long httpTTL = null;

    private Date newestItemTimestamp = null;

    private Integer numberNewItems = null;

    private Integer windowSize = null;

    /**
     * @return the feedID
     */
    public final int getFeedID() {
        return feedID;
    }

    /**
     * @param feedID the feedID to set
     */
    public final void setFeedID(int feedID) {
        this.feedID = feedID;
    }

    /**
     * @return the pollTimestamp
     */
    public final Date getPollTimestamp() {
        return pollTimestamp;
    }

    /**
     * @param pollTimestamp the pollTimestamp to set
     */
    public final void setPollTimestamp(Date pollTimestamp) {
        this.pollTimestamp = pollTimestamp;
    }

    /**
     * @return the httpETag
     */
    public final String getHttpETag() {
        return httpETag;
    }

    /**
     * @param httpETag the httpETag to set
     */
    public final void setHttpETag(String httpETag) {
        this.httpETag = httpETag;
    }

    /**
     * @return the httpDate
     */
    public final Date getHttpDate() {
        return httpDate;
    }

    /**
     * @param httpDate the httpDate to set
     */
    public final void setHttpDate(Date httpDate) {
        this.httpDate = httpDate;
    }

    /**
     * @return the httpLastModified
     */
    public final Date getHttpLastModified() {
        return httpLastModified;
    }

    /**
     * @param httpLastModified the httpLastModified to set
     */
    public final void setHttpLastModified(Date httpLastModified) {
        this.httpLastModified = httpLastModified;
    }

    /**
     * @return the httpExpires
     */
    public final Date getHttpExpires() {
        return httpExpires;
    }

    /**
     * @param httpExpires the httpExpires to set
     */
    public final void setHttpExpires(Date httpExpires) {
        this.httpExpires = httpExpires;
    }

    /**
     * @return the httpTTL
     */
    public final Long getHttpTTL() {
        return httpTTL;
    }

    /**
     * @param httpTTL the httpTTL to set
     */
    public final void setHttpTTL(Long httpTTL) {
        this.httpTTL = httpTTL;
    }

    /**
     * @return the newestItemTimestamp
     */
    public final Date getNewestItemTimestamp() {
        return newestItemTimestamp;
    }

    /**
     * @param newestItemTimestamp the newestItemTimestamp to set
     */
    public final void setNewestItemTimestamp(Date newestItemTimestamp) {
        this.newestItemTimestamp = newestItemTimestamp;
    }

    /**
     * @return the numberNewItems
     */
    public final Integer getNumberNewItems() {
        return numberNewItems;
    }

    /**
     * @param numberNewItems the numberNewItems to set
     */
    public final void setNumberNewItems(Integer numberNewItems) {
        this.numberNewItems = numberNewItems;
    }

    /**
     * @return the windowSize
     */
    public final Integer getWindowSize() {
        return windowSize;
    }

    /**
     * @param windowSize the windowSize to set
     */
    public final void setWindowSize(Integer windowSize) {
        this.windowSize = windowSize;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + feedID;
        result = prime * result + ((httpDate == null) ? 0 : httpDate.hashCode());
        result = prime * result + ((httpETag == null) ? 0 : httpETag.hashCode());
        result = prime * result + ((httpExpires == null) ? 0 : httpExpires.hashCode());
        result = prime * result + ((httpLastModified == null) ? 0 : httpLastModified.hashCode());
        result = prime * result + ((httpTTL == null) ? 0 : httpTTL.hashCode());
        result = prime * result + ((newestItemTimestamp == null) ? 0 : newestItemTimestamp.hashCode());
        result = prime * result + ((numberNewItems == null) ? 0 : numberNewItems.hashCode());
        result = prime * result + ((pollTimestamp == null) ? 0 : pollTimestamp.hashCode());
        result = prime * result + ((windowSize == null) ? 0 : windowSize.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PollMetaInformation other = (PollMetaInformation) obj;
        if (feedID != other.feedID)
            return false;
        if (httpDate == null) {
            if (other.httpDate != null)
                return false;
        } else if (!httpDate.equals(other.httpDate))
            return false;
        if (httpETag == null) {
            if (other.httpETag != null)
                return false;
        } else if (!httpETag.equals(other.httpETag))
            return false;
        if (httpExpires == null) {
            if (other.httpExpires != null)
                return false;
        } else if (!httpExpires.equals(other.httpExpires))
            return false;
        if (httpLastModified == null) {
            if (other.httpLastModified != null)
                return false;
        } else if (!httpLastModified.equals(other.httpLastModified))
            return false;
        if (httpTTL == null) {
            if (other.httpTTL != null)
                return false;
        } else if (!httpTTL.equals(other.httpTTL))
            return false;
        if (newestItemTimestamp == null) {
            if (other.newestItemTimestamp != null)
                return false;
        } else if (!newestItemTimestamp.equals(other.newestItemTimestamp))
            return false;
        if (numberNewItems == null) {
            if (other.numberNewItems != null)
                return false;
        } else if (!numberNewItems.equals(other.numberNewItems))
            return false;
        if (pollTimestamp == null) {
            if (other.pollTimestamp != null)
                return false;
        } else if (!pollTimestamp.equals(other.pollTimestamp))
            return false;
        if (windowSize == null) {
            if (other.windowSize != null)
                return false;
        } else if (!windowSize.equals(other.windowSize))
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "PollMetaInformation [feedID=" + feedID + ", pollTimestamp=" + pollTimestamp + ", httpETag=" + httpETag
                + ", httpDate=" + httpDate + ", httpLastModified=" + httpLastModified + ", httpExpires=" + httpExpires
                + ", httpTTL=" + httpTTL + ", newestItemTimestamp=" + newestItemTimestamp + ", numberNewItems="
                + numberNewItems + ", windowSize=" + windowSize + "]";
    }

}
