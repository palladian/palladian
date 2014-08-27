package ws.palladian.classification.text;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.FeatureSetting.TextFeatureType;
import ws.palladian.core.Token;
import ws.palladian.extraction.token.CharacterNGramTokenizer;
import ws.palladian.extraction.token.NGramWrapperIterator;
import ws.palladian.extraction.token.WordTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.nlp.StringHelper;

public class Preprocessor implements Function<String, Iterator<String>> {

    private final FeatureSetting featureSetting;

    public Preprocessor(FeatureSetting featureSetting) {
        Validate.notNull(featureSetting, "featureSetting must not be null");
        this.featureSetting = featureSetting;
    }

    @Override
    public Iterator<String> compute(String input) {
        String content = input;
        if (!featureSetting.isCaseSensitive()) {
            content = content.toLowerCase();
        }
        int minNGramLength = featureSetting.getMinNGramLength();
        int maxNGramLength = featureSetting.getMaxNGramLength();
        Iterator<Token> tokenIterator;
        if (featureSetting.getTextFeatureType() == TextFeatureType.CHAR_NGRAMS) {
            tokenIterator = new CharacterNGramTokenizer(minNGramLength, maxNGramLength).iterateSpans(content);
        } else if (featureSetting.getTextFeatureType() == TextFeatureType.WORD_NGRAMS) {
            tokenIterator = new WordTokenizer().iterateSpans(content);
            tokenIterator = new NGramWrapperIterator(tokenIterator, minNGramLength, maxNGramLength);
        } else {
            throw new UnsupportedOperationException("Unsupported feature type: " + featureSetting.getTextFeatureType());
        }
        if (featureSetting.isWordUnigrams()) {
            tokenIterator = CollectionHelper.filter(tokenIterator, new Filter<Token>() {
                int minTermLength = featureSetting.getMinimumTermLength();
                int maxTermLength = featureSetting.getMaximumTermLength();

                @Override
                public boolean accept(Token item) {
                    return item.getValue().length() >= minTermLength && item.getValue().length() <= maxTermLength;
                }
            });
        }
        // XXX looks a bit "magic" to me, does that really improve results in general?
        tokenIterator = CollectionHelper.filter(tokenIterator, new Filter<Token>() {
            @Override
            public boolean accept(Token item) {
                String value = item.getValue();
                return !StringHelper.containsAny(value, Arrays.asList("&", "/", "=")) && !StringHelper.isNumber(value);
            }
        });
        return CollectionHelper.convert(tokenIterator, Token.STRING_CONVERTER);
    }

}
