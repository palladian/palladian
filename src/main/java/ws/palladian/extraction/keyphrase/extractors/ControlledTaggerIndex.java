package ws.palladian.extraction.keyphrase.extractors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import ws.palladian.classification.FastWordCorrelationMatrix;
import ws.palladian.classification.WordCorrelationMatrix;

/**
 * The ControlledTaggerIndex contains all necessary index data for the Tagger. This includes the controlled vocabulary,
 * word correlations, stems. This class can be serialized to disk via the Tagger.
 * 
 * @author Philipp Katz
 * 
 */
public class ControlledTaggerIndex implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 3361457438803333623L;
    /** Index over all documents with tags, to calculate IDF. Counts how many documents contain a specific tag. */
    private Bag<String> idfIndex;
    /** The controlled vocabulary with all available tags. */
    private Bag<String> tagVocabulary;
    /** The controlled vocabulary with all available tags, in stemmed form. */
    private Bag<String> stemmedTagVocabulary;
    /** Map with stemmed tags and their most common, unstemmed form. For example: softwar > software */
    private Map<String, String> unstemMap;
    /** Number of documents in the idf index. */
    private int idfCount;
    /** Number of documents with which the tagger was trained. */
    private int trainCount;
    /** Average occurence a tag in the controlled vocabulary has been assigned. */
    private float averageTagOccurence;
    /**
     * The WordCorrelationMatrix keeps correlations between pairs of tags from the vocabulary to improve tagging
     * accuracy.
     */
    private WordCorrelationMatrix wcm;
    /** Flag to indicate that index has changed and data, like stems and correlations have to be re-calculated. */
    private boolean dirtyIndex;

    /** Use package visibility, as the index is meant to be instantiated only by the Tagger itself. */
    ControlledTaggerIndex() {
        this(new HashBag<String>(), new HashBag<String>(), new HashBag<String>(), new HashMap<String, String>(), 0, 0,
                0, new FastWordCorrelationMatrix(), false);
    }

    ControlledTaggerIndex(Bag<String> idfIndex, Bag<String> tagVocabulary, Bag<String> stemmedTagVocabulary,
            Map<String, String> unstemMap, int idfCount, int trainCount, float averageTagOccurence,
            WordCorrelationMatrix wcm, boolean dirtyIndex) {
        this.idfIndex = idfIndex;
        this.tagVocabulary = tagVocabulary;
        this.stemmedTagVocabulary = stemmedTagVocabulary;
        this.unstemMap = unstemMap;
        this.idfCount = idfCount;
        this.trainCount = trainCount;
        this.averageTagOccurence = averageTagOccurence;
        this.wcm = wcm;
        this.dirtyIndex = dirtyIndex;
    }

    public Bag<String> getIdfIndex() {
        return idfIndex;
    }

    public void setIdfIndex(Bag<String> idfIndex) {
        this.idfIndex = idfIndex;
    }

    public Bag<String> getTagVocabulary() {
        return tagVocabulary;
    }

    public void setTagVocabulary(Bag<String> tagVocabulary) {
        this.tagVocabulary = tagVocabulary;
    }

    public Bag<String> getStemmedTagVocabulary() {
        return stemmedTagVocabulary;
    }

    public void setStemmedTagVocabulary(Bag<String> stemmedTagVocabulary) {
        this.stemmedTagVocabulary = stemmedTagVocabulary;
    }

    public Map<String, String> getUnstemMap() {
        return unstemMap;
    }

    public void setUnstemMap(Map<String, String> unstemMap) {
        this.unstemMap = unstemMap;
    }

    public int getIdfCount() {
        return idfCount;
    }

    public void setIdfCount(int idfCount) {
        this.idfCount = idfCount;
    }

    public int getTrainCount() {
        return trainCount;
    }

    public void setTrainCount(int trainCount) {
        this.trainCount = trainCount;
    }

    public float getAverageTagOccurence() {
        return averageTagOccurence;
    }

    public void setAverageTagOccurence(float averageTagOccurence) {
        this.averageTagOccurence = averageTagOccurence;
    }

    public WordCorrelationMatrix getWcm() {
        return wcm;
    }

    public void setWcm(WordCorrelationMatrix wcm) {
        this.wcm = wcm;
    }

    public boolean isDirtyIndex() {
        return dirtyIndex;
    }

    public void setDirtyIndex(boolean dirtyIndex) {
        this.dirtyIndex = dirtyIndex;
    }
    
    
    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder();

        
        sb.append("ControlledTaggerIndex");
        sb.append("\nsize idfIndex=").append(this.getIdfIndex().uniqueSet().size());
        sb.append("\nsize tagVocabulary=").append(this.getTagVocabulary().uniqueSet().size());
        sb.append("\nsize stemmedTagVocabulary=").append(this.getStemmedTagVocabulary().uniqueSet().size());
        sb.append("\nsize unstemMap=").append(this.getUnstemMap().size());
        sb.append("\nsize wcm=").append(this.getWcm().getCorrelations().size());
        sb.append("\nidfCount=").append(this.getIdfCount());
        sb.append("\ntrainCount=").append(this.getTrainCount());
        sb.append("\navergateTagOccurence=").append(this.getAverageTagOccurence());
        sb.append("\ndirtyIndex=").append(this.isDirtyIndex());

        
        
        // TODO Auto-generated method stub
        return sb.toString();
    }
}