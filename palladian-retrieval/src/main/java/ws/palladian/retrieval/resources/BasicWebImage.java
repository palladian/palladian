package ws.palladian.retrieval.resources;

import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.images.ImageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@link BasicWebImage}s represent search results from image searches on web search engines. For instantiation use the
 * {@link Builder}.
 *
 * @author Philipp Katz
 * @author David Urbansky
 */
public class BasicWebImage extends BasicWebContent implements WebImage {
    /**
     * Builder for creating new instances of {@link WebImage}.
     *
     * @author Philipp Katz
     */
    public static class Builder extends BasicWebContent.Builder {
        protected String imageUrl;
        protected String thumbnailUrl;
        protected int width = -1;
        protected int height = -1;
        protected License license = License.UNKNOWN;
        protected String licenseLink;
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

        public Builder setSize(int width, int height) {
            this.width = width;
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

    protected BasicWebImage(Builder builder) {
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
        if (width < 0 || height < 0) {
            return -1;
        }
        return Math.abs(width) * Math.abs(height);
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
    protected List<String> getToStringParts() {
        List<String> toStringParts = new ArrayList<>(super.getToStringParts());
        if (imageUrl != null) {
            toStringParts.add(String.format("imageUrl=%s", imageUrl));
        }
        if (thumbnailUrl != null) {
            toStringParts.add(String.format("thumbnailUrl=%s", thumbnailUrl));
        }
        if (width != -1) {
            toStringParts.add(String.format("width=%s", width));
        }
        if (height != -1) {
            toStringParts.add(String.format("height=%s", height));
        }
        if (license != null && license != License.UNKNOWN) {
            toStringParts.add(String.format("license=%s", license));
        }
        if (licenseLink != null) {
            toStringParts.add(String.format("licenseLink=%s", licenseLink));
        }
        if (imageType != null && imageType != ImageType.UNKNOWN) {
            toStringParts.add(String.format("imageType=%s", imageType));
        }
        if (fileType != null) {
            toStringParts.add(String.format("fileType=%s", fileType));
        }
        return toStringParts;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(fileType, height, imageType, imageUrl, license, licenseLink, thumbnailUrl, width);
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
        BasicWebImage other = (BasicWebImage) obj;
        return Objects.equals(fileType, other.fileType) && height == other.height && imageType == other.imageType && Objects.equals(imageUrl, other.imageUrl)
                && license == other.license && Objects.equals(licenseLink, other.licenseLink) && Objects.equals(thumbnailUrl, other.thumbnailUrl) && width == other.width;
    }
}