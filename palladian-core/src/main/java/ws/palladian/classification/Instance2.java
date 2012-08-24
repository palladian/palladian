package ws.palladian.classification;

import ws.palladian.processing.features.FeatureVector;

public class Instance2<T> {

    public FeatureVector featureVector;
    public T target;
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Instance2 [featureVector=");
        builder.append(featureVector);
        builder.append(", target=");
        builder.append(target);
        builder.append("]");
        return builder.toString();
    }
    
    
    
    
}