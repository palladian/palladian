package tud.iir.classification.controlledtagging;

import java.util.regex.Pattern;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import tud.iir.classification.Stopwords;

public class KeyphraseExtractorSettings {
    /** The stemmer to use. Snowball offers stemmer implementations for various languages. */
    private SnowballStemmer stemmer;
    /** List of stopwords to use. */
    private Stopwords stopwords;
    private boolean controlledMode;
    private AssignmentMode assignmentMode;
    private ReRankingMode reRankingMode;
    private int keyphraseCount;
    private float keyphraseThreshold;
    private float correlationWeight;
    private String modelPath;
    private Pattern pattern;
    private int phraseLength = 5;
    private int minOccurenceCount = 1;
    
    public KeyphraseExtractorSettings() {
        this(
                new englishStemmer(), 
                new Stopwords(Stopwords.Predefined.EN), 
                false,
                AssignmentMode.FIXED_COUNT, 
                ReRankingMode.DEEP_CORRELATION_RERANKING, 
                10, 
                0.75f, 
                90000, 
                "data/models/keyphraseExtractorCorpus.ser", 
                Pattern.compile("[a-zA-Z\\s]{3,}")
                );
    }

    public KeyphraseExtractorSettings(SnowballStemmer stemmer, Stopwords stopwords, boolean controlledMode,
            AssignmentMode assignmentMode, ReRankingMode reRankingMode, int keyphraseCount, float keyphraseThreshold,
            float correlationWeight, String modelPath, Pattern pattern) {
        this.stemmer = stemmer;
        this.stopwords = stopwords;
        this.controlledMode = controlledMode;
        this.assignmentMode = assignmentMode;
        this.reRankingMode = reRankingMode;
        this.keyphraseCount = keyphraseCount;
        this.keyphraseThreshold = keyphraseThreshold;
        this.correlationWeight = correlationWeight;
        this.modelPath = modelPath;
        this.pattern = pattern;
    }

    public SnowballStemmer getStemmer() {
        return stemmer;
    }

    public void setStemmer(SnowballStemmer stemmer) {
        this.stemmer = stemmer;
    }

    public Stopwords getStopwords() {
        return stopwords;
    }

    public void setStopwords(Stopwords stopwords) {
        this.stopwords = stopwords;
    }

    public boolean isControlledMode() {
        return controlledMode;
    }

    public void setControlledMode(boolean controlledMode) {
        this.controlledMode = controlledMode;
    }

    public AssignmentMode getAssignmentMode() {
        return assignmentMode;
    }

    public void setAssignmentMode(AssignmentMode assignmentMode) {
        this.assignmentMode = assignmentMode;
    }

    public ReRankingMode getReRankingMode() {
        return reRankingMode;
    }

    public void setReRankingMode(ReRankingMode reRankingMode) {
        this.reRankingMode = reRankingMode;
    }

    public int getKeyphraseCount() {
        return keyphraseCount;
    }

    public void setKeyphraseCount(int keyphraseCount) {
        this.keyphraseCount = keyphraseCount;
    }

    public float getKeyphraseThreshold() {
        return keyphraseThreshold;
    }

    public void setKeyphraseThreshold(float keyphraseThreshold) {
        this.keyphraseThreshold = keyphraseThreshold;
    }

    public float getCorrelationWeight() {
        return correlationWeight;
    }

    public void setCorrelationWeight(float correlationWeight) {
        this.correlationWeight = correlationWeight;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }
    
    public Pattern getPattern() {
        return pattern;
    }
    
    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }
    
    public void setPattern(String patternRegEx) {
        this.pattern = Pattern.compile(patternRegEx);
    }
    
    public int getPhraseLength() {
        return phraseLength;
    }
    
    public void setPhraseLength(int phraseLength) {
        this.phraseLength = phraseLength;
    }
    
    public int getMinOccurenceCount() {
        return minOccurenceCount;
    }
    
    public void setMinOccurenceCount(int minOccurenceCount) {
        this.minOccurenceCount = minOccurenceCount;
    }
}

enum ReRankingMode {
    NO_RERANKING, SHALLOW_CORRELATION_RERANKING, DEEP_CORRELATION_RERANKING
}

/** Different assignment strategies. */
enum AssignmentMode {

    /** Assign maximum count of keyphrases (e.g. 10 keyphrases or less). */
    FIXED_COUNT,

    /** Assign keyphrases which exceed a specified threshold (e.g. all keyphrases with weights > 0.75). */
    THRESHOLD,

    /**
     * Assign maximum count of keyphrases or more if they exceed the specified threshold (e.g. all keyphrases with
     * weights > 0.75 or 10 or less).
     */
    COMBINED

}