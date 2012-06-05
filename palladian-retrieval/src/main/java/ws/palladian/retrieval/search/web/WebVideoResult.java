package ws.palladian.retrieval.search.web;

import java.util.Date;

/**
 * <p>
 * A {@link WebResult} representing video links.
 * </p>
 * 
 * @author Philipp Katz
 */
public class WebVideoResult extends WebResult {

    private final Long runTime;

    /**
     * <p>
     * Instantiate a new {@link WebVideoResult}.
     * </p>
     * 
     * @param url The URL linking to the video.
     * @param title The title of the video.
     * @param runTime The run time of the video in seconds.
     */
    public WebVideoResult(String url, String title, Long runTime) {
        super(url, title);
        this.runTime = runTime;
    }

    /**
     * <p>
     * Instantiate a new {@link WebVideoResult}.
     * </p>
     * 
     * @param url The URL linking to the video.
     * @param title The title of the video.
     * @param date The publish date of the video.
     */
    public WebVideoResult(String url, String title, Date date) {
        super(url, title, null, date);
        this.runTime = null;
    }

    /**
     * <p>
     * Get the run time of the video in seconds.
     * </p>
     * 
     * @return the runTime The run time of the video, or <code>null</code> if no run time specified.
     */
    public Long getRunTime() {
        return runTime;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WebVideoResult [runTime=");
        builder.append(runTime);
        builder.append(", getUrl()=");
        builder.append(getUrl());
        builder.append(", getTitle()=");
        builder.append(getTitle());
        builder.append("]");
        return builder.toString();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (int)(runTime ^ (runTime >>> 32));
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
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        WebVideoResult other = (WebVideoResult)obj;
        if (runTime != other.runTime)
            return false;
        return true;
    }

}
