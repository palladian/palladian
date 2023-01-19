package ws.palladian.extraction.location.sources;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;

import java.util.*;
import java.util.concurrent.*;

/**
 * <p>
 * Wrapper around another {@link LocationSource} which parallelizes requests.
 *
 * @author Philipp Katz
 */
public final class ParallelizedRequestLocationSource extends MultiQueryLocationSource {

    private final LocationSource source;

    private final int numThreads;

    /**
     * Create a new {@link ParallelizedRequestLocationSource} wrapping another {@link LocationSource}.
     *
     * @param source     The {@link LocationSource} to wrap, not <code>null</code>.
     * @param numThreads The number of parallel requests, greater zero.
     */
    public ParallelizedRequestLocationSource(LocationSource source, int numThreads) {
        Validate.notNull(source, "source must not be null");
        Validate.isTrue(numThreads > 0, "numThreads must be greater zero");
        this.source = source;
        this.numThreads = numThreads;
    }

    @Override
    public MultiMap<String, Location> getLocations(Collection<String> locationNames, final Set<Language> languages) {
        List<Callable<Pair<String, Collection<Location>>>> tasks = new ArrayList<>();
        for (final String locationName : locationNames) {
            tasks.add(new Callable<Pair<String, Collection<Location>>>() {
                @Override
                public Pair<String, Collection<Location>> call() throws Exception {
                    Collection<Location> locations = source.getLocations(locationName, languages);
                    return Pair.of(locationName, locations);
                }
            });
        }
        List<Pair<String, Collection<Location>>> result = executeParallel(tasks, numThreads);
        MultiMap<String, Location> locationMap = DefaultMultiMap.createWithSet();
        for (Pair<String, Collection<Location>> item : result) {
            locationMap.put(item.getLeft(), item.getRight());
        }
        return locationMap;
    }

    private static <T> List<T> executeParallel(List<Callable<T>> tasks, int numThreads) {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<T> results = new ArrayList<>();
        try {
            List<Future<T>> futures = executor.invokeAll(tasks);
            for (Future<T> future : futures) {
                results.add(future.get());
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException(cause);
        } finally {
            executor.shutdown();
        }
        return results;
    }

    @Override
    public List<Location> getLocations(final List<Integer> locationIds) {
        List<Callable<Location>> tasks = new ArrayList<>();
        for (final Integer locationId : locationIds) {
            tasks.add(new Callable<Location>() {
                @Override
                public Location call() throws Exception {
                    return source.getLocation(locationId);
                }
            });
        }
        List<Location> locations = executeParallel(tasks, numThreads);
        // sort the returned list, so that we have the order of the given locations IDs
        Collections.sort(locations, new Comparator<Location>() {
            @Override
            public int compare(Location l0, Location l1) {
                return locationIds.indexOf(l0.getId()) - locationIds.indexOf(l1.getId());
            }
        });
        return locations;
    }

    @Override
    public List<Location> getLocations(GeoCoordinate coordinate, double distance) {
        return source.getLocations(coordinate, distance);
    }

    @Override
    public Iterator<Location> getLocations() {
        return source.getLocations();
    }

    @Override
    public int size() {
        return source.size();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ParallelizedRequestLocationSource.class.getSimpleName());
        stringBuilder.append(" (");
        stringBuilder.append(source);
        stringBuilder.append(", NumThreads=").append(numThreads);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

}
