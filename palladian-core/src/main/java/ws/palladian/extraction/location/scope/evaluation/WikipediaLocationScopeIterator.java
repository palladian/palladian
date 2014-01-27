package ws.palladian.extraction.location.scope.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.helper.ProgressMonitor;
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

    private static final String UNDETERMINED = "undetermined";
    private final File datasetPath;
    private final File[] wikiPages;

    public WikipediaLocationScopeIterator(File datasetPath) {
        Validate.notNull(datasetPath, "datasetPath must not be null");
        if (!datasetPath.isDirectory()) {
            throw new IllegalArgumentException(datasetPath + " does not point to a directory.");
        }
        this.datasetPath = datasetPath;
        this.wikiPages = FileHelper.getFiles(datasetPath.getPath(), "mediawiki");
        if (this.wikiPages.length == 0) {
            throw new IllegalArgumentException("No wiki pages found at " + datasetPath + ".");
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
                Location scopeLocation = new ImmutableLocation(-1, UNDETERMINED, LocationType.UNDETERMINED, scope, null);
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
