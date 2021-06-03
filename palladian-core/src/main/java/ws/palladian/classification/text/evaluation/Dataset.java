package ws.palladian.classification.text.evaluation;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.parser.json.JsonObject;

import java.util.Optional;

/**
 * <p>
 * A simple representation of a dataset.
 * </p>
 * <p>
 * Sample dataset json
 * <pre>
 * {
 * 		"name": "Intents",
 * 		"separator": "###",
 * 		"train_path": "train.csv",
 * 		"learningIndex": 0,
 * 		"classIndex": 1
 *        }
 * </pre>
 *
 * @author David Urbansky
 */
public class Dataset {
    /**
     * The name of the dataset to identify it.
     */
    private String name = "NONAME";

    /**
     * The path pointing to the dataset file.
     */
    private String path = "";

    /**
     * The path to the root folder of the dataset.
     */
    private String rootPath = "";

    /**
     * The character sequence that splits the training data and the class in the input file.
     */
    private String separationString = " ";

    /**
     * What percentage of the dataset should be used for training (0,100].
     */
    private int usePercentTraining = 100;

    /**
     * Whether the dataset is a column formatted file for Named Entity Recognition.
     */
    private boolean columnNer = false;

    /**
     * Whether the first field that is separated by the separation string links to a document or is the document itself.
     */
    private boolean firstFieldLink = false;

    /**
     * The index of the column with the data to learn from.
     */
    private int learningIndex = 0;

    /**
     * The index of the column with the target class.
     */
    private int classIndex = 1;

    public Dataset() {
    }

    public Dataset(JsonObject datasetJson) {
        setName(Optional.ofNullable(datasetJson.tryGetString("name")).orElse("NONAME"));
        setSeparationString(datasetJson.tryGetString("separator"));
        setPath(datasetJson.tryGetString("train_path"));
        setLearningIndex(Optional.ofNullable(datasetJson.tryGetInt("learningIndex")).orElse(0));
        setClassIndex(Optional.ofNullable(datasetJson.tryGetInt("classIndex")).orElse(1));

        // some datasets might point to files
        String source = datasetJson.tryGetString("source");
        if (source != null) {
            setFirstFieldLink(true);
            setRootPath(source);
        }
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
        this.columnNer = dataset.columnNer;
        this.firstFieldLink = dataset.firstFieldLink;
        this.learningIndex = dataset.learningIndex;
        this.classIndex = dataset.classIndex;
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
        if (rootPath.isEmpty()) {
            setRootPath(FileHelper.getFilePath(getPath()));
        }
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

    public void setColumnNer(boolean columnNer) {
        this.columnNer = columnNer;
    }

    public boolean isColumnNer() {
        return columnNer;
    }

    public int getLearningIndex() {
        return learningIndex;
    }

    public void setLearningIndex(int learningIndex) {
        this.learningIndex = learningIndex;
    }

    public int getClassIndex() {
        return classIndex;
    }

    public void setClassIndex(int classIndex) {
        this.classIndex = classIndex;
    }

    @Override
    public String toString() {
        return name;
    }

}