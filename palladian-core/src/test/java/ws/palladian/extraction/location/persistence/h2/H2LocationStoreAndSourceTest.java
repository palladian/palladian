package ws.palladian.extraction.location.persistence.h2;

import java.io.File;
import java.io.IOException;

import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.persistence.AbstractLocationStoreTest;
import ws.palladian.extraction.location.persistences.h2.H2LocationSource;
import ws.palladian.extraction.location.persistences.h2.H2LocationStore;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.io.FileHelper;

public class H2LocationStoreAndSourceTest extends AbstractLocationStoreTest {
	private static int idx = 0;
	private File tempFileName;

	@Override
	protected LocationStore createLocationStore() throws IOException {
		File tempDir = FileHelper.getTempDir();
		tempFileName = new File(tempDir, "location_db_test_" + (idx++) + ".mv.db");
		return H2LocationStore.create(tempFileName);
	}

	@Override
	protected LocationSource createLocationSource() {
		return H2LocationSource.open(tempFileName);
	}
}
