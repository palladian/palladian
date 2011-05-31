package ws.palladian.preprocessing.featureextraction;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;

/**
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 */
public class Token {

    private int startPosition;
    private int endPosition;
    private FeatureVector featureVector;
    private PipelineDocument document;

    public Token(PipelineDocument document, int startPosition, int endPosition) {
        featureVector = new FeatureVector();
        this.document = document;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    /**
     * @return the startPosition
     */
    public int getStartPosition() {
        return startPosition;
    }

    /**
     * @return the endPosition
     */
    public int getEndPosition() {
        return endPosition;
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

    public PipelineDocument getDocument() {
        return document;
    }
    
    protected void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }
    
    protected void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }
    
    void setDocument(PipelineDocument document) {
        this.document = document;
    }

}
