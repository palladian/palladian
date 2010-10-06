package tud.iir.daterecognition.technique;

import java.util.ArrayList;

import org.w3c.dom.Document;

/**
 * Template for getter techniques.<br>
 * Each technique, that found dates in or around webpages should implement this.
 * 
 * @author Martin Gregor
 * 
 * @param <T>
 */
public abstract class TechniqueDateGetter<T> {

    protected String url;
    protected Document document;

    /**
     * Returns a List of found dates, where type is depending of special technique.
     * 
     * @return
     */
    public abstract ArrayList<T> getDates();

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

}
