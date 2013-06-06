// package ws.palladian.extraction.location;
//
// import java.util.Collection;
// import java.util.Collections;
// import java.util.List;
// import java.util.Set;
//
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
//
// import ws.palladian.helper.collection.CollectionHelper;
// import ws.palladian.helper.collection.MultiMap;
// import ws.palladian.processing.features.Annotated;
//
// public class ClusteringDisambiguation implements LocationDisambiguation {
//
// private static final class LocationCluster implements Comparable<LocationCluster> {
// Set<Location> locations = CollectionHelper.newHashSet();
// final Set<String> locationNames;
//
// public LocationCluster(Set<String> locationNames) {
// this.locationNames = locationNames;
// }
//
// @SuppressWarnings("unused")
// double getSmallestDistance(Location location) {
// double smallestDistance = Double.MAX_VALUE;
// for (Location l : locations) {
// double distance = GeoUtils.getDistance(location, l);
// smallestDistance = Math.min(smallestDistance, distance);
// }
// return smallestDistance;
// }
//
// double getDistanceToCenter(Location location) {
// GeoCoordinate midpoint = GeoUtils.getMidpoint(locations);
// return GeoUtils.getDistance(midpoint, location);
// }
//
// double getMaximumDistanceFromCenter() {
// double maximumDistance = 0;
// GeoCoordinate midpoint = GeoUtils.getMidpoint(locations);
// for (Location l : locations) {
// double distance = GeoUtils.getDistance(l, midpoint);
// maximumDistance = Math.max(maximumDistance, distance);
// }
// return maximumDistance;
// }
//
// void add(Location location) {
// locations.add(location);
// }
//
// @Override
// public String toString() {
// return locations.toString();
// }
//
// public int size() {
// return locations.size();
// }
//
// public long totalPopulation() {
// long totalPopulation = 0;
// for (Location location : locations) {
// totalPopulation += location.getPopulation();
// }
// return totalPopulation;
// }
//
// public void condense() {
// for (Location l1 : locations) {
// for (Location l2 : locations) {
// if (!l1.equals(l2) && sameName(l1, l2)) {
// System.out.println(l1 + " and " + l2 + " have common name!");
// }
// }
// }
// }
//
// public boolean nameClash(Location location) {
// for (Location l : locations) {
// if (GeoUtils.getDistance(l, location) > NAME_DISTANCE && sameName(l, location)) {
// return true;
// }
// }
// return false;
// }
//
// public double getCoverage() {
// int match = 0;
// name: for (String name : locationNames) {
// for (Location l : locations) {
// boolean matchesAlternatives = false;
// for (AlternativeName aN : l.getAlternativeNames()) {
// if (aN.getName().equals(name)) {
// matchesAlternatives = true;
// break;
// }
// }
// if (matchesAlternatives || l.getPrimaryName().equals(name)) {
// match++;
// continue name;
// }
// }
// }
// return (double)match / locationNames.size();
// }
//
// @Override
// public int compareTo(LocationCluster o) {
// return Double.valueOf(o.getCoverage()).compareTo(getCoverage());
// }
// }
//
// /** The logger for this class. */
// private static final Logger LOGGER = LoggerFactory.getLogger(ClusteringDisambiguation.class);
//
// private static final double MAX_DISTANCE = 250d;
//
// private static final double NAME_DISTANCE = 50d;
//
// @Override
// public List<LocationAnnotation> disambiguate(List<Annotated> annotations, MultiMap<String, Location> locations) {
// List<LocationAnnotation> result = CollectionHelper.newArrayList();
//
// Set<String> locationNames = CollectionHelper.newHashSet();
// for (Annotated annotated : annotations) {
// locationNames.add(annotated.getValue());
// }
//
// Collection<Location> anchors = ProximityDisambiguation.getAnchors(annotations, locations);
//
// for (Location location : anchors) {
// locationNames.remove(location.getPrimaryName());
// for (AlternativeName an : location.getAlternativeNames()) {
// locationNames.remove(an.getName());
// }
// }
//
// List<Location> allLocations = CollectionHelper.newArrayList();
// for (Annotated annotation : annotations) {
// if (locationNames.contains(annotation.getValue())) {
// LOGGER.debug("Candiate {}", annotation);
// Collection<Location> cluster = locations.get(annotation.getValue());
// allLocations.addAll(cluster);
// } else {
// LOGGER.info("Skip {}", annotation);
// }
// }
//
// // do the clustering thing
// List<LocationCluster> clusters = CollectionHelper.newArrayList();
// for (Location location : allLocations) {
// // do not add countries to clusters
// if (location.getType() == LocationType.COUNTRY) {
// continue;
// }
// // do not add units
// if (location.getType() == LocationType.UNIT) {
// continue;
// }
// // do not add anchor locaitons to cluster
// if (location.getType() != LocationType.CITY && anchors.contains(location)) {
// continue;
// }
// double smallestDistance = Double.MAX_VALUE;
// LocationCluster nearestCluster = null;
// for (LocationCluster cluster : clusters) {
// // double distance = cluster.getSmallestDistance(location);
// double distance = cluster.getDistanceToCenter(location);
// if (distance < smallestDistance) {
// smallestDistance = distance;
// nearestCluster = cluster;
// }
// }
// if (nearestCluster != null && smallestDistance < MAX_DISTANCE) {
// boolean nameClash = nearestCluster.nameClash(location);
// if (nameClash) {
// LOGGER.debug("Name clash for {}, create new cluster", location);
// LocationCluster newCluster = new LocationCluster(locationNames);
// newCluster.add(location);
// clusters.add(newCluster);
// } else {
// nearestCluster.add(location);
// }
// } else {
// LocationCluster newCluster = new LocationCluster(locationNames);
// newCluster.add(location);
// clusters.add(newCluster);
// }
// }
//
// Collections.sort(clusters);
// int i = 0;
// for (LocationCluster locationCluster : clusters) {
// Set<Integer> anchorIds = CollectionHelper.newHashSet();
// for (Location anchorLocation : anchors) {
// anchorIds.add(anchorLocation.getId());
// }
// boolean anchored = false;
// out: for (Location location : locationCluster.locations) {
// for (Integer ancestorId : location.getAncestorIds()) {
// if (anchorIds.contains(ancestorId)) {
// anchored = true;
// break out;
// }
// }
// }
// i++;
// // if (locationCluster.totalPopulation() > 10000)
// // locationCluster.condense();
// // if (anchored)
// LOGGER.info("Cluster " + i + ", size " + locationCluster.size() + ", totalPopulation "
// + locationCluster.totalPopulation() /*
// * + ", maxDistanceFromCenter "
// * + locationCluster.getMaximumDistanceFromCenter()
// */+ ", anchored " + anchored + ", coverage "
// + locationCluster.getCoverage() + " : "
// + locationCluster);
// }
// LOGGER.info("# clusters " + i);
//
// return result;
// }
//
// public static boolean sameName(Location l1, Location l2) {
// Set<String> names1 = CollectionHelper.newHashSet();
// names1.add(l1.getPrimaryName());
// for (AlternativeName alternativeName : l1.getAlternativeNames()) {
// names1.add(alternativeName.getName());
// }
// Set<String> names2 = CollectionHelper.newHashSet();
// names2.add(l2.getPrimaryName());
// for (AlternativeName alternativeName : l2.getAlternativeNames()) {
// names2.add(alternativeName.getName());
// }
// names2.retainAll(names1);
// return names2.size() > 0;
// }
//
// }
