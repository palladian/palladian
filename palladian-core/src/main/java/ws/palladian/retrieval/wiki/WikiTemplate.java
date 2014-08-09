package ws.palladian.retrieval.wiki;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoUtils;

/**
 * <p>
 * Template (infobox, geobox, etc.) on a Wikipedia page.
 * </p>
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Help:Template">Help:Template</a>
 * @author katz
 */
public class WikiTemplate {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiTemplate.class);

    private final String name;
    private final Map<String, String> content;

    public WikiTemplate(String name, Map<String, String> content) {
        this.name = name;
        this.content = content;
    }

    /**
     * @return The name of this template, in case it is a infobox or a geobox, that value is trimmed (e.g. type
     *         <code>geobox|river</code> returns <code>river</code>.
     * @deprecated Prefer getting the complete template name using {@link #getTemplateName()}.
     */
    @Deprecated
    public String getName() {
        if (name == null) {
            return null;
        }
        return name.replaceAll("^(?:infobox\\s+|geobox\\|)", "").toLowerCase();
    }

    public String getTemplateName() {
        return name;
    }

    /**
     * @param key The key to retrieve.
     * @return String value for the key, or <code>null</code> in case no entry exists.
     */
    public String getEntry(String key) {
        return content.get(key);
    }

    /**
     * @param keys The names of the keys to retrieve.
     * @return First matching value for the keys, or <code>null</code> in case no entry exists.
     */
    public String getEntry(String... keys) {
        return CollectionHelper.getTrying(content, keys);
    }

    /**
     * @param keys The names of the keys to retrieve.
     * @return All matching values for the keys, or an empty {@link List} in case no entries exist.
     */
    public List<String> getEntries(String... keys) {
        List<String> entries = CollectionHelper.newArrayList();
        for (String key : keys) {
            entries.add(getEntry(key));
        }
        return entries;
    }

    public int size() {
        return content.size();
    }

    /**
     * <p>
     * Extract coordinates which typically occur in a Wikipedia infobox.
     * </p>
     * 
     * @return Set with all extracted {@link MarkupCoordinate}s, or an empty Set, never <code>null</code>.
     */
    public Set<MarkupCoordinate> getCoordinates() {
        Set<MarkupCoordinate> coordinates = CollectionHelper.newHashSet();

        String display = getEntry("coordinates_display");
        String type = getEntry("coordinates_type");

        // try lat/long_deg/min_sec
        try {
            String latDeg = getEntry("lat_deg", "latd", "lat_d", "lat_degrees", "source_lat_d", "mouth_lat_d");
            String lngDeg = getEntry("lon_deg", "longd", "long_d", "long_degrees", "source_long_d", "mouth_long_d");
            if (StringUtils.isNotBlank(latDeg) && StringUtils.isNotBlank(lngDeg)) {
                String latMin = getEntry("lat_min", "latm", "lat_m", "lat_minutes", "source_lat_m", "mouth_lat_m");
                String latSec = getEntry("lat_sec", "lats", "lat_s", "lat_seconds", "source_lat_s", "mouth_lat_s");
                String lngMin = getEntry("lon_min", "longm", "long_m", "long_minutes", "source_long_m", "mouth_long_m");
                String lngSec = getEntry("lon_sec", "longs", "long_s", "long_seconds", "source_long_s", "mouth_long_s");
                String latNS = getEntry("latNS", "lat_direction", "lat_NS", "source_lat_NS", "mouth_lat_NS");
                String lngEW = getEntry("longEW", "long_direction", "long_EW", "source_long_EW", "mouth_long_EW");
                double lat = MediaWikiUtil.parseComponents(latDeg, latMin, latSec, latNS);
                double lng = MediaWikiUtil.parseComponents(lngDeg, lngMin, lngSec, lngEW);
                coordinates.add(new MarkupCoordinate(lat, lng, display, type));
            }
        } catch (Exception e) {
            LOGGER.warn("Error while parsing: {}", e.getMessage());
        }

        // try all-in-one format
        String lat = getEntry("latitude");
        String lng = getEntry("longitude");
        if (StringUtils.isNotBlank(lat) && StringUtils.isNotBlank(lng)) {
            try {
                // try decimal format
                coordinates.add(new MarkupCoordinate(Double.parseDouble(lat), Double.parseDouble(lng), display, type));
            } catch (Exception e) {
                try {
                    // try DMS format
                    coordinates
                            .add(new MarkupCoordinate(GeoUtils.parseDms(lat), GeoUtils.parseDms(lng), display, type));
                } catch (Exception e1) {
                    // try decdeg markup
                    try {
                        coordinates.add(new MarkupCoordinate(MediaWikiUtil.parseDecDeg(lat), MediaWikiUtil
                                .parseDecDeg(lng), display, type));
                    } catch (Exception e2) {
                        LOGGER.warn("Error while parsing: {} and/or {}: {}", lat, lng, e2.getMessage());
                    }
                }
            }
        }
        return coordinates;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WikipediaInfobox [name=");
        builder.append(name);
        builder.append(", content=");
        builder.append(content);
        builder.append("]");
        return builder.toString();
    }

}