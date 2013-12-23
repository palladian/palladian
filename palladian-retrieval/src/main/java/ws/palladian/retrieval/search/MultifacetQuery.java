package ws.palladian.retrieval.search;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.constants.Language;

/**
 * <p>
 * A query which combines multiple facets (text, time, space, language). Use the {@link Builder} to create instances.
 * </p>
 * 
 * @author Philipp Katz
 */
public class MultifacetQuery {

    /**
     * <p>
     * Builder for creating a {@link MultifacetQuery}.
     * </p>
     * 
     * @author Philipp Katz
     */
    public static class Builder implements Factory<MultifacetQuery> {

        /** Default number of results if not specified explicitly. */
        private static final int DEFAULT_RESULT_COUNT = 10;

        /** Default offset if not specified explicitly. */
        private static final int DEFAULT_PAGE = 0;

        private String id;
        private Set<String> tags;
        private String text;
        private Date startDate;
        private Date endDate;
        private int resultCount = DEFAULT_RESULT_COUNT;
        private GeoCoordinate coordinate;
        private Double radius;
        private Language language;
        private int resultPage = DEFAULT_PAGE;

        // additional facets, in the future, we might want to make this super-generic and also replace the dedicated
        // fields given above by facets
        private final Map<String, Facet> facets = CollectionHelper.newHashMap();

        /**
         * <p>
         * Set a source internal ID for which to query. For Twitter e.g., one might specify the status ID of the Tweet
         * to retrieve. In general, this makes all other parameters unnecessary.
         * </p>
         * 
         * @param id The ID to set.
         * @return The builder.
         */
        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setTags(Collection<String> tags) {
            this.tags = tags == null ? Collections.<String> emptySet() : new HashSet<String>(tags);
            return this;
        }

        public Builder setText(String text) {
            this.text = text;
            return this;
        }

        public Builder setStartDate(Date startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder setEndDate(Date endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder setResultCount(int resultCount) {
            Validate.isTrue(resultCount > 0, "resultCount must be greater zero.");
            this.resultCount = resultCount;
            return this;
        }

        public Builder setCoordinate(GeoCoordinate coordinate) {
            this.coordinate = coordinate;
            return this;
        }

        public Builder setCoordinate(double lat, double lng) {
            this.coordinate = new ImmutableGeoCoordinate(lat, lng);
            return this;
        }

        /**
         * <p>
         * Set the distance from the given coordinate.
         * </p>
         * 
         * @param radius The distance in kilometers, greater/equal zero.
         * @return The builder.
         */
        public Builder setRadius(Double radius) {
            if (radius != null) {
                Validate.isTrue(radius >= 0, "radius must be greater/equal zero.");
            }
            this.radius = radius;
            return this;
        }

        public Builder setLanguage(Language language) {
            this.language = language;
            return this;
        }

        public Builder addFacet(Facet facet) {
            facets.put(facet.getIdentifier(), facet);
            return this;
        }

        public Builder setResultPage(int resultPage) {
            Validate.isTrue(resultPage >= 0, "page must be greater/equal zero.");
            this.resultPage = resultPage;
            return this;
        }

        @Override
        public MultifacetQuery create() {
            return new MultifacetQuery(this);
        }

    }

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final String id;
    private final Set<String> tags;
    private final String text;
    private final Date startDate;
    private final Date endDate;
    private final int resultCount;
    private final GeoCoordinate coordinate;
    private final Double radius;
    private final Language language;
    private final Map<String, Facet> facets;
    private final int resultPage;

    /** Created by the builder. */
    private MultifacetQuery(Builder builder) {
        this.id = builder.id;
        this.tags = builder.tags;
        this.text = builder.text;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.resultCount = builder.resultCount;
        this.coordinate = builder.coordinate;
        this.radius = builder.radius;
        this.language = builder.language;
        this.facets = builder.facets;
        this.resultPage = builder.resultPage;
    }
    
    public String getId() {
        return id;
    }

    public Set<String> getTags() {
        return tags != null ? Collections.unmodifiableSet(tags) : Collections.<String> emptySet();
    }

    public String getText() {
        return text;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public int getResultCount() {
        return resultCount;
    }

    public GeoCoordinate getCoordinate() {
        return coordinate;
    }

    public Double getRadius() {
        return radius;
    }

    public Language getLanguage() {
        return language;
    }

    public Facet getFacet(String identifier) {
        return facets.get(identifier);
    }

    public int getResultPage() {
        return resultPage;
    }

    @Override
    public String toString() {
        DateFormat format = new SimpleDateFormat(DATE_FORMAT);
        List<String> toStringParts = CollectionHelper.newArrayList();

        if (id != null) {
            toStringParts.add(String.format("id=%s", id));
        }
        if (tags != null) {
            toStringParts.add(String.format("tags=%s", tags));
        }
        if (text != null) {
            toStringParts.add(String.format("text=%s", text));
        }
        if (startDate != null) {
            toStringParts.add(String.format("startDate=%s", format.format(startDate)));
        }
        if (endDate != null) {
            toStringParts.add(String.format("endDate=%s", format.format(endDate)));
        }
        toStringParts.add(String.format("resultCount=%s", resultCount));
        if (coordinate != null && radius != null) {
            toStringParts.add(String.format("coordinate=%s", coordinate));
            toStringParts.add(String.format("radius=%s", radius));
        }
        if (language != null) {
            toStringParts.add(String.format("language=%s", language));
        }
        if (facets.size() > 0) {
            toStringParts.add(String.format("facets=%s", facets));
        }
        toStringParts.add(String.format("resultPage=%s", resultPage));

        return String.format("MultifacetQuery [%s]", StringUtils.join(toStringParts, ','));
    }

}
