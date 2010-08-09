package tud.iir.extraction.event;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tud.iir.classification.FeatureObject;
import tud.iir.knowledge.Extractable;
import tud.iir.web.WebResult;

import com.aliasi.chunk.Chunk;

/**
 * @author Martin Wunderwald
 *
 */
public class Event extends Extractable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1023303092784471374L;
	private FeatureObject features;
	private Map<Integer, FeatureObject> entityFeatures;
	private Map<Integer, Set<Chunk>> entityChunks;
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

	public Event() {
		super();
		setExtractedAt(new Date(System.currentTimeMillis()));
	}

	public Event(String url) {
		super();
		this.url = url;
		
		setExtractedAt(new Date(System.currentTimeMillis()));
	}

	public Event(String title, String text) {
		this.text = text;
		this.title = title;
		setExtractedAt(new Date(System.currentTimeMillis()));
	}

	public Event(String title, String text, String url) {
		this.text = text;
		this.title = title;
		this.url = url;

		setExtractedAt(new Date(System.currentTimeMillis()));
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public FeatureObject getFeatures() {
		return features;
	}

	public void setFeatures(FeatureObject features) {
		this.features = features;
	}

	public Map<Integer, FeatureObject> getEntityFeatures() {
		return entityFeatures;
	}

	public void setEntityFeatures(Map<Integer, FeatureObject> entityFeatures) {
		this.entityFeatures = entityFeatures;
	}

	public Map<Integer, Set<Chunk>> getEntityChunks() {
		return entityChunks;
	}

	public void setEntityChunks(Map<Integer, Set<Chunk>> entityChunks) {
		this.entityChunks = entityChunks;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<WebResult> getWebresults() {
		return webresults;
	}

	public void setWebresults(List<WebResult> webresult) {
		this.webresults = webresult;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getWho() {
		return who;
	}

	public void setWho(String who) {
		this.who = who;
	}

	public String getWhere() {
		return where;
	}

	public void setWhere(String where) {
		this.where = where;
	}

	public String getWhat() {
		return what;
	}

	public void setWhat(String what) {
		this.what = what;
	}

	public String getWhy() {
		return why;
	}

	public void setWhy(String why) {
		this.why = why;
	}

	public String getWhen() {
		return when;
	}

	public void setWhen(String when) {
		this.when = when;
	}

	public String getHow() {
		return how;
	}

	public void setHow(String how) {
		this.how = how;
	}
}
