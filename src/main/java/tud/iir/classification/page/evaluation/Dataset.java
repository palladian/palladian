package tud.iir.classification.page.evaluation;

public class Dataset {

    /** The path pointing to the dataset file. */
    private String path = "";

    /** The character sequence that splits the training data and the class in the input file. */
    private String separationString = " ";

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
