package ws.palladian.helper.math;

/**
 * <p>
 * This class covers the use case where simple statistics such as min, max, and average values need to be computed for a
 * parameter. E.g. the min, max, and average execution time of a query.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class SimpleStatistic {

    private double highest = Double.MIN_VALUE;
    private double lowest = Double.MAX_VALUE;
    private double total = 0.;
    private long numValues = 0L;

    public void add(double value) {
        if (value > highest) {
            highest = value;
        }
        if (value < lowest) {
            lowest = value;
        }

        total += value;
        numValues++;
    }

    public double getAverage() {
        return total / numValues;
    }

    public double getHighest() {
        return highest;
    }

    public void setHighest(double highest) {
        this.highest = highest;
    }

    public double getLowest() {
        return lowest;
    }

    public void setLowest(double lowest) {
        this.lowest = lowest;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Max Value: ").append(highest).append("\n");
        stringBuilder.append("Min Value: ").append(lowest).append("\n");
        stringBuilder.append("Average Value: ").append(getAverage()).append("\n");
        stringBuilder.append("Total: ").append(total).append("\n");
        stringBuilder.append("Num Values: ").append(numValues).append("\n");

        return stringBuilder.toString();
    }

}
