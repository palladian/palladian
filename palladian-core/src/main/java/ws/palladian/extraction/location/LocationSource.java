package ws.palladian.extraction.location;

import java.util.List;

public interface LocationSource {

    List<Location> retrieveLocations(String locationName);
    Location retrieveLocation(int locationId);
    List<Location> getHierarchy(Location location);
    
    // for writeble implementations only
    void save(Location location);
    void addHierarchy(int childId, int parentId, String type);

}
