package ws.palladian.helper.math;

public final class ListSimilarity {

    private final double shiftSimilarity;
    private final double squaredShiftSimilarity;
    private final double rmse;

    ListSimilarity(double shiftSimilarity, double squaredShiftSimilarity, double rmse) {
        this.shiftSimilarity = shiftSimilarity;
        this.squaredShiftSimilarity = squaredShiftSimilarity;
        this.rmse = rmse;
    }

    public double getShiftSimilarity() {
        return shiftSimilarity;
    }

    public double getSquaredShiftSimilarity() {
        return squaredShiftSimilarity;
    }

    public double getRmse() {
        return rmse;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ListSimilarity [shiftSimilarity=");
        builder.append(shiftSimilarity);
        builder.append(", squaredShiftSimilarity=");
        builder.append(squaredShiftSimilarity);
        builder.append(", rmse=");
        builder.append(rmse);
        builder.append("]");
        return builder.toString();
    }

}