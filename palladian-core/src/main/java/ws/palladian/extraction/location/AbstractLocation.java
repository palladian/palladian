package ws.palladian.extraction.location;

import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Common implementation of {@link Location} interface with utility functionality.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class AbstractLocation extends AbstractGeoCoordinate implements Location {

    @Override
    public final boolean descendantOf(Location other) {
        Validate.notNull(other, "other must not be null");
        return this.getAncestorIds().contains(other.getId());
    }

    @Override
    public final boolean childOf(Location other) {
        Validate.notNull(other, "other must not be null");
        Integer firstId = CollectionHelper.getFirst(this.getAncestorIds());
        if (firstId == null) {
            return false;
        }
        return firstId == other.getId();
    }

    @Override
    public final boolean commonName(Location other) {
        Validate.notNull(other, "other must not be null");
        Set<String> names1 = this.collectAlternativeNames();
        Set<String> names2 = other.collectAlternativeNames();
        names1.retainAll(names2);
        return names1.size() > 0;
    }

    @Override
    public final Set<String> collectAlternativeNames() {
        Set<String> names = CollectionHelper.newHashSet();
        names.add(LocationExtractorUtils.normalizeName(this.getPrimaryName()));
        for (AlternativeName alternativeName : this.getAlternativeNames()) {
            names.add(LocationExtractorUtils.normalizeName(alternativeName.getName()));
        }
        return names;
    }

}
