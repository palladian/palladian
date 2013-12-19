package ws.palladian.retrieval.resources;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Factory;

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
     * @author katz
     */
    public static class Builder implements Factory<WebContent> {

        protected int id = -1;
        protected String url;
        protected String title;
        protected String summary;
        protected Date published;
        protected GeoCoordinate coordinate;
        protected String identifier;
        protected Set<String> tags = CollectionHelper.newHashSet();
        protected String source;

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
            this.coordinate = new ImmutableGeoCoordinate(latitude, longitude);
            return this;
        }

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setTags(Set<String> tags) {
            this.tags = tags != null ? new HashSet<String>(tags) : Collections.<String> emptySet();
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

        public Builder setWebContent(WebContent webContent) {
            this.id = webContent.getId();
            this.url = webContent.getUrl();
            this.title = webContent.getTitle();
            this.summary = webContent.getSummary();
            this.published = webContent.getPublished();
            this.coordinate = webContent.getCoordinate();
            this.identifier = webContent.getIdentifier();
            this.tags = new HashSet<String>(webContent.getTags());
            this.source = webContent.getSource();
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

    protected BasicWebContent(WebContent webResult) {
        this.id = webResult.getId();
        this.url = webResult.getUrl();
        this.title = webResult.getTitle();
        this.summary = webResult.getSummary();
        this.published = webResult.getPublished();
        this.coordinate = webResult.getCoordinate();
        this.identifier = webResult.getIdentifier();
        this.tags = new HashSet<String>(webResult.getTags());
        this.source = webResult.getSource();
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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WebContent [");
        if (id != -1) {
            builder.append("id=");
            builder.append(id);
        }
        if (url != null) {
            builder.append("url=");
            builder.append(url);
        }
        if (title != null) {
            builder.append(", title=");
            builder.append(title);
        }
        if (summary != null) {
            builder.append(", summary=");
            builder.append(summary);
        }
        if (published != null) {
            builder.append(", published=");
            builder.append(published);
        }
        if (coordinate != null) {
            builder.append(", coordinate=");
            builder.append(coordinate);
        }
        if (identifier != null) {
            builder.append(", identifier=");
            builder.append(identifier);
        }
        if (tags != null && tags.size() > 0) {
            builder.append(", tags=");
            builder.append(tags);
        }
        if (source != null) {
            builder.append(", source");
            builder.append(source);
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((published == null) ? 0 : published.hashCode());
        result = prime * result + ((summary == null) ? 0 : summary.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
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
        BasicWebContent other = (BasicWebContent)obj;
        if (published == null) {
            if (other.published != null)
                return false;
        } else if (!published.equals(other.published))
            return false;
        if (summary == null) {
            if (other.summary != null)
                return false;
        } else if (!summary.equals(other.summary))
            return false;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }

}