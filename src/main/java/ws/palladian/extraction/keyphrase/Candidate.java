package ws.palladian.extraction.keyphrase;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import ws.palladian.helper.nlp.Tokenizer;

/**
 * Represents a Candidate, used for keyphrase extraction.
 * 
 * @author Philipp Katz
 * 
 */
class Candidate {

    private DocumentModel document;

    private String value;
    private String stemmedValue;
    private int count;
    private int uppercaseCount;
    private int totalUppercaseCount;
    private int firstPos = Integer.MAX_VALUE;
    private int lastPos = Integer.MIN_VALUE;
    private SummaryStatistics correlationStats = new SummaryStatistics();
    private String posTag;

    private Boolean positive;
    private double regressionValue;

    public Candidate(DocumentModel document) {
        this.document = document;

        // prevent NaN/Infinity
        correlationStats.addValue(0);
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setStemmedValue(String stemmedValue) {
        this.stemmedValue = stemmedValue;
    }

    public String getStemmedValue() {
        return stemmedValue;
    }

    public void setFirstPos(int firstPos) {
        this.firstPos = firstPos;
    }

    public int getFirstPos() {
        return firstPos;
    }

    public void setLastPos(int lastPos) {
        this.lastPos = lastPos;
    }

    public int getLastPos() {
        return lastPos;
    }

    public void addPosition(int position) {
        firstPos = Math.min(getFirstPos(), position);
        lastPos = Math.max(getLastPos(), position);
    }

    public void incrementCount() {
        count++;
    }

    public int getCount() {
        return count;
    }

    public void incrementUppercaseCount() {
        uppercaseCount++;
    }

    public int getUppercaseCount() {
        return uppercaseCount;
    }

    public float getUppercasePercentage() {
        return (float) uppercaseCount / count;
    }
    
    public void incrementTotalUppercaseCount() {
        totalUppercaseCount++;
    }
    
    public int getTotalUppercaseCount() {
        return totalUppercaseCount;
    }
    
    public float getTotalUppercasePercentage() {
        return (float) totalUppercaseCount / count;
    }

    public int getWordCount() {
        return Tokenizer.tokenize(value).size();
    }

    public float getFrequency() {
        return (float) count / document.getTokenCount();
    }

    public float getInverseDocumentFrequency() {
        return document.getCorpus().getInverseDocumentFrequency(this);
    }

    public float getTermFrequencyInverseDocumentFrequency() {
        return getFrequency() * getInverseDocumentFrequency();
    }

    public int getSpread() {
        return lastPos - firstPos;
    }

    public float getFirstPosRel() {
        int wordCount = Math.max(1, document.getTokenCount());
        return (float) firstPos / wordCount;
    }

    public float getLastPosRel() {
        int wordCount = Math.max(1, document.getTokenCount());
        return (float) lastPos / wordCount;
    }

    public float getSpreadRel() {
        int wordCount = Math.max(1, document.getTokenCount());
        return (float) getSpread() / wordCount;
    }

    public int getLength() {
        return value.length();
    }

    public float getPrior() {
        return document.getCorpus().getPrior(this);
    }

    public void addCorrelation(double correlation) {
        correlationStats.addValue(correlation);
    }

    public double getCorrelationSum() {
        return correlationStats.getSum();
    }

    public double getCorrelationMax() {
        return correlationStats.getMax();
    }

    public double getCorrelationMin() {
        return correlationStats.getMin();
    }

    public double getCorrelationMean() {
        return correlationStats.getMean();
    }

    public int getCorrelationCount() {
        return (int) correlationStats.getN() - 1;
    }
    
    public String getPosTag() {
        return posTag;
    }
    
    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }

    public void setPositive(Boolean positive) {
        this.positive = positive;
    }

    public double getRegressionValue() {
        return regressionValue;
    }

    public void setRegressionValue(double regressionValue) {
        this.regressionValue = regressionValue;
    }

    // TODO necessary?
    public void increaseRegressionValue(double by) {
        regressionValue += by;
    }

    public Map<String, Object> getFeatures() {

        Map<String, Object> features = new LinkedHashMap<String, Object>();

        features.put("count", (double) getCount());
        features.put("uppercaseCount", (double) getUppercaseCount());
        features.put("uppercasePercentage", (double) getUppercasePercentage());
        features.put("totalUppercaseCount", (double) getTotalUppercaseCount());
        features.put("totalUppercasePercentage", (double) getTotalUppercasePercentage());
        features.put("wordCount", (double) getWordCount());
        features.put("firstPosition", (double) getFirstPos());
        features.put("lastPosition", (double) getLastPos());
        features.put("frequency", (double) getFrequency());
        features.put("inverseDocumentFrequency", (double) getInverseDocumentFrequency());
        features.put("termFrequencyInverseDocumentFrequency", (double) getTermFrequencyInverseDocumentFrequency());
        features.put("spread", (double) getSpread());
        features.put("firstPositionRelative", (double) getFirstPosRel());
        features.put("lastPositionRelative", (double) getLastPosRel());
        features.put("spreadRelative", (double) getSpreadRel());
        features.put("length", (double) getLength());
        features.put("prior", (double) getPrior());
        features.put("correlationSum", getCorrelationSum());
        features.put("correlationMax", getCorrelationMax());
        features.put("correlationMin", getCorrelationMin());
        features.put("correlationMean", getCorrelationMean());
        // features.put("correlationCount", (double) getCorrelationCount());
        features.put("posTag", getPosTag());

        // debugging
//        for (Entry<String, Double> entry : features.entrySet()) {
//            Double value = entry.getValue();
//            assert !Double.isInfinite(value);
//            assert !Double.isNaN(value);
//        }

        // value for the Classifier, if we are in training mode.
        if (positive != null) {
            // features.put("positive", positive ? 1.0 : 0.0);
            features.put("positive", String.valueOf(positive));
        }

        return features;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Candidate [");
        // builder.append("capitalCount=").append(capitalCount);
        builder.append("count=").append(count);
        builder.append(" capitalPercentage=").append(getUppercasePercentage());
        // builder.append(" firstPos=").append(firstPos);
        // builder.append(" lastPos=").append(lastPos);
        builder.append(" stemmedValue=").append(stemmedValue);
        builder.append(" value=").append(value);
        // builder.append(" wordCount=").append(wordCount);
        builder.append(" firstPosRel=").append(getFirstPosRel());
        builder.append(" freq=").append(getFrequency());
        builder.append(" invDocFreq=").append(getInverseDocumentFrequency());
        builder.append(" termFreqInvDocFreq=").append(getTermFrequencyInverseDocumentFrequency());
        builder.append(" lastPosRel=").append(getLastPosRel());
        builder.append(" length=").append(getLength());
        // builder.append(" spread=").append(getSpread());
        builder.append(" spreadRel=").append(getSpreadRel());
        builder.append(" wordCount=").append(getWordCount());
        builder.append("]");
        return builder.toString();
    }
}