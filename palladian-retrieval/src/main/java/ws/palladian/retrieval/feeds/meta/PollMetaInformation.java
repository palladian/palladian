package ws.palladian.retrieval.feeds.meta;

import java.sql.Timestamp;
import java.util.Date;

import ws.palladian.helper.date.DateHelper;

/**
 * <p>Store meta information for a single poll.</p>
 * 
 * @author Sandro Reichert
 * 
 */
public class PollMetaInformation {

    private int feedID = -1;

    private Date pollTimestamp = null;

    private String httpETag = null;

    private Date httpDate = null;

    private Date httpLastModified = null;

    private Date httpExpires = null;

    private Date newestItemTimestamp = null;

    private Integer numberNewItems = null;

    private Integer windowSize = null;

    private int httpStatusCode = -1;

    /**
     * The size in bytes of the received HTTP response. Null if unknown.
     */
    private Integer responseSize = null;

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

    public Timestamp getPollSQLTimestamp() {
        if (pollTimestamp != null) {
            return new Timestamp(pollTimestamp.getTime());
        }
        return null;
    }

    /**
     * If date's year is > 9999, we set it to null!
     * 
     * @param pollTimestamp the pollTimestamp to set
     */
    public final void setPollTimestamp(Date pollTimestamp) {
        this.pollTimestamp = DateHelper.validateYear(pollTimestamp);
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

    public Timestamp getHttpDateSQLTimestamp() {
        if (httpDate != null) {
            return new Timestamp(httpDate.getTime());
        }
        return null;
    }

    /**
     * If date's year is > 9999, we set it to null!
     * 
     * @param httpDate the httpDate to set
     */
    public final void setHttpDate(Date httpDate) {
        this.httpDate = DateHelper.validateYear(httpDate);
    }

    /**
     * @return the httpLastModified
     */
    public final Date getHttpLastModified() {
        return httpLastModified;
    }

    /**
     * @return httpLastModified as sql time stamp
     */
    public Timestamp getHttpLastModifiedSQLTimestamp() {
        if (httpLastModified != null) {
            return new Timestamp(httpLastModified.getTime());
        }
        return null;
    }

    /**
     * If date's year is > 9999, we set it to null!
     * 
     * @param httpLastModified the httpLastModified to set
     */
    public final void setHttpLastModified(Date httpLastModified) {
        this.httpLastModified = DateHelper.validateYear(httpLastModified);
    }

    /**
     * @return the httpExpires
     */
    public final Date getHttpExpires() {
        return httpExpires;
    }

    /**
     * @return httpExpires as sql time stamp
     */
    public Timestamp getHttpExpiresSQLTimestamp() {
        if (httpExpires != null) {
            return new Timestamp(httpExpires.getTime());
        }
        return null;
    }

    /**
     * If date's year is > 9999, we set it to null!
     * 
     * @param httpExpires the httpExpires to set
     */
    public final void setHttpExpires(Date httpExpires) {
        this.httpExpires = DateHelper.validateYear(httpExpires);
    }


    /**
     * @return the newestItemTimestamp
     */
    public final Date getNewestItemTimestamp() {
        return newestItemTimestamp;
    }

    /**
     * @return newestItemTimestamp as sql time stamp
     */
    public Timestamp getNewestItemSQLTimestamp() {
        if (newestItemTimestamp != null) {
            return new Timestamp(newestItemTimestamp.getTime());
        }
        return null;
    }

    /**
     * If date's year is > 9999, we set it to null!
     * 
     * @param newestItemTimestamp the newestItemTimestamp to set
     */
    public final void setNewestItemTimestamp(Date newestItemTimestamp) {
        this.newestItemTimestamp = DateHelper.validateYear(newestItemTimestamp);
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

    /**
     * @return the httpStatusCode
     */
    public final int getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * @param httpStatusCode the httpStatusCode to set
     */
    public final void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * @return The size in bytes of the received HTTP response.
     */
    public final Integer getResponseSize() {
        return responseSize;
    }

    /**
     * @param responseSize The size in bytes of the received HTTP response.
     */
    public final void setResponseSize(Integer responseSize) {
        this.responseSize = responseSize;
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
        result = prime * result + httpStatusCode;
        result = prime * result + ((newestItemTimestamp == null) ? 0 : newestItemTimestamp.hashCode());
        result = prime * result + ((numberNewItems == null) ? 0 : numberNewItems.hashCode());
        result = prime * result + ((pollTimestamp == null) ? 0 : pollTimestamp.hashCode());
        result = prime * result + ((responseSize == null) ? 0 : responseSize.hashCode());
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
        if (httpStatusCode != other.httpStatusCode)
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
        if (responseSize == null) {
            if (other.responseSize != null)
                return false;
        } else if (!responseSize.equals(other.responseSize))
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
        StringBuilder builder = new StringBuilder();
        builder.append("PollMetaInformation [feedID=");
        builder.append(feedID);
        builder.append(", pollTimestamp=");
        builder.append(pollTimestamp);
        builder.append(", httpETag=");
        builder.append(httpETag);
        builder.append(", httpDate=");
        builder.append(httpDate);
        builder.append(", httpLastModified=");
        builder.append(httpLastModified);
        builder.append(", httpExpires=");
        builder.append(httpExpires);
        builder.append(", newestItemTimestamp=");
        builder.append(newestItemTimestamp);
        builder.append(", numberNewItems=");
        builder.append(numberNewItems);
        builder.append(", windowSize=");
        builder.append(windowSize);
        builder.append(", httpStatusCode=");
        builder.append(httpStatusCode);
        builder.append(", responseSize=");
        builder.append(responseSize);
        builder.append("]");
        return builder.toString();
    }

}
