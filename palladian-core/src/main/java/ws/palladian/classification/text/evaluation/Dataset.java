package ws.palladian.classification.text.evaluation;

import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * A simple representation of a dataset.
 * </p>
 * 
 * @author David Urbansky
 */
public class Dataset {

    /** The name of the dataset to identify it. */
    private String name = "NONAME";

    /** The path pointing to the dataset file. */
    private String path = "";

    /** The path to the root folder of the dataset. */
    private String rootPath = "";

    /** The character sequence that splits the training data and the class in the input file. */
    private String separationString = " ";

    /** What percentage of the dataset should be used for training (0,100]. */
    private int usePercentTraining = 100;

    /** Whether the dataset is a column formatted file for Named Entity Recognition. */
    private boolean columnNER = false;

    /**
     * Whether the first field that is separated by the separation string links to a document or is the document itself.
     */
    private boolean firstFieldLink = false;

    public Dataset() {
    }

    /**
     * Create a new copy of the given dataset.
     * 
     * @param dataset The dataset which should be copied.
     */
    public Dataset(Dataset dataset) {
        this.name = dataset.name;
        this.path = dataset.path;
        this.rootPath = dataset.rootPath;
        this.separationString = dataset.separationString;
        this.usePercentTraining = dataset.usePercentTraining;
        this.columnNER = dataset.columnNER;
        this.firstFieldLink = dataset.firstFieldLink;
    }

    public Dataset(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
        setRootPath(FileHelper.getFilePath(getPath()));
    }

    /**
     * Returns the path to the dataset, this is either the complete dataset if {@link isFirstFieldLink()} == false or
     * the index file of the dataset otherwise.
     * 
     * @return The path to the dataset or the index file.
     */
    public String getPath() {
        return path;
    }

    /**
     * Get the path to the root folder of the dataset.
     * 
     * @return The path to the root folder of the dataset.
     */
    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        if (rootPath.isEmpty()) {
            this.rootPath = "";
        } else {
            this.rootPath = FileHelper.addTrailingSlash(rootPath);
        }
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

    public void setColumnNER(boolean columnNER) {
        this.columnNER = columnNER;
    }

    public boolean isColumnNER() {
        return columnNER;
    }

    @Override
    public String toString() {
        return name;
    }

}