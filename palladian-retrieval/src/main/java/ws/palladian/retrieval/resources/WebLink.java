package ws.palladian.retrieval.resources;

/**
 * <p>An extracted link from a Web page.</p>
 * 
 * @author David Urbansky
 */
public class WebLink {

    private String url = "";
    private String text = "";
    private String title = "";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WebLink [url=");
        builder.append(url);
        builder.append(", text=");
        builder.append(text);
        builder.append(", title=");
        builder.append(title);
        builder.append("]\n");
        return builder.toString();
    }

}
