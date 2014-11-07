package ws.palladian.retrieval.resources;

/**
 * <p>
 * A {@link BasicWebContent} representing video links.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class BasicWebVideo extends BasicWebContent implements WebVideo {

    /**
     * <p>
     * Builder for creating new instances of {@link WebVideo}.
     * </p>
     * 
     * @author katz
     */
    public static class Builder extends BasicWebContent.Builder {

        protected String videoUrl;
        protected String thumbnailUrl;
        protected Long duration;
        protected Integer views;
        protected Double rating;

        public Builder setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
            return this;
        }

        public Builder setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return this;
        }

        public Builder setDuration(Long duration) {
            this.duration = duration;
            return this;
        }

        public Builder setViews(Integer views) {
            this.views = views;
            return this;
        }

        public Builder setRating(Double rating) {
            this.rating = rating;
            return this;
        }

        public Builder setWebVideo(WebVideo webVideo) {
            super.setWebContent(webVideo);
            this.videoUrl = webVideo.getVideoUrl();
            this.thumbnailUrl = webVideo.getThumbnailUrl();
            this.duration = webVideo.getDuration();
            this.views = webVideo.getViews();
            this.rating = webVideo.getRating();
            return this;
        }

        @Override
        public WebVideo create() {
            return new BasicWebVideo(this);
        }

    }

    private final String videoUrl;
    private final String thumbnailUrl;
    private final Long duration;
    private final Integer views;
    private final Double rating;

    private BasicWebVideo(Builder builder) {
        super(builder);
        this.videoUrl = builder.videoUrl;
        this.thumbnailUrl = builder.thumbnailUrl;
        this.duration = builder.duration;
        this.views = builder.views;
        this.rating = builder.rating;
    }

    @Override
    public String getVideoUrl() {
        return videoUrl;
    }

    @Override
    public Long getDuration() {
        return duration;
    }

    @Override
    public Integer getViews() {
        return views;
    }

    @Override
    public Double getRating() {
        return rating;
    }

    @Override
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WebVideo [");
        if (videoUrl != null) {
            builder.append("videoUrl=");
            builder.append(videoUrl);
        }
        if (thumbnailUrl != null) {
            builder.append(", thumbnailUrl=");
            builder.append(thumbnailUrl);
        }
        if (duration != null) {
            builder.append(", duration=");
            builder.append(duration);
        }
        if (views != null) {
            builder.append(", views=");
            builder.append(views);
        }
        if (rating != null) {
            builder.append(", rating=");
            builder.append(rating);
        }
        if (getUrl() != null) {
            builder.append(", url=");
            builder.append(getUrl());
        }
        if (getTitle() != null) {
            builder.append(", title=");
            builder.append(getTitle());
        }
        if (getSummary() != null) {
            builder.append(", summary=");
            builder.append(getSummary());
        }
        if (getPublished() != null) {
            builder.append(", published=");
            builder.append(getPublished());
        }
        if (getCoordinate() != null) {
            builder.append(", coordinate=");
            builder.append(getCoordinate());
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((duration == null) ? 0 : duration.hashCode());
        result = prime * result + ((rating == null) ? 0 : rating.hashCode());
        result = prime * result + ((thumbnailUrl == null) ? 0 : thumbnailUrl.hashCode());
        result = prime * result + ((videoUrl == null) ? 0 : videoUrl.hashCode());
        result = prime * result + ((views == null) ? 0 : views.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        BasicWebVideo other = (BasicWebVideo)obj;
        if (duration == null) {
            if (other.duration != null)
                return false;
        } else if (!duration.equals(other.duration))
            return false;
        if (rating == null) {
            if (other.rating != null)
                return false;
        } else if (!rating.equals(other.rating))
            return false;
        if (thumbnailUrl == null) {
            if (other.thumbnailUrl != null)
                return false;
        } else if (!thumbnailUrl.equals(other.thumbnailUrl))
            return false;
        if (videoUrl == null) {
            if (other.videoUrl != null)
                return false;
        } else if (!videoUrl.equals(other.videoUrl))
            return false;
        if (views == null) {
            if (other.views != null)
                return false;
        } else if (!views.equals(other.views))
            return false;
        return true;
    }

}
