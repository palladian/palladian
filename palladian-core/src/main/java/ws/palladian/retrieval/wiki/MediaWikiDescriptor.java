package ws.palladian.retrieval.wiki;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.constants.Language;
import ws.palladian.helper.functional.Factory;

/**
 * <p>
 * Denotes a specific MediaWiki platform, identified by its API endpoint. Use the {@link Builder} to instantiate.
 * 
 * @author pk
 */
public interface MediaWikiDescriptor {
    /**
     * @return The URL of the MediaWiki API endpoint (e.g. <code>http://en.wikipedia.org/w/api.php</code> for the
     *         English Wikipedia).
     */
    String getEndpoint();

    public static final class Builder implements Factory<MediaWikiDescriptor> {

        public static Builder wikipedia() {
            return new Builder("http://{language}.wikipedia.org/w/api.php");
        }

        public static Builder wikimedia() {
            return new Builder("http://commons.wikimedia.org/w/api.php");
        }

        public static Builder wikinews() {
            return new Builder("http://{language}.wikinews.org/w/api.php");
        }

        public static Builder wikiquote() {
            return new Builder("http://{language}.wikiquote.org/w/api.php");
        }

        public static Builder wiktionary() {
            return new Builder("http://{language}.wiktionary.org/w/api.php");
        }

        private final String url;

        private Language language = Language.ENGLISH;

        public Builder(String url) {
            Validate.notEmpty(url, "url must not be empty");
            this.url = url;
        }

        public Builder language(Language language) {
            Validate.notNull(language, "language must not be null");
            this.language = language;
            return this;
        }

        @Override
        public MediaWikiDescriptor create() {
            return new MediaWikiDescriptor() {
                @Override
                public String getEndpoint() {
                    return url.replace("{language}", language.getIso6391());
                }
            };
        }

    }

}
