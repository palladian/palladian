package ws.palladian.extraction.location.disambiguation;

import org.apache.commons.lang3.Validate;
import ws.palladian.core.Annotation;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.location.*;
import ws.palladian.extraction.location.scope.ScopeDetector;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.search.Searcher;
import ws.palladian.retrieval.search.SearcherException;

import java.util.*;
import java.util.Map.Entry;

import static ws.palladian.extraction.location.LocationFilters.*;
import static ws.palladian.helper.collection.CollectionHelper.coalesce;
import static ws.palladian.helper.functional.Predicates.equal;
import static ws.palladian.helper.functional.Predicates.not;

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
    public Set<ClassifiableLocation> extract(String text, MultiMap<? extends ClassifiedAnnotation, Location> locations) {
        Set<ClassifiableLocation> instances = new HashSet<>();
        LocationSet allLocations = new LocationSet(locations.allValues());
        LocationSet uniqLocations = new LocationSet(getUniqueLocations(locations.values()));
        LocationSet continents = allLocations.where(type(LocationType.CONTINENT));
        LocationSet countries = allLocations.where(type(LocationType.COUNTRY));
        LocationSet units = allLocations.where(type(LocationType.UNIT));
        List<GeoCoordinate> scopes = determineTextScopes(text);
        List<List<? extends GeoCoordinate>> scopes2 = determineTextScopes2(text);
        MultiMap<Location, String> mentions = createMentionMap(locations);

        Set<String> alreadyChecked = new HashSet<>();

        for (Entry<? extends ClassifiedAnnotation, Collection<Location>> entry : locations.entrySet()) {

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
            LocationSet currentLocations = new LocationSet(candidates);
            LocationSet otherLocations = allLocations.where(not(equal(candidates)));
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
                GeoCoordinate coordinate = location.getCoords().orElse(GeoCoordinate.NULL);

                InstanceBuilder builder = new InstanceBuilder();

                builder.set("numCharacters", numCharacters);
                builder.set("numTokens", numTokens);
                builder.set("acronym", acronym);
                builder.set("caseSignature", caseSignature);
                createMarkerFeatures(value, builder);

                builder.set("locationType", location.getType().toString());
                builder.set("population", population);
                if (setting.useHierarchyFeatures()) {
                    builder.set("hierarchyDepth", location.getAncestorIds().size());
                }
                builder.set("nameAmbiguity", nameAmbiguity);

                if (setting.useHierarchyFeatures()) {
                    builder.set("leaf", currentLocations.where(childOf(location)).size() == 0);
                }
                builder.set("nameDiversity", 1. / location.collectAlternativeNames().size());
                builder.set("geoDiversity", geoDiversity);
                builder.set("unique", unique);
                builder.set("altMention", mentions.get(location).size() > 1);

                if (setting.useHierarchyFeatures()) {
                    int numAncestors = otherLocations.where(ancestorOf(location)).size();
                    int numChildren = otherLocations.where(childOf(location)).size();
                    int numDescendants = otherLocations.where(descendantOf(location)).size();
                    int numParents = otherLocations.where(parentOf(location)).size();
                    int numSiblings = otherLocations.where(siblingOf(location)).size();
                    builder.set("contains(ancestor)", numAncestors > 0);
                    builder.set("contains(child)", numChildren > 0);
                    builder.set("contains(descendant)", numDescendants > 0);
                    builder.set("contains(parent)", numParents > 0);
                    builder.set("contains(sibling)", numSiblings > 0);
                    builder.set("num(ancestor)", numAncestors);
                    builder.set("num(child)", numChildren);
                    builder.set("num(descendant)", numDescendants);
                    builder.set("num(sibling)", numSiblings);
                }

                for (int d : setting.getDistanceValues()) {
                    LocationSet otherInDist = otherLocations.where(radius(coordinate, d));
                    LocationSet allInDist = allLocations.where(radius(coordinate, d));
                    builder.set(String.format("numLocIn(%d)", d), otherInDist.size());
                    builder.set(String.format("popIn(%d,true)", d), allInDist.totalPopulation());
                    builder.set(String.format("popIn(%d,false)", d), otherInDist.where(not(equal(location))).totalPopulation());
                    builder.set(String.format("uniqueIn(%d)", d), uniqLocations.where(radius(coordinate, d)).size() > 0);
                }
                for (int p : setting.getPopulationValues()) {
                    double distOther = otherLocations.where(population(p)).where(not(equal(location))).minDistance(coordinate);
                    double distAll = allLocations.where(population(p)).minDistance(coordinate);
                    builder.set(String.format("distLoc(%d,true)", p), distAll);
                    builder.set(String.format("distLoc(%d,false)", p), distOther);
                    for (int d : setting.getDistanceValues()) {
                        builder.set(String.format("hasLoc(%d,%d,true)", p, d), distAll < d);
                        builder.set(String.format("hasLoc(%d,%d,false)", p, d), distOther < d);
                    }
                }
                builder.set("primaryName", value.equalsIgnoreCase(location.getPrimaryName()));
                if (setting.useHierarchyFeatures()) {
                    builder.set("inContinent", continents.where(ancestorOf(location)).size() > 0);
                    builder.set("inCountry", countries.where(ancestorOf(location)).size() > 0);
                    builder.set("inUnit", units.where(ancestorOf(location)).size() > 0);
                }

                CategoryEntries typeClassification = annotation.getCategoryEntries();
                for (String categoryName : setting.getEntityCategories()) {
                    double probability = typeClassification.getProbability(categoryName);
                    builder.set(String.format("category(%s)", categoryName), probability);
                }

                for (int n = 0; n < scopes.size(); n++) {
                    GeoCoordinate scope = scopes.get(n);
                    scope = scope != null ? scope : GeoCoordinate.NULL;
                    builder.set("scopeDistance-" + n, coordinate.distance(scope));
                }
                for (int n = 0; n < scopes2.size(); n++) {
                    List<? extends GeoCoordinate> scopeCoords = scopes2.get(n);
                    // minDist to scopes
                    // maxDist to scopes
                    // in distance to scope
                    double minDist = Double.MAX_VALUE;
                    double maxDist = Double.MIN_VALUE;
                    for (GeoCoordinate scopeCoord : scopeCoords) {
                        minDist = Math.min(minDist, scopeCoord.distance(coordinate));
                        maxDist = Math.max(maxDist, scopeCoord.distance(coordinate));
                    }
                    for (double dist : setting.getDistanceValues()) {
                        builder.set("scopeDistance-" + n + "-" + dist, minDist < dist);
//                      System.out.println("scopeDistance-" + n + "-" + dist + ":" + (minDist < dist));
                    }
                    builder.set("scopeDistance-" + n + "-min", minDist);
                    builder.set("scopeDistance-" + n + "-max", maxDist);
//                    System.out.println("scopeDistance-" + n + "-min:" +  minDist);
//                    System.out.println("scopeDistance-" + n + "-max:" +  maxDist);
                }
                for (Entry<String, Long> searcherCount : indexCounts.entrySet()) {
                    String indexName = searcherCount.getKey();
                    Long indexCount = searcherCount.getValue();
                    double indexPopulationQuotient = (double) population / (indexCount + 1);
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

    private MultiMap<Location, String> createMentionMap(MultiMap<? extends Annotation, Location> locations) {
        MultiMap<Location, String> mentionMap = DefaultMultiMap.createWithSet();
        for (Entry<? extends Annotation, Collection<Location>> candidateEntry : locations.entrySet()) {
            String mentionedValue = candidateEntry.getKey().getValue();
            for (Location location : candidateEntry.getValue()) {
                mentionMap.add(location, mentionedValue);
            }
        }
        return mentionMap;
    }

    private List<GeoCoordinate> determineTextScopes(String text) {
        List<GeoCoordinate> result = new ArrayList<>();
        for (ScopeDetector scopeDetector : setting.getScopeDetectors()) {
            result.add(scopeDetector.getScope(text));
        }
        return result;
    }
    
    private List<List<? extends GeoCoordinate>> determineTextScopes2(String text) {
        List<List<? extends GeoCoordinate>> result = new ArrayList<>();
        for (ScopeDetector scopeDetector : setting.getScopeDetectors()) {
            result.add(scopeDetector.getScopes(text));
        }
        return result;
    }

    private Map<String, Long> getIndexCounts(String value) {
        Map<String, Long> counts = new HashMap<>();
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
        Set<Location> uniqueLocations = new HashSet<>();
        for (Collection<Location> group : locationGroups) {
            if (new LocationSet(group).where(coordinate()).largestDistance() < setting.getEqualDistance()) {
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
