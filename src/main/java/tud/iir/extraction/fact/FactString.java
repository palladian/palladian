package tud.iir.extraction.fact;

/**
 * The fact string is the string where the fact value is expected to be found in this string can have been derived from different methods depending on where the
 * attribute has been found e.g. if in free text
 * "The i8510 INNOV8 offers 16GB of built-in memory in addition to a microSD card slot for even more storage options" or in a colon of a table
 * "16GB internal, microSD card slot" the distinction is important as the value extraction can differ for these types
 * 
 * @author David Urbansky
 */
public class FactString {

    private String factString;
    private int extractionType;

    public FactString(String factString, int extractionType) {
        this.setFactString(factString);
        this.setExtractionType(extractionType);
    }

    public String getFactString() {
        return factString.trim();
    }

    public void setFactString(String factString) {
        this.factString = factString;
    }

    public int getExtractionType() {
        return extractionType;
    }

    public void setExtractionType(int type) {
        this.extractionType = type;
    }

    @Override
    public String toString() {
        return getFactString();
    }
}