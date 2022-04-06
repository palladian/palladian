package ws.palladian.extraction.location.persistence;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.store.FSDirectory;

import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.io.FileHelper;

public class LuceneLocationSourceTest extends AbstractLocationStoreTest {

    private final File indexFile = FileHelper.getTempFile();

    @Override
    public LocationStore createLocationStore() {
        return new LuceneLocationStore(indexFile);
    }

    @Override
    public LocationSource createLocationSource() {
        try {
            return new LuceneLocationSource(FSDirectory.open(indexFile.toPath()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
