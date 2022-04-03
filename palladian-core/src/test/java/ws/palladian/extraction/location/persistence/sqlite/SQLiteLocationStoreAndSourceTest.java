package ws.palladian.extraction.location.persistence.sqlite;

import java.io.File;
import java.io.IOException;

import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.persistence.AbstractLocationStoreTest;
import ws.palladian.extraction.location.persistences.sqlite.SQLiteLocationSource;
import ws.palladian.extraction.location.persistences.sqlite.SQLiteLocationStore;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.io.FileHelper;

public class SQLiteLocationStoreAndSourceTest extends AbstractLocationStoreTest {
	private static int idx = 0;
	private File tempFileName;

	@Override
	protected LocationStore createLocationStore() throws IOException {
		File tempDir = FileHelper.getTempDir();
		tempFileName = new File(tempDir, "location_db_test_" + (idx++));
		return SQLiteLocationStore.create(tempFileName);
	}

	@Override
	protected LocationSource createLocationSource() {
		return SQLiteLocationSource.open(tempFileName);
	}
}
