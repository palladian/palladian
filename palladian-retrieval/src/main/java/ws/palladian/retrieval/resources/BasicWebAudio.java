package ws.palladian.retrieval.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A {@link BasicWebContent} representing audio links.
 *
 * @author Philipp Katz
 */
public class BasicWebAudio extends BasicWebContent implements WebAudio {

    /**
     * Builder for creating new instances of {@link WebAudio}.
     *
     * @author Philipp Katz
     */
    public static class Builder extends BasicWebContent.Builder {

        protected String audioUrl;
        protected Integer duration;

        public Builder setAudioUrl(String audioUrl) {
            this.audioUrl = audioUrl;
            return this;
        }

        public Builder setDuration(Integer duration) {
            this.duration = duration;
            return this;
        }

        public Builder setWebAudio(WebAudio webVideo) {
            super.setWebContent(webVideo);
            this.audioUrl = webVideo.getAudioUrl();
            this.duration = webVideo.getDuration();
            return this;
        }

        @Override
        public WebAudio create() {
            return new BasicWebAudio(this);
        }

    }

    private final String audioUrl;
    private final Integer duration;

    private BasicWebAudio(Builder builder) {
        super(builder);
        this.audioUrl = builder.audioUrl;
        this.duration = builder.duration;
    }

    @Override
    public String getAudioUrl() {
        return audioUrl;
    }

    @Override
    public Integer getDuration() {
        return duration;
    }

    @Override
    protected List<String> getToStringParts() {
        List<String> toStringParts = new ArrayList<>(super.getToStringParts());
        if (audioUrl != null) {
            toStringParts.add(String.format("audioUrl=%s", audioUrl));
        }
        if (duration != null) {
            toStringParts.add(String.format("duration=%s", duration));
        }
        return toStringParts;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(audioUrl, duration);
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
        BasicWebAudio other = (BasicWebAudio) obj;
        return Objects.equals(audioUrl, other.audioUrl) && Objects.equals(duration, other.duration);
    }

}
