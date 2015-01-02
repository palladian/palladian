package ws.palladian.extraction.location.persistence;

import java.util.*;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationBuilder;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.SingleQueryLocationSource;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;

/**
 * <p>
 * In-memory location store; location lookup by name is pretty fast; ID lookup is slow, because the underlying array
 * needs to be iterated (this functionality is no commonly used though).
 * 
 * @author pk
 * @see <a href="http://stackoverflow.com/questions/10064422/java-on-memory-efficient-key-value-store">Foundation for
 *      code: Stack Overflow: Java On-Memory Efficient Key-Value Store</a>
 */
public final class InMemoryLocationStore extends SingleQueryLocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryLocationStore.class);

    private static final int NULL = 0;

    private final int[] keys;
    private final LocationContainer[] locations;
    private int size;

    public InMemoryLocationStore(int capacity) {
        keys = new int[capacity];
        locations = new LocationContainer[capacity];
    }

    /**
     * Initialize in-memory store from another {@link LocationSource} by copying all content.
     * 
     * @param source The location source from which to copy locations.
     */
    public InMemoryLocationStore(LocationSource source) {
        this(source.size() * 3); // load factor determined empirically
        ProgressMonitor progressMonitor = new ProgressMonitor(source.size(), 1);
        Iterator<Location> sourceIterator = source.getLocations();
        while (sourceIterator.hasNext()) {
            add(sourceIterator.next());
            progressMonitor.incrementAndPrintProgress();
        }
    }

    @Override
    public Collection<Location> getLocations(String locationName, Set<Language> languages) {
        int hash = normalizeName(locationName).hashCode();
        Validate.isTrue(hash != NULL, "key cannot be " + NULL);
        int index = indexFor(hash);
        Collection<Location> hits = new HashSet<>();
        while (keys[index] != NULL) {
            if (keys[index] == hash) {
                LocationContainer location = locations[index];
                if (location.hasName(locationName, languages)) {
                    hits.add(locations[index].createLocation());
                }
            }
            index = successor(index);
        }
        return hits;
    }

    private int indexFor(int key) {
        return Math.abs(key % keys.length);
    }

    private int successor(int index) {
        return (index + 1) % keys.length;
    }

    @Override
    public Location getLocation(int locationId) {
        for (LocationContainer location : locations) {
            if (location.id == locationId) {
                return location.createLocation();
            }
        }
        return null;
    }

    @Override
    public Iterator<Location> getLocations() {
        // FIXME iterator gives duplicates; only return locations here, where hash key matches primary name?
        return new AbstractIterator<Location>() {
            int idx = 0;

            @Override
            protected Location getNext() throws Finished {
                if (idx >= locations.length) {
                    throw new Finished();
                }
                return locations[idx++].createLocation();
            }
        };
    }

    @Override
    public int size() {
        return size;
    }

    public void add(Location location) {
        LocationContainer locationContainer = new LocationContainer(location);
        for (String name : location.collectAlternativeNames()) {
            add(name, locationContainer);
        }
    }

    private void add(String name, LocationContainer location) {
        int hash = normalizeName(name).hashCode();
        if (hash == NULL) {
            LOGGER.debug("Encountered {} key for {}; object was not inserted", NULL, name);
            return;
        }
        Validate.isTrue(size < keys.length, "map is full");

        int index = indexFor(hash);
        while (keys[index] != NULL) {
            index = successor(index);
        }
        keys[index] = hash;
        locations[index] = location;
        ++size;
    }

    private static String normalizeName(String name) {
        return StringUtils.stripAccents(name.toLowerCase());
    }

    private static boolean namesAreEqual(String primaryName, String locationName) {
        return normalizeName(primaryName).equals(normalizeName(locationName));
    }

    /**
     * More memory efficient storage than a {@link Location}. Avoids memory intensive collection and wrapper objects and
     * uses primitive data types as far as possible.
     * 
     * @author pk
     */
    private static final class LocationContainer {

        /** Character to separate languages. */
        private static final char LANG_SEPARATOR = '#';

        /** Character to separate names. */
        private static final char NAME_SEPARATOR = '§';

        private final int id;

        /**
         * Alternative names, encoded like "Berlin#de#en#§Berlino#it§...". Storing this data in a char[] is more memory
         * efficient than using one/multiple Strings. The format is like follows: First name in the string is always the
         * location's primary name, languages for a name are listed after the name, separated by # characters. An empty
         * string between two # characters means, the language is null. Each name and its language variant are separated
         * by § characters.
         */
        private final char[] nameData;

        /** Type of the location. */
        private final LocationType type;

        /** Population; value of -1 means, that the location has no population. */
        private final long population;

        /** IDs of the location's ancestors. */
        private final int[] ancestorIds;

        /** Latitude; value of NaN means, that the location has no latitude. */
        private final float lat;

        /** Longitude; value of NaN means, that the location has no longitude. */
        private final float lng;

        public LocationContainer(Location location) {
            Validate.notNull(location, "location must not be null");
            this.id = location.getId();
            this.nameData = encodeNames(location);
            this.type = location.getType();
            this.population = location.getPopulation() != null ? location.getPopulation() : -1;
            List<Integer> tempIds = location.getAncestorIds();
            if (tempIds.isEmpty()) {
                this.ancestorIds = null;
            } else {
                this.ancestorIds = new int[tempIds.size()];
                for (int i = 0; i < tempIds.size(); i++) {
                    this.ancestorIds[i] = tempIds.get(i);
                }
            }
            GeoCoordinate coordinate = location.getCoordinate();
            this.lat = coordinate != null ? (float)coordinate.getLatitude() : Float.NaN;
            this.lng = coordinate != null ? (float)coordinate.getLongitude() : Float.NaN;
        }

        /**
         * Encode all names of the location into a compressed char[].
         * 
         * @param location The location.
         * @return The char[] which contains all name information.
         */
        private static final char[] encodeNames(Location location) {
            StringBuilder builder = new StringBuilder();
            Set<String> temp = new LinkedHashSet<>();
            temp.add(location.getPrimaryName());
            for (AlternativeName altName : location.getAlternativeNames()) {
                temp.add(altName.getName());
            }
            boolean first = true;
            for (String nameString : temp) {
                if (first) {
                    first = false;
                } else {
                    builder.append(NAME_SEPARATOR);
                }
                builder.append(nameString);
                for (AlternativeName altName : location.getAlternativeNames()) {
                    if (altName.getName().equals(nameString)) {
                        builder.append(LANG_SEPARATOR);
                        Language language = altName.getLanguage();
                        String langString = language != null ? language.getIso6391() : StringUtils.EMPTY;
                        builder.append(langString);
                    }
                }
            }
            return builder.toString().toCharArray();
        }

        public Location createLocation() {
            LocationBuilder builder = new LocationBuilder();
            builder.setId(id);
            String[] split = StringUtils.split(new String(nameData), NAME_SEPARATOR);
            for (int i = 0; i < split.length; i++) {
                String[] nameTemp = StringUtils.splitPreserveAllTokens(split[i], LANG_SEPARATOR);
                String name = nameTemp[0];
                if (i == 0) {
                    builder.setPrimaryName(name);
                }
                for (int j = 1; j < nameTemp.length; j++) {
                    Language language = null;
                    if (nameTemp[j].length() > 1) {
                        language = Language.getByIso6391(nameTemp[j]);
                    }
                    builder.addAlternativeName(name, language);
                }
            }
            builder.setType(type);
            builder.setPopulation(population == -1 ? null : population);
            builder.setAncestorIds(ancestorIds);
            builder.setCoordinate(Float.isNaN(lat) ? null : new ImmutableGeoCoordinate(lat, lng));
            return builder.create();
        }

        public boolean hasName(String locationName, Set<Language> languages) {
            String[] split = StringUtils.split(new String(nameData), NAME_SEPARATOR);
            for (String part : split) {
                String[] nameTemp = StringUtils.splitPreserveAllTokens(part, LANG_SEPARATOR);
                String name = nameTemp[0];
                if (namesAreEqual(locationName, name)) {
                    if (nameTemp.length == 1) { // only primary name
                        return true;
                    }
                    for (int j = 1; j < nameTemp.length; j++) { // multiple languages
                        String langStr = nameTemp[j];
                        if (langStr.isEmpty() || languages.contains(Language.getByIso6391(langStr))) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

    }

}
