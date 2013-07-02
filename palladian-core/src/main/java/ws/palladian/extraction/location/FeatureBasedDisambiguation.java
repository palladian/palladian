package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.processing.features.Annotated;

public class FeatureBasedDisambiguation implements LocationDisambiguation {

    private final Set<LocationInstance> instances = CollectionHelper.newHashSet();

    @Override
    public List<LocationAnnotation> disambiguate(List<Annotated> annotations, MultiMap<String, Location> locations) {

        Set<LocationInstance> instances = extractFeatures(annotations, locations);
        CollectionHelper.print(instances);

        // TODO Auto-generated method stub
        return Collections.emptyList();
    }

    public void addTrainData(List<Annotated> annotations, MultiMap<String, Location> locations, Set<Location> positive) {
        Set<LocationInstance> instances = extractFeatures(annotations, locations);
        int numPos = markPositiveInstances(instances, positive);
        this.instances.addAll(instances);
        System.out.println("Marked " + numPos + " of " + instances.size() + " as positive.");
        // CollectionHelper.print(instances);
    }

    private int markPositiveInstances(Set<LocationInstance> instances, Set<Location> positive) {
        int numPositive = 0;
        for (LocationInstance instance : instances) {
            boolean isPositive = false;
            for (Location positiveLocation : positive) {
                if (instance.coordinates == null) {
                    continue;
                }
                double distance = GeoUtils.getDistance(instance.coordinates, positiveLocation);
                boolean sameName = LocationExtractorUtils.normalizeName(instance.value).equalsIgnoreCase(
                        positiveLocation.getPrimaryName());
                if (distance < 50 && sameName) {
                    isPositive = true;
                    break;
                }
            }
            instance.positive = isPositive;
            if (isPositive) {
                numPositive++;
            }
        }
        return numPositive;
    }

    public void buildModel() {

        System.out.println("... model should be built here ...");
        System.out.println("# instances: " + instances.size());

        int positive = 0;
        // write CSV file
        StringBuilder builder = new StringBuilder();
        builder.append("type;population;populationMagnitude;numToken;numChars;ambiguity;acronym;count;parentOccurs;ancestorOccurs;locationsIn10;locationsIn50;locationsIn100;locationsIn250;"
                + "distLoc1000000;distLoc100000;distLoc10000;distLoc1000;populationIn10;populationIn50;populationIn100;populationIn250;siblingCount;siblingOccurs;positive\n");
        for (LocationInstance instance : instances) {
            if (instance.positive) {
                positive++;
            }
            builder.append(instance.type.toString()).append(';');
            builder.append(instance.population).append(';');
            builder.append(instance.populationOrderOfMagnitude).append(';');
            builder.append(instance.numTokens).append(';');
            builder.append(instance.numCharacters).append(';');
            builder.append(instance.ambiguity).append(';');
            builder.append(instance.acronym).append(';');
            builder.append(instance.count).append(';');
            builder.append(instance.parentOccurs).append(';');
            builder.append(instance.ancestorOccurs).append(';');
            builder.append(instance.numLocationsIn10).append(';');
            builder.append(instance.numLocationsIn50).append(';');
            builder.append(instance.numLocationsIn100).append(';');
            builder.append(instance.numLocationsIn250).append(';');
            builder.append(instance.distanceToLocationWith1000000).append(';');
            builder.append(instance.distanceToLocationWith100000).append(';');
            builder.append(instance.distanceToLocationWith10000).append(';');
            builder.append(instance.distanceToLocationWith1000).append(';');
            builder.append(instance.populationIn10).append(';');
            builder.append(instance.populationIn50).append(';');
            builder.append(instance.populationIn100).append(';');
            builder.append(instance.populationIn250).append(';');
            builder.append(instance.siblingCount).append(';');
            builder.append(instance.siblingOccurs).append(';');
            builder.append(instance.positive);
            builder.append('\n');
        }
        FileHelper.writeToFile("instances.csv", builder);
        System.out.println("# positive: " + positive);
    }

    private Set<LocationInstance> extractFeatures(List<Annotated> annotations, MultiMap<String, Location> locations) {
        Set<LocationInstance> instances = CollectionHelper.newHashSet();
        CountMap<String> counts = getCounts(annotations);
        for (Annotated annotation : annotations) {
            String value = annotation.getValue();
            Collection<Location> candidates = locations.get(LocationExtractorUtils.normalizeName(value));
            for (Location location : candidates) {
                LocationInstance instance = new LocationInstance();
                instance.value = value;
                if (location.getLatitude() != null) {
                    instance.coordinates = new ImmutableGeoCoordinate(location.getLatitude(), location.getLongitude());
                }
                instance.type = location.getType();
                instance.population = location.getPopulation();
                instance.populationOrderOfMagnitude = MathHelper.getOrderOfMagnitude(location.getPopulation());
                instance.numTokens = value.split("\\s").length;
                instance.numCharacters = value.length();
                instance.ambiguity = 1. / candidates.size();
                instance.acronym = isAcronym(annotation.getValue());
                instance.count = counts.getCount(value);
                instance.parentOccurs = parentOccurs(location, locations.allValues());
                instance.ancestorOccurs = ancestorOccurs(location, locations.allValues());
                instance.numLocationsIn10 = getNumLocationsInArea(10, location, locations.allValues());
                instance.numLocationsIn50 = getNumLocationsInArea(50, location, locations.allValues());
                instance.numLocationsIn100 = getNumLocationsInArea(100, location, locations.allValues());
                instance.numLocationsIn250 = getNumLocationsInArea(250, location, locations.allValues());
                instance.distanceToLocationWith1000000 = getDistanceToLocationWith(1000000, location,
                        locations.allValues());
                instance.distanceToLocationWith100000 = getDistanceToLocationWith(100000, location,
                        locations.allValues());
                instance.distanceToLocationWith10000 = getDistanceToLocationWith(10000, location, locations.allValues());
                instance.distanceToLocationWith1000 = getDistanceToLocationWith(1000, location, locations.allValues());
                instance.populationIn10 = getPopulationIn(10, location, locations.allValues());
                instance.populationIn50 = getPopulationIn(50, location, locations.allValues());
                instance.populationIn100 = getPopulationIn(100, location, locations.allValues());
                instance.populationIn250 = getPopulationIn(250, location, locations.allValues());
                instance.siblingCount = siblingCount(location, locations.allValues());
                instance.siblingOccurs = instance.siblingCount > 0;
                instances.add(instance);
            }
        }
        return instances;
    }

