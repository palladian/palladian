package tud.iir.classification.controlledtagging;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import tud.iir.classification.Stopwords;

/**
 * This class bundles all settings for the {@link ControlledTagger}.
 * 
 * @author Philipp Katz
 * 
 */
public class ControlledTaggerSettings {

    // //////// enumerations for settings ///////////

    public enum TaggingType {
        THRESHOLD, FIXED_COUNT
    }

    // attn: If TaggingCorrelationType is set to NO_CORRELATIONS, no correlation matrix is built, so it's not possible
    // to switch afterwards.
    public enum TaggingCorrelationType {
        NO_CORRELATIONS, SHALLOW_CORRELATIONS, DEEP_CORRELATIONS
    }

    // //////// default settings ///////////

    public static final float DEFAULT_TFIDF_THRESHOLD = 0.005f;

    public static final int DEFAULT_TAG_COUNT = 10;

    public static final float DEFAULT_CORRELATION_WEIGHT = 50.0f;

    public static final float DEFAULT_PRIOR_WEIGHT = 1.0f;

    public static final Pattern DEFAULT_TAG_MATCH_PATTERN = Pattern.compile(".*\\w.*");

    // //////// customizable settings ///////////
    // see their corresponding setters for documentation.
    private TaggingType taggingType = TaggingType.THRESHOLD;
    private TaggingCorrelationType correlationType = TaggingCorrelationType.NO_CORRELATIONS;
    private float tfidfThreshold = DEFAULT_TFIDF_THRESHOLD;
    private int tagCount = DEFAULT_TAG_COUNT;
    private float correlationWeight = DEFAULT_CORRELATION_WEIGHT;
    private float priorWeight = DEFAULT_PRIOR_WEIGHT;
    /** Tags must match this pattern, to be accepted. This way we drop tags which contain no "word" character, like "," */
    private Pattern tagMatchPattern = DEFAULT_TAG_MATCH_PATTERN;
    /** The Stopwords which are ignored as tags; no stopwords by default. */
    private Set<String> stopwords = Collections.emptySet();
    /** The Stemmer. This is not serializable and must be re-created upon de-serialization, see {@link #setup()}. */
    private SnowballStemmer stemmer = new englishStemmer();

    public ControlledTaggerSettings(TaggingType taggingType, TaggingCorrelationType correlationType,
            float tfidfThreshold, int tagCount, float correlationWeight, float priorWeight, Pattern tagMatchPattern,
            Set<String> stopwords) {
        this.taggingType = taggingType;
        this.correlationType = correlationType;
        this.tfidfThreshold = tfidfThreshold;
        this.tagCount = tagCount;
        this.correlationWeight = correlationWeight;
        this.priorWeight = priorWeight;
        this.tagMatchPattern = tagMatchPattern;
        this.stopwords = stopwords;
    }

    public ControlledTaggerSettings() {

    }

    public TaggingType getTaggingType() {
        return taggingType;
    }

    public void setTaggingType(TaggingType taggingType) {
        this.taggingType = taggingType;
    }

    public TaggingCorrelationType getCorrelationType() {
        return correlationType;
    }

    public void setCorrelationType(TaggingCorrelationType correlationType) {
        this.correlationType = correlationType;
    }

    public float getTfidfThreshold() {
        return tfidfThreshold;
    }

    /**
     * Set the threshold for the TFIDF value when in {@link TaggingType#THRESHOLD} mode.
     * 
     * @param tfidfThreshold
     */
    public void setTfidfThreshold(float tfidfThreshold) {
        this.tfidfThreshold = tfidfThreshold;
    }

    public int getTagCount() {
        return tagCount;
    }

    /**
     * Set max. number of tags to assign when in {@link TaggingType#FIXED_COUNT} mode.
     * 
     * @param tagCount
     */
    public void setTagCount(int tagCount) {
        this.tagCount = tagCount;
    }

    public float getCorrelationWeight() {
        return correlationWeight;
    }

    public void setCorrelationWeight(float correlationWeight) {
        this.correlationWeight = correlationWeight;
    }

    public float getPriorWeight() {
        return priorWeight;
    }

    /**
     * When enabled, tags from the controlled vocabulary which have a high occurence are preferred.
     * Set to -1 to disable.
     * 
     * @param usePriors
     */
    public void setPriorWeight(float priorWeight) {
        this.priorWeight = priorWeight;
    }

    public Pattern getTagMatchPattern() {
        return tagMatchPattern;
    }

    public void setTagMatchPattern(Pattern tagMatchPattern) {
        this.tagMatchPattern = tagMatchPattern;
    }

    public Set<String> getStopwords() {
        return stopwords;
    }

    /**
     * Set the Set of Stopwords to use, for example {@link Stopwords}.
     * 
     * @param stopwords
     */
    public void setStopwords(Set<String> stopwords) {
        this.stopwords = stopwords;
    }

    public SnowballStemmer getStemmer() {
        return stemmer;
    }

    public void setStemmer(SnowballStemmer stemmer) {
        this.stemmer = stemmer;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ControlledTaggerSettings:");
        sb.append("taggingType=").append(this.getTaggingType());
        sb.append(",correlationType=").append(this.getCorrelationType());
        if (getTaggingType() == TaggingType.THRESHOLD) {
            sb.append(",tfidfThreshold=").append(this.getTfidfThreshold());
        }
        if (getTaggingType() == TaggingType.FIXED_COUNT) {
            sb.append(",tagCount=").append(this.getTagCount());
        }
        if (getCorrelationType() != TaggingCorrelationType.NO_CORRELATIONS) {
            sb.append(",correlationWeight=").append(this.getCorrelationWeight());
        }
        if (getPriorWeight() != -1) {
            sb.append(",priorWeight=").append(this.getPriorWeight());
        }
        if (!getStopwords().isEmpty()) {
            sb.append(",numberStopwords=").append(getStopwords());
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        ControlledTaggerSettings settings = new ControlledTaggerSettings();
        // settings.setPriorWeight(-1);
        System.out.println(settings);
    }
}
