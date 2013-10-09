package ws.palladian.retrieval.resources;

import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.images.ImageType;

/**
 * <p>
 * {@link BasicWebImage}s represent search results from image searches on web search engines. For instantiation use the
 * {@link Builder}.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class BasicWebImage extends BasicWebContent implements WebImage {

    /**
     * <p>
     * Builder for creating new instances of {@link WebImage}.
     * </p>
     * 
     * @author katz
     */
    public static class Builder extends BasicWebContent.Builder {

        protected String imageUrl;
        protected String thumbnailUrl;
        protected int width;
        protected int height;
        protected License license = License.UNKNOWN;
        protected String licenseLink = "";
        protected ImageType imageType = ImageType.UNKNOWN;
        protected String fileType;

        public Builder setImageType(ImageType imageType) {
            this.imageType = imageType;
            return this;
        }

        public Builder setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return this;

        }

        public Builder setWidth(int width) {
            this.width = width;
            return this;

        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;

        }

        public Builder setLicense(License license) {
            this.license = license;
            return this;

        }

        public Builder setLicenseLink(String licenseLink) {
            this.licenseLink = licenseLink;
            return this;

        }

        public Builder setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;

        }

        public Builder setFileType(String fileType) {
            this.fileType = fileType;
            return this;
        }

        public Builder setWebImage(WebImage webImage) {
            super.setWebContent(webImage);
            this.imageUrl = webImage.getImageUrl();
            this.thumbnailUrl = webImage.getThumbnailUrl();
            this.width = webImage.getWidth();
            this.height = webImage.getHeight();
            this.license = webImage.getLicense();
            this.licenseLink = webImage.getLicenseLink();
            this.imageType = webImage.getImageType();
            this.fileType = webImage.getFileType();
            return this;
        }

        @Override
        public WebImage create() {
            return new BasicWebImage(this);
        }

    }

    private String imageUrl;
    private String thumbnailUrl;
    private int width;
    private int height;
    private License license;
    private String licenseLink;
    private ImageType imageType;
    private String fileType;

    protected BasicWebImage(WebImage webImage) {
        super(webImage);
        this.imageUrl = webImage.getImageUrl();
        this.thumbnailUrl = webImage.getThumbnailUrl();
        this.width = webImage.getWidth();
        this.height = webImage.getHeight();
        this.license = webImage.getLicense();
        this.licenseLink = webImage.getLicenseLink();
        this.imageType = webImage.getImageType();
        this.fileType = webImage.getFileType();
    }

    private BasicWebImage(Builder builder) {
        super(builder);
        this.imageUrl = builder.imageUrl;
        this.thumbnailUrl = builder.thumbnailUrl;
        this.width = builder.width;
        this.height = builder.height;
        this.license = builder.license;
        this.licenseLink = builder.licenseLink;
        this.imageType = builder.imageType;
        this.fileType = builder.fileType;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getSize() {
        return width * height;
    }

    @Override
    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @Override
    public License getLicense() {
        return license;
    }

    @Override
    public ImageType getImageType() {
        return imageType;
    }

    @Override
    public String getLicenseLink() {
        return licenseLink;
    }

    @Override
    public String getFileType() {
        return fileType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BasicWebImage [imageUrl=");
        builder.append(imageUrl);
        builder.append(", thumbnailUrl=");
        builder.append(thumbnailUrl);
        builder.append(", width=");
        builder.append(width);
        builder.append(", height=");
        builder.append(height);
        builder.append(", license=");
        builder.append(license);
        builder.append(", licenseLink=");
        builder.append(licenseLink);
        builder.append(", imageType=");
        builder.append(imageType);
        builder.append(", fileType=");
        builder.append(fileType);
        builder.append(", getUrl()=");
        builder.append(getUrl());
        builder.append(", getTitle()=");
        builder.append(getTitle());
        builder.append(", getSummary()=");
        builder.append(getSummary());
        builder.append(", getPublished()=");
        builder.append(getPublished());
        builder.append(", getCoordinate()=");
        builder.append(getCoordinate());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((fileType == null) ? 0 : fileType.hashCode());
        result = prime * result + height;
        result = prime * result + ((imageType == null) ? 0 : imageType.hashCode());
        result = prime * result + ((imageUrl == null) ? 0 : imageUrl.hashCode());
        result = prime * result + ((license == null) ? 0 : license.hashCode());
        result = prime * result + ((licenseLink == null) ? 0 : licenseLink.hashCode());
        result = prime * result + ((thumbnailUrl == null) ? 0 : thumbnailUrl.hashCode());
        result = prime * result + width;
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
        BasicWebImage other = (BasicWebImage)obj;
        if (fileType == null) {
            if (other.fileType != null)
                return false;
        } else if (!fileType.equals(other.fileType))
            return false;
        if (height != other.height)
            return false;
        if (imageType != other.imageType)
            return false;
        if (imageUrl == null) {
            if (other.imageUrl != null)
                return false;
        } else if (!imageUrl.equals(other.imageUrl))
            return false;
        if (license != other.license)
            return false;
        if (licenseLink == null) {
            if (other.licenseLink != null)
                return false;
        } else if (!licenseLink.equals(other.licenseLink))
            return false;
        if (thumbnailUrl == null) {
            if (other.thumbnailUrl != null)
                return false;
        } else if (!thumbnailUrl.equals(other.thumbnailUrl))
            return false;
        if (width != other.width)
            return false;
        return true;
    }

}
