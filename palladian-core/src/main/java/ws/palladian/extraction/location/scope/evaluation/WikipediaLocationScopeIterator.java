package ws.palladian.extraction.location.scope.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.wikipedia.WikipediaPage;

/**
 * <p>
 * Iterator for the Wikipedia scope location dataset.
 * </p>
 * 
 * @author pk
 */
public final class WikipediaLocationScopeIterator implements Iterable<LocationDocument> {

    private final File datasetPath;
    private final File[] wikiPages;

    /**
     * <p>
     * Create a new Wikipedia scope dataset iterator where files are iterated in alphabetical order.
     * </p>
     * 
     * @param datasetPath The path to the dataset, which contains files with MediaWiki markup with file name extension
     *            ".mediawiki", not <code>null</code>.
     */
    public WikipediaLocationScopeIterator(File datasetPath) {
        this(datasetPath, false);
    }

    /**
     * <p>
     * Create a new Wikipedia scope dataset iterator.
     * </p>
     * 
     * @param datasetPath The path to the dataset, which contains files with MediaWiki markup with file name extension
     *            ".mediawiki", not <code>null</code>.
     * @param shuffle <code>true</code> to shuffle the files randomly (useful, in case the filenames have the pages'
     *            titles, but degrades reproducibility in later runs), <code>false</code> to keep the file names' order.
     */
    public WikipediaLocationScopeIterator(File datasetPath, boolean shuffle) {
        Validate.notNull(datasetPath, "datasetPath must not be null");
        if (!datasetPath.isDirectory()) {
            throw new IllegalArgumentException(datasetPath + " does not point to a directory.");
        }
        this.datasetPath = datasetPath;
        this.wikiPages = FileHelper.getFiles(datasetPath.getPath(), "mediawiki");
        if (this.wikiPages.length == 0) {
            throw new IllegalArgumentException("No wiki pages found at " + datasetPath + ".");
        }
        if (shuffle) {
            CollectionHelper.shuffle(this.wikiPages);
        }
    }

    @Override
    public Iterator<LocationDocument> iterator() {
        return new Iterator<LocationDocument>() {
            private int idx = 0;
            private final ProgressMonitor monitor = new ProgressMonitor(wikiPages.length, 1);

            @Override
            public boolean hasNext() {
                return idx < wikiPages.length;
            }

            @Override
            public LocationDocument next() {
                monitor.incrementAndPrintProgress();
                File currentFile = wikiPages[idx++];
                String markupContent;
                try {
                    markupContent = FileHelper.readFileToString(currentFile);
                } catch (IOException e) {
                    throw new IllegalStateException("Could not read '" + currentFile + "': " + e.getMessage(), e);
                }
                WikipediaPage page = new WikipediaPage(0, 0, StringUtils.EMPTY, markupContent);
                GeoCoordinate scope = page.getCoordinate();
                if (scope != null) {
                    // save some memory, we don't need all that additional information in MarkupGeoCoordinate
                    scope = new ImmutableGeoCoordinate(scope.getLatitude(), scope.getLongitude());
                }
                Location scopeLocation = new ImmutableLocation(-1, LocationDocument.UNDETERMINED,
                        LocationType.UNDETERMINED, scope, null);
                return new LocationDocument(currentFile.getName(), page.getCleanText(), null, scopeLocation);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WikipediaLocationScopeIterator [datasetPath=");
        builder.append(datasetPath);
        builder.append(", numFiles=");
        builder.append(wikiPages.length);
        builder.append("]");
        return builder.toString();
    }

}
