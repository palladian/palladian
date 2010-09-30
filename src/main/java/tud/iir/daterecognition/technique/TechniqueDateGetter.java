package tud.iir.daterecognition.technique;

import java.util.ArrayList;

import org.w3c.dom.Document;

public abstract class TechniqueDateGetter<T> {

    protected String url;
    protected Document document;

    /**
     * Returns a List of Dates, where type is depending of special technique.
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
