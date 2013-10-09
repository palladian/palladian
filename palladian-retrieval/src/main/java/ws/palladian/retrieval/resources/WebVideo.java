package ws.palladian.retrieval.resources;

public interface WebVideo extends WebContent {

    /**
     * @return The URL linking to the video.
     */
    String getVideoUrl();

    String getThumbnailUrl();

    /**
     * @return The runtime of this video seconds, or <code>null</code> if no runtime was specified.
     */
    Long getDuration();
    
    Integer getViews();
    
    Double getRating();

}
