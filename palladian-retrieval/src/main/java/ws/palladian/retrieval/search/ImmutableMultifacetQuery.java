package ws.palladian.retrieval.search;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;

/**
 * Default implementation of a {@link MultifacetQuery}.
 * 
 * @author Philipp Katz
 */
class ImmutableMultifacetQuery implements MultifacetQuery {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final String id;
    private final String url;
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
    ImmutableMultifacetQuery(Builder builder) {
        this.id = builder.id;
        this.url = builder.url;
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

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public Set<String> getTags() {
        return tags != null ? Collections.unmodifiableSet(tags) : Collections.<String> emptySet();
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public int getResultCount() {
        return resultCount;
    }

    @Override
    public GeoCoordinate getCoordinate() {
        return coordinate;
    }

    @Override
    public Double getRadius() {
        return radius;
    }

    @Override
    public Language getLanguage() {
        return language;
    }

    @Override
    public Facet getFacet(String identifier) {
        return facets.get(identifier);
    }

    @Override
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
        if (url != null) {
            toStringParts.add(String.format("url=%s", url));
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
