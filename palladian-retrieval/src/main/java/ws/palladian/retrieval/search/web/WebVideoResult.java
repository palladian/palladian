package ws.palladian.retrieval.search.web;

import java.util.Date;

/**
 * <p>
 * A {@link WebResult} representing video links.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class WebVideoResult extends WebResult {

    private final String videoUrl;
    private final Long runTime;
    private Integer views;
    private Double rating;

    /**
     * <p>
     * Instantiate a new {@link WebVideoResult}.
     * </p>
     * 
     * @param url The URL linking to the page containing the video.
     * @param videoUrl The URL linking to the video file.
     * @param title The title of the video.
     * @param runTime The run time of the video in seconds.
     */
    public WebVideoResult(String url, String videoUrl, String title, Long runTime, Date date) {
        super(url, title, null, date);
        this.videoUrl = videoUrl;
        this.runTime = runTime;

    }

    /**
     * <p>
     * Get the URL linking to the video.
     * </p>
     * 
     * @return The URL linking directly to the video.
     */
    public String getVideoUrl() {
        return videoUrl;
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

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WebVideoResult [videoUrl=");
        builder.append(videoUrl);
        builder.append(", runTime=");
        builder.append(runTime);
        builder.append(", views=");
        builder.append(views);
        builder.append(", rating=");
        builder.append(rating);
        builder.append(", getUrl()=");
        builder.append(getUrl());
        builder.append(", getTitle()=");
        builder.append(getTitle());
        builder.append(", getSummary()=");
        builder.append(getSummary());
        builder.append(", getDate()=");
        builder.append(getDate());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (int)(runTime ^ (runTime >>> 32));
        return result;
    }

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
