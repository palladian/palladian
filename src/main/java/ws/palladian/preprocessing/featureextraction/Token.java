package ws.palladian.preprocessing.featureextraction;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;

/**
 * 
 * @author Philipp Katz
 * 
 */
public class Token {

    private int startPosition;
    private int endPosition;
    private FeatureVector featureVector;
    private PipelineDocument document;

    public Token(PipelineDocument document) {
        featureVector = new FeatureVector();
        this.document = document;
    }

    /**
     * @return the startPosition
     */
    public int getStartPosition() {
        return startPosition;
    }

    /**
     * @param startPosition the startPosition to set
     */
    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    /**
     * @return the endPosition
     */
    public int getEndPosition() {
        return endPosition;
    }

    /**
     * @param endPosition the endPosition to set
     */
    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

    public String getValue() {
        String text = getDocument().getOriginalContent();
        // return text.substring(getStartPosition(), getEndPosition());
        
        // return a copy of the String, elsewise we will run into memory problems,
        // as the original String from the document might never get GC'ed, as long 
        // as we keep its Tokens in memory
        // http://fishbowl.pastiche.org/2005/04/27/the_string_memory_gotcha/
        return new String(text.substring(getStartPosition(), getEndPosition()));
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Token [value=");
        builder.append(getValue());
        builder.append(", startPosition=");
        builder.append(startPosition);
        builder.append(", endPosition=");
        builder.append(endPosition);
        builder.append(", featureVector=");
        builder.append(featureVector);
        builder.append("]");
        return builder.toString();
    }

    public void setFeatureVector(FeatureVector featureVector) {
        this.featureVector = featureVector;
    }

    public FeatureVector getFeatureVector() {
        return featureVector;
    }

    public void setDocument(PipelineDocument document) {
        this.document = document;
    }

    public PipelineDocument getDocument() {
        return document;
    }

}
