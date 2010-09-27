package tud.iir.classification.page.evaluation;

/**
 * A simple representation of a dataset.
 * 
 * @author David Urbansky
 * 
 */
public class Dataset {

    /** The path pointing to the dataset file. */
    private String path = "";

    /** The character sequence that splits the training data and the class in the input file. */
    private String separationString = " ";

    /**
     * Whether the first field that is separated by the separation string links to a document or is the document itself.
     */
    private boolean firstFieldLink = false;

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setSeparationString(String separationString) {
        this.separationString = separationString;
    }

    public String getSeparationString() {
        return separationString;
    }

    public void setFirstFieldLink(boolean firstFieldLink) {
        this.firstFieldLink = firstFieldLink;
    }

    public boolean isFirstFieldLink() {
        return firstFieldLink;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Dataset [path=");
        builder.append(path);
        builder.append(", separationString=");
        builder.append(separationString);
        builder.append("]");
        return builder.toString();
    }

}