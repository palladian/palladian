package ws.palladian.extraction.location.persistence;

import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.sources.LocationStore;

public class InMemoryLocationStoreTest extends AbstractLocationStoreTest {
    
    private InMemoryLocationStore store;

    @Override
    public LocationStore createLocationStore() {
        store = new InMemoryLocationStore(10);
        return store;
    }

    @Override
    public LocationSource createLocationSource() {
        return store;
    }
    
    @Override
    public void testGetLocationByRadius() {
        // nop
    }

}
