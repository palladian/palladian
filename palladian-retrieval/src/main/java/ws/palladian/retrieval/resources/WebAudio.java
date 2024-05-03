package ws.palladian.retrieval.resources;

public interface WebAudio extends WebContent {

    /**
     * @return The URL linking to the video.
     */
    String getAudioUrl();

    /**
     * @return The runtime of this video seconds, or <code>null</code> if no runtime was specified.
     */
    Integer getDuration();

}
