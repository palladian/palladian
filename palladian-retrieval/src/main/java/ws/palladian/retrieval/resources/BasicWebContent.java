package ws.palladian.retrieval.resources;

import org.apache.commons.lang3.StringUtils;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.geo.GeoCoordinate;

import java.util.*;

/**
 * <p>
 * {@link BasicWebContent}s represent search results from web search engines. For instantiation use the {@link Builder}.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public class BasicWebContent implements WebContent {

    /**
     * <p>
     * Builder for creating new instances of {@link WebContent}.
     * </p>
     *
     * @author Philipp Katz
     */
    public static class Builder implements Factory<WebContent> {

        protected int id = -1;
        protected String url;
        protected String title;
        protected String summary;
        protected Date published;
        protected GeoCoordinate coordinate;
        protected String identifier;
        protected Set<String> tags = new HashSet<>();
        protected String source;
        protected Map<String, Object> additionalData = new HashMap<>();

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setSummary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder setPublished(Date published) {
            this.published = published;
            return this;
        }

        public Builder setCoordinate(GeoCoordinate coordinate) {
            this.coordinate = coordinate;
            return this;
        }

        public Builder setCoordinate(double latitude, double longitude) {
            this.coordinate = GeoCoordinate.from(latitude, longitude);
            return this;
        }

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setTags(Set<String> tags) {
            this.tags = tags != null ? new HashSet<>(tags) : Collections.<String>emptySet();
            return this;
        }

        public Builder addTag(String tag) {
            this.tags.add(tag);
            return this;
        }

        public Builder setSource(String source) {
            this.source = source;
            return this;
        }

        public Builder setAdditionalData(String key, Object value) {
            this.additionalData.put(key, value);
            return this;
        }

        public Builder setWebContent(WebContent webContent) {
            this.id = webContent.getId();
            this.url = webContent.getUrl();
            this.title = webContent.getTitle();
            this.summary = webContent.getSummary();
            this.published = webContent.getPublished();
            this.coordinate = webContent.getCoordinate();
            this.identifier = webContent.getIdentifier();
            this.tags = new HashSet<>(webContent.getTags());
            this.source = webContent.getSource();
            this.additionalData = new HashMap<>(webContent.getAdditionalData());
            return this;
        }

        @Override
        public WebContent create() {
            return new BasicWebContent(this);
        }

    }

    private final int id;

    private final String url;

    private final String title;

    private final String summary;

    private final Date published;

    private final GeoCoordinate coordinate;

    private final String identifier;

    private final Set<String> tags;

    private final String source;

    private final Map<String, Object> additionalData;

    protected BasicWebContent(WebContent webResult) {
        this.id = webResult.getId();
        this.url = webResult.getUrl();
        this.title = webResult.getTitle();
        this.summary = webResult.getSummary();
        this.published = webResult.getPublished();
        this.coordinate = webResult.getCoordinate();
        this.identifier = webResult.getIdentifier();
        this.tags = new HashSet<>(webResult.getTags());
        this.source = webResult.getSource();
        this.additionalData = new HashMap<>(webResult.getAdditionalData());
    }

    protected BasicWebContent(Builder builder) {
        this.id = builder.id;
        this.url = builder.url;
        this.title = builder.title;
        this.summary = builder.summary;
        this.published = builder.published;
        this.coordinate = builder.coordinate;
        this.identifier = builder.identifier;
        this.tags = builder.tags;
        this.source = builder.source;
        this.additionalData = builder.additionalData;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public Date getPublished() {
        return published;
    }

    @Override
    public GeoCoordinate getCoordinate() {
        return coordinate;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public Map<String, Object> getAdditionalData() {
        return Collections.unmodifiableMap(additionalData);
    }

    @Override
    public final String toString() {
        List<String> toStringParts = getToStringParts();
        String className = getClass().getSimpleName();
        return String.format("%s [%s]", className, StringUtils.join(toStringParts, ','));
    }

    /**
     * @return All attributes to return in the {@link #toString()} method. Take care to invoke the super method when
     * overriding this in sub classes.
     */
    protected List<String> getToStringParts() {
        List<String> toStringParts = new ArrayList<>();
        if (id != -1) {
            toStringParts.add(String.format("id=%s", id));
        }
        if (url != null) {
            toStringParts.add(String.format("url=%s", url));
        }
        if (title != null) {
            toStringParts.add(String.format("title=%s", title));
        }
        //        if (summary != null) {
        //            toStringParts.add(String.format("summary=%s", StringHelper.shortenEllipsis(summary, 100)));
        //        }
        if (published != null) {
            toStringParts.add(String.format("published=%s", published));
        }
        if (coordinate != null) {
            toStringParts.add(String.format("coordinate=%s", coordinate));
        }
        if (identifier != null) {
            toStringParts.add(String.format("identifier=%s", identifier));
        }
        if (tags != null && tags.size() > 0) {
            toStringParts.add(String.format("tags=%s", tags));
        }
        if (source != null) {
            toStringParts.add(String.format("source=%s", source));
        }
        return toStringParts;
    }

    @Override
    public int hashCode() {
        return Objects.hash(published, summary, title, url);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BasicWebContent other = (BasicWebContent) obj;
        return Objects.equals(published, other.published) && Objects.equals(summary, other.summary)
                && Objects.equals(title, other.title) && Objects.equals(url, other.url);
    }

}
