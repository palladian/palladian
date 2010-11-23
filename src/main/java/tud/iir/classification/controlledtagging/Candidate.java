package tud.iir.classification.controlledtagging;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import tud.iir.helper.Tokenizer;

/**
 * Represents a Candidate, used for keyphrase extraction.
 * 
 * @author Philipp Katz
 * 
 */
public class Candidate {

    private DocumentModel document;

    private String value;
    private String stemmedValue;
    private int count;
    private int capitalCount;
    private int firstPos = Integer.MAX_VALUE;
    private int lastPos = Integer.MIN_VALUE;
    private Boolean positive;
    private float regressionValue;

    public Candidate(DocumentModel document) {
        this.document = document;
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

    public void incrementCapitalCount() {
        capitalCount++;
    }

    public int getCapitalCount() {
        return capitalCount;
    }

    public float getCapitalPercentage() {
        return (float) capitalCount / count;
    }

    public int getWordCount() {
        return Tokenizer.tokenize(value).size();
    }

    public float getFrequency() {
        return (float) count / document.getCandidateCount();
    }

    public float getInverseDocumentFrequency() {
        return document.getInverseDocumentFrequency(this);
    }

    public float getTermFrequencyInverseDocumentFrequency() {
        return getFrequency() * getInverseDocumentFrequency();
    }

    public int getSpread() {
        return lastPos - firstPos;
    }

    // FIXME dirty
    public float getFirstPosRel() {
        // return (float) firstPos / (document.getWordCount() - 1);
        int wordCount = document.getWordCount();
        wordCount = wordCount > 1 ? wordCount - 1 : 1;
        return (float) firstPos / wordCount;
    }

    // FIXME dirty
    public float getLastPosRel() {
        // return (float) lastPos / (document.getWordCount() - 1);
        int wordCount = document.getWordCount();
        wordCount = wordCount > 1 ? wordCount - 1 : 1;
        return (float) lastPos / wordCount;
    }

    // FIXME dirty
    public float getSpreadRel() {
        // return (float) getSpread() / (document.getWordCount() - 1);
        int wordCount = document.getWordCount();
        wordCount = wordCount > 1 ? wordCount - 1 : 1;
        return (float) getSpread() / wordCount;
    }

    public int getLength() {
        return value.length();
    }

    // TODO this should be the stemmed value!
    public float getPrior() {
        // return document.getPrior(this.getValue().toLowerCase());
        return document.getPrior(this.getStemmedValue().toLowerCase());
    }

    public void setPositive(Boolean positive) {
        this.positive = positive;
    }

    public float getRegressionValue() {
        return regressionValue;
    }

    public void setRegressionValue(float regressionValue) {
        this.regressionValue = regressionValue;
    }

    public Map<String, Double> getFeatures() {

        Map<String, Double> features = new LinkedHashMap<String, Double>();

        features.put("count", (double) getCount());
        features.put("capitalCount", (double) getCapitalCount());
        features.put("capitalPercentage", (double) getCapitalPercentage());
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

        // value for the Classifier, if we are in training mode.
        if (positive != null) {
            features.put("positive", positive ? 1.0 : 0.0);
        }

        // XXX debugging
        for (Entry<String, Double> entry : features.entrySet()) {
            double value = entry.getValue();
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                System.out.println(entry.getKey());
            }
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
        builder.append(" capitalPercentage=").append(getCapitalPercentage());
        // builder.append(" firstPos=").append(firstPos);
        // builder.append(" lastPos=").append(lastPos);
        builder.append(" stemmedValue=").append(stemmedValue);
        builder.append(" value=").append(value);
        // builder.append(" wordCount=").append(wordCount);
        builder.append(" firstPosRel=").append(getFirstPosRel());
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