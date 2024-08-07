package ws.palladian.retrieval.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
     * @author Philipp Katz
     */
    public static class Builder extends BasicWebContent.Builder {

        protected String videoUrl;
        protected String thumbnailUrl;
        protected Integer duration;
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

        public Builder setDuration(Integer duration) {
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
    private final Integer duration;
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
    public Integer getDuration() {
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
    protected List<String> getToStringParts() {
        List<String> toStringParts = new ArrayList<>(super.getToStringParts());
        if (videoUrl != null) {
            toStringParts.add(String.format("videoUrl=%s", videoUrl));
        }
        if (thumbnailUrl != null) {
            toStringParts.add(String.format("thumbnailUrl=%s", thumbnailUrl));
        }
        if (duration != null) {
            toStringParts.add(String.format("duration=%s", duration));
        }
        if (views != null) {
            toStringParts.add(String.format("views=%s", views));
        }
        if (rating != null) {
            toStringParts.add(String.format("rating=%s", rating));
        }
        return toStringParts;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(duration, rating, thumbnailUrl, videoUrl, views);
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
        BasicWebVideo other = (BasicWebVideo) obj;
        return Objects.equals(duration, other.duration) && Objects.equals(rating, other.rating)
                && Objects.equals(thumbnailUrl, other.thumbnailUrl) && Objects.equals(videoUrl, other.videoUrl)
                && Objects.equals(views, other.views);
    }

}
