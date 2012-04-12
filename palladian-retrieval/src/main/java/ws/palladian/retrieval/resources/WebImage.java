package ws.palladian.retrieval.resources;

import ws.palladian.helper.io.FileHelper;

/**
 * <p>An extracted image from a Web page.</p>
 * 
 * @author David Urbansky
 */
public class WebImage {

    private String url = "";
    private int width = 0;
    private int height = 0;
    private String alt = "";
    private String title = "";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
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

    public int getSize() {
        return getWidth() * getHeight();
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return FileHelper.getFileType(getUrl());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WebImage [url=");
        builder.append(url);
        builder.append(", width=");
        builder.append(width);
        builder.append(", height=");
        builder.append(height);
        builder.append(", alt=");
        builder.append(alt);
        builder.append(", title=");
        builder.append(title);
        builder.append(", type=");
        builder.append(getType());
        builder.append("]\n");
        return builder.toString();
    }

}
