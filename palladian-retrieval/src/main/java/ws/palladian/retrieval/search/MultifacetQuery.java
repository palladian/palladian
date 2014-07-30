package ws.palladian.retrieval.search;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;

/**
 * <p>
 * A query which combines multiple facets (text, time, space, language). Use the {@link Builder} to create instances.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface MultifacetQuery {

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

        String id;
        String url;
        Set<String> tags;
        String text;
        Date startDate;
        Date endDate;
        int resultCount = DEFAULT_RESULT_COUNT;
        GeoCoordinate coordinate;
        Double radius;
        Language language;
        int resultPage = DEFAULT_PAGE;

        // additional facets, in the future, we might want to make this super-generic and also replace the dedicated
        // fields given above by facets
        final Map<String, Facet> facets = CollectionHelper.newHashMap();

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

        public Builder setUrl(String url) {
            this.url = url;
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
            Validate.isTrue(resultCount > 0, "resultCount must be greater zero, was %d.", resultCount);
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
            if (startDate != null && endDate != null) {
                Validate.isTrue(startDate.before(endDate), "startDate must be before endDate");
            }
            return new ImmutableMultifacetQuery(this);
        }

    }

    /**
     * @return The source-specific identifier of the sought content, or <code>null</code> in case not specified.
     */
    String getId();

    /**
     * @return The URL of the sought content, or <code>null</code> in case not specified.
     */
    // XXX in StackExchangeSearcher, this searches for *contained* URL, make more clear, what's going on here, or
    // provide this as dedicated property?
    String getUrl();

    /**
     * @return The tags, or an empty set if not specified.
     */
    Set<String> getTags();

    /**
     * @return The text, or <code>null</code> in case not specified.
     */
    String getText();

    /**
     * @return The start date of the interval to search, or <code>null</code> in case not specified.
     */
    Date getStartDate();

    /**
     * @return The end date of the interval to search, or <code>null</code> in case not specified.
     */
    Date getEndDate();

    /**
     * @return The number of desired results, must be greater zero.
     */
    int getResultCount();

    /**
     * @return The coordinate where to search, or <code>null</code> in case not specified.
     */
    GeoCoordinate getCoordinate();

    /**
     * @return The radius in kilometers, or <code>null</code> in case no radius was specified.
     */
    Double getRadius();

    /**
     * @return The language of the sought content, or <code>null</code> in case not specified.
     */
    Language getLanguage();

    /**
     * <p>
     * Get a searcher-specific additional facet which is not included in this API. The convention is, that additional
     * facets should be specified as static nested classes within the corresponding searcher.
     * </p>
     * 
     * @param identifier The identifier of the facet to retrieve, not <code>null</code>.
     * @return The facet for the specified identifier, or <code>null</code> in case no facet with the given identifier
     *         exists.
     */
    Facet getFacet(String identifier);

    /**
     * @return The page of the result list to get.
     */
    int getResultPage();

}
