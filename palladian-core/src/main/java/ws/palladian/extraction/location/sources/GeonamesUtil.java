package ws.palladian.extraction.location.sources;

import java.util.Collections;
import java.util.Map;

import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Utility class which takes care of mapping between Geonames feature class and feature code and Palladian's
 * {@link LocationType}.
 * </p>
 * 
 * @see <a href="http://download.geonames.org/export/dump/featureCodes_en.txt">List with feature codes</a>
 * @see <a href="http://www.geonames.org/export/codes.html">List with feature codes (more clear)</a>
 * @author Philipp Katz
 */
final class GeonamesUtil {

    /** Mapping from feature codes from the dataset to LocationType. */
    private static final Map<String, LocationType> FEATURE_MAPPING;

    static {
        Map<String, LocationType> temp = CollectionHelper.newHashMap();
        temp.put("A", LocationType.UNIT);
        temp.put("A.PCL", LocationType.COUNTRY);
        temp.put("A.PCLD", LocationType.COUNTRY);
        temp.put("A.PCLF", LocationType.COUNTRY);
        temp.put("A.PCLH", LocationType.COUNTRY);
        temp.put("A.PCLI", LocationType.COUNTRY);
        temp.put("A.PCLIX", LocationType.COUNTRY);
        temp.put("A.PCLS", LocationType.COUNTRY);
        temp.put("H", LocationType.LANDMARK);
        temp.put("L", LocationType.POI);
        temp.put("L.AREA", LocationType.REGION);
        temp.put("L.COLF", LocationType.REGION);
        temp.put("L.CONT", LocationType.CONTINENT);
        temp.put("L.RGN", LocationType.REGION);
        temp.put("L.RGNE", LocationType.REGION);
        temp.put("L.RGNH", LocationType.REGION);
        temp.put("L.RGNL", LocationType.REGION);
        temp.put("P", LocationType.CITY);
        temp.put("R", LocationType.POI);
        temp.put("S", LocationType.POI);
        temp.put("T", LocationType.LANDMARK);
        temp.put("U", LocationType.LANDMARK);
        temp.put("U.BDLU", LocationType.REGION);
        temp.put("U.PLNU", LocationType.REGION);
        temp.put("U.PRVU", LocationType.REGION);
        temp.put("V", LocationType.POI);
        FEATURE_MAPPING = Collections.unmodifiableMap(temp);
    }

    static LocationType mapType(String featureClass, String featureCode) {
        // first, try lookup by full feature code (e.g. 'L.CONT')
        LocationType locationType = FEATURE_MAPPING.get(String.format("%s.%s", featureClass, featureCode));
        if (locationType != null) {
            return locationType;
        }
        // second, try lookup only be feature class (e.g. 'A')
        locationType = FEATURE_MAPPING.get(featureClass);
        if (locationType != null) {
            return locationType;
        }
        return LocationType.UNDETERMINED;
    }

    private GeonamesUtil() {
        // do not construct me.
    }

}
