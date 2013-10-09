package ws.palladian.retrieval.resources;

import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.images.ImageType;

public interface WebImage extends WebContent {
	
    /**
     * @return The URL of this image. In contrast to {@link #getUrl()}, which usually links to the page on which this
     *         image was found, this URL points to the actual image file.
     */
	String getImageUrl();
	
	String getThumbnailUrl();

	/**
	 * @return The width of this image in pixels.
	 */
	int getWidth();
	
	/**
	 * @return The height of this image in pixels.
	 */
	int getHeight();
	
	/**
	 * @return The total number of pixels in this image.
	 */
	int getSize();
	
	License getLicense();
	
	String getLicenseLink();
	
	ImageType getImageType();
	
	String getFileType();

}
