package ws.palladian.extraction.location.disambiguation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.core.Annotation;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.feature.StopWordRemover;
import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationExtractorUtils;
import ws.palladian.extraction.location.LocationFilters;
import ws.palladian.extraction.location.LocationSet;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Extracts features used by the {@link FeatureBasedDisambiguation} and {@link FeatureBasedDisambiguationLearner}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class DefaultLocationFeatureExtractor implements LocationFeatureExtractor {

//    /** The logger for this class. */
//    private static final Logger LOGGER = LoggerFactory.getLogger(LocationFeatureExtractor.class);

    public static boolean debug = false;

    private final StopWordRemover stopTokenRemover = new StopWordRemover(Language.ENGLISH);
    
    private final int contextSize;
    
    /**
     * <p>
     * Create a new {@link DefaultLocationFeatureExtractor} without a context limit.
     * </p>
     */
    public DefaultLocationFeatureExtractor() {
        this(-1);
    }

    /**
     * <p>
     * Create a new {@link DefaultLocationFeatureExtractor} with the specified context size.
     * </p>
     * 
     * @param contextSize The context size, -1 means no limit.
     */
    public DefaultLocationFeatureExtractor(int contextSize) {
        this.contextSize = contextSize;
    }

//    private final Set<String> locationMarkers = new HashSet<String>(
//            FileHelper.readFileToArray(FeatureBasedDisambiguation.class.getResourceAsStream("/locationMarkers.txt")));
    
//    private final Searcher<ClueWebResult> clueWebIndex = new CachingSearcher<ClueWebResult>(10000, new ClueWebSearcher(
//            new File("/Volumes/LaCie500/ClueWeb09")));

    /* (non-Javadoc)
     * @see ws.palladian.extraction.location.disambiguation.ILocationFeatureExtractor#extractFeatures(java.lang.String, ws.palladian.helper.collection.MultiMap)
     */
    @Override
    public Set<ClassifiableLocation> extract(String text, MultiMap<ClassifiedAnnotation, Location> locations) {

        Set<ClassifiableLocation> instances = CollectionHelper.newHashSet();
        // Collection<Location> allLocations = locations.allValues();
        // CountMap<String> counts = getCounts(locations.keySet());
        // int annotationCount = locations.keySet().size();
        Set<Location> uniqLocations = getUniqueLocations(locations);
        // Map<Location, Double> sentenceProximities = buildSentenceProximityMap(text, locations);
        // double largestDistance = LocationExtractorUtils.getLargestDistance(allLocations);
        // Set<Location> continents = CollectionHelper.filterSet(allLocations, new LocationTypeFilter(CONTINENT));
        // Set<Location> countries = CollectionHelper.filterSet(allLocations, new LocationTypeFilter(COUNTRY));
        // Set<Location> units = CollectionHelper.filterSet(allLocations, new LocationTypeFilter(UNIT));
        DisambiguationContext context = new DisambiguationContext(locations);
        
        for (ClassifiedAnnotation annotation : locations.keySet()) {
            
            Collection<Location> allLocations = context.getLocations(annotation, contextSize);
            Collection<Location> candidates = locations.get(annotation);
            // all locations except for the current annotation
            Set<Location> otherLocations = new HashSet<Location>(allLocations);
            otherLocations.removeAll(candidates);

            String value = annotation.getValue();
            String normalizedValue = LocationExtractorUtils.normalizeName(value);
            Location biggestLocation = new LocationSet(candidates).biggest();
            long maxPopulation = Math.max(1, biggestLocation != null ? biggestLocation.getPopulation() : 1);
            // boolean unique = isUnique(candidates);
            // boolean uniqueAndLong = unique && annotation.getValue().split("\\s").length > 2;
            // int maxDepth = getMaxDepth(candidates);
            boolean stopword = stopTokenRemover.isStopWord(value);

            // int firstOccurence = getFirstOccurence(annotation, locations.keySet());
            // double firstOccurenceRelative = (double)firstOccurence / text.length();
            // boolean unlikelyCandidate = isUnlikelyCandidate(annotation);
            // boolean likelyCandidate = isLikelyCandidate(annotation);
            // double indexScore = getIndexScore(locations, annotation);
            // boolean partialAnnotation = annotation.getTag().equals(EntityPreprocessingTagger.SPLIT_ANNOTATION_TAG);
            // double geoDiversity = getGeoDiversity(candidates, largestDistance);

            for (Location location : candidates) {

                Long population = location.getPopulation();

                // extract features and add them to the feature vector
                InstanceBuilder builder = new InstanceBuilder();

                // annotation features
                // fv.add(new NumericFeature("numCharacters", value.length()));
                // fv.add(new NumericFeature("numTokens", value.split("\\s").length));
                // fv.add(new BooleanFeature("acronym", isAcronym(annotation, locations)));
                builder.set("stopword", stopword);
                builder.set("caseSignature", StringHelper.getCaseSignature(normalizedValue));
                // createMarkerFeatures(value, fv); // + AusDM all in one
                // fv.add(new BooleanFeature("partialAnnotation", partialAnnotation)); // + AusDM

                // text features
                // fv.add(new NumericFeature("count", counts.getCount(value)));
                // fv.add(new NumericFeature("frequency", (double)counts.getCount(value) / annotationCount));
                // fv.add(new NumericFeature("firstOccurence", firstOccurence)); // + AusDM
                // fv.add(new NumericFeature("firstOccurenceRelative", firstOccurenceRelative)); // + AusDM

                // corpus features
                // fv.add(new BooleanFeature("unlikelyCandidate", unlikelyCandidate));
                // fv.add(new BooleanFeature("likelyCandidate", likelyCandidate)); // + AusDM
                // fv.add(new NumericFeature("indexScore", indexScore)); // + AusDM

                // gazetteer features
                // fv.add(new NominalFeature("locationType", location.getType().toString()));
                builder.set("country", location.getType() == LocationType.COUNTRY);
                // fv.add(new BooleanFeature("continent", location.getType() == LocationType.CONTINENT));
                builder.set("city", location.getType() == LocationType.CITY);
                // fv.add(new NumericFeature("population", population));
                // fv.add(new NumericFeature("populationMagnitude", MathHelper.getOrderOfMagnitude(population)));
                builder.set("populationNorm", (double)population / maxPopulation);
                builder.set("hierarchyDepth", location.getAncestorIds().size());
                // fv.add(new NumericFeature("hierarchyDepthNorm", (double)location.getAncestorIds().size() / maxDepth));
                builder.set("nameAmbiguity", 1. / candidates.size());
                builder.set("leaf", isLeaf(location, candidates));
                // fv.add(new NumericFeature("nameDiversity", getNameDiversity(location)));
                // fv.add(new NumericFeature("geoDiversity", geoDiversity));
                // fv.add(new BooleanFeature("unique", unique)); // + AusDM
                // fv.add(new BooleanFeature("uniqueAndLong", uniqueAndLong)); // + AusDM
                // fv.add(new BooleanFeature("altMention", isMentionedAlt(annotation, location, locations))); // + AusDM

                // text and gazetteer features
                //fv.add(new BooleanFeature("contains(ancestor)", ancestorCount(location, others) > 0));
                //fv.add(new BooleanFeature("contains(child)", childCount(location, others) > 0));
                //fv.add(new BooleanFeature("contains(descendant)", descendantCount(location, others) > 0));
                //fv.add(new BooleanFeature("contains(parent)", parentOccurs(location, others)));
                //fv.add(new BooleanFeature("contains(sibling)", siblingCount(location, others) > 0));
                //fv.add(new NumericFeature("num(ancestor)", ancestorCount(location, others)));
                //fv.add(new NumericFeature("num(child)", childCount(location, others)));
                //fv.add(new NumericFeature("num(descendant)", descendantCount(location, others)));
                //fv.add(new NumericFeature("num(sibling)", siblingCount(location, others)));
                //fv.add(new NumericFeature("numLocIn(10)", countLocationsInDistance(location, others, 10)));
                builder.set("numLocIn(50)", countLocationsInDistance(location, otherLocations, 50));
                //fv.add(new NumericFeature("numLocIn(100)", countLocationsInDistance(location, others, 100)));
                //fv.add(new NumericFeature("numLocIn(250)", countLocationsInDistance(location, others, 250)));
                //fv.add(new NumericFeature("distLoc(1m)", getDistanceToPopulation(location, others, 1000000)));
                //fv.add(new NumericFeature("distLoc(100k)", getDistanceToPopulation(location, others, 100000)));
                //fv.add(new NumericFeature("distLoc(10k)", getDistanceToPopulation(location, others, 10000)));
                //fv.add(new NumericFeature("distLoc(1k)", getDistanceToPopulation(location, others, 1000)));
                //fv.add(new NumericFeature("popIn(10)", getPopulationInRadius(location, others, 10)));
                builder.set("popIn(50)", getPopulationInRadius(location, otherLocations, 50));
                //fv.add(new NumericFeature("popIn(100)", getPopulationInRadius(location, others, 100)));
                //fv.add(new NumericFeature("popIn(250)", getPopulationInRadius(location, others, 250)));
                //fv.add(new BooleanFeature("locSentence(10)", sentenceProximities.get(location) <= 10));
                //fv.add(new BooleanFeature("locSentence(50)", sentenceProximities.get(location) <= 50));
                //fv.add(new BooleanFeature("locSentence(100)", sentenceProximities.get(location) <= 100));
                //fv.add(new BooleanFeature("locSentence(250)", sentenceProximities.get(location) <= 250));
                //fv.add(new BooleanFeature("uniqueIn(10)", countLocationsInDistance(location, uniqLocations, 10) > 0));
                //fv.add(new BooleanFeature("uniqueIn(50)", countLocationsInDistance(location, uniqLocations, 50) > 0));
                //fv.add(new BooleanFeature("uniqueIn(100)", countLocationsInDistance(location, uniqLocations, 100) > 0));
                builder.set("uniqueIn(250)", countLocationsInDistance(location, uniqLocations, 250) > 0);
                //fv.add(new BooleanFeature("primaryName", value.equals(location.getPrimaryName()))); // + AusDM
                //fv.add(new NumericFeature("distLoc2(1m)", getDistanceToPopulation2(location, others, 1000000))); // +
                //fv.add(new NumericFeature("distLoc2(100k)", getDistanceToPopulation2(location, others, 100000)));// +
                //fv.add(new NumericFeature("distLoc2(10k)", getDistanceToPopulation2(location, others, 10000)));// +
                //fv.add(new NumericFeature("distLoc2(1k)", getDistanceToPopulation2(location, others, 1000)));// +
                //fv.add(new BooleanFeature("hasLoc(1m,10)", getDistanceToPopulation(location, others, 1000000) < 10));// +
                //fv.add(new BooleanFeature("hasLoc(100k,10)", getDistanceToPopulation(location, others, 100000) < 10));// +
                //fv.add(new BooleanFeature("hasLoc(10k,10)", getDistanceToPopulation(location, others, 10000) < 10));// +
                //fv.add(new BooleanFeature("hasLoc(1k,10)", getDistanceToPopulation(location, others, 1000) < 10));// +
                //fv.add(new BooleanFeature("hasLoc(1m,50)", getDistanceToPopulation(location, others, 1000000) < 50));// +
                builder.set("hasLoc(100k,50)", getDistanceToPopulation(location, otherLocations, 100000) < 50);// +
                //fv.add(new BooleanFeature("hasLoc(10k,50)", getDistanceToPopulation(location, others, 10000) < 50));// +
                //fv.add(new BooleanFeature("hasLoc(1k,50)", getDistanceToPopulation(location, others, 1000) < 50));// +
                //fv.add(new BooleanFeature("hasLoc(1m,100)", getDistanceToPopulation(location, others, 1000000) < 100));// +
                //fv.add(new BooleanFeature("hasLoc(100k,100)", getDistanceToPopulation(location, others, 100000) < 100));// +
                builder.set("hasLoc(10k,100)", getDistanceToPopulation(location, otherLocations, 10000) < 100);// +
                //fv.add(new BooleanFeature("hasLoc(1k,100)", getDistanceToPopulation(location, others, 1000) < 100));// +
                //fv.add(new BooleanFeature("hasLoc(1m,250)", getDistanceToPopulation(location, others, 1000000) < 250));// +
                //fv.add(new BooleanFeature("hasLoc(100k,250)", getDistanceToPopulation(location, others, 100000) < 250));// +
                //fv.add(new BooleanFeature("hasLoc(10k,250)", getDistanceToPopulation(location, others, 10000) < 250));// +
                //fv.add(new BooleanFeature("hasLoc(1k,250)", getDistanceToPopulation(location, others, 1000) < 250));// +

                //fv.add(new BooleanFeature("hasLoc2(1m,10)", getDistanceToPopulation2(location, others, 1000000) < 10));// +
                //fv.add(new BooleanFeature("hasLoc2(100k,10)", getDistanceToPopulation2(location, others, 100000) < 10));// +
                //fv.add(new BooleanFeature("hasLoc2(10k,10)", getDistanceToPopulation2(location, others, 10000) < 10));// +
                //fv.add(new BooleanFeature("hasLoc2(1k,10)", getDistanceToPopulation2(location, others, 1000) < 10));// +
                //fv.add(new BooleanFeature("hasLoc2(1m,50)", getDistanceToPopulation2(location, others, 1000000) < 50));// +
                //fv.add(new BooleanFeature("hasLoc2(100k,50)", getDistanceToPopulation2(location, others, 100000) < 50));// +
                //fv.add(new BooleanFeature("hasLoc2(10k,50)", getDistanceToPopulation2(location, others, 10000) < 50));// +
                //fv.add(new BooleanFeature("hasLoc2(1k,50)", getDistanceToPopulation2(location, others, 1000) < 50));// +
                //fv.add(new BooleanFeature("hasLoc2(1m,100)", getDistanceToPopulation2(location, others, 1000000) < 100));// +
                //fv.add(new BooleanFeature("hasLoc2(100k,100)", getDistanceToPopulation2(location, others, 100000) < 100));// +
                //fv.add(new BooleanFeature("hasLoc2(10k,100)", getDistanceToPopulation2(location, others, 10000) < 100));// +
                //fv.add(new BooleanFeature("hasLoc2(1k,100)", getDistanceToPopulation2(location, others, 1000) < 100));// +
                //fv.add(new BooleanFeature("hasLoc2(1m,250)", getDistanceToPopulation2(location, others, 1000000) < 250));// +
                builder.set("hasLoc2(100k,250)", getDistanceToPopulation2(location, otherLocations, 100000) < 250);// +
                //fv.add(new BooleanFeature("hasLoc2(10k,250)", getDistanceToPopulation2(location, others, 10000) < 250));// +
                //fv.add(new BooleanFeature("hasLoc2(1k,250)", getDistanceToPopulation2(location, others, 1000) < 250));// +
                builder.set("popIn2(10)", getPopulationInRadius(location, allLocations, 10));// +
                //fv.add(new NumericFeature("popIn2(50)", getPopulationInRadius(location, allLocations, 50)));// +
                //fv.add(new NumericFeature("popIn2(100)", getPopulationInRadius(location, allLocations, 100)));// +
                //fv.add(new NumericFeature("popIn2(250)", getPopulationInRadius(location, allLocations, 250)));// +

                // boolean inContinent = containedInAny(location, continents);
                // boolean inCountry = containedInAny(location, countries);
                // boolean inUnit = containedInAny(location, units);
                //fv.add(new BooleanFeature("inContinent", inContinent)); // +
                //fv.add(new BooleanFeature("inCountry", inCountry)); // +
                //fv.add(new BooleanFeature("inUnit", inUnit)); // +
                //fv.add(new BooleanFeature("in(Country|Unit)", inCountry || inUnit)); // +
                //fv.add(new BooleanFeature("in(Continent|Country|Unit)", inContinent || inCountry || inUnit)); // +

                // TODO type equivalence relations of "neighbors"; e.g. for phrases like "Germany, France and Italy".
                // TODO distance from first location

                // just for debugging purposes
                // fv.add(new NominalFeature("locationId", String.valueOf(location.getId())));
                // fv.add(new NominalFeature("documentId", fileName));
                if (debug) {
                    String tempIdentifier = annotation.getValue() + annotation.getStartPosition()
                            + annotation.getEndPosition() + location.getId();
                    String hash = String.valueOf(tempIdentifier.hashCode());
                    builder.set("identifier", hash);
                }

                instances.add(new ClassifiableLocation(location, builder.create()));
            }
        }
        return instances;
    }

//    /**
//     * Check, whether the given location occurs within (at least) one location in the collection.
//     */
//    private static boolean containedInAny(Location location, Collection<Location> others) {
//        for (Location other : others) {
//            if (location.descendantOf(other)) {
//                return true;
//            }
//        }
//        return false;
//    }

//    /**
//     * Check, whether a location is mentioned with at least two different names in the text; e.g.
//     * "Los Angeles is also called L.A."
//     */
//    private static boolean isMentionedAlt(Annotation annotation, Location location,
//            MultiMap<? extends Annotation, Location> locations) {
//        Set<String> altNames = location.collectAlternativeNames();
//        for (Annotation temp : locations.keySet()) {
//            String tempValue = temp.getValue();
//            if (!tempValue.equalsIgnoreCase(annotation.getValue()) && locations.get(temp).contains(location)) {
//                for (String altName : altNames) {
//                    if (altName.equalsIgnoreCase(tempValue)) {
//                        LOGGER.trace("Alternative mentioned of {} with {}", annotation.getValue(), tempValue);
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
//    }

//    private double getIndexScore(MultiMap<? extends Annotation, Location> locations, Annotation annotation) {
//        double indexScore;
//        try {
//            long population = LocationExtractorUtils.getHighestPopulation(locations.get(annotation));
//            long count = clueWebIndex.getTotalResultCount(String.format("\"%s\"", annotation.getValue()));
//            indexScore = (double)(population + 1000) / (count + 1);
//        } catch (SearcherException e) {
//            throw new IllegalStateException("Error while searching for " + annotation.getValue() + "\": "
//                    + e.getMessage(), e);
//        }
//        return indexScore;
//    }

//    private static boolean isUnlikelyCandidate(ClassifiedAnnotation annotation) {
//        return annotation.getCategoryEntries().getProbability("PER") == 1;
//    }

//    private static boolean isLikelyCandidate(ClassifiedAnnotation annotation) {
//        return annotation.getCategoryEntries().getProbability("LOC") == 1;
//    }

//    private int getFirstOccurence(Annotation annotation, Collection<? extends Annotation> all) {
//        int firstOccurence = annotation.getStartPosition();
//        for (Annotation other : all) {
//            if (other.getValue().equals(annotation.getValue())) {
//                firstOccurence = Math.min(other.getStartPosition(), firstOccurence);
//            }
//        }
//        return firstOccurence;
//    }

//    private void createMarkerFeatures(String value, FeatureVector featureVector) {
//        boolean containsAny = false;
//        for (String marker : locationMarkers) {
//            boolean containsWord = StringHelper.containsWord(marker, value);
//            featureVector.add(new BooleanFeature("containsMarker(" + marker.toLowerCase() + ")", containsWord));
//            containsAny |= containsWord;
//        }
//        featureVector.add(new BooleanFeature("containsMarker(*)", containsAny));
//    }

//    private static Map<Location, Double> buildSentenceProximityMap(String text,
//            MultiMap<? extends Annotation, Location> locations) {
//        Map<Location, Double> proximityMap = LazyMap.create(ConstantFactory.create(Double.MAX_VALUE));
//        List<String> sentences = Tokenizer.getSentences(text);
//        for (String sentence : sentences) {
//            int start = text.indexOf(sentence);
//            int end = start + sentence.length();
//            List<? extends Annotation> currentAnnotations = getAnnotations(locations.keySet(), start, end);
//            for (Annotation value1 : currentAnnotations) {
//                Collection<Location> locations1 = locations.get(value1);
//                for (Location location1 : locations1) {
//                    for (Annotation value2 : currentAnnotations) {
//                        if (!value1.getValue().equals(value2.getValue())) {
//                            // XXX to make this even more secure, we might use #normalizeName
//                            Collection<Location> locations2 = locations.get(value2);
//                            for (Location location2 : locations2) {
//                                if (!location1.equals(location2)) {
//                                    Double temp = proximityMap.get(location1);
//                                    double distance = GeoUtils.getDistance(location1, location2);
//                                    proximityMap.put(location1, Math.min(temp, distance));
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        LOGGER.debug("Sentence proximity map contains {} entries", proximityMap.size());
//        return proximityMap;
//    }

//    // XXX move to some utility class
//    /**
//     * <p>
//     * Get annotations in the specified span.
//     * </p>
//     * 
//     * @param annotations {@link Collection} of annotations.
//     * @param start The start offset.
//     * @param end The end offset.
//     * @return All annotations between (including) start/end.
//     */
//    private static <A extends Annotation> List<A> getAnnotations(Collection<A> annotations, int start, int end) {
//        List<A> result = CollectionHelper.newArrayList();
//        for (A annotation : annotations) {
//            if (annotation.getStartPosition() >= start && annotation.getEndPosition() <= end) {
//                result.add(annotation);
//            }
//        }
//        return result;
//    }

    private static Set<Location> getUniqueLocations(MultiMap<? extends Annotation, Location> locations) {
        Set<Location> uniqueLocations = CollectionHelper.newHashSet();
        for (Collection<Location> group : locations.values()) {
            if (isUnique(group)) {
                uniqueLocations.addAll(group);
            }
        }
        return uniqueLocations;
    }

//    private static int getMaxDepth(Collection<Location> locations) {
//        int maxDepth = 1;
//        for (Location location : locations) {
//            maxDepth = Math.max(maxDepth, location.getAncestorIds().size());
//        }
//        return maxDepth;
//    }

    private static boolean isUnique(Collection<Location> locations) {
        LocationSet group = new LocationSet(locations).whereConditionally(LocationFilters.coordinate());
//        Set<GeoCoordinate> coordinates = CollectionHelper.convertSet(group, LOCATION_COORDINATE_FUNCTION);
//        return LocationExtractorUtils.largestDistanceBelow(50, coordinates);
//        return LocationExtractorUtils.getLargestDistance(coordinates) < 50;
        return group.largestDistance() < 50;
    }

    private static long getPopulationInRadius(Location location, Collection<Location> others, double distance) {
        GeoCoordinate locationCoordinate = location.getCoordinate();
        if (locationCoordinate == null) {
            return 0;
        }
        long population = 0;
//        if (locationCoordinate != null) {
//            for (Location other : others) {
//                GeoCoordinate otherCoordinate = other.getCoordinate();
//                Long otherPopulation = other.getPopulation();
//                if (otherCoordinate != null && otherPopulation != null && otherPopulation > 0
//                        && locationCoordinate.distance(otherCoordinate) <= distance) {
//                    population += otherPopulation;
//                }
//            }
//        }
        for (Location other : CollectionHelper.filterSet(others, LocationFilters.radius(locationCoordinate, distance))) {
            Long otherPopulation = other.getPopulation();
            if (otherPopulation != null) {
                population += otherPopulation;
            }
        }
        return population;
    }

    private static int getDistanceToPopulation(Location location, Collection<Location> others, int population) {
        int distance = Integer.MAX_VALUE;
        GeoCoordinate locationCoordinate = location.getCoordinate();
        if (locationCoordinate != null) {
            for (Location other : others) {
                GeoCoordinate otherCoordinate = other.getCoordinate();
                if (otherCoordinate != null && other.getPopulation() >= population) {
                    distance = (int)Math.min(distance, otherCoordinate.distance(locationCoordinate));
                }
            }
        }
        return distance;
    }

    /**
     * In contrast to #getDistanceToPopulation we consider the location's population itself, too.
     */
    private static int getDistanceToPopulation2(Location location, Collection<Location> others, int population) {
        if (location.getPopulation() != null && location.getPopulation() >= population) {
            return 0;
        }
        return getDistanceToPopulation(location, others, population);
    }

    private static int countLocationsInDistance(Location location, Collection<Location> others, double distance) {
        GeoCoordinate locationCoordinate = location.getCoordinate();
        if (locationCoordinate == null) {
            return 0;
        }
//        int count = 0;
//        for (Location other : others) {
//            GeoCoordinate otherCoordinate = other.getCoordinate();
//            if (otherCoordinate != null && locationCoordinate.distance(otherCoordinate) < distance) {
//                count++;
//            }
//        }
//        return count;
        return CollectionHelper.filterSet(others, LocationFilters.radius(locationCoordinate, distance)).size();
    }
    
//    private static boolean hasLocationsInDistance(Location location, Set<Location> others, double distance) {
//        GeoCoordinate locationCoordinate = location.getCoordinate();
//        if (locationCoordinate == null) {
//            return false;
//        }
//        for (Location other : others) {
//            GeoCoordinate otherCoordinate = other.getCoordinate();
//            if (otherCoordinate != null && locationCoordinate.distance(otherCoordinate) < distance) {
//                return true;
//            }
//        }
//        return false;
//    }

//    private static int childCount(Location location, Collection<Location> others) {
//        int count = 0;
//        for (Location other : others) {
//            if (other.childOf(location)) {
//                count++;
//            }
//        }
//        return count;
//    }
//
//    private static int descendantCount(Location location, Collection<Location> others) {
//        int count = 0;
//        for (Location other : others) {
//            if (other.descendantOf(location)) {
//                count++;
//            }
//        }
//        return count;
//    }
//
//    private static int ancestorCount(Location location, Collection<Location> others) {
//        int count = 0;
//        for (Location other : others) {
//            if (location.descendantOf(other)) {
//                count++;
//            }
//        }
//        return count;
//    }
//
//    private static boolean parentOccurs(Location location, Collection<Location> others) {
//        for (Location other : others) {
//            if (location.childOf(other)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private static int siblingCount(Location location, Collection<Location> others) {
//        int count = 0;
//        for (Location other : others) {
//            if (location.getAncestorIds().equals(other.getAncestorIds())) {
//                count++;
//            }
//        }
//        return count;
//    }
//
//    private static CountMap<String> getCounts(Collection<? extends Annotation> annotations) {
//        CountMap<String> frequencies = CountMap.create();
//        for (Annotation annotation : annotations) {
//            frequencies.add(LocationExtractorUtils.normalizeName(annotation.getValue()));
//        }
//        return frequencies;
//    }
//
//    private static boolean isAcronym(Annotation annotation, MultiMap<? extends Annotation, Location> locations) {
//        for (Location location : locations.get(annotation)) {
//            Set<String> names = location.collectAlternativeNames();
//            for (String name : names) {
//                if (name.equals(LocationExtractorUtils.normalizeName(annotation.getValue()))) {
//                    if (name.matches("[A-Z]+|([A-Z]\\.)+")) {
//                        LOGGER.trace("{} is an acronym", annotation.getValue());
//                        return true;
//                    }
//                }
//            }
//        }
//        return false;
//    }

    private static boolean isLeaf(Location location, Collection<Location> others) {
        for (Location other : others) {
            if (other.descendantOf(location)) {
                return false;
            }
        }
        return true;
    }

//    private static double getNameDiversity(Location location) {
//        return 1. / location.collectAlternativeNames().size();
//    }
//
//    private static double getGeoDiversity(Collection<Location> locations, double normalization) {
//        if (normalization == 0) {
//            return 0.;
//        }
//        return LocationExtractorUtils.getLargestDistance(locations) / normalization;
//    }

    /**
     * <p>
     * The disambiguation context is a window around the current candidate. This way, the number of computations
     * (distance calculation, etc.) can be reduced, because we simply ignore everything outside the window. For small
     * documents, the context has little/no impact, but for very large documents, the processing time is greatly reduced
     * (or so to say, an extraction becomes feasible).
     * </p>
     * 
     * @author pk
     */
    private static final class DisambiguationContext {

        private final List<ClassifiedAnnotation> sortedAnnotations;

        private final MultiMap<ClassifiedAnnotation, Location> locations;

        DisambiguationContext(MultiMap<ClassifiedAnnotation, Location> locations) {
            this.locations = locations;
            this.sortedAnnotations = new ArrayList<ClassifiedAnnotation>(locations.keySet());
            Collections.sort(this.sortedAnnotations);
        }

        /**
         * <p>
         * Get the context around the specified {@link Annotation}.
         * </p>
         * 
         * @param annotation The annotation.
         * @param contextSize The context size/lookaround.
         * @return The context within the specified scope.
         */
        public MultiMap<ClassifiedAnnotation, Location> getCandidates(Annotation annotation, int contextSize) {
            if (contextSize == -1) {
                return locations;
            }
            MultiMap<ClassifiedAnnotation, Location> context = DefaultMultiMap.createWithSet();
            int contextStart = annotation.getStartPosition() - contextSize;
            int contextEnd = annotation.getEndPosition() + contextSize;
            for (ClassifiedAnnotation current : sortedAnnotations) {
                if (current.getStartPosition() >= contextStart && current.getEndPosition() <= contextEnd) {
                    context.put(current, locations.get(current));
                }
            }
            return context;
        }

        public Collection<Location> getLocations(ClassifiedAnnotation annotation, int contextSize) {
//            Collection<Location> result = CollectionHelper.newHashSet();
//            for (Collection<Location> locations : getCandidates(annotation, contextSize).values()) {
//                result.addAll(locations);
//            }
//            return result;
            return getCandidates(annotation, contextSize).allValues();
        }

    }

}
