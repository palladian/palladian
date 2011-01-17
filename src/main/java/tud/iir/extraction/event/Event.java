package tud.iir.extraction.event;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import tud.iir.classification.FeatureObject;
import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.knowledge.Extractable;
import tud.iir.web.WebResult;

/**
 * Event Object Class.
 *
 * @author Martin Wunderwald
 */
public class Event extends Extractable {

    /** serial version uid. **/
    private static final long serialVersionUID = 1023303092784471374L;

    /** holds the co-referenced Annotations and one featureObject each. **/
    private Map<Annotations, FeatureObject> annotationFeatures;

    /** holds the simple annotations in text and headline. **/
    private Annotations annotations;

    /** holds the coreferenced annotations. **/
    private HashMap<Integer, Annotations> corefAnnotations;

    /** clean event text. **/
    private String text;
    /** raw text extracted from webpage. **/
    private String rawText;
    /** title of the event. **/
    private String title;
    /** the initial url of the article. **/
    private String url;
    /** a list of webresults refering to the article. **/
    private List<WebResult> webresults;

    /** the webpage as document. **/
    private Document document;

    /** the sentences. **/
    private String[] sentences;

    /** the extracted who. **/
    private String who = "";
    /** the extracted where. **/
    private String where = "";
    /** the extracted what. **/
    private String what = "";
    /** the extracted why. **/
    private String why = "";
    /** the extracted when. **/
    private String when = "";
    /** the extracted how. **/
    private String how = "";

    /** The who candidates. **/
    private Map<String, Double> whoCandidates = new HashMap<String, Double>();

    /** The where Candidates. **/
    private Map<String, Double> whereCandidates = new HashMap<String, Double>();

    /** The what Candidates. **/
    private Map<String, Double> whatCandidates = new HashMap<String, Double>();

    /** The why Candidates. **/
    private Map<String, Double> whyCandidates = new HashMap<String, Double>();

    /** The when Candidates. **/
    private Map<String, Double> whenCandidates = new HashMap<String, Double>();

    /** The how Candidates. **/
    private Map<String, Double> howCandidates = new HashMap<String, Double>();

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
        super();
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
        super();
        this.text = text;
        this.title = title;
        this.url = url;

