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
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.CloseableIteratorAdapter;
import ws.palladian.helper.io.FileHelper;

import java.util.Iterator;
import java.util.List;

import static ws.palladian.classification.text.PalladianTextClassifier.VECTOR_TEXT_IDENTIFIER;

/**
 * <p>
 * An {@link Iterator} over {@link Dataset}s.
 * </p>
 *
 * @author Philipp Katz
 * @author David Urbansky
 *
 */
public class TextDatasetIterator extends AbstractDataset {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TextDatasetIterator.class);

    private final String name;
    private final List<String> fileLines;
    private final String separationString;
    private final boolean isFirstFieldLink;
    private final String datasetRootPath;
    private final int learningIndex;
    private final int classIndex;
    private final boolean markBeginningAndEnd;

    public TextDatasetIterator(Dataset dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        this.name = dataset.getName();
        this.fileLines = FileHelper.readFileToArray(dataset.getPath());
        this.separationString = dataset.getSeparationString();
        this.isFirstFieldLink = dataset.isFirstFieldLink();
        this.datasetRootPath = dataset.getRootPath();
        this.learningIndex = dataset.getLearningIndex();
        this.classIndex = dataset.getClassIndex();
        this.markBeginningAndEnd = dataset.isMarkBeginningAndEnd();
    }

    public TextDatasetIterator(String filePath, String separator, boolean firstFieldLink) {
        Validate.notNull(filePath, "filePath must not be null");
        Validate.notEmpty(separator, "separator must not be empty");
        this.fileLines = FileHelper.readFileToArray(filePath);
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
        final Iterator<String> lineIterator = fileLines.iterator();
        final int totalLines = fileLines.size();
        final ProgressMonitor progressMonitor = new ProgressMonitor();
        progressMonitor.startTask("Dataset: " + name, totalLines);

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
                    // XXX how to handle?
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
                progressMonitor.increment();
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
        return fileLines.size();
    }
}
