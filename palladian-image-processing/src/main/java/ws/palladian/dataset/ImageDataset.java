package ws.palladian.dataset;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author David Urbansky
 */
public class ImageDataset {

    public static final int TRAIN = 1;
    public static final int TEST = 2;

    private String name;
    private String basePath;
    private String folderedPath;
    private String separator;
    private String trainFilePath;
    private String testFilePath;
    private int numberOfClasses;
    private List<String> classNames = new ArrayList<>();

    public ImageDataset(File dataset) throws IOException, JsonException {
        JsonObject json = new JsonObject(FileHelper.readFileToString(dataset));
        System.out.println(json);
        this.basePath = dataset.getParentFile().getAbsolutePath() + File.separator;
        this.folderedPath = basePath + json.tryGetString("folderedPath");
        this.separator = Optional.ofNullable(json.tryGetString("separator")).orElse("\t");
        this.trainFilePath = json.tryGetString("train");
        this.testFilePath = json.tryGetString("test");
        this.name = Optional.ofNullable(json.tryGetString("name")).orElse("unnamed");
        this.numberOfClasses = json.getInt("numberOfClasses");
    }

    public List<String> getClassNames() {
        if (classNames.isEmpty()) {
            // get possible class names from the folderedPath
            File[] files = new File(getFolderedPath()).listFiles();
            for (File file : files) {
                classNames.add(file.getName());
            }
        }
        return classNames;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumberOfClasses() {
        return numberOfClasses;
    }

    public void setNumberOfClasses(int numberOfClasses) {
        this.numberOfClasses = numberOfClasses;
    }

    public String getFolderedPath() {
        return folderedPath;
    }

    public void setFolderedPath(String folderedPath) {
        this.folderedPath = folderedPath;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getTrainFilePath() {
        return trainFilePath;
    }

    public void setTrainFilePath(String trainFilePath) {
        this.trainFilePath = trainFilePath;
    }

    public String getTestFilePath() {
        return testFilePath;
    }

    public void setTestFilePath(String testFilePath) {
        this.testFilePath = testFilePath;
    }

    /**
     * Get the file with the data to train.
     *
     * @return
     */
    public File getTrainFile() {
        return new File(basePath + trainFilePath);
    }

    /**
     * Get the file with data to test.
     *
     * @return
     */
    public File getTestFile() {
        return new File(basePath + testFilePath);
    }

    /**
     * Get the file with the extracted features for the training data.
     *
     * @return
     */
    public File getTrainFeaturesFile() {
        return new File(basePath + "train-features.csv");
    }

    /**
     * Get the file with the extracted features for the testing data.
     *
     * @return
     */
    public File getTestFeaturesFile() {
        return new File(basePath + "test-features.csv");
    }
}
