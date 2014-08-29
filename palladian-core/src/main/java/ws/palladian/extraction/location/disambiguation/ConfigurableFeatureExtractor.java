package ws.palladian.extraction.location.disambiguation;

import static ws.palladian.extraction.location.LocationFilters.ancestorOf;
import static ws.palladian.extraction.location.LocationFilters.childOf;
import static ws.palladian.extraction.location.LocationFilters.coordinate;
import static ws.palladian.extraction.location.LocationFilters.descendantOf;
import static ws.palladian.extraction.location.LocationFilters.parentOf;
import static ws.palladian.extraction.location.LocationFilters.population;
import static ws.palladian.extraction.location.LocationFilters.radius;
import static ws.palladian.extraction.location.LocationFilters.siblingOf;
import static ws.palladian.extraction.location.LocationFilters.type;
import static ws.palladian.helper.collection.CollectionHelper.coalesce;
import static ws.palladian.helper.functional.Filters.equal;
import static ws.palladian.helper.functional.Filters.not;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationExtractorUtils;
import ws.palladian.extraction.location.LocationStats;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.scope.ScopeDetector;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.search.Searcher;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Extracts features used by the {@link FeatureBasedDisambiguation} and {@link FeatureBasedDisambiguationLearner}.
 * 
 * @author Philipp Katz
 */
public class ConfigurableFeatureExtractor implements LocationFeatureExtractor {

    private final FeatureExtractorSetting setting;

    /**
     * Create a new {@link ConfigurableFeatureExtractor}.
     * 
     * @param settings The settings, not <code>null</code>.
     */
    public ConfigurableFeatureExtractor(FeatureExtractorSetting settings) {
        Validate.notNull(settings, "settings must not be null");
        this.setting = settings;
    }

    /**
     * Create a new {@link ConfigurableFeatureExtractor} using the default configuration (
     * {@link FeatureExtractorSetting#DEFAULT}).
     */
    public ConfigurableFeatureExtractor() {
        this(FeatureExtractorSetting.DEFAULT);
    }

    @Override
    public Set<ClassifiableLocation> extract(String text, MultiMap<ClassifiedAnnotation, Location> locations) {
        Set<ClassifiableLocation> instances = CollectionHelper.newHashSet();
        LocationStats allLocations = new LocationStats(locations.allValues());
        LocationStats uniqLocations = new LocationStats(getUniqueLocations(locations.values()));
        LocationStats continents = allLocations.where(type(LocationType.CONTINENT));
        LocationStats countries = allLocations.where(type(LocationType.COUNTRY));
        LocationStats units = allLocations.where(type(LocationType.UNIT));
        List<GeoCoordinate> scopes = determineTextScopes(text);
        MultiMap<Location, String> mentions = createMentionMap(locations);

        Set<String> alreadyChecked = CollectionHelper.newHashSet();

        for (Entry<ClassifiedAnnotation, Collection<Location>> entry : locations.entrySet()) {

            ClassifiedAnnotation annotation = entry.getKey();
            Collection<Location> candidates = entry.getValue();
            if (candidates.isEmpty()) {
                continue;
            }
            String value = annotation.getValue();
            if (!alreadyChecked.add(value)) { // skip candidates which we already extracted
                continue;
            }
            String normalizedValue = LocationExtractorUtils.normalizeName(value);
            LocationStats currentLocations = new LocationStats(candidates);
            LocationStats otherLocations = allLocations.where(not(equal(candidates)));
            int numCharacters = value.length();
            int numTokens = value.split("\\s").length;
            boolean acronym = value.matches("[A-Z]+|([A-Z]\\.)+");
            String caseSignature = StringHelper.getCaseSignature(normalizedValue);
            double nameAmbiguity = 1. / candidates.size();
            double geoDiversity = currentLocations.largestDistance();
            boolean unique = currentLocations.where(coordinate()).largestDistance() < setting.getEqualDistance();
            Map<String, Long> indexCounts = getIndexCounts(normalizedValue);

            for (Location location : candidates) {

                Long population = coalesce(location.getPopulation(), 0l);
                GeoCoordinate coordinate = coalesce(location.getCoordinate(), GeoCoordinate.NULL);

                InstanceBuilder builder = new InstanceBuilder();

                builder.set("numCharacters", numCharacters);
                builder.set("numTokens", numTokens);
                builder.set("acronym", acronym);
                builder.set("caseSignature", caseSignature);
                createMarkerFeatures(value, builder);

                builder.set("locationType", location.getType().toString());
                builder.set("population", population);
                builder.set("hierarchyDepth", location.getAncestorIds().size());
                builder.set("nameAmbiguity", nameAmbiguity);

                builder.set("leaf", currentLocations.where(childOf(location)).count() == 0);
                builder.set("nameDiversity", 1. / location.collectAlternativeNames().size());
                builder.set("geoDiversity", geoDiversity);
                builder.set("unique", unique);
                builder.set("altMention", mentions.get(location).size() > 1);

                int numAncestors = otherLocations.where(ancestorOf(location)).count();
                int numChildren = otherLocations.where(childOf(location)).count();
                int numDescendants = otherLocations.where(descendantOf(location)).count();
                int numParents = otherLocations.where(parentOf(location)).count();
                int numSiblings = otherLocations.where(siblingOf(location)).count();
                builder.set("contains(ancestor)", numAncestors > 0);
                builder.set("contains(child)", numChildren > 0);
                builder.set("contains(descendant)", numDescendants > 0);
                builder.set("contains(parent)", numParents > 0);
                builder.set("contains(sibling)", numSiblings > 0);
                builder.set("num(ancestor)", numAncestors);
                builder.set("num(child)", numChildren);
                builder.set("num(descendant)", numDescendants);
                builder.set("num(sibling)", numSiblings);

                for (int d : setting.getDistanceValues()) {
                    LocationStats otherInDist = otherLocations.where(radius(coordinate, d));
                    LocationStats allInDist = allLocations.where(radius(coordinate, d));
                    builder.set(String.format("numLocIn(%d)", d), otherInDist.count());
                    builder.set(String.format("popIn(%d,true)", d), allInDist.totalPopulation());
                    builder.set(String.format("popIn(%d,false)", d), otherInDist.where(not(equal(location)))
                            .totalPopulation());
                    builder.set(String.format("uniqueIn(%d)", d),
                            uniqLocations.where(radius(coordinate, d)).count() > 0);
                }
                for (int p : setting.getPopulationValues()) {
                    double distOther = otherLocations.where(population(p)).where(not(equal(location)))
                            .distance(coordinate);
                    double distAll = allLocations.where(population(p)).distance(coordinate);
                    builder.set(String.format("distLoc(%d,true)", p), distAll);
                    builder.set(String.format("distLoc(%d,false)", p), distOther);
                    for (int d : setting.getDistanceValues()) {
                        builder.set(String.format("hasLoc(%d,%d,true)", p, d), distAll < d);
                        builder.set(String.format("hasLoc(%d,%d,false)", p, d), distOther < d);
                    }
                }
                builder.set("primaryName", value.equalsIgnoreCase(location.getPrimaryName()));
                builder.set("inContinent", continents.where(ancestorOf(location)).count() > 0);
                builder.set("inCountry", countries.where(ancestorOf(location)).count() > 0);
                builder.set("inUnit", units.where(ancestorOf(location)).count() > 0);

                for (int n = 0; n < scopes.size(); n++) {
                    GeoCoordinate scope = scopes.get(n);
                    scope = scope != null ? scope : GeoCoordinate.NULL;
                    builder.set("scopeDistance-" + n, coordinate.distance(scope));
                }
                for (Entry<String, Long> searcherCount : indexCounts.entrySet()) {
                    String indexName = searcherCount.getKey();
                    Long indexCount = searcherCount.getValue();
                    double indexPopulationQuotient = (double)population / (indexCount + 1);
                    builder.set(String.format("indexCount(%s)", indexName), indexCount);
                    builder.set(String.format("indexPopulationQuotient(%s)", indexName), indexPopulationQuotient);
                }
                if (setting.isDebug()) {
                    builder.set("textHash", StringHelper.sha1(text));
                    builder.set("annotationOffset", annotation.getStartPosition());
                    builder.set("annotationValue", value);
                    builder.set("locationId", String.valueOf(location.getId()));
                }
                instances.add(new ClassifiableLocation(location, builder.create()));
            }
        }
        return instances;
    }

