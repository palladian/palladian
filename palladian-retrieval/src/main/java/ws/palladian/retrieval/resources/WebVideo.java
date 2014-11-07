package ws.palladian.retrieval.resources;

public interface WebVideo extends WebContent {

    /**
     * @return The URL linking to the video.
     */
    String getVideoUrl();

    /**
     * @return The URL to a thumbnail image for the video.
     */
    String getThumbnailUrl();

    /**
     * @return The runtime of this video seconds, or <code>null</code> if no runtime was specified.
     */
    Long getDuration();

    /**
     * @return The number of views of this video, or <code>null</code> in case no view count was specified.
     */
    Integer getViews();

    /**
     * @return The rating of this video in a range [0, 1], where higher values denote a better rating, or
     *         <code>null</code> in case no rating was specified.
     */
    Double getRating();

}
