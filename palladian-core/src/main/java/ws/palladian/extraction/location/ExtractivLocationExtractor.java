package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.Map;

import ws.palladian.extraction.entity.tagger.ExtractivNer;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * {@link LocationExtractor} based on Extractiv (for more information see {@link ExtractivNer}).
 * </p>
 * 
 * @author Philipp Katz
 */
public final class ExtractivLocationExtractor extends MappingLocationExtractor {

    private static final Map<String, LocationType> MAPPING;

    static {
        Map<String, LocationType> temp = CollectionHelper.newHashMap();
        temp.put("ADDRESS", LocationType.UNDETERMINED);
        temp.put("AIRPORT", LocationType.POI);
        temp.put("BRIDGE", LocationType.POI);
        temp.put("BUILDING", LocationType.POI);
        temp.put("CANAL", LocationType.LANDMARK);
        temp.put("CITY", LocationType.CITY);
        temp.put("CONTINENT", LocationType.CONTINENT);
        temp.put("COUNTRY", LocationType.COUNTRY);
        temp.put("COUNTY", LocationType.UNIT);
        temp.put("EDUCATIONAL_ORG", LocationType.POI);
        temp.put("GULF", LocationType.LANDMARK);
        temp.put("HEMISPHERE", LocationType.REGION);
        temp.put("HOSPITAL", LocationType.POI);
        temp.put("INTERNATIONAL_REGION", LocationType.REGION);
        temp.put("ISLAND", LocationType.LANDMARK);
        temp.put("LAKE", LocationType.LANDMARK);
        temp.put("LAND_REGION", LocationType.REGION);
        temp.put("LOCATION", LocationType.UNDETERMINED); // ???
        temp.put("MEDICAL_FACILITY", LocationType.POI);
        temp.put("MOUNTAIN", LocationType.LANDMARK);
        temp.put("MOUNTAINRANGE", LocationType.LANDMARK);
        temp.put("OCEAN", LocationType.LANDMARK);
        temp.put("PLANET", LocationType.REGION);
        temp.put("RESTAURANT", LocationType.POI);
        temp.put("RIVER", LocationType.LANDMARK);
        temp.put("ROAD", LocationType.STREET);
        temp.put("SETTLEMENT", LocationType.REGION); // ???
        temp.put("SEA", LocationType.LANDMARK);
        temp.put("STADIUM", LocationType.POI);
        temp.put("UNIVERSITY", LocationType.POI);
        temp.put("US_STATE", LocationType.UNIT);
        temp.put("VALLEY", LocationType.LANDMARK);
        temp.put("WATER_BODY", LocationType.LANDMARK);
        temp.put("WORLDHERITAGESITE", LocationType.LANDMARK);
        MAPPING = Collections.unmodifiableMap(temp);
    }

    public ExtractivLocationExtractor() {
        super(new ExtractivNer(), MAPPING);
    }

}