    private MultiMap<Location, String> createMentionMap(MultiMap<ClassifiedAnnotation, Location> locations) {
        MultiMap<Location, String> mentionMap = DefaultMultiMap.createWithSet();
        for (Entry<ClassifiedAnnotation, Collection<Location>> candidateEntry : locations.entrySet()) {
            String mentionedValue = candidateEntry.getKey().getValue();
            for (Location location : candidateEntry.getValue()) {
                mentionMap.add(location, mentionedValue);
            }
        }
        return mentionMap;
    }

    private List<GeoCoordinate> determineTextScopes(String text) {
        List<GeoCoordinate> result = CollectionHelper.newArrayList();
        for (ScopeDetector scopeDetector : setting.getScopeDetectors()) {
            result.add(scopeDetector.getScope(text));
        }
        return result;
    }

    private Map<String, Long> getIndexCounts(String value) {
        Map<String, Long> counts = CollectionHelper.newHashMap();
        String query = String.format("\"%s\"", value);
        for (Searcher<?> searcher : setting.getIndexSearchers()) {
            try {
                long resultCount = searcher.getTotalResultCount(query);
                counts.put(searcher.getName(), resultCount);
            } catch (SearcherException e) {
                String msg = String.format("Error while searching for %s with %s: %s", query, searcher, e.getMessage());
                throw new IllegalStateException(msg, e);
            }
        }
        return counts;
    }

    private void createMarkerFeatures(String value, InstanceBuilder builder) {
        if (setting.getLocationMarkers().length == 0) {
            return;
        }
        boolean containsAny = false;
        for (String marker : setting.getLocationMarkers()) {
            boolean containsWord = StringHelper.containsWord(marker, value);
            builder.set("marker(" + marker.toLowerCase() + ")", containsWord);
            containsAny |= containsWord;
        }
        builder.set("marker(*)", containsAny);
    }

    private Set<Location> getUniqueLocations(Collection<Collection<Location>> locationGroups) {
        Set<Location> uniqueLocations = CollectionHelper.newHashSet();
        for (Collection<Location> group : locationGroups) {
            if (new LocationStats(group).where(coordinate()).largestDistance() < setting.getEqualDistance()) {
                uniqueLocations.addAll(group);
            }
        }
        return uniqueLocations;
    }

    @Override
    public String toString() {
        return "ConfigurableFeatureExtractor [" + setting + "]";
    }

}
