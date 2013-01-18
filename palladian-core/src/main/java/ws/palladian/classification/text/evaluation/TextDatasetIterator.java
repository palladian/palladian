package ws.palladian.classification.text.evaluation;

import java.util.Iterator;
import java.util.List;

import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.ClassifiedTextDocument;
import ws.palladian.processing.Trainable;

/**
 * <p>
 * An {@link Iterator} over {@link Dataset}s.
 * </p>
 * 
 * @author Philipp Katz
 */
public class TextDatasetIterator implements Iterable<ClassifiedTextDocument> {

    private final List<String> fileLines;
    private final String separationString;
    private final boolean isFirstFieldLink;
    private final String datasetRootPath;

    private TextDatasetIterator(Dataset dataset) {
        this.fileLines = FileHelper.readFileToArray(dataset.getPath());
        this.separationString = dataset.getSeparationString();
        this.isFirstFieldLink = dataset.isFirstFieldLink();
        this.datasetRootPath = dataset.getRootPath();
    }

    public static TextDatasetIterator createIterator(Dataset dataset) {
        return new TextDatasetIterator(dataset);
    }

    @Override
    public Iterator<ClassifiedTextDocument> iterator() {
        final Iterator<String> lineIterator = fileLines.iterator();
        final int totalLines = fileLines.size();
        final StopWatch stopWatch = new StopWatch();

        return new Iterator<ClassifiedTextDocument>() {

            int counter = 0;

            @Override
            public boolean hasNext() {
                return lineIterator.hasNext();
            }

            @Override
            public ClassifiedTextDocument next() {
                String nextLine = lineIterator.next();
                counter++;
                String[] parts = nextLine.split(separationString);
                if (parts.length != 2) {
                    // XXX how to handle?
                }

                String learningText;
                if (isFirstFieldLink) {
                    learningText = FileHelper.readFileToString(datasetRootPath + parts[0]);
                } else {
                    learningText = parts[0];
                }
                String instanceCategory = parts[1];
                ProgressHelper.printProgress(counter, totalLines, 1., stopWatch);
                return new ClassifiedTextDocument(instanceCategory, learningText);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Modifications are not allowed.");
            }
        };
    }

    public static void main(String[] args) {
        String JRC_TRAIN_FILE = "/Users/pk/Dropbox/Uni/Datasets/Wikipedia76Languages/languageDocumentIndex_random1000_train.txt";
        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath(JRC_TRAIN_FILE);

        TextDatasetIterator datasetIterator = createIterator(dataset);
        for (Trainable trainable : datasetIterator) {
            assert (trainable != null);
        }
    }

}
