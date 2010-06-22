package tud.iir.knowledge;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;

import tud.iir.helper.StringHelper;
import tud.iir.normalization.DateNormalizer;

/**
 * The abstract class of what can be extracted.
 * 
 * @author David Urbansky
 */
public abstract class Extractable implements Serializable {

    private static final long serialVersionUID = -3001186499840401489L;

    private int id = -1;
    private String name;

    // trust in the correctness of the entity, trust 1 means human checked / training entity
    private double trust = 0.5;

    // when was the extractable last searched
    private Date lastSearched = null;

    // when has the extractable been extracted
    private Date extractedAt = null;

    // sources from which the extraction was performed
    protected Sources<Source> sources;

    // whether the extractable is used as training, testing or unknown instance
    public static int UNKNOWN = 0;
    public static int TRAINING = 1;
    public static int TESTING = 2;
    private int type = UNKNOWN;

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getSafeName() {
        return StringHelper.makeSafeName(getName().toLowerCase());
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTrust() {
        return trust;
    }

    public void setTrust(double trust) {
        this.trust = trust;
    }

    public Date getLastSearched() {
        return lastSearched;
    }

    public void setLastSearched(Date lastSearched) {
        this.lastSearched = lastSearched;
    }

    public String getExtractedAtAsUTCString() {
        return DateNormalizer.normalizeDateFormat(extractedAt, "yyyy-MM-dd HH:mm:ss");
    }

    public Date getExtractedAt() {
        return extractedAt;
    }

    public void setExtractedAt(Date extractedAt) {
        this.extractedAt = extractedAt;
    }

    public Sources<Source> getSources() {
        return sources;
    }

    public void setSources(Sources<Source> sources) {
        this.sources = sources;
    }

    public void addSource(Source source) {
        this.sources.add(source);
    }

    public void addSources(Sources<Source> sources) {
        Iterator<Source> sourceIterator = sources.iterator();
        while (sourceIterator.hasNext()) {
            Source currentSource = sourceIterator.next();
            if (!this.sources.contains(currentSource)) {
                this.sources.add(currentSource);
            }
        }
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}