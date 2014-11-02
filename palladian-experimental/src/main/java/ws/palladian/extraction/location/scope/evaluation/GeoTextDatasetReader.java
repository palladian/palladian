package ws.palladian.extraction.location.scope.evaluation;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationBuilder;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineIterator;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Iterator for the GeoText scope dataset. See
 * "<a href="http://www.cs.cmu.edu/~nasmith/papers/eisenstein+oconnor+smith+xing.emnlp10.pdf"> A Latent Variable Model
 * for Geographic Lexical Variation.</a>" Jacob Eisenstein, Brendan O'Connor, Noah A. Smith, and Eric P. Xing.
 * </p>
 * 
 * @author pk
 */
public final class GeoTextDatasetReader implements Iterable<LocationDocument> {

    /**
     * Subset of the dataset as defined by the authors (see README.txt in dataset directory.)
     * 
     * @author pk
     */
    public static enum SubSet {
        TRAIN(1, 2, 3), DEV(4), TEST(5);
        int[] folds;

        SubSet(int... folds) {
            this.folds = folds;
        }

        public boolean contains(int fold) {
            for (int currentFold : folds) {
                if (currentFold == fold) {
                    return true;
                }
            }
            return false;
        }

    }

    public static enum Combination {
        /** No combination, treat each Tweet individually. */
        SINGLE,
        /** Combine all user's Tweets to one document; this is way the evaluation was done by Eisenschwein et al. */
        USER;
    }

    private final File fullTextFile;

    private final int numTotalEntries;

    private final SubSet subSet;

    private final Combination combination;

    /**
     * Create a new {@link GeoTextDatasetReader}.
     * 
     * @param fullTextFile The path to the file "full_text.txt" in the dataset, not <code>null</code>.
     * @param subSet Limit to the specified subset (or set to <code>null</code> to iterate the whole dataset).
     * @param combination Specify whether to iterate each Tweet separately, or combine by user.
     */
    public GeoTextDatasetReader(File fullTextFile, SubSet subSet, Combination combination) {
        Validate.notNull(fullTextFile, "fullTextFile must not be null");
        Validate.notNull(combination, "combination must not be null");
        this.fullTextFile = fullTextFile;
        this.numTotalEntries = FileHelper.getNumberOfLines(fullTextFile);
        this.subSet = subSet;
        this.combination = combination;
    }

    @Override
    public Iterator<LocationDocument> iterator() {
        Iterator<LocationDocument> iterator = new DatasetIterator(fullTextFile, numTotalEntries, subSet);
        if (combination == Combination.USER) {
            iterator = new CombininingIterator(iterator);
        }
        return iterator;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GeoTextDatasetScopeIterator [fullTextFile=").append(fullTextFile);
        if (subSet != null) {
            builder.append(", subSet=").append(subSet);
        }
        builder.append("]");
        return builder.toString();
    }

    private static final class DatasetIterator extends AbstractIterator<LocationDocument> {
        final LineIterator lineIterator;
        final ProgressReporter progress;
        final SubSet subSet;

        DatasetIterator(File fullTextFile, int numTotalEntries, SubSet subSet) {
            this.lineIterator = new LineIterator(fullTextFile);
            this.progress = new ProgressMonitor();
            String taskName = "Reading GeoText";
            if (subSet != null) {
                taskName += " " + subSet;
            }
            this.progress.startTask(taskName, numTotalEntries);
            this.subSet = subSet;
        }

        @Override
        protected LocationDocument getNext() throws Finished {
            while (lineIterator.hasNext()) {
                progress.increment();
                String line = lineIterator.next();
                String[] lineSplit = line.split("\\t");
                if (lineSplit.length != 6) {
                    throw new IllegalStateException("Illegal format: '" + line + "', expected 6 columns, got "
                            + lineSplit.length + ".");
                }
                String userName = lineSplit[0];
                long userId = Long.parseLong(userName.replace("USER_", ""), 16);
                int fold = getFold(userId);
                if (subSet == null || subSet.contains(fold)) {
                    double lat = Double.parseDouble(lineSplit[3]);
                    double lng = Double.parseDouble(lineSplit[4]);
                    String text = lineSplit[5];
                    GeoCoordinate scope = new ImmutableGeoCoordinate(lat, lng);
                    Location scopeLocation = new ImmutableLocation(-1, LocationDocument.UNDETERMINED,
                            LocationType.UNDETERMINED, scope, null);
                    String documentName = userName + "#" + StringHelper.sha1(text);
                    return new LocationDocument(documentName, text, null, scopeLocation);
                }
            }
            throw FINISHED;
        }

        private static int getFold(long userId) {
            long fold = userId % 5;
            return fold == 0 ? 5 : (int)fold;
        }
    }

    /**
     * Combine all Tweets of a user to one document (Tweets must be ordered by user, which is the case for the dataset).
     * 
     * @author pk
     */
    private static final class CombininingIterator extends AbstractIterator<LocationDocument> {

        private final Iterator<LocationDocument> wrapped;

        private StringBuilder buffer = new StringBuilder();
        private String userName;
        private GeoCoordinate coordinate;

        CombininingIterator(Iterator<LocationDocument> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        protected LocationDocument getNext() throws Finished {
            while (wrapped.hasNext()) {
                LocationDocument currentDocument = wrapped.next();
                LocationDocument combinedDocument = null;
                String currentName = currentDocument.getFileName().split("#")[0];
                if (userName != null && !currentName.equals(userName)) {
                    combinedDocument = createAndClear();
                }
                if (coordinate == null) {
                    coordinate = currentDocument.getMainLocation().getCoordinate();
                    userName = currentName;
                }
                buffer.append(currentDocument.getText()).append('\n');
                if (combinedDocument != null) {
                    return combinedDocument;
                }
            }
            if (buffer.toString().length() > 0) { // last document in the buffer
                return createAndClear();
            }
            throw FINISHED;
        }

        private LocationDocument createAndClear() {
            try {
                LocationBuilder builder = new LocationBuilder();
                builder.setPrimaryName(LocationDocument.UNDETERMINED);
                builder.setCoordinate(coordinate);
                return new LocationDocument(userName, buffer.toString().trim(), null, builder.create());
            } finally {
                buffer = new StringBuilder();
                userName = null;
                coordinate = null;
            }
        }
    }

    public static void main(String[] args) {
        String file = "/Users/pk/Desktop/GeoText.2010-10-12/full_text.txt";
        GeoTextDatasetReader dataset = new GeoTextDatasetReader(new File(file), null, Combination.USER);
        Iterator<LocationDocument> iterator = dataset.iterator();
        System.out.println(CollectionHelper.count(iterator));
        
        System.exit(0);
        while (iterator.hasNext()) {
            LocationDocument doc = iterator.next();
            System.out.println(doc);
            // System.out.println(doc.getText());
        }
    }

}
