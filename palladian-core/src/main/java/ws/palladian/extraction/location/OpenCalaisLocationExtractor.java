package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.entity.tagger.OpenCalaisNer;
import ws.palladian.helper.collection.CollectionHelper;

public class OpenCalaisLocationExtractor extends WebBasedLocationExtractor {

    static {
        Map<String, LocationType> temp = CollectionHelper.newHashMap();
        temp.put("continent", LocationType.CONTINENT);
        temp.put("city", LocationType.CITY);
        temp.put("country", LocationType.COUNTRY);
        temp.put("facility", LocationType.POI);
        temp.put("naturalfeature", LocationType.LANDMARK);
        temp.put("region", LocationType.REGION);
        temp.put("provinceorstate", LocationType.UNIT);
        LOCATION_MAPPING = Collections.unmodifiableMap(temp);
    }

    public OpenCalaisLocationExtractor(String apiKey) {
        super(new OpenCalaisNer(apiKey));
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        OpenCalaisLocationExtractor alchemyLocationExtractor = new OpenCalaisLocationExtractor("get your own key");
        List<Location> detectedLocations = alchemyLocationExtractor
                .detectLocations("Dresden (Saxony) and Berlin are cities in Germany which lies in Europe on planet Earth, the middle east is somewhere else");
        CollectionHelper.print(detectedLocations);
    }

}
