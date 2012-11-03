package ws.palladian.classification;

/**
 * <p>
 * Hold information about how relevant a category is.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class CategoryEntry {

    private final String name;
    private final double probability;

    public CategoryEntry(String name, double probability) {
        this.name = name;
        this.probability = probability;
    }

    public String getName() {
        return name;
    }

    public double getProbability() {
        return probability;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CategoryEntry [name=");
        builder.append(name);
        builder.append(", probability=");
        builder.append(probability);
        builder.append("]");
        return builder.toString();
    }

}