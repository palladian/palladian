package ws.palladian.classification.text;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.FeatureSetting.TextFeatureType;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Function;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.nlp.StringHelper;

public class Preprocessor implements Function<String, Iterator<String>> {

    private final FeatureSetting featureSetting;

    public Preprocessor(FeatureSetting featureSetting) {
        Validate.notNull(featureSetting, "featureSetting must not be null");
        this.featureSetting = featureSetting;
    }

    @Override
    public Iterator<String> compute(String input) {
        String lowercaseContent = input.toLowerCase();
        int minNGramLength = featureSetting.getMinNGramLength();
        int maxNGramLength = featureSetting.getMaxNGramLength();
        Iterator<String> tokenIterator;
        if (featureSetting.getTextFeatureType() == TextFeatureType.CHAR_NGRAMS) {
            tokenIterator = new CharacterNGramIterator(lowercaseContent, minNGramLength, maxNGramLength);
        } else if (featureSetting.getTextFeatureType() == TextFeatureType.WORD_NGRAMS) {
            tokenIterator = new TokenIterator(lowercaseContent);
            tokenIterator = new NGramWrapperIterator(tokenIterator, minNGramLength, maxNGramLength);
        } else {
            throw new UnsupportedOperationException("Unsupported feature type: " + featureSetting.getTextFeatureType());
        }
        if (featureSetting.isWordUnigrams()) {
            tokenIterator = CollectionHelper.filter(tokenIterator, new Filter<String>() {
                int minTermLength = featureSetting.getMinimumTermLength();
                int maxTermLength = featureSetting.getMaximumTermLength();

                @Override
                public boolean accept(String item) {
                    return item.length() >= minTermLength && item.length() <= maxTermLength;
                }
            });
        }
        // XXX looks a bit "magic" to me, does that really improve results in general?
        tokenIterator = CollectionHelper.filter(tokenIterator, new Filter<String>() {
            @Override
            public boolean accept(String item) {
                return !StringHelper.containsAny(item, Arrays.asList("&", "/", "=")) && !StringHelper.isNumber(item);
            }
        });
        return tokenIterator;
    }

}