    private int getPopulationIn(int i, Location location, Collection<Location> allValues) {
        int population = 0;
        for (Location locationToCheck : allValues) {
            double currentDistance = GeoUtils.getDistance(locationToCheck, location);
            if (currentDistance > i) {
                continue;
            }
            if (locationToCheck.getPrimaryName().equals(location.getPrimaryName())) {
                continue;
            }
            population += locationToCheck.getPopulation();
        }
        return population;
    }

    private int getDistanceToLocationWith(int i, Location location, Collection<Location> allValues) {
        int distance = Integer.MAX_VALUE;
        for (Location locationToCheck : allValues) {
            if (locationToCheck.getPopulation() < i) {
                continue;
            }
            double currentDistance = GeoUtils.getDistance(locationToCheck, location);
            if (currentDistance == 0 || locationToCheck.getPrimaryName().equals(location.getPrimaryName())) {
                continue;
            }
            distance = (int)Math.min(distance, currentDistance);
        }
        return distance;
    }

    private int getNumLocationsInArea(int i, Location location, Collection<Location> allValues) {
        int count = 0;
        for (Location locationToCheck : allValues) {
            double distance = GeoUtils.getDistance(locationToCheck, location);
            if (distance < i) {
                count++;
            }
        }
        return count;
    }

    private boolean ancestorOccurs(Location location, Collection<Location> allValues) {
        for (Location locationToCheck : allValues) {
            boolean isChild = LocationExtractorUtils.isChildOf(location, locationToCheck);
            if (isChild) {
                return true;
            }
        }
        return false;
    }

    private boolean parentOccurs(Location location, Collection<Location> allValues) {
        for (Location locationToCheck : allValues) {
            boolean isChild = LocationExtractorUtils.isDirectChildOf(location, locationToCheck);
            if (isChild) {
                return true;
            }
        }
        return false;
    }

    private int siblingCount(Location location, Collection<Location> allValues) {
        int count = 0;
        for (Location locationToCheck : allValues) {
            if (locationToCheck.equals(location)) {
                continue;
            }
            if (location.getAncestorIds().equals(locationToCheck.getAncestorIds())) {
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

    private static boolean isAcronym(String value) {
        return value.matches("[A-Z]+|([A-Z]\\.)+");
    }

    private static final class LocationInstance {

        String value;
        GeoCoordinate coordinates;
        LocationType type;
        long population;
        long populationOrderOfMagnitude;
        int numTokens;
        int numCharacters;
        double ambiguity;
        boolean acronym;
        int count;
        boolean positive;
        boolean parentOccurs;
        boolean ancestorOccurs;
        int numLocationsIn10;
        int numLocationsIn50;
        int numLocationsIn100;
        int numLocationsIn250;
        int distanceToLocationWith1000000;
        int distanceToLocationWith100000;
        int distanceToLocationWith10000;
        int distanceToLocationWith1000;
        int populationIn10;
        int populationIn50;
        int populationIn100;
        int populationIn250;
        int siblingCount;
        boolean siblingOccurs;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LocationInstance [value=");
            builder.append(value);
            builder.append(", coordinates=");
            builder.append(coordinates);
            builder.append(", type=");
            builder.append(type);
            builder.append(", population=");
            builder.append(population);
            builder.append(", populationOrderOfMagnitude=");
            builder.append(populationOrderOfMagnitude);
            builder.append(", numTokens=");
            builder.append(numTokens);
            builder.append(", numCharacters=");
            builder.append(numCharacters);
            builder.append(", ambiguity=");
            builder.append(ambiguity);
            builder.append(", acronym=");
            builder.append(acronym);
            builder.append(", count=");
            builder.append(count);
            builder.append(", positive=");
            builder.append(positive);
            builder.append("]");
            return builder.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (acronym ? 1231 : 1237);
            long temp;
            temp = Double.doubleToLongBits(ambiguity);
            result = prime * result + (int)(temp ^ (temp >>> 32));
            result = prime * result + numCharacters;
            result = prime * result + numTokens;
            result = prime * result + (int)(population ^ (population >>> 32));
            result = prime * result + (int)(populationOrderOfMagnitude ^ (populationOrderOfMagnitude >>> 32));
            result = prime * result + ((type == null) ? 0 : type.hashCode());
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
            if (acronym != other.acronym)
                return false;
            if (Double.doubleToLongBits(ambiguity) != Double.doubleToLongBits(other.ambiguity))
                return false;
            if (numCharacters != other.numCharacters)
                return false;
            if (numTokens != other.numTokens)
                return false;
            if (population != other.population)
                return false;
            if (populationOrderOfMagnitude != other.populationOrderOfMagnitude)
                return false;
            if (type != other.type)
                return false;
            return true;
        }

    }

}
