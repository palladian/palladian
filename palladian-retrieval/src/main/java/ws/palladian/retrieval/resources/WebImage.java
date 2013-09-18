package ws.palladian.retrieval.resources;

import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.images.ImageType;

public interface WebImage extends WebContent {
	
	String getImageUrl();
	
	String getThumbnailUrl();
	
	int getWidth();
	
	int getHeight();
	
	int getSize();
	
	License getLicense();
	
	String getLicenseLink();
	
	ImageType getImageType();
	
	String getFileType();

}
