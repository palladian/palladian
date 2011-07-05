package ws.palladian.daterecognition;

import java.util.ArrayList;
import java.util.Collection;

import org.w3c.dom.Document;

import ws.palladian.daterecognition.dates.AbstractDate;
import ws.palladian.daterecognition.dates.MetaDate;
import ws.palladian.daterecognition.technique.ArchiveDateGetter;
import ws.palladian.daterecognition.technique.ContentDateGetter;
import ws.palladian.daterecognition.technique.HTTPDateGetter;
import ws.palladian.daterecognition.technique.HeadDateGetter;
import ws.palladian.daterecognition.technique.MetaDateGetter;
import ws.palladian.daterecognition.technique.ReferenceDateGetter;
import ws.palladian.daterecognition.technique.StructureDateGetter;
import ws.palladian.daterecognition.technique.URLDateGetter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * This class is responsible for rating dates. <br>
 * Therefore it coordinates each technique-rater-class. <br>
 * 
 * @author Martin Gregor (mail@m-gregor.de)
 * @param <T>
 * 
 * 
 */
public class DateGetter {

    private boolean tech_URL = true;
    private boolean tech_Meta = true;
    private boolean tech_HTML_struct = true;
    private boolean tech_HTML_content = true;
    private boolean tech_reference = true;
    private boolean tech_archive = true;

    private MetaDateGetter mdg = new MetaDateGetter();
    private URLDateGetter udg = new URLDateGetter();
    private ContentDateGetter cdg = new ContentDateGetter();
    private StructureDateGetter sdg = new StructureDateGetter();
    private ArchiveDateGetter adg = new ArchiveDateGetter();
    private ReferenceDateGetter rdg = new ReferenceDateGetter();

    private ArrayList<MetaDate> httpDates;
    private boolean externHttpDates = false;
    public void setHttpDates(ArrayList<MetaDate> httpDates){
    	this.httpDates = httpDates; 
    	externHttpDates = true;
    }
    
    /** URL that will be called */
    private String url;

    private Document document;

    public DateGetter() {
        super();
    }

    /**
     * Constructor creates a new DateGetter with a given URL.
     * 
     * @param url URL that will be analyzed
     */
    public DateGetter(final String url) {
        this.url = url;
    }

    /**
     * Constructor creates a new DateGetter with a given document.
     * 
     * @param url URL that will be analyzed
     */
    public DateGetter(final Document document) {
        this.document = document;
    }

    /**
     * Constructor creates a new DateGetter with a given URL and document.
     * 
     * @param url URL that will be analyzed
     */
    public DateGetter(final String url, Document document) {
        this.url = url;
        this.document = document;
    }

    /**
     * Analyzes a webpage by different techniques to find dates. The techniques are found in DateGetterHelper. <br>
     * Type of the found dates are ExtractedDate.
     * 
     * @param <T>
     * 
     * @return A array of ExtractedDates.
     */
    @SuppressWarnings("unchecked")
	public <T> ArrayList<T> getDate() {

        ArrayList<T> dates = new ArrayList<T>();
        DocumentRetriever crawler = new DocumentRetriever();

        if (url != null) {
           if (tech_URL) {
                udg.setUrl(url);
                dates.addAll((Collection<? extends T>) udg.getDates());
            }
            if (tech_archive) {
                adg.setUrl(url);
                dates.addAll((Collection<? extends T>) adg.getDates());
            }
            if (tech_Meta || tech_HTML_struct || tech_HTML_content || tech_reference) {
                Document document = this.document;
                if (document == null) {
                    document = crawler.getWebDocument(url);
                }
                if (document != null) {
                    if (tech_Meta) {
                    	mdg.setLookHttpDates(!externHttpDates);
                    	if(externHttpDates){
                    		dates.addAll((Collection<? extends T>) httpDates);
                    	}
                    	mdg.setUrl(url);
                        mdg.setDocument(document);
                        dates.addAll((Collection<? extends T>) mdg.getDates());
                    }
                    if (tech_HTML_struct) {
                        sdg.setDocument(document);
                        dates.addAll((Collection<? extends T>) sdg.getDates());
                    }

                    if (tech_HTML_content) {
                        cdg.setDocument(document);
                        dates.addAll((Collection<? extends T>) cdg.getDates());
                    }
                    if (tech_reference) {
                        rdg.setDocument(document);
                        dates.addAll((Collection<? extends T>) rdg.getDates());
                    }
                }
            }
        }
        dates = CollectionHelper.removeNullElements(dates);
        return dates;

    }

    /**
     * Getter for global variable URL.
     * 
     * @return URL.
     */
    public String getURL() {
        return this.url;
    }

    /**
     * Setter for global variable URL.
     * 
     * @return URL.
     */
    public void setURL(final String url) {
        this.url = url;
    }

   /**
     * Activate or disable url-technique.
     * 
     * @param value
     */
    public void setTechURL(boolean value) {
        tech_URL = value;
    }

    /**
     * Activate or disable Meta-technique.
     * 
     * @param value
     */
    public void setTechMeta(boolean value) {
        tech_Meta = value;
    }

    /**
     * Activate or disable HTLM-structure-technique.
     * 
     * @param value
     */
    public void setTechHTMLStruct(boolean value) {
        tech_HTML_struct = value;
    }

    /**
     * Activate or disable HTML-content-technique.
     * 
     * @param value
     */
    public void setTechHTMLContent(boolean value) {
        tech_HTML_content = value;
    }

    /**
     * Activate or disable reference-technique.
     * 
     * @param value
     */
    public void setTechReference(boolean value) {
        tech_reference = value;
    }

    /**
     * Activate or disable archive-technique.
     * 
     * @param value
     */
    public void setTechArchive(boolean value) {
        tech_archive = value;
    }

    /**
     * Disable all techniques.
     */
    public void setAllFalse() {
        tech_Meta = false;
        tech_URL = false;
        tech_HTML_struct = false;
        tech_HTML_content = false;
        tech_reference = false;
        tech_archive = false;
    }

    /**
     * Activate all techniques.
     */
    public void setAllTrue() {
        tech_Meta = true;
        tech_URL = true;
        tech_HTML_struct = true;
        tech_HTML_content = true;
        tech_reference = true;
        tech_archive = true;
    }
    
    public void setDocument(Document document){
    	this.document = document;
    }
    public Document getDocument(){
    	return this.document;
    }

}
