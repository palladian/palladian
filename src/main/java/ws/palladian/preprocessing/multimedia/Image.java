package ws.palladian.preprocessing.multimedia;

import java.awt.image.BufferedImage;

/**
 * An image.
 * 
 * @author David Urbansky
 */
public class Image {
    private String url = "";
    private BufferedImage imageContent = null;
    private int width = -1;
    private int height = -1;

    public Image() {
    }

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public double getWidthHeightRatio() {
        return (double) getWidth() / (double) getHeight();
    }

    public BufferedImage getImageContent() {
        return imageContent;
    }

    public void setImageContent(BufferedImage imageContent) {
        this.imageContent = imageContent;
    }
}