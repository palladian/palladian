package tud.iir.extraction.keyphrase;

import java.util.Set;
import java.util.regex.Pattern;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import tud.iir.classification.Stopwords;
import tud.iir.extraction.keyphrase.TokenizerPlus.TokenizerSettings;

public class KeyphraseExtractorSettings implements TokenizerSettings {

    public enum ReRankingMode {
        NO_RERANKING, SHALLOW_CORRELATION_RERANKING, DEEP_CORRELATION_RERANKING
    }

    /** Different assignment strategies. */
    public enum AssignmentMode {

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

    /** The stemmer to use. Snowball offers stemmer implementations for various languages. */
    private SnowballStemmer stemmer = new englishStemmer();

    /** List of stopwords to use. */
    private Set<String> stopwords = new Stopwords(Stopwords.Predefined.EN);

    /** If enabled, only those keyphrases are assigned, which have been trained before. */
    private boolean controlledMode = false;

    private AssignmentMode assignmentMode = AssignmentMode.FIXED_COUNT;

    private ReRankingMode reRankingMode = ReRankingMode.NO_RERANKING;

    private int keyphraseCount = 10;

    private float keyphraseThreshold = 0.75f;

    private float correlationWeight = 90000;

    /** Path in the file system where to put the models. This will be created as directory, containing all files. */
    private String modelPath = "data/models/PalladianKeyphraseExtractor";

    private Pattern pattern = Pattern.compile("[a-zA-Z\\s]{3,}");

    private int minPhraseLength = 1;
    private int maxPhraseLength = 5;

    /** Minimum occurrence of a candidate to be considered. Can be set to values greater 1 for long documents. */
    private int minOccurenceCount = 1;

    public KeyphraseExtractorSettings() {
    }

    /*public KeyphraseExtractorSettings(SnowballStemmer stemmer, Stopwords stopwords, boolean controlledMode,
            AssignmentMode assignmentMode, ReRankingMode reRankingMode, int keyphraseCount, float keyphraseThreshold,
            float correlationWeight, Pattern pattern) {
        this.stemmer = stemmer;
        this.stopwords = stopwords;
        this.controlledMode = controlledMode;
        this.assignmentMode = assignmentMode;
        this.reRankingMode = reRankingMode;
        this.keyphraseCount = keyphraseCount;
        this.keyphraseThreshold = keyphraseThreshold;
        this.correlationWeight = correlationWeight;
        this.pattern = pattern;
    }*/

    @Override
    public SnowballStemmer getStemmer() {
        return stemmer;
    }

    public void setStemmer(SnowballStemmer stemmer) {
        this.stemmer = stemmer;
    }

    @Override
    public Set<String> getStopwords() {
        return stopwords;
    }

    public void setStopwords(Set<String> stopwords) {
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

        // remove trailing slash
        if (modelPath.endsWith("/")) {
            modelPath = modelPath.substring(0, modelPath.length() - 1);
        }

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
    
    public int getMinPhraseLength() {
        return minPhraseLength;
    }
    
    public void setMinPhraseLength(int minPhraseLength) {
        this.minPhraseLength = minPhraseLength;
    }

    public int getMaxPhraseLength() {
        return maxPhraseLength;
    }

    public void setMaxPhraseLength(int phraseLength) {
        this.maxPhraseLength = phraseLength;
    }

    public int getMinOccurenceCount() {
        return minOccurenceCount;
    }

    public void setMinOccurenceCount(int minOccurenceCount) {
        this.minOccurenceCount = minOccurenceCount;
    }

}
