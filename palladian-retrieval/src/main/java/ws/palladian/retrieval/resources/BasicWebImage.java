package ws.palladian.retrieval.resources;

import java.util.Date;

import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.images.ImageType;

/**
 * <p>
 * {@link WebImageResult}s represent search results from image searches on web search engines.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class BasicWebImage extends BasicWebContent implements WebImage {

    private final String imageUrl;
    private String thumbImageUrl;
    private final int width;
    private final int height;
    private License license = License.UNKNOWN;
    private String licenseLink = "";
    private ImageType imageType = ImageType.UNKNOWN;
    private String fileType = null;

    /**
     * <p>
     * Create a new {@link WebImageResult}
     * </p>
     * 
     * @param url The URL to the result. This should usually point to an HTML page on which the image is situated.
     * @param imageUrl The URL to the image. This should usually point directly to the image file (e.g. JPEG, PNG, etc.)
     * @param title
     * @param summary
     * @param width
     * @param height
     * @param date
     * @param imageContent
     */
    public BasicWebImage(String url, String imageUrl, String title, String summary, int width, int height, Date date) {
        super(url, title, summary, date);
        this.imageUrl = imageUrl;
        this.width = width;
        this.height = height;
    }

    protected BasicWebImage(WebImage webImage) {
        super(webImage);
        this.imageUrl = webImage.getImageUrl();
        this.width = webImage.getWidth();
        this.height = webImage.getHeight();
        this.license = webImage.getLicense();
        this.licenseLink = webImage.getLicenseLink();
        this.imageType = webImage.getImageType();
    }

    /**
     * @return The width of the image.
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * @return The height of the image.
     */
    @Override
    public int getHeight() {
        return height;
    }
    
    @Override
    public int getSize() {
    	return width * height;
    }

    /**
     * @return The URL of the image. In contrast to {@link #getUrl()}, which links to a (HTML) page surrounding the
     *         actual image, this URL points directly to the image file.
     */
    @Override
	public String getImageUrl() {
        return imageUrl;
    }
    
    @Override
    public String getThumbnailUrl() {
    	return thumbImageUrl;
    }

    public void setThumbnailUrl(String thumbImageUrl) {
        this.thumbImageUrl = thumbImageUrl;
    }

    public double getWidthHeightRatio() {
        return (double)getWidth() / (double)getHeight();
    }

    @Override
	public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    @Override
	public ImageType getImageType() {
        return imageType;
    }

    public void setImageType(ImageType imageType) {
        this.imageType = imageType;
    }

    @Override
	public String getLicenseLink() {
        return licenseLink;
    }

    public void setLicenseLink(String licenseLink) {
        this.licenseLink = licenseLink;
    }
    
    @Override
    public String getFileType() {
    	return fileType;
    }
    
    public void setFileType(String fileType) {
		this.fileType = fileType;
	}

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BasicWebImage [width=");
        builder.append(width);
        builder.append(", height=");
        builder.append(height);
        builder.append(", url=");
        builder.append(getUrl());
        builder.append(", imageUrl=");
        builder.append(getImageUrl());
        builder.append(", title=");
        builder.append(getTitle());
        builder.append(", summary=");
        builder.append(getSummary());
        builder.append(", date=");
        builder.append(getPublished());
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
        result = prime * result + height;
        result = prime * result + width;
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
        BasicWebImage other = (BasicWebImage)obj;
        if (height != other.height)
            return false;
        if (width != other.width)
            return false;
        return true;
    }

}
