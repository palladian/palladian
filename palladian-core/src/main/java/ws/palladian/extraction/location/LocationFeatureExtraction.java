package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.extraction.feature.StopTokenRemover;
import ws.palladian.extraction.location.LocationExtractorUtils.CoordinateFilter;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.ConstantFactory;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.Annotated;
import ws.palladian.processing.features.BooleanFeature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

class LocationFeatureExtraction {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationFeatureExtraction.class);

    private final EntityPreprocessingTagger tagger = new EntityPreprocessingTagger();

    private final ContextClassifier contextClassifier = new ContextClassifier();

    private final StopTokenRemover stopTokenRemover = new StopTokenRemover(Language.ENGLISH);

    private final Set<String> locationMarkers = new HashSet<String>(
            FileHelper.readFileToArray(FeatureBasedDisambiguation.class.getResourceAsStream("/locationMarkers.txt")));

    private Set<Annotated> getUnlikelyCandidates(String text, MultiMap<String, Location> locations) {

        // get *all* annotations
        List<Annotated> annotations = tagger.getAnnotations(text);

        // the logic employed here is a bit weird,
        // wouldn't it be better to check the MultiMap, take those candidates which have zero locations assigned,
        // and take them as unlikely parts?

        Set<String> unlikelyParts = CollectionHelper.newHashSet();
        for (Annotated annotation : annotations) {
            if (!locations.containsKey(LocationExtractorUtils.normalizeName(annotation.getValue()))) {
                LOGGER.trace("[unlikely] {}", annotation);
                String[] parts = annotation.getValue().split("\\s");
                for (String part : parts) {
                    unlikelyParts.add(part.toLowerCase());
                }
            }
        }
        LOGGER.trace("Unlikely parts: {}", unlikelyParts);

        Set<Annotated> unlikelyCandidates = CollectionHelper.newHashSet();
        for (Annotated annotation : annotations) {
            String[] parts = annotation.getValue().split("\\s");
            for (String part : parts) {
                if (unlikelyParts.contains(part.toLowerCase())) {
                    unlikelyCandidates.add(annotation);
                }
            }
        }
        LOGGER.trace("{} Unlikely candidates: {}", unlikelyCandidates.size(), unlikelyCandidates);
        return unlikelyCandidates;
    }

    public Set<LocationInstance> makeInstances(String text, List<Annotated> annotations,
            MultiMap<String, Location> locations) {

        Set<Annotated> unlikelyCandidates = getUnlikelyCandidates(text, locations);
        Set<LocationInstance> instances = CollectionHelper.newHashSet();
        Collection<Location> allLocations = locations.allValues();
        CountMap<String> counts = getCounts(annotations);
        int annotationCount = annotations.size();
        Set<Location> uniqueLocations = getUniqueLocations(locations);
        Map<Location, Double> sentenceProximities = buildSentenceProximityMap(text, annotations, locations);
        Map<String, CategoryEntries> contextClassification = createContextClassification(text, annotations);
        double largestDistance = LocationExtractorUtils.getLargestDistance(allLocations);

        for (Annotated annotation : annotations) {

            String value = annotation.getValue();
            String normalizedValue = LocationExtractorUtils.normalizeName(value);
            Collection<Location> candidates = locations.get(normalizedValue);
            Location biggestLocation = LocationExtractorUtils.getBiggest(candidates);
            long maxPopulation = Math.max(1, biggestLocation != null ? biggestLocation.getPopulation() : 1);
            boolean unique = isUnique(candidates);
            boolean uniqueAndLong = unique && annotation.getValue().split("\\s").length > 2;
            int maxDepth = getMaxDepth(candidates);
            boolean unlikelyCandidate = unlikelyCandidates.contains(annotation);
            CategoryEntries temp = contextClassification.get(normalizedValue);
            double locContextProbability = temp != null ? temp.getProbability("LOC") : 0;
            boolean stopword = stopTokenRemover.isStopword(value);

            for (Location location : candidates) {

                // all locations except the current one
                Set<Location> others = new HashSet<Location>(allLocations);
                others.remove(location);

                Long population = location.getPopulation();

                // extract features and add them to the feature vector
                FeatureVector fv = new FeatureVector();
                fv.add(new NominalFeature("locationType", location.getType().toString()));
                fv.add(new BooleanFeature("country", location.getType() == LocationType.COUNTRY));
                fv.add(new BooleanFeature("continent", location.getType() == LocationType.CONTINENT));
                fv.add(new BooleanFeature("city", location.getType() == LocationType.CITY));
                fv.add(new NumericFeature("population", population));
                fv.add(new NumericFeature("populationMagnitude", MathHelper.getOrderOfMagnitude(population)));
                fv.add(new NumericFeature("populationNorm", (double)population / maxPopulation));
                fv.add(new NumericFeature("numTokens", value.split("\\s").length));
                fv.add(new NumericFeature("numCharacters", value.length()));
                fv.add(new NumericFeature("ambiguity", 1. / candidates.size()));
                fv.add(new BooleanFeature("acronym", isAcronym(value, locations)));
                fv.add(new NumericFeature("count", counts.getCount(value)));
                fv.add(new NumericFeature("frequency", (double)counts.getCount(value) / annotationCount));
                fv.add(new BooleanFeature("parentOccurs", parentOccurs(location, others)));
                fv.add(new NumericFeature("ancestorCount", ancestorCount(location, others)));
                fv.add(new BooleanFeature("ancestorOccurs", ancestorCount(location, others) > 0));
                fv.add(new BooleanFeature("childOccurs", childCount(location, others) > 0));
                fv.add(new NumericFeature("childCount", childCount(location, others)));
                fv.add(new BooleanFeature("descendantOccurs", descendantCount(location, others) > 0));
                fv.add(new NumericFeature("descendantCount", descendantCount(location, others)));
                fv.add(new NumericFeature("numLocIn10", countLocationsInDistance(location, others, 10)));
                fv.add(new NumericFeature("numLocIn50", countLocationsInDistance(location, others, 50)));
                fv.add(new NumericFeature("numLocIn100", countLocationsInDistance(location, others, 100)));
                fv.add(new NumericFeature("numLocIn250", countLocationsInDistance(location, others, 250)));
                fv.add(new NumericFeature("distLoc1m", getDistanceToPopulation(location, others, 1000000)));
                fv.add(new NumericFeature("distLoc100k", getDistanceToPopulation(location, others, 100000)));
                fv.add(new NumericFeature("distLoc10k", getDistanceToPopulation(location, others, 10000)));
                fv.add(new NumericFeature("distLoc1k", getDistanceToPopulation(location, others, 1000)));
                fv.add(new NumericFeature("popIn10", getPopulationInRadius(location, others, 10)));
                fv.add(new NumericFeature("popIn50", getPopulationInRadius(location, others, 50)));
                fv.add(new NumericFeature("popIn100", getPopulationInRadius(location, others, 100)));
                fv.add(new NumericFeature("popIn250", getPopulationInRadius(location, others, 250)));
                fv.add(new NumericFeature("siblingCount", siblingCount(location, others)));
                fv.add(new BooleanFeature("siblingOccurs", siblingCount(location, others) > 0));
                fv.add(new NumericFeature("hierarchyDepth", location.getAncestorIds().size()));
                fv.add(new NumericFeature("hierarchyDepthNorm", (double)location.getAncestorIds().size() / maxDepth));
                fv.add(new BooleanFeature("unique", unique));
                fv.add(new BooleanFeature("uniqueAndLong", uniqueAndLong));
                fv.add(new BooleanFeature("unlikelyCandidate", unlikelyCandidate));
                fv.add(new BooleanFeature("uniqLocIn10", countLocationsInDistance(location, uniqueLocations, 10) > 0));
                fv.add(new BooleanFeature("uniqLocIn50", countLocationsInDistance(location, uniqueLocations, 50) > 0));
                fv.add(new BooleanFeature("uniqLocIn100", countLocationsInDistance(location, uniqueLocations, 100) > 0));
                fv.add(new BooleanFeature("uniqLocIn250", countLocationsInDistance(location, uniqueLocations, 250) > 0));
                fv.add(new BooleanFeature("distLoc10Sentence", sentenceProximities.get(location) <= 10));
                fv.add(new BooleanFeature("distLoc50Sentence", sentenceProximities.get(location) <= 50));
                fv.add(new BooleanFeature("distLoc100Sentence", sentenceProximities.get(location) <= 100));
                fv.add(new BooleanFeature("distLoc250Sentence", sentenceProximities.get(location) <= 250));
                fv.add(new NumericFeature("context", locContextProbability));
                fv.add(new BooleanFeature("stopword", stopword));
                fv.add(new NominalFeature("caseSignature", StringHelper.getCaseSignature(normalizedValue)));
                fv.add(new BooleanFeature("leaf", isLeaf(location, candidates)));
                fv.add(new NumericFeature("nameDiversity", getNameDiversity(location)));
                fv.add(new NumericFeature("geoDiversity", getGeoDiversity(candidates, largestDistance)));

                createMarkerFeatures(value, fv);

                // just for debugging purposes
                // fv.add(new NominalFeature("locationId", String.valueOf(location.getId())));
                // fv.add(new NominalFeature("documentId", fileName));

                instances.add(new LocationInstance(location, fv));
            }
        }
        return instances;
    }

    private void createMarkerFeatures(String value, FeatureVector featureVector) {
        for (String marker : locationMarkers) {
            boolean containsWord = StringHelper.containsWord(marker, value);
            featureVector.add(new BooleanFeature("marker=" + marker.toLowerCase(), containsWord));
        }
    }

    private Map<String, CategoryEntries> createContextClassification(String text, List<Annotated> annotations) {
        Map<String, CategoryEntries> result = CollectionHelper.newHashMap();
        for (Annotated annotation : annotations) {
            CategoryEntries classification = contextClassifier.classify(text, annotation);
            String value = LocationExtractorUtils.normalizeName(annotation.getValue());
            CategoryEntries existing = result.get(value);
            if (existing == null) {
                result.put(value, classification);
            } else {
                result.put(value, CategoryEntriesMap.merge(classification, existing));
            }
        }
        return result;
    }

    private static Map<Location, Double> buildSentenceProximityMap(String text, List<Annotated> annotations,
            MultiMap<String, Location> locations) {
        Map<Location, Double> proximityMap = LazyMap.create(ConstantFactory.create(Double.MAX_VALUE));
        List<String> sentences = Tokenizer.getSentences(text);
        for (String sentence : sentences) {
            int start = text.indexOf(sentence);
            int end = start + sentence.length();
            List<Annotated> currentAnnotations = getAnnotations(annotations, start, end);
            Set<String> values = CollectionHelper.newHashSet();
            for (Annotated annotated : currentAnnotations) {
                values.add(LocationExtractorUtils.normalizeName(annotated.getValue()));
            }
            for (String value1 : values) {
                Collection<Location> locations1 = locations.get(value1);
                for (Location location1 : locations1) {
                    for (String value2 : values) {
                        if (!value1.equals(value2)) {
                            Collection<Location> locations2 = locations.get(value2);
                            for (Location location2 : locations2) {
                                Double temp = proximityMap.get(location1);
                                double distance = GeoUtils.getDistance(location1, location2);
                                proximityMap.put(location1, Math.min(temp, distance));
                            }
                        }
                    }
                }
            }
        }
        LOGGER.debug("Sentence proximity map contains {} entries", proximityMap.size());
        return proximityMap;
    }

    // XXX move to some utility class
    private static List<Annotated> getAnnotations(List<Annotated> annotations, int start, int end) {
        List<Annotated> result = CollectionHelper.newArrayList();
        for (Annotated annotation : annotations) {
            if (annotation.getStartPosition() >= start && annotation.getEndPosition() <= end) {
                result.add(annotation);
            }
        }
        return result;
    }

    private static Set<Location> getUniqueLocations(MultiMap<String, Location> locations) {
        Set<Location> uniqueLocations = CollectionHelper.newHashSet();
        for (Collection<Location> group : locations.values()) {
            if (isUnique(group)) {
                uniqueLocations.addAll(group);
            }
        }
        return uniqueLocations;
    }

    private static int getMaxDepth(Collection<Location> locations) {
        int maxDepth = 1;
        for (Location location : locations) {
            maxDepth = Math.max(maxDepth, location.getAncestorIds().size());
        }
        return maxDepth;
    }

    private static boolean isUnique(Collection<Location> locations) {
        Set<Location> group = LocationExtractorUtils.filterConditionally(locations, new CoordinateFilter());
        return LocationExtractorUtils.getLargestDistance(group) < 50;
    }

    private static int getPopulationInRadius(Location location, Collection<Location> others, double distance) {
        int population = 0;
        for (Location other : others) {
            if (GeoUtils.getDistance(location, other) <= distance) {
                population += other.getPopulation();
            }
        }
        return population;
    }

    private static int getDistanceToPopulation(Location location, Collection<Location> others, int population) {
        int distance = Integer.MAX_VALUE;
        for (Location other : others) {
            if (other.getPopulation() >= population) {
                distance = (int)Math.min(distance, GeoUtils.getDistance(other, location));
            }
        }
        return distance;
    }

    private static int countLocationsInDistance(Location location, Collection<Location> others, double distance) {
        int count = 0;
        for (Location other : others) {
            if (GeoUtils.getDistance(location, other) < distance) {
                count++;
            }
        }
        return count;
    }

    private static int childCount(Location location, Collection<Location> others) {
        int count = 0;
        for (Location other : others) {
            if (LocationExtractorUtils.isDirectChildOf(other, location)) {
                count++;
            }
        }
        return count;
    }

    private static int descendantCount(Location location, Collection<Location> others) {
        int count = 0;
        for (Location other : others) {
            if (LocationExtractorUtils.isChildOf(other, location)) {
                count++;
            }
        }
        return count;
    }

    private static int ancestorCount(Location location, Collection<Location> others) {
        int count = 0;
        for (Location other : others) {
            if (LocationExtractorUtils.isChildOf(location, other)) {
                count++;
            }
        }
        return count;
    }

    private static boolean parentOccurs(Location location, Collection<Location> others) {
        for (Location other : others) {
            if (LocationExtractorUtils.isDirectChildOf(location, other)) {
                return true;
            }
        }
        return false;
    }

    private static int siblingCount(Location location, Collection<Location> others) {
        int count = 0;
        for (Location other : others) {
            if (location.getAncestorIds().equals(other.getAncestorIds())) {
                count++;
            }
        }
        return count;
    }

    private static CountMap<String> getCounts(List<Annotated> annotations) {
        CountMap<String> frequencies = CountMap.create();
        for (Annotated annotation : annotations) {
            frequencies.add(LocationExtractorUtils.normalizeName(annotation.getValue()));
        }
        return frequencies;
    }

    private static boolean isAcronym(String value, MultiMap<String, Location> locations) {
        for (Location location : locations.get(value)) {
            Set<String> names = LocationExtractorUtils.collectNames(location);
            for (String name : names) {
                if (name.equals(value)) {
                    if (name.matches("[A-Z]+|([A-Z]\\.)+")) {
                        LOGGER.trace("{} is an acronym", value);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isLeaf(Location location, Collection<Location> others) {
        for (Location other : others) {
            if (LocationExtractorUtils.isChildOf(other, location)) {
                return false;
            }
        }
        return true;
    }

    private static double getNameDiversity(Location location) {
        return 1. / LocationExtractorUtils.collectNames(location).size();
    }

    private static double getGeoDiversity(Collection<Location> locations, double normalization) {
        if (normalization == 0) {
            return 0.;
        }
        return LocationExtractorUtils.getLargestDistance(locations) / normalization;
    }

    static final class LocationInstance implements Location, Classifiable {

        private final Location location;
        private final FeatureVector featureVector;

        public LocationInstance(Location location, FeatureVector featureVector) {
            this.location = location;
            this.featureVector = featureVector;
        }

        @Override
        public Double getLatitude() {
            return location.getLatitude();
        }

        @Override
        public Double getLongitude() {
            return location.getLongitude();
        }

        @Override
        public int getId() {
            return location.getId();
        }

        @Override
        public String getPrimaryName() {
            return location.getPrimaryName();
        }

        @Override
        public Collection<AlternativeName> getAlternativeNames() {
            return location.getAlternativeNames();
        }

        @Override
        public LocationType getType() {
            return location.getType();
        }

        @Override
        public Long getPopulation() {
            return location.getPopulation();
        }

        @Override
        public List<Integer> getAncestorIds() {
            return location.getAncestorIds();
        }

        @Override
        public FeatureVector getFeatureVector() {
            return featureVector;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((featureVector == null) ? 0 : featureVector.hashCode());
            result = prime * result + ((location == null) ? 0 : location.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LocationInstance other = (LocationInstance)obj;
            if (featureVector == null) {
                if (other.featureVector != null)
                    return false;
            } else if (!featureVector.equals(other.featureVector))
                return false;
            if (location == null) {
                if (other.location != null)
                    return false;
            } else if (!location.equals(other.location))
                return false;
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LocationInstance [location=");
            builder.append(location);
            builder.append(", featureVector=");
            builder.append(featureVector);
            builder.append("]");
            return builder.toString();
        }

    }

}
