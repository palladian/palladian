package ws.palladian.retrieval.resources;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

/**
 * <p>A source from which an extraction was performed.</p>
 * 
 * @author David Urbansky
 */
public class Source implements Serializable {

    private static final long serialVersionUID = 958602024883353847L;

    private int id = -1;

    /** the URL of the source */
    private String url;

    /** how much can the source be trusted in giving correct information */
    private double trust = 0.5;

    public Source(String url) {
        setUrl(url);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url == null) {
            Logger.getRootLogger().error("source url was set with NULL value");
            new Error().printStackTrace();
            // System.exit(1);
        }
        this.url = url;
    }

    public double getTrust() {
        return trust;
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }


    /**
     * Get the top level domain (TLD) of the source URL.
     * 
     * @return The TLD of the source URL.
     */
    public String getTLD() {
        try {
            URL currentUrl = new URL(url);
            String host = currentUrl.getHost();
            return host.substring(host.lastIndexOf(".") + 1).toLowerCase();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        Source s = (Source) obj;
        if (s.getUrl().equalsIgnoreCase(getUrl())) {
            return true;
        }
        return false;
    }
    

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + (getUrl()==null ? 0 :getUrl().hashCode());
        return hash;

    }

    @Override
    public String toString() {
        return "Source:" + url;
    }

}