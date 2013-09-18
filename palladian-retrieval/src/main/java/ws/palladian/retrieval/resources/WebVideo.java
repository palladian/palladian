package ws.palladian.retrieval.resources;

public interface WebVideo extends WebContent {
	
	String getVideoUrl();
	
	String getThumbnailUrl();
	
	Long getDuration();

}
