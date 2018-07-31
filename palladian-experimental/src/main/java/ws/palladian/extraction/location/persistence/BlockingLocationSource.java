package ws.palladian.extraction.location.persistence;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.sources.MultiQueryLocationSource;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.BloomFilter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.functional.Functions;
import java.util.function.Predicate;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

public class BlockingLocationSource extends MultiQueryLocationSource {

    /** The estimated number of items we will be adding to the Bloom filter. */
    private static final int ESTIMATED_SIZE = 15000000;

    /** The accepted false positive probability (i.e. 1%). */
    private static final double FALSE_POSITIVE_PROBABILITY = 0.01;

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingLocationSource.class);

    private final LocationSource wrapped;

    private final Predicate<String> filter;

    private int blockedItems;

    private int acceptedItems;

    private int requests;

    private int passedRequests;

    public BlockingLocationSource(LocationSource wrapped) {
        this(wrapped, initializeBloomFilter(wrapped));
    }

    public BlockingLocationSource(LocationSource wrapped, Predicate<String> filter) {
        this.wrapped = wrapped;
        this.filter = filter;
    }

    public static final BloomFilter<String> initializeBloomFilter(LocationSource source) {
        LOGGER.info("Initializing bloom filter (this takes some time)...");
        BloomFilter<String> filter = new BloomFilter<String>(FALSE_POSITIVE_PROBABILITY, ESTIMATED_SIZE);
        Iterator<Location> iterator = source.getLocations();
        ProgressMonitor monitor = new ProgressMonitor();
        monitor.startTask("Initializing filter", source.size());
        while (iterator.hasNext()) {
            Set<String> locationNames = iterator.next().collectAlternativeNames();
            Set<String> lowercaseLocationNames = CollectionHelper.convertSet(locationNames, Functions.LOWERCASE);
            filter.addAll(lowercaseLocationNames);
            monitor.increment();
        }
        return filter;
    }

    @Override
    public MultiMap<String, Location> getLocations(Collection<String> locationNames, Set<Language> languages) {
        Set<String> lowercaseLocationNames = CollectionHelper.convertSet(locationNames, Functions.LOWERCASE);
        Set<String> needsLookup = CollectionHelper.filterSet(lowercaseLocationNames, filter);
        int blocked = new HashSet<>(lowercaseLocationNames).size() - needsLookup.size();
        LOGGER.debug("get {} locations, blocked {} locations", needsLookup.size(), blocked);
        blockedItems += blocked;
        acceptedItems += needsLookup.size();
        requests++;
        passedRequests += needsLookup.size() > 0 ? 1 : 0;
        return wrapped.getLocations(needsLookup, languages);
    }

    @Override
    public List<Location> getLocations(List<Integer> locationIds) {
        requests++;
        passedRequests++;
        return wrapped.getLocations(locationIds);
    }

    @Override
    public String toString() {
        double blockPercentage = (double)blockedItems / (blockedItems + acceptedItems);
        StringBuilder builder = new StringBuilder();
        builder.append("BlockingLocationSource [");
        builder.append(wrapped);
        builder.append(", BlockedItems=").append(blockedItems);
        builder.append(", AcceptedItems=").append(acceptedItems);
        builder.append(", BlockPercentage=").append(blockPercentage);
        builder.append(", Requests=").append(requests);
        builder.append(", PassedRequests=").append(passedRequests);
        builder.append(", Filter=").append(filter);
        builder.append("]");
        return builder.toString();
    }

    public static void main(String[] args) throws IOException {
        LocationSource source = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        BloomFilter<String> bloomFilter = initializeBloomFilter(source);
        FileHelper.serialize(bloomFilter, "bloomFilter.ser");
    }

}
