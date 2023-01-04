package ws.palladian.classification.text.evaluation;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.TextValue;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.CloseableIteratorAdapter;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineIterator;

import java.io.File;
import java.util.Iterator;

import static ws.palladian.classification.text.PalladianTextClassifier.VECTOR_TEXT_IDENTIFIER;

/**
 * <p>
 * An {@link Iterator} over {@link Dataset}s that are too large to keep in memory.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public class LargeTextDatasetIterator extends AbstractDataset {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LargeTextDatasetIterator.class);

    private final String name;
    private final int lineCount;
    private final String filePath;
    private final String separationString;
    private final boolean isFirstFieldLink;
    private final String datasetRootPath;
    private final int learningIndex;
    private final int classIndex;
    private final boolean markBeginningAndEnd;

    public LargeTextDatasetIterator(Dataset dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        this.name = dataset.getName();
        this.filePath = dataset.getPath();
        this.lineCount = FileHelper.getNumberOfLines(dataset.getPath());
        this.separationString = dataset.getSeparationString();
        this.isFirstFieldLink = dataset.isFirstFieldLink();
        this.datasetRootPath = dataset.getRootPath();
        this.learningIndex = dataset.getLearningIndex();
        this.classIndex = dataset.getClassIndex();
        this.markBeginningAndEnd = dataset.isMarkBeginningAndEnd();
    }

    public LargeTextDatasetIterator(String filePath, String separator, boolean firstFieldLink) {
        Validate.notNull(filePath, "filePath must not be null");
        Validate.notEmpty(separator, "separator must not be empty");
        this.filePath = filePath;
        this.lineCount = FileHelper.getNumberOfLines(filePath);
        this.separationString = separator;
        this.isFirstFieldLink = firstFieldLink;
        this.datasetRootPath = FileHelper.getFilePath(filePath);
        this.name = FileHelper.getFileName(datasetRootPath);
        this.learningIndex = 0;
        this.classIndex = 1;
        this.markBeginningAndEnd = false;
    }

    @Override
    public CloseableIterator<Instance> iterator() {
        LineIterator lineIterator = new LineIterator(new File(filePath));
        Iterator<Instance> iterator = new Iterator<Instance>() {
            @Override
            public boolean hasNext() {
                return lineIterator.hasNext();
            }

            @Override
            public Instance next() {
                String nextLine = lineIterator.next();
                String[] parts = nextLine.split(separationString);
                if (parts.length < 2) {
                    LOGGER.error("problem with line: " + nextLine);
                    throw new RuntimeException();
                }

                String learningText;
                if (isFirstFieldLink) {
                    learningText = FileHelper.tryReadFileToString(datasetRootPath + parts[0]);
                } else {
                    learningText = parts[learningIndex];
                }

                if (markBeginningAndEnd) {
                    learningText = "_" + learningText + "_";
                }

                String instanceCategory = parts[classIndex];
                Instance instance;
                try {
                    instance = new InstanceBuilder().setText(learningText).create(instanceCategory);
                } catch (Exception e) {
                    LOGGER.error("problem with line: " + parts[0] + " " + parts[1]);
                    throw e;
                }
                return instance;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Modifications are not allowed.");
            }
        };
        return new CloseableIteratorAdapter<>(iterator);
    }

    @Override
    public FeatureInformation getFeatureInformation() {
        return new FeatureInformationBuilder().set(VECTOR_TEXT_IDENTIFIER, TextValue.class).create();
    }

    @Override
    public long size() {
        return lineCount;
    }
}
