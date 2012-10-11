package ws.palladian.classification.nominal;

import ws.palladian.classification.Model;
import ws.palladian.helper.collection.CountMap2D;

public final class NominalClassifierModel implements Model {

    private static final long serialVersionUID = 1L;

    private final CountMap2D cooccurrenceMatrix;

    public NominalClassifierModel(CountMap2D cooccurrenceMatrix) {
        this.cooccurrenceMatrix = cooccurrenceMatrix;
    }

    public CountMap2D getCooccurrenceMatrix() {
        return cooccurrenceMatrix;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NominalClassifierModel [cooccurrenceMatrix=");
        builder.append(cooccurrenceMatrix);
        builder.append("]");
        return builder.toString();
    }

}
