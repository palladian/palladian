package ws.palladian.classification.text.evaluation;

import java.util.Iterator;
import java.util.List;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.ClassifiedTextDocument;
import ws.palladian.processing.Trainable;

public class TextDatasetIterator implements Iterable<Trainable> {
    
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
    public Iterator<Trainable> iterator() {
        final Iterator<String> lineIterator = fileLines.iterator();
        return new Iterator<Trainable>() {
            @Override
            public boolean hasNext() {
                return lineIterator.hasNext();
            }

            @Override
            public Trainable next() {
                String nextLine = lineIterator.next();
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
            System.out.println(trainable);
        }
    }

}
