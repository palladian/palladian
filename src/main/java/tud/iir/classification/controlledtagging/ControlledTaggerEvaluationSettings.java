package tud.iir.classification.controlledtagging;


/**
 * Extends {@link ControlledTaggerEvaluationSettings} with evaluation specific parameters.
 * 
 * @author Philipp Katz
 *
 */
public class ControlledTaggerEvaluationSettings extends ControlledTaggerSettings {
    
    private int trainLimit;
    private int testLimit;
    
    public ControlledTaggerEvaluationSettings() {
        super();
    }
    
    /**
     * Monstous nearly-all-parameter-constructor for evaluation.
     * 
     * @param taggingType
     * @param correlationType
     * @param tfidfThreshold
     * @param tagCount
     * @param correlationWeight
     * @param priorWeight
     * @param tagMatchPattern
     * @param stopwords
     * @param trainLimit
     * @param testLimit
     */
    public ControlledTaggerEvaluationSettings(
            int trainLimit, 
            int testLimit,
            TaggingType taggingType, 
            TaggingCorrelationType correlationType,
            float tfidfThreshold, 
            int tagCount, 
            float correlationWeight, 
            float priorWeight,
            int phraseLength) {
        super();
        
        setTaggingType(taggingType);
        setCorrelationType(correlationType);
        setTfidfThreshold(tfidfThreshold);
        setTagCount(tagCount);
        setCorrelationWeight(correlationWeight);
        setPriorWeight(priorWeight);
        setPhraseLength(phraseLength);
        
        this.trainLimit = trainLimit;
        this.testLimit = testLimit;
    }

    /**
     * @return the trainLimit
     */
    public int getTrainLimit() {
        return trainLimit;
    }
    /**
     * @param trainLimit the trainLimit to set
     */
    public void setTrainLimit(int trainLimit) {
        this.trainLimit = trainLimit;
    }
    /**
     * @return the testLimit
     */
    public int getTestLimit() {
        return testLimit;
    }
    /**
     * @param testLimit the testLimit to set
     */
    public void setTestLimit(int testLimit) {
        this.testLimit = testLimit;
    }
    
    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        sb.append("ControlledTaggerEvaluationSettings:");
        sb.append("trainLimit:").append(getTrainLimit());
        sb.append(",testLimit:").append(getTestLimit());
        sb.append(",").append(super.toString());
        
        return sb.toString();
    }

}
