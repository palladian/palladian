package ws.palladian.retrieval.resources;

import java.util.Date;

import ws.palladian.extraction.location.GeoCoordinate;
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

        protected String url;
        protected String title;
        protected String summary;
        protected Date published;
        protected GeoCoordinate coordinate;
        protected String identifier;

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
        
        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setWebContent(WebContent webContent) {
            this.url = webContent.getUrl();
            this.title = webContent.getTitle();
            this.summary = webContent.getSummary();
            this.published = webContent.getPublished();
            this.coordinate = webContent.getCoordinate();
            return this;
        }

        @Override
        public WebContent create() {
            return new BasicWebContent(this);
        }

    }

    private final String url;

    private final String title;

    private final String summary;

    private final Date published;

    private final GeoCoordinate coordinate;
    
    private final String identifier;

    protected BasicWebContent(WebContent webResult) {
        this.url = webResult.getUrl();
        this.title = webResult.getTitle();
        this.summary = webResult.getSummary();
        this.published = webResult.getPublished();
        this.coordinate = webResult.getCoordinate();
        this.identifier = webResult.getIdentifier();
    }

    protected BasicWebContent(Builder builder) {
        this.url = builder.url;
        this.title = builder.title;
        this.summary = builder.summary;
        this.published = builder.published;
        this.coordinate = builder.coordinate;
        this.identifier = builder.identifier;
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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WebContent [");
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