package ws.palladian.extraction.location;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.dt.BaggedDecisionTreeClassifier;
import ws.palladian.classification.dt.BaggedDecisionTreeModel;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.extraction.location.LocationExtractorUtils.CoordinateFilter;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.TrainableWrap;
import ws.palladian.processing.features.Annotated;
import ws.palladian.processing.features.BooleanFeature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

public class FeatureBasedDisambiguation implements LocationDisambiguation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureBasedDisambiguation.class);

    private final EntityPreprocessingTagger tagger = new EntityPreprocessingTagger();

    private final Set<Trainable> trainInstanceCollection = CollectionHelper.newHashSet();

    private final BaggedDecisionTreeClassifier classifier = new BaggedDecisionTreeClassifier();

    private BaggedDecisionTreeModel model;

    // XXX ugly
    public void setModel(BaggedDecisionTreeModel model) {
        this.model = model;
    }

    @Override
    public List<LocationAnnotation> disambiguate(String text, List<Annotated> annotations,
            MultiMap<String, Location> locations) {


        Set<LocationInstance> instances = makeInstances(text, annotations, locations, "foo");
        Map<Integer, Double> scoredLocations = CollectionHelper.newHashMap();

        for (LocationInstance instance : instances) {
            CategoryEntries classification = classifier.classify(instance, model);
            scoredLocations.put(instance.getId(), classification.getProbability("true"));
        }
        
        List<LocationAnnotation> result = CollectionHelper.newArrayList();
        for (Annotated annotation : annotations) {
            String value = LocationExtractorUtils.normalizeName(annotation.getValue());
            Collection<Location> candidates = locations.get(value);
            
            double highestScore = 0;
            Location selectedLocation = null;
            
            for (Location location : candidates) {
                double score = scoredLocations.get(location.getId());
                if (score > highestScore) {
                    highestScore = score;
                    selectedLocation = location;
                }
            }
            
            if (selectedLocation != null && highestScore >= 0.5) {
                result.add(new LocationAnnotation(annotation, selectedLocation));
                Object[] logArgs = new Object[] {annotation.getValue(), highestScore, selectedLocation};
                LOGGER.debug("[+] '{}' was classfied as location with {}: {}", logArgs);
            } else {
                LOGGER.debug("[-] '{}' was classfied as no location with {}", annotation.getValue(), highestScore);
            }
        }
        return result;
    }

    private Set<Annotated> getUnlikelyCandidates(String text, MultiMap<String, Location> locations) {

        // get *all* annotations
        List<Annotated> annotations = tagger.getAnnotations(text);

        // the logic employed here is a bit weird,
        // wouldn't it be better to check the MultiMap, take those candidates which have zero locations assigned,
        // and take them as unlikely parts?

        Set<String> unlikelyParts = CollectionHelper.newHashSet();
        for (Annotated annotation : annotations) {
            if (!locations.containsKey(LocationExtractorUtils.normalizeName(annotation.getValue()))) {
                LOGGER.debug("[unlikely] {}", annotation);
                String[] parts = annotation.getValue().split("\\s");
                for (String part : parts) {
                    unlikelyParts.add(part.toLowerCase());
                }
            }
        }
        LOGGER.debug("Unlikely parts: {}", unlikelyParts);

        Set<Annotated> unlikelyCandidates = CollectionHelper.newHashSet();
        for (Annotated annotation : annotations) {
            String[] parts = annotation.getValue().split("\\s");
            for (String part : parts) {
                if (unlikelyParts.contains(part.toLowerCase())) {
                    unlikelyCandidates.add(annotation);
                }
            }
        }
        LOGGER.debug("{} Unlikely candidates: {}", unlikelyCandidates.size(), unlikelyCandidates);

        return unlikelyCandidates;
    }

    public void addTrainData(String text, List<Annotated> annotations, MultiMap<String, Location> locations,
            Set<Location> positive, String fileName) {
        Set<LocationInstance> instances = makeInstances(text, annotations, locations, fileName);
        Set<Trainable> trainInstances = markPositiveInstances(instances, positive);
        trainInstanceCollection.addAll(trainInstances);
    }

    private static Set<Trainable> markPositiveInstances(Set<LocationInstance> instances, Set<Location> positive) {
        Set<Trainable> result = CollectionHelper.newHashSet();
        int numPositive = 0;
        for (LocationInstance instance : instances) {
            boolean positiveClass = false;
            for (Location location : positive) {
                // we cannot determine the correct location, if the training data did not provide coordinates
                if (instance.getLatitude() == null || instance.getLongitude() == null) {
                    continue;
                }
                boolean samePlace = GeoUtils.getDistance(instance, location) < 50;
                boolean sameName = LocationExtractorUtils.commonName(instance, location);
                // consider locations as positive samples, if they have same name and have max. distance of 50 kms
                if (samePlace && sameName) {
                    numPositive++;
                    positiveClass = true;
                    break;
                }
            }
            result.add(new TrainableWrap(instance, String.valueOf(positiveClass)));
        }

        double positivePercentage = MathHelper.round((float)numPositive / instances.size() * 100, 2);
        LOGGER.debug("{} positive instances in {} ({}%)", new Object[] {numPositive, instances.size(),
                positivePercentage});
        return result;
    }

    public void buildModel() {
        String baseFileName = String.format("location_disambiguation_%s", System.currentTimeMillis());
        ClassificationUtils.writeToCsv(trainInstanceCollection, new File(baseFileName + ".csv"));

        StopWatch stopWatch = new StopWatch();
        model = classifier.train(trainInstanceCollection);
        FileHelper.serialize(model, baseFileName + ".model");
        LOGGER.info("Built and serialized model in {}.", stopWatch.getTotalElapsedTimeString());
    }

    private Set<LocationInstance> makeInstances(String text, List<Annotated> annotations,
            MultiMap<String, Location> locations, String fileName) {

        Set<Annotated> unlikelyCandidates = getUnlikelyCandidates(text, locations);

        Set<LocationInstance> instances = CollectionHelper.newHashSet();
        Collection<Location> allLocations = locations.allValues();

        CountMap<String> counts = getCounts(annotations);
        int annotationCount = annotations.size();

        Set<Location> uniqueLocations = getUniqueLocations(locations);

        for (Annotated annotation : annotations) {

            String value = annotation.getValue();
            Collection<Location> candidates = locations.get(LocationExtractorUtils.normalizeName(value));
            Location biggestLocation = LocationExtractorUtils.getBiggest(candidates);
            long maxPopulation = Math.max(1, biggestLocation != null ? biggestLocation.getPopulation() : 1);
            boolean unique = isUnique(candidates);
            boolean uniqueAndLong = unique && annotation.getValue().split("\\s").length > 2;
            int maxDepth = getMaxDepth(candidates);
            boolean unlikelyCandidate = unlikelyCandidates.contains(annotation);

            for (Location location : candidates) {

                // all locations except the current one
                Set<Location> others = new HashSet<Location>(allLocations);
                others.remove(location);

                Long population = location.getPopulation();

                // extract features and add them to the feature vector
                FeatureVector fv = new FeatureVector();
                fv.add(new NominalFeature("locationType", location.getType().toString()));
                fv.add(new NumericFeature("population", population));
                fv.add(new NumericFeature("populationMagnitude", MathHelper.getOrderOfMagnitude(population)));
                fv.add(new NumericFeature("populationNorm", (double)population / maxPopulation));
                fv.add(new NumericFeature("numTokens", value.split("\\s").length));
                fv.add(new NumericFeature("numCharacters", value.length()));
                fv.add(new NumericFeature("ambiguity", 1. / candidates.size()));
                fv.add(new BooleanFeature("acronym", isAcronym(annotation.getValue())));
                fv.add(new NumericFeature("count", counts.getCount(value)));
                fv.add(new NumericFeature("frequency", (double)counts.getCount(value) / annotationCount));
                fv.add(new BooleanFeature("parentOccurs", parentOccurs(location, others)));
                fv.add(new NumericFeature("ancestorCount", ancestorCount(location, others)));
                fv.add(new BooleanFeature("ancestorOccurs", ancestorCount(location, others) > 0));
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

                // just for debugging purposes
                // fv.add(new NominalFeature("locationId", String.valueOf(location.getId())));
                // fv.add(new NominalFeature("documentId", fileName));

                instances.add(new LocationInstance(location, fv));
            }
        }
        return instances;
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
        // XXX maybe use token count also
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

    // XXX check, if it is really spelled capitalized by comparing to looked up locations
    private static boolean isAcronym(String value) {
        return value.matches("[A-Z]+|([A-Z]\\.)+");
    }


    private static final class LocationInstance implements Location, Classifiable {

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
