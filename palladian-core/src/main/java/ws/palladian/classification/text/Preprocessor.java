package ws.palladian.classification.text;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.FeatureSetting.TextFeatureType;
import ws.palladian.core.ImmutableToken;
import ws.palladian.core.Token;
import ws.palladian.extraction.feature.Stemmer;
import ws.palladian.extraction.feature.StopWordRemover;
import ws.palladian.extraction.token.CharacterNGramTokenizer;
import ws.palladian.extraction.token.NGramWrapperIterator;
import ws.palladian.extraction.token.WordTokenizer;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.collection.CollectionHelper;
import java.util.function.Function;
import java.util.function.Predicate;

public class Preprocessor implements Function<String, Iterator<String>> {

    /** Used as “marker” for a token to remove (e.g. stopword). */
    public static final Token REMOVED_TOKEN = new ImmutableToken(0, "[REMOVED]");

    private final FeatureSetting featureSetting;

    public Preprocessor(FeatureSetting featureSetting) {
        Validate.notNull(featureSetting, "featureSetting must not be null");
        this.featureSetting = featureSetting;
    }

    @Override
    public Iterator<String> apply(String input) {
        String content = input;
        if (!featureSetting.isCaseSensitive()) {
            content = content.toLowerCase();
        }
        int minNGramLength = featureSetting.getMinNGramLength();
        int maxNGramLength = featureSetting.getMaxNGramLength();
        Iterator<Token> tokenIterator;
        if (featureSetting.getTextFeatureType() == TextFeatureType.CHAR_NGRAMS) {
            tokenIterator = new CharacterNGramTokenizer(minNGramLength, maxNGramLength,
                    featureSetting.isCharacterPadding()).iterateTokens(content);
        } else if (featureSetting.getTextFeatureType() == TextFeatureType.WORD_NGRAMS) {
            tokenIterator = new WordTokenizer().iterateTokens(content);
            if (featureSetting.isStem()) {
                tokenIterator = applyStemming(tokenIterator);
            }
            if (featureSetting.isRemoveStopwords()) {
                tokenIterator = removeStopwords(tokenIterator);
            }
            tokenIterator = new NGramWrapperIterator(tokenIterator, minNGramLength, maxNGramLength);
            if (featureSetting.isCreateSkipGrams()) {
            	tokenIterator = new SkipGramWrapperIterator(tokenIterator);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported feature type: " + featureSetting.getTextFeatureType());
        }
        if (featureSetting.getTextFeatureType() == TextFeatureType.WORD_NGRAMS) {
            int minTermLength = featureSetting.getMinimumTermLength();
            int maxTermLength = featureSetting.getMaximumTermLength();
            tokenIterator = CollectionHelper.filter(tokenIterator,
                    (Predicate<Token>) item -> item.getValue().length() >= minTermLength
                            && item.getValue().length() <= maxTermLength);
        }
        tokenIterator = CollectionHelper.filter(tokenIterator, (Predicate<Token>) t -> t != REMOVED_TOKEN);
        // XXX looks a bit "magic" to me, does that really improve results in general?
        /* tokenIterator = CollectionHelper.filter(tokenIterator, new Filter<Token>() {
            @Override
            public boolean accept(Token item) {
                String value = item.getValue();
                return !StringHelper.containsAny(value, Arrays.asList("&", "/", "=")) && !StringHelper.isNumber(value);
            }
        }); */
        return CollectionHelper.convert(tokenIterator, Token.VALUE_CONVERTER);
    }

    private Iterator<Token> applyStemming(Iterator<Token> tokenIterator) {
        Stemmer stemmer = new Stemmer(featureSetting.getLanguage());
        return new AbstractIterator2<Token>() {
            @Override
            protected Token getNext() {
                if (!tokenIterator.hasNext()) {
                    return finished();
                }
                Token token = tokenIterator.next();
                String stemmedValue = stemmer.stem(token.getValue());
                return new ImmutableToken(token.getStartPosition(), stemmedValue);
            }
        };
    }

    private Iterator<Token> removeStopwords(Iterator<Token> tokenIterator) {
        StopWordRemover stopwordRemover = new StopWordRemover();
        return new AbstractIterator2<Token>() {
            @Override
            protected Token getNext() {
                if (!tokenIterator.hasNext()) {
                    return finished();
                }
                Token token = tokenIterator.next();
                boolean stopWord = stopwordRemover.isStopWord(token.getValue());
                return stopWord ? REMOVED_TOKEN : token;
            }
        };
    }

}
