package ws.palladian.classification;

import java.io.Serializable;

import ws.palladian.processing.features.FeatureVector;

public class NominalInstance implements Serializable {

    /**
     * <p>
     * Used for serializing objects of this class. Should only change if the attribute set of this class changes.
     * </p>
     */
    private static final long serialVersionUID = -317834925209130296L;
    public FeatureVector featureVector;
    public String targetClass;

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Instance2 [featureVector=");
        builder.append(featureVector);
        builder.append(", target=");
        builder.append(targetClass);
        builder.append("]");
        return builder.toString();
    }

}