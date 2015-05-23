package ws.palladian.extraction.location;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;

/**
 * <p>
 * Common implementation of {@link Location} interface with utility functionality. {@link #hashCode()} and
 * {@link #equals(Object)} are determined via the {@link Location} ID ({@link #getId()}).
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class AbstractLocation implements Location {

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
        Set<String> names = new HashSet<>();
        names.add(LocationExtractorUtils.normalizeName(this.getPrimaryName()));
        for (AlternativeName alternativeName : this.getAlternativeNames()) {
            names.add(LocationExtractorUtils.normalizeName(alternativeName.getName()));
        }
        return names;
    }

    // deprecated getters (returning null values when no coordinate is present)

    @Override
    @Deprecated
    public final Double getLatitude() {
        return getCoordinate() != null ? getCoordinate().getLatitude() : null;
    }

    @Override
    @Deprecated
    public final Double getLongitude() {
        return getCoordinate() != null ? getCoordinate().getLongitude() : null;
    }

    // toString

    @Override
    public String toString() {
        List<String> toStringParts = new ArrayList<>();
        if (getId() != -1) {
            toStringParts.add(String.format("id=%s", getId()));
        }
        toStringParts.add(String.format("primaryName=%s", getPrimaryName()));
        if (getType() != LocationType.UNDETERMINED) {
            toStringParts.add(String.format("type=%s", getType()));
        }
        if (getCoordinate() != null) {
            toStringParts.add(String.format("coordinate=%s", getCoordinate()));
        }
        if (getPopulation() != null) {
            toStringParts.add(String.format("population=%s", getPopulation()));
        }
        return String.format("%s [%s]", getClass().getSimpleName(), StringUtils.join(toStringParts, ','));
    }

    @Override
    public boolean hasName(String name, Set<Language> languages) {
        Validate.notNull(name, "name must not be null");
        Validate.notNull(languages, "languages must not be null");
        if (equalName(getPrimaryName(), name)) {
            return true;
        }
        for (AlternativeName alternativeName : getAlternativeNames()) {
            String currentName = alternativeName.getName();
            Language currentLang = alternativeName.getLanguage();
            if (equalName(currentName, name) && (currentLang == null || languages.contains(currentLang))) {
                return true;
            }
        }
        return false;
    }

    private static boolean equalName(String name1, String name2) {
        String normalized1 = StringUtils.stripAccents(name1).toLowerCase();
        String normalized2 = StringUtils.stripAccents(name2).toLowerCase();
        return normalized1.equals(normalized2);
    }

    // hashCode and equals

    @Override
    public int hashCode() {
        return getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbstractLocation other = (AbstractLocation)obj;
        return getId() == other.getId();
    }

}
