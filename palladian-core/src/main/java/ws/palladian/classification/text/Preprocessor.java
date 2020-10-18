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
            tokenIterator = filterByTermLengths(tokenIterator);
            tokenIterator = new NGramWrapperIterator(tokenIterator, minNGramLength, maxNGramLength);
            if (featureSetting.isCreateSkipGrams()) {
            	tokenIterator = new SkipGramWrapperIterator(tokenIterator);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported feature type: " + featureSetting.getTextFeatureType());
        }
        tokenIterator = CollectionHelper.filter(tokenIterator, (Predicate<Token>) t -> t != REMOVED_TOKEN);
        return CollectionHelper.convert(tokenIterator, Token.VALUE_CONVERTER);
    }

    private Iterator<Token> applyStemming(Iterator<Token> tokenIterator) {
        Stemmer stemmer = new Stemmer(featureSetting.getLanguage());
        return CollectionHelper.convert(tokenIterator, (Function<Token, Token>) t -> {
            String stemmedValue = stemmer.stem(t.getValue());
            return new ImmutableToken(t.getStartPosition(), stemmedValue);
        });
    }

    private Iterator<Token> removeStopwords(Iterator<Token> tokenIterator) {
        StopWordRemover stopwordRemover = new StopWordRemover();
        return CollectionHelper.convert(tokenIterator, (Function<Token, Token>) t -> {
            boolean stopWord = stopwordRemover.isStopWord(t.getValue());
            return stopWord ? REMOVED_TOKEN : t;
        });
    }

    private Iterator<Token> filterByTermLengths(Iterator<Token> tokenIterator) {
        int minTermLength = featureSetting.getMinimumTermLength();
        int maxTermLength = featureSetting.getMaximumTermLength();
        return CollectionHelper.convert(tokenIterator, (Function<Token, Token>) token -> {
            boolean keep = token.getValue().length() >= minTermLength && token.getValue().length() <= maxTermLength;
            return keep ? token : REMOVED_TOKEN;
        });
    }

}