        setExtractedAt(new Date(System.currentTimeMillis()));
    }

    /**
     * @return the annotations
     */
    public final Annotations getAnnotations() {
        return annotations;
    }

    /**
     * @param annotations
     *            the annotations to set
     */
    public final void setAnnotations(final Annotations annotations) {
        this.annotations = annotations;
    }

    /**
     * @return the corefAnnotations
     */
    public HashMap<Integer, Annotations> getCorefAnnotations() {
        return corefAnnotations;
    }

    /**
     * @param corefAnnotations
     *            the corefAnnotations to set
     */
    public void setCorefAnnotations(
            HashMap<Integer, Annotations> corefAnnotations) {
        this.corefAnnotations = corefAnnotations;
    }

    /**
     * Getter for text.
     *
     * @return the text
     */
    public final String getText() {
        return text;
    }

    /**
     * Sets text.
     *
     * @param text
     *            the text to set
     */
    public final void setText(final String text) {
        this.text = text;
    }

    /**
     * @return the rawText
     */
    public final String getRawText() {
        return rawText;
    }

    /**
     * @param rawText
     *            the rawText to set
     */
    public final void setRawText(final String rawText) {
        this.rawText = rawText;
    }

    /**
     * @return the document
     */
    public Document getDocument() {
        return document;
    }

    /**
     * @param document
     *            the document to set
     */
    public void setDocument(Document document) {
        this.document = document;
    }

    /**
     * @return the sentences
     */
    public final String[] getSentences() {
        return sentences;
    }

    /**
     * @param sentences
     *            the sentences to set
     */
    public void setSentences(final String[] sentences) {
        this.sentences = sentences;
    }

    /**
     * @return the title
     */
    public final String getTitle() {
        return title;
    }

    /**
     * @param title
     *            the title to set
     */
    public final void setTitle(final String title) {
        this.title = title;
    }

    /**
     * @return the url
     */
    public final String getUrl() {
        return url;
    }

    /**
     * @param url
     *            the url to set
     */
    public final void setUrl(final String url) {
        this.url = url;
    }

    /**
     * @return the webresults
     */
    public final List<WebResult> getWebresults() {
        return webresults;
    }

    /**
     * @param webresults
     *            the webresults to set
     */
    public final void setWebresults(final List<WebResult> webresults) {
        this.webresults = webresults;
    }

    /**
     * @return the annotationFeatures
     */
    public final Map<Annotations, FeatureObject> getAnnotationFeatures() {
        return annotationFeatures;
    }

    /**
     * @param annotationFeatures
     *            the annotationFeatures to set
     */
    public final void setAnnotationFeatures(
            final Map<Annotations, FeatureObject> annotationFeatures) {
        this.annotationFeatures = annotationFeatures;
    }

    /**
     * @return the who
     */
    public final String getWho() {
        return who;
    }

    /**
     * @param who
     *            the who to set
     */
    public void setWho(final String who) {
        this.who = who;
    }

    /**
     * @return the where
     */
    public final String getWhere() {
        return where;
    }

    /**
     * @param where
     *            the where to set
     */
    public final void setWhere(final String where) {
        this.where = where;
    }

    /**
     * @return the what
     */
    public final String getWhat() {
        return what;
    }

    /**
     * @param what
     *            the what to set
     */
    public final void setWhat(final String what) {
        this.what = what;
    }

    /**
     * @return the why
     */
    public final String getWhy() {
        return why;
    }

    /**
     * @param why
     *            the why to set
     */
    public final void setWhy(final String why) {
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
    public final void setWhen(final String when) {
        this.when = when;
    }

    /**
     * @return the how
     */
    public final String getHow() {
        return how;
    }

    /**
     * @param how
     *            the how to set
     */
    public final void setHow(final String how) {
        this.how = how;
    }

    /**
     * @return the whoCandidates
     */
    public final Map<String, Double> getWhoCandidates() {
        return whoCandidates;
    }

    /**
     * @param whoCandidates
     *            the whoCandidates to set
     */
    public void setWhoCandidates(final Map<String, Double> whoCandidates) {
        this.whoCandidates = whoCandidates;
    }

    /**
     * @return the whereCandidates
     */
    public final Map<String, Double> getWhereCandidates() {
        return whereCandidates;
    }

    /**
     * @return the whenCandidates
     */
    public final Map<String, Double> getWhenCandidates() {
        return whenCandidates;
    }

    /**
     * @param whenCandidates
     *            the whenCandidates to set
     */
    public final void setWhenCandidates(final Map<String, Double> whenCandidates) {
        this.whenCandidates = whenCandidates;
    }

    /**
     * @return the howCandidates
     */
    public final Map<String, Double> getHowCandidates() {
        return howCandidates;
    }

    /**
     * @param howCandidates
     *            the howCandidates to set
     */
    public final void setHowCandidates(final Map<String, Double> howCandidates) {
        this.howCandidates = howCandidates;
    }

    /**
     * @param whereCandidates
     *            the whereCandidates to set
     */
    public final void setWhereCandidates(
            final Map<String, Double> whereCandidates) {
        this.whereCandidates = whereCandidates;
    }

    /**
     * @return the whatCandidates
     */
    public final Map<String, Double> getWhatCandidates() {
        return whatCandidates;
    }

    /**
     * @param whatCandidates
     *            the whatCandidates to set
     */
    public final void setWhatCandidates(final Map<String, Double> whatCandidates) {
        this.whatCandidates = whatCandidates;
    }

    /**
     * @return the whyCandidates
     */
    public final Map<String, Double> getWhyCandidates() {
        return whyCandidates;
    }

    /**
     * @param whyCandidates
     *            the whyCandidates to set
     */
    public final void setWhyCandidates(Map<String, Double> whyCandidates) {
        this.whyCandidates = whyCandidates;
    }

}
