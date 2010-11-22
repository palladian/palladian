package tud.iir.classification.controlledtagging;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


public class Candidate {
    
    private DocumentModel document;

    private String value;
    private String stemmedValue;
    private int count;
    private int capitalCount;
    private int wordCount = Integer.MAX_VALUE;
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
    
    public void setWordCount(int numWords) {
        this.wordCount = Math.min(this.wordCount, numWords);
    }
    
    public int getWordCount() {
        return wordCount;
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
    
    public float getFirstPosRel() {
        return (float) firstPos / (document.getWordCount() - 1);
    }
    
    public float getLastPosRel() {
        return (float) lastPos / (document.getWordCount() - 1);
    }
    
    public float getSpreadRel() {
        return (float) getSpread() / (document.getWordCount() - 1);
    }
    
    public int getLength() {
        return value.length();
    }
    
    // TODO
    public float getPrior() {
        return document.getPrior(this.getValue().toLowerCase());
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
        
        if (positive != null) {
            features.put("positive", positive ? 1.0 : 0.0);
        }
        
        // XXX debugging
        for (Entry<String,Double> entry : features.entrySet()) {
            double value = entry.getValue();
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                System.out.println(entry.getKey());
            }
        }
        
        return features;
    }
    
    

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
//        builder.append("Candidate [capitalCount=");
//        builder.append(capitalCount);
//        builder.append(", count=");
//        builder.append(count);
//        builder.append(", firstPos=");
//        builder.append(firstPos);
//        builder.append(", lastPos=");
//        builder.append(lastPos);
//        builder.append(", stemmedValue=");
        builder.append(stemmedValue);
//        builder.append(", value=");
//        builder.append(value);
//        builder.append(", wordCount=");
//        builder.append(wordCount);
//        builder.append(", getFirstPosRel()=");
//        builder.append(getFirstPosRel());
//        builder.append(", getInverseDocumentFrequency()=");
//        builder.append(getInverseDocumentFrequency());
//        builder.append(", getTermFrequencyInverseDocumentFrequency()=");
//        builder.append(getTermFrequencyInverseDocumentFrequency());
//        builder.append(", getLastPosRel()=");
//        builder.append(getLastPosRel());
//        builder.append(", getLength()=");
//        builder.append(getLength());
//        builder.append(", getSpread()=");
//        builder.append(getSpread());
//        builder.append(", getSpreadRel()=");
//        builder.append(getSpreadRel());
//        builder.append(", getWordCount()=");
//        builder.append(getWordCount());
//        builder.append("]");
        return builder.toString();
    }
    
    

}