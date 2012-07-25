package ws.palladian.extraction.date.getter;

import java.util.List;

import org.w3c.dom.Document;

import ws.palladian.helper.date.ExtractedDate;

/**
 * <p>
 * Template for getter techniques. Each technique, that found dates in or around webpages should implement this.
 * </p>
 * 
 * @author Martin Gregor
 * 
 * @param <T> subtype of {@link ExtractedDate} which concrete technique implementations extract.
 */
public abstract class TechniqueDateGetter<T extends ExtractedDate> {

    protected String url;
    protected Document document;

    /**
     * Returns a List of found dates, where type is depending of special technique.
     * 
     * @return
     */
    public abstract List<T> getDates();

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

//    public String getUrl() {
//        return this.url;
//    }

//    public Document getDocument() {
//        return this.document;
//    }

    public void reset() {
    }

}
