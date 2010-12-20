package tud.iir.extraction.qa;

import java.io.Serializable;

/**
 * A question answer URL with its crawling classification.
 * 
 * @author David Urbansky
 * 
 */
public class QAUrl implements Serializable {

    private static final long serialVersionUID = -1702458137622101290L;

    public static String UKNOWN = "UNKNOWN TYPE";
    public static String GREEN = "GREEN";
    public static String YELLOW = "YELLOW";
    public static String NON_RED = "NON-RED";

    private String url = "";
    private String parentURL = "";
    private String type = UKNOWN;

    public QAUrl(String url, String parentURL) {
        setUrl(url);
        setParentURL(parentURL);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getParentURL() {
        return parentURL;
    }

    public void setParentURL(String parentURL) {
        this.parentURL = parentURL;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}