package ws.palladian.classification.nominal;

import ws.palladian.classification.Model;
import ws.palladian.helper.collection.CountMatrix;

public final class NominalClassifierModel implements Model {

    private static final long serialVersionUID = 1L;

    private final CountMatrix<String> cooccurrenceMatrix;

    public NominalClassifierModel(CountMatrix<String> cooccurrenceMatrix) {
        this.cooccurrenceMatrix = cooccurrenceMatrix;
    }

    public CountMatrix<String> getCooccurrenceMatrix() {
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
