package ws.palladian.preprocessing.featureextraction;

import ws.palladian.preprocessing.PipelineDocument;

/**
 * An annotation which points to text fragments in a {@link PipelineDocument}.
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 */
public class PositionAnnotation extends Annotation {

    private int startPosition;
    private int endPosition;

    public PositionAnnotation(PipelineDocument document, int startPosition, int endPosition) {
        super(document);
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    /* (non-Javadoc)
     * @see ws.palladian.preprocessing.featureextraction.Annotation#getStartPosition()
     */
    @Override
    public int getStartPosition() {
        return startPosition;
    }

    /* (non-Javadoc)
     * @see ws.palladian.preprocessing.featureextraction.Annotation#getEndPosition()
     */
    @Override
    public int getEndPosition() {
        return endPosition;
    }

    /* (non-Javadoc)
     * @see ws.palladian.preprocessing.featureextraction.Annotation#getValue()
     */
    @Override
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
        builder.append("Annotation [value=");
        builder.append(getValue());
        builder.append(", startPosition=");
        builder.append(getStartPosition());
        builder.append(", endPosition=");
        builder.append(getEndPosition());
        builder.append(", featureVector=");
        builder.append(getFeatureVector());
        builder.append("]");
        return builder.toString();
    }

    /* (non-Javadoc)
     * @see ws.palladian.preprocessing.featureextraction.Annotation#setFeatureVector(ws.palladian.model.features.FeatureVector)
     */
//    @Override
//    public void setFeatureVector(FeatureVector featureVector) {
//        this.featureVector = featureVector;
//    }


    /* (non-Javadoc)
     * @see ws.palladian.preprocessing.featureextraction.Annotation#getDocument()
     */
//    @Override
//    public PipelineDocument getDocument() {
//        return document;
//    }
    
    protected void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }
    
    protected void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }
    
//    void setDocument(PipelineDocument document) {
//        this.document = document;
//    }

}
