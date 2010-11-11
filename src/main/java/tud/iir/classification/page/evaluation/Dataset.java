package tud.iir.classification.page.evaluation;

import tud.iir.helper.FileHelper;

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

    /** What percentage of the dataset should be used for training (0,100]. */
    private int usePercentTraining = 100;

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

    /**
     * Get the path to the root folder of the dataset.
     * 
     * @return The path to the root folder of the dataset.
     */
    public String getRootPath() {
        return FileHelper.getFilePath(getPath());
    }

    public void setSeparationString(String separationString) {
        this.separationString = separationString;
    }

    public String getSeparationString() {
        return separationString;
    }

    public void setUsePercentTraining(int usePercentTraining) {
        this.usePercentTraining = usePercentTraining;
    }

    public int getUsePercentTraining() {
        return usePercentTraining;
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