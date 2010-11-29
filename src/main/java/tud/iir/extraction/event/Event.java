package tud.iir.extraction.event;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tud.iir.classification.FeatureObject;
import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.knowledge.Extractable;
import tud.iir.web.WebResult;

/**
 * Event Object Class
 * 
 * @author Martin Wunderwald
 */
public class Event extends Extractable {

    private static final long serialVersionUID = 1023303092784471374L;

    /* holds the co-referenced Annotations and one featureObject each. */
    private Map<Annotations, FeatureObject> annotationFeatures;

    /* holds the simple annotations in text and headline. */
    private Annotations annotations;

    private String text;
    private String title;
    private String url;
    private List<WebResult> webresults;

    private String who;
    private String where;
    private String what;
    private String why;
    private String when;
    private String how;

    /** The who candidates. **/
    Map<String, Double> whoCandidates = new HashMap<String, Double>();

    /** The where Candidates. **/
    Map<String, Double> whereCandidates = new HashMap<String, Double>();

    /** The what Candidates. **/
    Map<String, Double> whatCandidates = new HashMap<String, Double>();

    /** The why Candidates. **/
    Map<String, Double> whyCandidates = new HashMap<String, Double>();

    /** The when Candidates. **/
    Map<String, Double> whenCandidates = new HashMap<String, Double>();

    /** The how Candidates. **/
    Map<String, Double> howCandidates = new HashMap<String, Double>();

    /**
     * Constructor.
     */
    public Event() {
        super();
        setExtractedAt(new Date(System.currentTimeMillis()));
    }

    /**
     * Constructor.
     * 
     * @param url
     */
    public Event(String url) {
        super();
        this.url = url;

        setExtractedAt(new Date(System.currentTimeMillis()));
    }

    /**
     * Constructor.
     * 
     * @param title
     * @param text
     */
    public Event(String title, String text) {
        this.text = text;
        this.title = title;
        setExtractedAt(new Date(System.currentTimeMillis()));
    }

    /**
     * Constructor.
     * 
     * @param title
     * @param text
     * @param url
     */
    public Event(String title, String text, String url) {
        this.text = text;
        this.title = title;
        this.url = url;

        setExtractedAt(new Date(System.currentTimeMillis()));
    }

    /**
     * @return the annotations
     */
    public Annotations getAnnotations() {
        return annotations;
    }

    /**
     * @param annotations
     *            the annotations to set
     */
    public void setAnnotations(Annotations annotations) {
        this.annotations = annotations;
    }

    /**
     * Getter for text.
     * 
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets text.
     * 
     * @param text
     *            the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title
     *            the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url
     *            the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the webresults
     */
    public List<WebResult> getWebresults() {
        return webresults;
    }

    /**
     * @param webresults
     *            the webresults to set
     */
    public void setWebresults(List<WebResult> webresults) {
        this.webresults = webresults;
    }

    /**
     * @return the annotationFeatures
     */
    public Map<Annotations, FeatureObject> getAnnotationFeatures() {
        return annotationFeatures;
    }

    /**
     * @param annotationFeatures
     *            the annotationFeatures to set
     */
    public void setAnnotationFeatures(
            Map<Annotations, FeatureObject> annotationFeatures) {
        this.annotationFeatures = annotationFeatures;
    }

    /**
     * @return the who
     */
    public String getWho() {
        return who;
    }

    /**
     * @param who
     *            the who to set
     */
    public void setWho(String who) {
        this.who = who;
    }

    /**
     * @return the where
     */
    public String getWhere() {
        return where;
    }

    /**
     * @param where
     *            the where to set
     */
    public void setWhere(String where) {
        this.where = where;
    }

    /**
     * @return the what
     */
    public String getWhat() {
        return what;
    }

    /**
     * @param what
     *            the what to set
     */
    public void setWhat(String what) {
        this.what = what;
    }

    /**
     * @return the why
     */
    public String getWhy() {
        return why;
    }

    /**
     * @param why
     *            the why to set
     */
    public void setWhy(String why) {
        this.why = why;
    }

    /**
     * @return the when
     */
    public String getWhen() {
        return when;
    }

    /**
     * @param when
     *            the when to set
     */
    public void setWhen(String when) {
        this.when = when;
    }

    /**
     * @return the how
     */
    public String getHow() {
        return how;
    }

    /**
     * @param how
     *            the how to set
     */
    public void setHow(String how) {
        this.how = how;
    }

    /**
     * @return the whoCandidates
     */
    public Map<String, Double> getWhoCandidates() {
        return whoCandidates;
    }

    /**
     * @param whoCandidates
     *            the whoCandidates to set
     */
    public void setWhoCandidates(Map<String, Double> whoCandidates) {
        this.whoCandidates = whoCandidates;
    }

    /**
     * @return the whereCandidates
     */
    public Map<String, Double> getWhereCandidates() {
        return whereCandidates;
    }

    /**
     * @return the whenCandidates
     */
    public Map<String, Double> getWhenCandidates() {
        return whenCandidates;
    }

    /**
     * @param whenCandidates
     *            the whenCandidates to set
     */
    public void setWhenCandidates(Map<String, Double> whenCandidates) {
        this.whenCandidates = whenCandidates;
    }

    /**
     * @return the howCandidates
     */
    public Map<String, Double> getHowCandidates() {
        return howCandidates;
    }

    /**
     * @param howCandidates
     *            the howCandidates to set
     */
    public void setHowCandidates(Map<String, Double> howCandidates) {
        this.howCandidates = howCandidates;
    }

    /**
     * @param whereCandidates
     *            the whereCandidates to set
     */
    public void setWhereCandidates(Map<String, Double> whereCandidates) {
        this.whereCandidates = whereCandidates;
    }

    /**
     * @return the whatCandidates
     */
    public Map<String, Double> getWhatCandidates() {
        return whatCandidates;
    }

    /**
     * @param whatCandidates
     *            the whatCandidates to set
     */
    public void setWhatCandidates(Map<String, Double> whatCandidates) {
        this.whatCandidates = whatCandidates;
    }

    /**
     * @return the whyCandidates
     */
    public Map<String, Double> getWhyCandidates() {
        return whyCandidates;
    }

    /**
     * @param whyCandidates
     *            the whyCandidates to set
     */
    public void setWhyCandidates(Map<String, Double> whyCandidates) {
        this.whyCandidates = whyCandidates;
    }

}
