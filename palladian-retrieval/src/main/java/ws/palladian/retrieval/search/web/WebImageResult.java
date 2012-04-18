package ws.palladian.retrieval.search.web;

import java.awt.image.BufferedImage;
import java.util.Date;

/**
 * <p>
 * {@link WebImageResult}s represent search results from image searches on web search engines.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class WebImageResult extends WebResult {

    private final int width;
    private final int height;
    private BufferedImage imageContent = null;

    public WebImageResult(String url, String title, String summary, int width, int height, Date date,
            BufferedImage imageContent) {
        super(url, title, summary, date);
        this.width = width;
        this.height = height;
        this.imageContent = imageContent;
    }

    public WebImageResult(String url, String title, String summary, int width, int height) {
        this(url, title, summary, width, height, null, null);
    }

    public WebImageResult(String url, String title, int width, int height) {
        this(url, title, null, width, height, null, null);
    }

    public WebImageResult(String url, String title, int width, int height, BufferedImage imageContent) {
        this(url, title, null, width, height, null, imageContent);
    }

    /**
     * @return The width of the image.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return The height of the image.
     */
    public int getHeight() {
        return height;
    }

    public double getWidthHeightRatio() {
        return (double)getWidth() / (double)getHeight();
    }

    public BufferedImage getImageContent() {
        return imageContent;
    }

    public void setImageContent(BufferedImage imageContent) {
        this.imageContent = imageContent;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WebImageResult [width=");
        builder.append(width);
        builder.append(", height=");
        builder.append(height);
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
        WebImageResult other = (WebImageResult) obj;
        if (height != other.height)
            return false;
        if (width != other.width)
            return false;
        return true;
    }

}
