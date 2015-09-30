package ws.palladian.classification.text.evaluation;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * An {@link Iterator} over {@link Dataset}s.
 * </p>
 * 
 * @author Philipp Katz
 */
public class TextDatasetIterator implements Iterable<Instance> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TextDatasetIterator.class);

    private final String name;
    private final List<String> fileLines;
    private final String separationString;
    private final boolean isFirstFieldLink;
    private final String datasetRootPath;

    public TextDatasetIterator(Dataset dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        this.name = dataset.getName();
        this.fileLines = FileHelper.readFileToArray(dataset.getPath());
        this.separationString = dataset.getSeparationString();
        this.isFirstFieldLink = dataset.isFirstFieldLink();
        this.datasetRootPath = dataset.getRootPath();
    }

    public TextDatasetIterator(String filePath, String separator, boolean firstFieldLink) {
        Validate.notNull(filePath, "filePath must not be null");
        Validate.notEmpty(separator, "separator must not be empty");
        this.fileLines = FileHelper.readFileToArray(filePath);
        this.separationString = separator;
        this.isFirstFieldLink = firstFieldLink;
        this.datasetRootPath = FileHelper.getFilePath(filePath);
        this.name = FileHelper.getFileName(datasetRootPath);
    }

    @Override
    public Iterator<Instance> iterator() {
        final Iterator<String> lineIterator = fileLines.iterator();
        final int totalLines = fileLines.size();
        final ProgressMonitor progressMonitor = new ProgressMonitor();
        progressMonitor.startTask("Dataset: " + name, totalLines);

        return new Iterator<Instance>() {

            @Override
            public boolean hasNext() {
                return lineIterator.hasNext();
            }

            @Override
            public Instance next() {
                String nextLine = lineIterator.next();
                String[] parts = nextLine.split(separationString);
                if (parts.length != 2) {
                    // XXX how to handle?
                }

                String learningText;
                if (isFirstFieldLink) {
                    learningText = FileHelper.tryReadFileToString(datasetRootPath + parts[0]);
                } else {
                    learningText = parts[0];
                }
                String instanceCategory = parts[1];
                progressMonitor.increment();
                Instance instance;
                try {
                    instance = new InstanceBuilder().setText(learningText).create(instanceCategory);
                } catch(Exception e) {
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
    }

}
